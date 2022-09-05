package com.xcsl.interfaces;

public class ErrorResult implements IResult {

	private int errCode=-1;
	private String msg="Error";

	public ErrorResult(String msg, int errCode) {
		this.msg=msg;this.errCode=errCode;
	}
	
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

	@Override
	public int getErrorCode() {
		// TODO Auto-generated method stub
		return errCode;
	}

}
