package com.inqwise.errors;

import com.google.common.base.Enums;
import com.inqwise.errors.spi.ErrorCodeProvider;

/** Default {@link ErrorCodeProvider} that resolves {@link ErrorCodes}. */
public class ErrorCodesProvider implements ErrorCodeProvider {

	/** Creates the provider. */
	public ErrorCodesProvider() {
	}

	@Override
	public String group() {
		return ErrorCodes.GROUP;
	}

	@Override
	public ErrorCode valueOf(String errorCodeName) {
		return Enums.getIfPresent(ErrorCodes.class, errorCodeName).orNull();
	}
}
