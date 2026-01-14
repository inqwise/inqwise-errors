package com.inqwise.errors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NotFoundExceptionTest {

	@Test
	void constructorWithMessageSetsDetails() {
		var exception = new NotFoundException("missing record");

		assertAll(
			() -> assertEquals("missing record", exception.getMessage()),
			() -> assertEquals(ErrorCodes.NotFound, exception.getErrorTicketBuilder().build().getError()),
			() -> assertEquals("missing record", exception.getErrorTicketBuilder().build().getErrorDetails())
		);
	}
}
