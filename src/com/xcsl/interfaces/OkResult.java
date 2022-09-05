package com.xcsl.interfaces;

public class OkResult implements IResult {

	String msg="ok";
	@Override
	public boolean isOK() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return msg;
	}
	
	public OkResult setMessage(String s) {
		msg=s;
		return this;
		
	}

	@Override
	public int getErrorCode() {
		// TODO Auto-generated method stub
		return 0;
	}

}
