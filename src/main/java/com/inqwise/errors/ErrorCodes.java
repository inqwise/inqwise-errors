package com.inqwise.errors;

/**
 * Default set of builtin error codes with associated HTTP status suggestions.
 */
public enum ErrorCodes implements ErrorCode {
	
	/** Generic unexpected error. */
	GeneralError(500),
	/** Entity already exists. */
	AlreadyExist(409),
	/** Missing argument. */
	ArgumentNull(400),
	/** Argument outside expected range. */
	ArgumentOutOfRange(400),
	/** Argument fails validation. */
	ArgumentWrong(400),
	/** Resource cannot be found. */
	NotFound(404),
	/** Operation not implemented. */
	NotImplemented(501),
	/** Authentication required. */
	NotLoggedIn(401),
	/** Action not permitted. */
	NotPermitted(403),
	/** Argument should have been null but wasn't. */
	ArgumentShouldBeNull(400);
	
	/** Default provider group for builtin codes. */
	public static final String GROUP = "default";
	
	private int statusCode;
	
	private ErrorCodes(int statusCode) {
		this.statusCode = statusCode;
		
	}
	
	@Override
	public String group() {
		return GROUP;
	}

	@Override
	public int statusCode() {
		return statusCode;
	}
}
