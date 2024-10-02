package com.inqwise.errors;

import org.apache.logging.log4j.message.ParameterizedMessage;

public class NotFoundException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = -6879971169398162987L;

	public NotFoundException(final String message) {
	   super(message);
    }

	public NotFoundException(final String message, final Object... args) {
	   this(null, message, args);
    }

    public NotFoundException(final Throwable cause, final String message, final Object... args) {
	   super("Item Not Found:" + ParameterizedMessage.format(message, args), cause);
    }

    public ErrorTicket toErrorTicket() {
    	return ErrorTickets.notFound(this);
    }
}
