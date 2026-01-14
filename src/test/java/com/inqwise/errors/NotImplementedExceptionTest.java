package com.inqwise.errors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class NotImplementedExceptionTest {

	@Test
	void defaultConstructorHasNoMessageOrCode() {
		var exception = new NotImplementedException();

		assertAll(
			() -> assertNull(exception.getMessage()),
			() -> assertNull(exception.getCode())
		);
	}

	@Test
	void constructorWithMessageAndCodePreservesValues() {
		var exception = new NotImplementedException("Feature missing", "NI-42");

		assertAll(
			() -> assertEquals("Feature missing", exception.getMessage()),
			() -> assertEquals("NI-42", exception.getCode())
		);
	}

	@Test
	void constructorWithCauseKeepsCause() {
		var cause = new IllegalStateException("cause");
		var exception = new NotImplementedException("Missing", cause, "NI-1");

		assertAll(
			() -> assertSame(cause, exception.getCause()),
			() -> assertEquals("Missing", exception.getMessage()),
			() -> assertEquals("NI-1", exception.getCode())
		);
	}

	@Test
	void errorTicketBuilderUsesNotImplementedCode() {
		var exception = new NotImplementedException("Missing endpoint", "NI-9");

		var ticket = exception.getErrorTicketBuilder().build();

		assertAll(
			() -> assertEquals(ErrorCodes.NotImplemented, ticket.getError()),
			() -> assertEquals("Missing endpoint", ticket.getErrorDetails())
		);
	}
}
