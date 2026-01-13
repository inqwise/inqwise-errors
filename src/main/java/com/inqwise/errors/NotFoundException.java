package com.inqwise.errors;

import org.apache.logging.log4j.message.ParameterizedMessage;

import com.inqwise.errors.ErrorTicket.Builder;

/** Runtime exception indicating a requested resource was not found. */
/** Runtime exception indicating a requested resource was not found. */
public class NotFoundException extends RuntimeException implements ProvidesErrorTicket {

	/**
	 *
	 */
	private static final long serialVersionUID = -6879971169398162987L;

	/**
	 * Constructs an exception with the specified detail message.
	 *
	 * @param message detail message
	 */
	public NotFoundException(final String message) {
		super(message);
	}

	/**
	 * Constructs a formatted not-found exception.
	 *
	 * @param message format string
	 * @param args arguments for the format
	 */
	public NotFoundException(final String message, final Object... args) {
		this(null, message, args);
	}

	/**
	 * Constructs a not-found exception with a cause and formatted message.
	 *
	 * @param cause underlying cause
	 * @param message format string
	 * @param args arguments for the format
	 */
	public NotFoundException(final Throwable cause, final String message, final Object... args) {
		super("Item Not Found:" + ParameterizedMessage.format(message, args), cause);
	}
		
	@Override
	public Builder getErrorTicketBuilder() {
		return ErrorTicket.builder()
				.withError(ErrorCodes.NotFound)
				.withErrorDetails(getMessage());
	}
}
