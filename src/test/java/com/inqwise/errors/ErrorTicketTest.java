package com.inqwise.errors;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;

@ExtendWith(VertxExtension.class)
public class ErrorTicketTest {
	private static final Logger logger = LogManager.getLogger(ErrorTicketTest.class);

	@BeforeEach
	protected void setUp(Vertx vertx) throws Exception {
	}

	@Test
	void testSerializationWithErrorFromDefaultGroup() throws Exception {
		ErrorTicket errorTicket = ErrorTicket.builder()
		.withError(ErrorCodes.GeneralError)
		.withErrorId(UUID.randomUUID()).build();

		JsonObject json = JsonObject.mapFrom(errorTicket);
		logger.debug("json:{}", json);
		ErrorTicket errorTicketOut = json.mapTo(ErrorTicket.class);

		Assertions.assertEquals(errorTicket.toJson().toString(), errorTicketOut.toJson().toString());
	}
	
	@Test
	void testSerializationWithErrorFromNonGroup() throws Exception {
		ErrorTicket errorTicket = ErrorTicket.builder()
		.withError(CustomErrorCodes.Test)
		.build();

		JsonObject json = JsonObject.mapFrom(errorTicket);
		logger.debug("json:{}", json);
		ErrorTicket errorTicketOut = json.mapTo(ErrorTicket.class);

		Assertions.assertEquals(errorTicket.toJson().toString(), errorTicketOut.toJson().toString());
	}
	
	@Test
	void testSerializationWithErrorAndGroup() throws Exception {
		ErrorTicket errorTicket = ErrorTicket.builder()
		.withError(ErrorCodes.GeneralError)
		.withErrorGroup(ErrorCodes.GROUP)
		.withErrorId(UUID.randomUUID()).build();

		JsonObject json = JsonObject.mapFrom(errorTicket);
		logger.debug("json:{}", json);
		ErrorTicket errorTicketOut = json.mapTo(ErrorTicket.class);

		Assertions.assertEquals(errorTicket.toJson().toString(), errorTicketOut.toJson().toString());
	}
	
	@Test
	void testSerializationWithIncorrectError() throws Exception {
		ErrorTicket errorTicket = ErrorTicket.builder()
		.withError(CustomErrorCodes.Test)
		.withErrorGroup(ErrorCodes.GROUP)
		.withErrorId(UUID.randomUUID()).build();

		JsonObject json = JsonObject.mapFrom(errorTicket);
		logger.debug("json:{}", json);
		ErrorTicket errorTicketOut = json.mapTo(ErrorTicket.class);

		Assertions.assertEquals(errorTicket.toJson().toString(), errorTicketOut.toJson().toString());
	}
	
	@Test
	void testJsonConstructor() throws Exception {
		logger.debug("test1");
		var expectedErrorJson = ErrorTicket.builder().withError(ErrorCodes.NotImplemented).build().toJson();
		var actualError = new ErrorTicket(expectedErrorJson);
		Assertions.assertEquals(expectedErrorJson, actualError.toJson());
		
		logger.debug("test2");
		expectedErrorJson = ErrorTicket.builder().withError(ErrorCodes.NotImplemented).withErrorGroup(ErrorCodes.GROUP).build().toJson();
		actualError = new ErrorTicket(expectedErrorJson);
		Assertions.assertEquals(expectedErrorJson, actualError.toJson());
		
		logger.debug("test3");
		expectedErrorJson = ErrorTicket.builder().withError(CustomErrorCodes.Test).withErrorGroup(ErrorCodes.GROUP).build().toJson();
		actualError = new ErrorTicket(expectedErrorJson);
		Assertions.assertEquals(expectedErrorJson, actualError.toJson());
	}
	
	@Test
	void testParseJson() throws Exception {
		logger.debug("test1");
		var expectedErrorJson = ErrorTicket.builder().withError(ErrorCodes.NotImplemented).build().toJson();
		var actualError = ErrorTicket.parse(expectedErrorJson);
		Assertions.assertEquals(expectedErrorJson, actualError.toJson());
		
		logger.debug("test2");
		expectedErrorJson = ErrorTicket.builder().withError(ErrorCodes.NotImplemented).withErrorGroup(ErrorCodes.GROUP).build().toJson();
		actualError = ErrorTicket.parse(expectedErrorJson);
		Assertions.assertEquals(expectedErrorJson, actualError.toJson());
		
		logger.debug("test3");
		var bug = Assertions.assertThrows(Bug.class, () -> {
			ErrorTicket.parse(ErrorTicket.builder().withError(CustomErrorCodes.Test).build().toJson());
		});
		logger.debug("error message: {}", bug.getMessage());
		Assertions.assertTrue(bug.getMessage().startsWith("BUG: group is mandatory when code provided."));
		
		
		logger.debug("test4");
		var nullPointerEx = Assertions.assertThrows(NullPointerException.class, () -> {
			ErrorTicket.parse(ErrorTicket.builder().withError(CustomErrorCodes.Test).withErrorGroup("bad group").build().toJson());
		});
		logger.debug("error message: {}", nullPointerEx.getMessage());
		Assertions.assertTrue(nullPointerEx.getMessage().startsWith("provider not found for group"));
		
		logger.debug("test5");
		bug = Assertions.assertThrows(Bug.class, () -> {
			ErrorTicket.parse(ErrorTicket.builder().withError(CustomErrorCodes.Test).withErrorGroup(ErrorCodes.GROUP).build().toJson());
		});
		logger.debug("error message: {}", bug.getMessage());
		Assertions.assertTrue(bug.getMessage().startsWith("BUG: error not found in group."));
		
		
		logger.debug("test6 - default group");
		expectedErrorJson = ErrorTicket.builder().withError(ErrorCodes.NotImplemented).build().toJson();
		actualError = ErrorTicket.parse(expectedErrorJson, ErrorCodes.GROUP);
		Assertions.assertEquals(actualError.getErrorGroup(), ErrorCodes.GROUP);
		Assertions.assertEquals(actualError.getError(), ErrorCodes.NotImplemented);
		
	}
}
