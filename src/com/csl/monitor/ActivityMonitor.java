package com.csl.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.csl.web.websockets.CSLWebSocket;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

public class ActivityMonitor {

	static String FIELD_NB_PACKETS="nb_packets";
	static String FIELD_DATA_SIZE="data_size";
	
	int nb_packets_total=0;
	long data_size_total=0;
	
	
	boolean showTicks=true;
	
	Map<String, Json> taps= new HashMap<String, Json>();
	
	
//	
	
	
	public void addTick(Json j) {
	
		String id=JsonUtil.getStringFromJson(j, "tap_id","???");
		if (id.isEmpty()) return ;
		
		Json desc=taps.get(id);
		
		int nb_packets0=0;
		long data_size0=0;
		if (desc!=null) {
			nb_packets0=JsonUtil.getIntFromJson(desc, "nb_packets",0);
			data_size0=JsonUtil.getLongFromJson(desc, "data_size", 0);
		}
		else
			desc=Json.object();
		
		int nb_packets=JsonUtil.getIntFromJson(j, "nb_packets",0);
		long data_size=JsonUtil.getLongFromJson(j, "data_size", 0);
		
		desc.set("tap_id", id);
		desc.set( "nb_packets",(nb_packets+nb_packets0));
		desc.set("data_size", (data_size+data_size0) );
		
		nb_packets_total=nb_packets_total+nb_packets;
		data_size_total=data_size_total+data_size;
		taps.put(id, desc);
	}
	
	
	
	
	public boolean isShowTicks() {
		return showTicks;
	}




	public void setShowTicks(boolean showTicks) {
		this.showTicks = showTicks;
	}




	public void sendTickFromIDS(Json jj) {
		
		//System.out.println("send tick to hmi:"+jj);
			Json j=Json.object();
	        j.set("line", jj.toString());
	        j.set("type", "tick_ids");
//			CSLWebSocketForConsole.broadcastMessageJson("log", j);
			CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j );
	
	}
	
	public void processEvent(Json jj) {
		
		
		if (showTicks) System.out.println("Process tick:"+jj);
		addTick(jj);
	}
	
	
	
	
	// build tic json
	synchronized Json tic2Json() {

				Json j = Json.object();
				// j.set("source", p.getSource().toString());
				j.set("timestamp", System.currentTimeMillis());
				j.set("type", "TIC");
				
				j.set("nb_packets",nb_packets_total);
				j.set("data_size",data_size_total);
				
				nb_packets_total=0;
				data_size_total=0;
				
				
				Json jtaps=Json.array();
				for (Map.Entry<String, Json> entry : taps.entrySet()) {
				 //   String key = entry.getKey();
				    Json value = entry.getValue();
				    jtaps.add(value);
				}
				j.set("taps",jtaps);
				
				
				taps.clear();
				return j;
			}
			
			
	
	public  void startTicTask() {
		
		
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		
		Runnable sendTic = new Runnable() {
			
			@Override
			public void run() {
				//System.out.println("TIC");
				Json j = tic2Json();
				
				sendTickFromIDS(j);
				
			}
		};
		
		scheduler.scheduleAtFixedRate(sendTic, 1, 1, TimeUnit.SECONDS);
	}
	
	
}
