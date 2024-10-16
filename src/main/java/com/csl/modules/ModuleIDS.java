package com.csl.modules;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.core.ModuleContext;
import com.csl.defaultclasses.FileLog;
import com.csl.monitor.ActivityMonitor;
import com.csl.util.EveMessageUtill;
import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.interfaces.ICSLFlowListener;
import com.ucsl.interfaces.IResult;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import com.wcsl.ids.IDSMainProcessor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

public class ModuleIDS {
    @Getter
    ActivityMonitor activityMonitor = new ActivityMonitor();

    @Setter
    boolean sendToBrowser = true;
    @Setter
    boolean sendToConsole = true;

    public IResult start(CSLContext context, ModuleContext mcontext) {
        //context.getGlobalVariablesTable().createDoubleVariable("u",0);


        return IResult.OK;
    }

    public IResult stop(CSLContext context, ModuleContext mcontext) {
        return IResult.OK;
    }

    static {
        CSLContext.instance.registerModuleClass("ModuleIDS", ModuleIDS.class);
    }

    CSLContext context = null;

    boolean loggingOn = false;            // record the packets
    boolean idsDetectOn = false;            // do the detection

    FileLog packetsLog, variablesLog, networkLog;

    IDSMainProcessor idsMainProcessor = null;

    boolean running = false;

    private String variablesFilename = "VARIABLES";

    private String packetsFilename = "PACKETS";

    private String networkFilename = "NETWORK";

    private long max_size = 10000000;

    public String runningState() {

        return "Running:" + running + " detect:" + idsDetectOn + " record:" + loggingOn + " sendToConsole:" + sendToConsole + " sendToBrowser:" + sendToBrowser;
    }

    public void setModeIdle() {
        idsDetectOn = false;
        loggingOn = false;
    }

    public void setModeRecord() {
        idsDetectOn = false;
        loggingOn = true;
    }

    public void setModeDetect() {
        idsDetectOn = true;
        loggingOn = true;
    }

    public void outDisplay(Json jj) {
        if (sendToBrowser) {
            Json j = Json.object();
            j.set("line", jj.toString());
            j.set("type", "packet");
            CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE, j);
        }
    }

    public void openLogFiles() {

//		Json j=context.getConfig();
        Config.IdsConf config = context.getConfig().IdsConf;

//		String datadir=JsonUtil.getStringFromJson(j, "ids_conf/packets_dir_for_recording", "./recorded_packets");
        String datadir = config.getPacketsDirForRecording();

//		this.variablesFilename=JsonUtil.getStringFromJson(j,"ids_conf/variables_prefix_filename", "vars");
        this.variablesFilename = config.getVariablesPrefixFilename();
//		this.packetsFilename=JsonUtil.getStringFromJson(j, "ids_conf/packets_prefix_filename", "pkt");
        this.packetsFilename = config.getPacketsPrefixFilename();
//		this.networkFilename=JsonUtil.getStringFromJson(j, "ids_conf/network_prefix_filename", "net");
        this.networkFilename = config.getNetworkPrefixFilename();


//		this.max_size=JsonUtil.getLongFromJson(j, "ids_conf/max_size_of_log_files", 10000000);
        this.max_size = config.getMaxSizeOfLogFiles();
        packetsLog = new FileLog(datadir, packetsFilename, max_size, CSLContext.instance::getSystemCurrentTimeMillis);
        variablesLog = new FileLog(datadir, variablesFilename, max_size, CSLContext.instance::getSystemCurrentTimeMillis);
        networkLog = new FileLog(datadir, networkFilename, max_size, CSLContext.instance::getSystemCurrentTimeMillis);
    }

    public void reOpenLogFiles(String datadir) {

        packetsLog = new FileLog(datadir, packetsFilename, max_size, CSLContext.instance::getSystemCurrentTimeMillis);
        variablesLog = new FileLog(datadir, variablesFilename, max_size, CSLContext.instance::getSystemCurrentTimeMillis);
        networkLog = new FileLog(datadir, networkFilename, max_size, CSLContext.instance::getSystemCurrentTimeMillis);
    }

    public IResult init(CSLContext context, ModuleContext mcontext) {
        this.context = context;
//		Json config=context.getConfig();
        Config.IdsConf config = Config.instance.IdsConf;


//		boolean showTicks=JsonUtil.getBooleanFromJson(config, "ids_conf/show_ticks", true);
        boolean showTicks = config.getShowTicks();

        activityMonitor.setShowTicks(showTicks);

//		int maxHistSize = JsonUtil.getIntFromJson(config, "ids_conf/history_length", 60);
        int maxHistSize = config.getHistoryLength();
        activityMonitor.setMaxHistorySize(maxHistSize);

        activityMonitor.startTicTask();

        CSLContext.instance.getStatusNotifier().registerStatusProvider("taps", activityMonitor);
        {

            openLogFiles();
        }
        idsMainProcessor = CSLContext.instance.getIDSMainProcessor();


        this.sendToBrowser = CSLContext.instance.getIdsParams().isSendToBrowser();
        this.sendToConsole = CSLContext.instance.getIdsParams().isSendToConsole();


        boolean on = config.getOn();
        running = on;

        int n_input = 1;
        ICSLFlowListener listener = new ICSLFlowListener() {
            public String getName() {
                return "IDS";
            }

            public int newElementOnQueue(Json jj) {
                if (!running) {
                    return ICSLFlowListener.REMOVE_FROM_QUEUE;
                }

                try {
                    if (jj.has("type")) {
                        String type = jj.get("type").asString();
                        if (type.compareTo("EVE") == 0) {
                            EveMessageUtill.reformatTimeStamp(jj);
                            if (loggingOn) packetsLog.RecordLogMessage(jj.toString());
                            idsMainProcessor.processSuricataEvent(jj);
                        } else if (type.compareTo("TIC") == 0) {
                            activityMonitor.processEvent(jj);
                        } else {
                            throw new UnsupportedOperationException("Not implemented yet");
                        }
                    } else {
                        outDisplay(jj);
                    }
                } catch (Exception e) {
                    System.out.println("Exception while processing " + jj);
                    System.out.println(e);
                }
                return ICSLFlowListener.REMOVE_FROM_QUEUE; // cancel next listeners
            }
        };

        CSLContext.instance.getCslUDPServer().addListener(n_input, listener);
        return IResult.OK;
    }


    static {
        CSLContext.instance.registerModuleClass("ModuleIDS", ModuleIDS.class);
    }
}

