package com.inqwise.errors;

public enum ErrorCodes implements ErrorCode {
	
	GeneralError,
	AlreadyExist,
	ArgumentNull,
	ArgumentOutOfRange,
	ArgumentWrong,
	NotFound,
	NotImplemented,
	NullPointer,
	NotLoggedIn,
	NotPermitted,
	ArgumentShouldBeNull;
	
	public static final String GROUP = "default";
	
	@Override
	public String group() {
		return GROUP;
	}
}