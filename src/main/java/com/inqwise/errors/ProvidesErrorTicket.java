package com.inqwise.errors;

/**
 * Contract for exceptions that can render themselves as {@link ErrorTicket}s.
 */
public interface ProvidesErrorTicket {
	/**
	 * Builds an immutable {@link ErrorTicket} using {@link #getErrorTicketBuilder()}.
	 *
	 * @return finalized ticket representation
	 */
	default ErrorTicket toErrorTicket() {
		return getErrorTicketBuilder().build();
	}
	
	/**
	 * Returns a builder seeded with this exception's data for further customization.
	 *
	 * @return builder representing this exception
	 */
	ErrorTicket.Builder getErrorTicketBuilder();
}
