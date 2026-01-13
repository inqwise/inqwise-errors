package com.inqwise.errors;

import com.inqwise.errors.ErrorTicket.Builder;

/** Indicates a requested operation is not yet implemented. */
public class NotImplementedException extends UnsupportedOperationException implements ProvidesErrorTicket {

    private static final long serialVersionUID = 4047403899948493127L;

	/** Optional application-specific code describing the missing feature. */
	private final String code;

	/** Creates an exception without message or code. */
	public NotImplementedException() {
		this.code = null;
	}

	/**
	 * Creates an exception with the specified message.
	 *
	 * @param message detail message
	 */
	public NotImplementedException(final String message) {
		this(message, (String) null);
	}

	/**
	 * Creates an exception with message and custom code.
	 *
	 * @param message detail message
	 * @param code domain-specific code
	 */
	public NotImplementedException(final String message, final String code) {
		super(message);
		this.code = code;
	}

	/**
	 * Creates an exception with message and cause.
	 *
	 * @param message detail message
	 * @param cause underlying cause
	 */
	public NotImplementedException(final String message, final Throwable cause) {
		this(message, cause, null);
	}

	/**
	 * Creates an exception with message, cause, and custom code.
	 *
	 * @param message detail message
	 * @param cause underlying cause
	 * @param code domain-specific code
	 */
	public NotImplementedException(final String message, final Throwable cause, final String code) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * Creates an exception with only a cause.
	 *
	 * @param cause underlying cause
	 */
	public NotImplementedException(final Throwable cause) {
		this(cause, null);
	}

	/**
	 * Creates an exception with cause and custom code.
	 *
	 * @param cause underlying cause
	 * @param code domain-specific code
	 */
	public NotImplementedException(final Throwable cause, final String code) {
		super(cause);
		this.code = code;
	}

	/**
	 * Returns the optional application-specific code describing the missing feature.
	 *
	 * @return custom code or {@code null}
	 */
	public String getCode() {
        return this.code;
    }
    
    @Override
	public Builder getErrorTicketBuilder() {
		return ErrorTicket.builder()
				.withError(ErrorCodes.NotImplemented)
				.withErrorDetails(getMessage());
	}
}
