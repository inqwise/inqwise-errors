package com.inqwise.errors;

public enum CustomErrorCodes implements ErrorCode {
	
	Test();
	
	public static final String GROUP = "custom";
	
	@Override
	public String group() {
		return GROUP;
	}
	
	private CustomErrorCodes(){
		
	}
}
