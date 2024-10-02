package com.inqwise.errors.spi;

import com.inqwise.errors.ErrorCode;

public interface ErrorCodeProvider {
	String group();
	ErrorCode valueOf(String errorCodeName);
}
