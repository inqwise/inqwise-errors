package com.inqwise.errors;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProvidesErrorTicketTest {

	private static final class SampleException extends RuntimeException implements ProvidesErrorTicket {
		@Override
		public ErrorTicket.Builder getErrorTicketBuilder() {
			return ErrorTicket.builder().withError(ErrorCodes.NotFound).withErrorDetails("missing");
		}
	}

	@Test
	void toErrorTicketDelegatesToBuilder() {
		var exception = new SampleException();
		var ticket = exception.toErrorTicket();
		assertAll(
			() -> assertEquals(ErrorCodes.NotFound, ticket.getError()),
			() -> assertEquals("missing", ticket.getErrorDetails())
		);
	}
}
