package com.csl.intercom.broker;

public interface ISocketMsgListener {
	
	public void messageArrived(String websocketName,String msg);

	
}
