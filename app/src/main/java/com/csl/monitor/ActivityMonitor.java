package com.csl.monitor;

import com.csl.intercom.status.IStatusProvider;
import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ActivityMonitor implements IStatusProvider {

	int nb_packets_total=0;
	long data_size_total=0;

	@Setter
    @Getter
    boolean showTicks=true;

	Map<String, Json> taps= new HashMap<String, Json>();

	ActivityHistory history = new ActivityHistory(60);
	Map<String, LocalDateTime> tapsLastActivity = new HashMap<>();
	private static final long inactivityDurationThreshold = 5;
	private static final long inactivityDurationDeletionThreshold = 300;

	public void addTick(Json j) {

		String id=JsonUtil.getStringFromJson(j, "tap_id","???");
		if (id.isEmpty()) return ;

		Json desc=taps.get(id);

		int nb_packets0=0;
		long data_size0=0;
		if (desc!=null) {
			nb_packets0=JsonUtil.getIntFromJson(desc, "nb_packets",0);
			data_size0=JsonUtil.getLongFromJson(desc, "data_size", 0);
		} else
			desc=Json.object();

		int nb_packets=JsonUtil.getIntFromJson(j, "nb_packets",0);
		long data_size=JsonUtil.getLongFromJson(j, "data_size", 0);

		desc.set("tap_id", id);
		desc.set( "nb_packets",(nb_packets+nb_packets0));
		desc.set("data_size", (data_size+data_size0) );

		nb_packets_total=nb_packets_total+nb_packets;
		data_size_total=data_size_total+data_size;
		taps.put(id, desc);
		tapsLastActivity.put(id, LocalDateTime.now());
	}

	public Json getStatus() {
		LocalDateTime currentTime = LocalDateTime.now();
		Json activeTaps = Json.array();
		for (Map.Entry<String, LocalDateTime> tapLastActivity: tapsLastActivity.entrySet()) {
			activeTaps.add(Json.object(
				"id", tapLastActivity.getKey(),
				"is_running", Math.abs(Duration.between(tapLastActivity.getValue(), currentTime).getSeconds()) <= inactivityDurationThreshold
			));
			if (Math.abs(Duration.between(tapLastActivity.getValue(), currentTime).getSeconds()) > inactivityDurationDeletionThreshold) {
				tapsLastActivity.remove(tapLastActivity.getKey());
			}
		}
		return Json.object("active_taps", activeTaps);
	}

	public void setMaxHistorySize(int size) {
		/**
		 * Set the maximum number of elements in the history
		 *
		 * @param size the new maximum length of the history
		 */
		history.setHistorySize(size);
	}

	public void sendTickFromIDS(Json jj) {
		Json j=Json.object();
		j.set("line", jj.toString());
		j.set("type", "tick_ids");
		CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j );

	}

	public void processEvent(Json jj) {
		if (showTicks) System.out.println("Process tick:"+jj);
		addTick(jj);
	}

	synchronized Json tic2Json() {

		Json tick = Json.object();
		tick.set("timestamp", System.currentTimeMillis());
		tick.set("type", "TIC");

		tick.set("nb_packets",nb_packets_total);
		tick.set("data_size",data_size_total);

		nb_packets_total=0;
		data_size_total=0;


		Json jtaps=Json.array();
		for (Map.Entry<String, Json> entry : taps.entrySet()) {
			Json value = entry.getValue();
			jtaps.add(value);
		}
		tick.set("taps",jtaps);


		taps.clear();

		history.addTick(tick);
		return tick;
	}

	public  void startTicTask() {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		Runnable sendTic = new Runnable() {

			@Override
			public void run() {
				Json tick = tic2Json();

				sendTickFromIDS(tick);

			}
		};

		scheduler.scheduleAtFixedRate(sendTic, 1, 1, TimeUnit.SECONDS);
	}

	public Json getHistoryJson() {
		return history.toJson();
	}

}
