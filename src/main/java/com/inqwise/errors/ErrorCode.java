package com.inqwise.errors;

/**
 * Identifies a categorizable error along with its logical group and optional HTTP status.
 */
public interface ErrorCode {
	/**
	 * Returns the provider group used to resolve this error.
	 *
	 * @return provider group key
	 */
	String group();

	/**
	 * Suggested HTTP status (or {@code 0} when unspecified).
	 *
	 * @return HTTP status code hint
	 */
	default int statusCode() { return 0; }
}
