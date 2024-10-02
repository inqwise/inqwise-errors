package com.inqwise.errors;

import com.google.common.base.Enums;
import com.inqwise.errors.spi.ErrorCodeProvider;

public class ErrorCodesProvider implements ErrorCodeProvider {

	@Override
	public String group() {
		return ErrorCodes.GROUP;
	}

	@Override
	public ErrorCode valueOf(String errorCodeName) {
		return Enums.getIfPresent(ErrorCodes.class, errorCodeName).orNull();
	}
}
