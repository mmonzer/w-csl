package com.xcsl.interfaces;
import com.xcsl.json.Json;


public interface ICSLFlowListener {
	static int DO_NOTHING=0;
	static int CANCEL_OTHER_LISTENERS=1;
	static int REMOVE_FROM_QUEUE=2;
	static int CANCEL_OTHER_LISTENERS_AND_REMOVE_FROM_QUEUE=3;
	
	
	// return cancel=true if the processing by the next listeners must be cancel
	public int newElementOnQueue(Json jj);
	
	public String getName();
}
