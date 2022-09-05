package com.xcsl.interfaces;

public interface IResult {

	static IResult OK = new OkResult();
	static IResult ERROR = new OkResult();
	
	public boolean isOK();
	public 	String getMessage();
	public 	int getErrorCode();
	
}
