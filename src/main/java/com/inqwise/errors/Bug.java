package com.inqwise.errors;

import org.apache.logging.log4j.message.ParameterizedMessage;

public class Bug
	   extends IllegalStateException {
    
	private static final long serialVersionUID = 6471911008970602096L;

	/**
	* Constructs a new {@code Bug} with the given parameters.
	*
	* @param message the formatted message, never missing
	* @param args any message format parameters
	*
	* @see String#format(String, Object...)
	*/
    public Bug(final String message, final Object... args) {
	   this(null, message, args);
    }

    /**
	* Constructs a new {@code Bug} with the given parameters.
	*
	* @param cause the root cause wrapped in this bug
	* @param message the formatted message, never missing
	* @param args any message format parameters
	*
	* @see String#format(String, Object...)
	*/
    public Bug(final Throwable cause, final String message, final Object... args) {
	   super("BUG: " + ParameterizedMessage.format(message, args), cause);
    }
}
