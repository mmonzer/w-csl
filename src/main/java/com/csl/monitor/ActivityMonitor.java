package com.csl.monitor;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import com.csl.core.CSLContext;
import com.csl.ids.Tap;
import com.csl.intercom.status.IStatusProvider;
import com.csl.logger.LoggerCustomEndpoints;
import com.csl.logger.LoggerInterfaces;
import com.csl.util.ThreadUtils;
import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import static com.csl.util.FileUtils.readJsonFromFile;

public class ActivityMonitor implements IStatusProvider {

	static final String FIELD_NB_PACKETS="nb_packets";
	static final String FIELD_DATA_SIZE="data_size";

	int nb_packets_total=0;
	long data_size_total=0;


	boolean showTicks=true;

	Map<String, Json> taps= new HashMap<>();
	Map<String, Tap> activeTaps;

	ActivityHistory history = new ActivityHistory(60);
	Map<String, LocalDateTime> tapsLastActivity = new HashMap<>();
	private static final long INACTIVITY_DURATION_THRESHOLD = 5;
	private static final long INACTIVITY_DURATION_DELETION_THRESHOLD = 300;


	public void addTick(Json j) {

		String id=JsonUtil.getStringFromJson(j, "tap_id","???");
		if (id.isEmpty()) return ;

		Json desc=taps.get(id);

		int nb_packets0=0;
		long data_size0=0;
		if (desc!=null) {
			nb_packets0=JsonUtil.getIntFromJson(desc, FIELD_NB_PACKETS,0);
			data_size0=JsonUtil.getLongFromJson(desc, FIELD_DATA_SIZE, 0);
		} else
			desc=Json.object();

		int nb_packets=JsonUtil.getIntFromJson(j, FIELD_NB_PACKETS,0);
		long data_size=JsonUtil.getLongFromJson(j, FIELD_DATA_SIZE, 0);

		desc.set("tap_id", id);
		desc.set("suricata_running",JsonUtil.getBooleanFromJson(j, "suricata_running",false));
		desc.set("monitor_running",JsonUtil.getBooleanFromJson(j, "monitor_running",false));
		desc.set(FIELD_NB_PACKETS,(nb_packets+nb_packets0));
		desc.set(FIELD_DATA_SIZE, (data_size+data_size0) );

		nb_packets_total=nb_packets_total+nb_packets;
		data_size_total=data_size_total+data_size;
		taps.put(id, desc);
		tapsLastActivity.put(id, LocalDateTime.now());
	}

	/**
	 * Specification of a method that sends every x time a notification to the HMI, so it knows that the module is alive
	 * @return json object with the identifier and state of the tap.
	 */
	@Override
	public Json getStatus() {
		LocalDateTime currentTime = LocalDateTime.now();
		Json activeTaps = Json.array();
		boolean is_http_api_reachable = false;
		Tap tap;
		for (Map.Entry<String, LocalDateTime> tapLastActivity: tapsLastActivity.entrySet()) {

			Json conf;
			ArrayList<Json> configuredTaps;
			String idsconf = CSLContext.getInstance().getCslConfDir();
			try {
				conf = readJsonFromFile(idsconf + "/taps/TapsConfiguration.json");
				if (conf.isArray()) {
					configuredTaps = (ArrayList<Json>) conf.asJsonList();
				} else {
					configuredTaps = new ArrayList<>();
				}
				for (Json j : configuredTaps) {
					if (j.at("idname").asString().equals(tapLastActivity.getKey())) {
						is_http_api_reachable = isTapApiReachable(
								new Tap(j.at("idname").asString(),
									j.at("id").asString(),
									j.at("ip").asString(),
									j.at("port").asInteger(),
									j.at("includes").asJsonList()
								)
						);
						break;
					}
				}
			} catch (IOException e1) {
				System.err.println("No tap config found");
			}
			activeTaps.add(Json.object(
				"id", tapLastActivity.getKey(),
					"is_udp_connected", Math.abs(Duration.between(tapLastActivity.getValue(), currentTime).getSeconds()) <= INACTIVITY_DURATION_THRESHOLD,
					"is_http_api_reachable", is_http_api_reachable
			));
			if (Math.abs(Duration.between(tapLastActivity.getValue(), currentTime).getSeconds()) > INACTIVITY_DURATION_DELETION_THRESHOLD) {
				tapsLastActivity.remove(tapLastActivity.getKey());
			}
		}
		return Json.object("active_taps", activeTaps);
	}

	/**
	 * Check if TAP api is reachable
	 * @param tap tap
	 * @return whether it is reachable or not
	 */
	private static boolean isTapApiReachable(Tap tap) {
		boolean is_http_api_reachable;
		Tap finalTap = tap;
		try {
			is_http_api_reachable = CompletableFuture.supplyAsync(() -> finalTap.sendQuietCmd("/config", "{\"cmd\":\"getConfig\"}"))
					.get(100, TimeUnit.MILLISECONDS).isSuccess();
		} catch (TimeoutException|InterruptedException|ExecutionException|CancellationException e) {
			is_http_api_reachable = false;
		}
		return is_http_api_reachable;
	}

	public boolean isShowTicks() {
		return showTicks;
	}

	public void setShowTicks(boolean showTicks) {
		this.showTicks = showTicks;
	}

	public int getHistorySize() {
		/**
		 * Get the number of elements in the history
		 *
		 * @return the number of elements held in the history
		 */
		return history.currentHistorySize();
	}

	public int getMaxHistorySize() {
		/**
		 * Get the maximum number of elements in the history
		 *
		 * @return the maximum length of the history
		 */
		return history.maxHistorySize();
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

		Json tick = Json.object();
		// j.set("source", p.getSource().toString());
		tick.set("timestamp", System.currentTimeMillis());
		tick.set("type", "TIC");

		tick.set(FIELD_NB_PACKETS,nb_packets_total);
		tick.set(FIELD_DATA_SIZE,data_size_total);

		nb_packets_total=0;
		data_size_total=0;


		Json jtaps=Json.array();
		for (Map.Entry<String, Json> entry : taps.entrySet()) {
			//   String key = entry.getKey();
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

		Runnable sendTic = () -> {
            //System.out.println("TIC");
            Json tick = tic2Json();

            sendTickFromIDS(tick);

        };
		ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
				scheduler,
				sendTic ,
				1, 1, TimeUnit.SECONDS,
				LoggerCustomEndpoints.TICS_MONITOR, LoggerInterfaces.CSL_CLIENT
		);
	}

	public Json getHistoryJson() {
		return history.toJson();
	}

}
