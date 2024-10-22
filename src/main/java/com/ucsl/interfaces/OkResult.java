package com.ucsl.interfaces;

public class OkResult implements IResult {

	String msg="ok";
	@Override
	public boolean isOK() {
		return true;
	}

	@Override
	public String getMessage() {
		return msg;
	}
	
	public OkResult setMessage(String s) {
		msg=s;
		return this;
		
	}

	@Override
	public int getErrorCode() {
		return 0;
	}

}
