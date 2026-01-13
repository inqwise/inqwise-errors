package com.inqwise.errors.spi;

import com.inqwise.errors.ErrorCode;

/**
 * Service-provider interface for resolving {@link ErrorCode} values.
 */
public interface ErrorCodeProvider {
	/**
	 * Returns the logical group name served by this provider.
	 *
	 * @return provider group name
	 */
	String group();

	/**
	 * Resolves an {@link ErrorCode} by its string name.
	 *
	 * @param errorCodeName textual error identifier
	 * @return matching error or {@code null}
	 */
	ErrorCode valueOf(String errorCodeName);
}
