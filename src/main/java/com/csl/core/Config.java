package com.csl.core;

import com.ucsl.json.Json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.ucsl.json.JsonUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
    static String configFileName = "application.json";
    Logger logger = LoggerFactory.getLogger(Config.class);
    Json jConfig;
    String separator = "__";
    public static Config instance = new Config(configFileName);

    public final Client Client;
    public final Config.Scan Scan;
    public final Config.Status Status;
    public final Config.NmapService NmapService;
    public final Config.CpeService CpeService;
    public final Tap TapService;
    public final Server Server;
    public final Config.UdpServerConf UdpServerConf;
    public final Config.ModuleExec ModuleExec;
    public final List<Module> Modules;
    public final Config.IdsConf IdsConf;
    public final Config.AlertViewer AlertViewer;
    public final Config.Autocrypt Autocrypt;

    public static Config reload(String configFile) {
        instance = new Config(configFile);
        return instance;
    }

    private Config(String configFile) {
        jConfig = readConfig(configFile);

        Server = new Server(jConfig.get("server"));
        Client = new Client(jConfig.get("client"));
        Status = new Status(jConfig.get("status"));
        UdpServerConf = new UdpServerConf(jConfig.get("udp_server_conf"));
        ModuleExec = new ModuleExec(jConfig.get("module_exec"));
        Modules = Module.makeList(jConfig.get("modules"));
        IdsConf = new IdsConf(jConfig.get("ids_conf"));
        AlertViewer = new AlertViewer(jConfig.get("alert_viewer"));
        Scan = new Scan(jConfig.get("discovery_service"));
        NmapService = new NmapService(jConfig.get("nmap_service"));
        CpeService = new CpeService(jConfig.get("cpe_service"));
        TapService = new Tap(jConfig.get("tap_service"));
        Autocrypt = new Autocrypt(jConfig.get("autocrypt_service"));
    }

    private Json readConfig(String f) {

        String content = "{}";
        try {
            content = readFile(f);
        } catch (IOException e) {
            logger.error("Cannot read config file :{}", f, e);
        }

        Json jConfig = Json.read(content);

        // Check the environment for existing variables
        Map<String, String> env = System.getenv();

        for (String section : jConfig.asJsonMap().keySet()) {
            if (jConfig.at(section).isObject()) {
                for (String var : jConfig.at(section).asJsonMap().keySet()) {
                    String env_var = "CSL" + separator + section.toUpperCase() + separator + var.toUpperCase();
                    if (env.containsKey(env_var)) {
                        jConfig.at(section).set(var, env.get(env_var));
                    }
                }
            }
        }
        return jConfig;
    }

    private static String readFile(String filename) throws IOException {
        try (InputStream inputStream = CSLContext.class.getClassLoader().getResourceAsStream(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + filename);
            }

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Setter
    @Getter
    public static class Client {
        // region define variables
        String ipServerRemote;
        Integer portServerRemote;
        String serverRemoteUrlPrefix;
        Boolean forceHostNameResolution;
        String apiKey;
        Boolean useSsl;
        Boolean launchWebApiServer;
        Integer webApiServerPort;
        String timezone;
        // endregion define variables

        public Client(Json config) {
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            ipServerRemote = config.get("ip_server_remote").asString();
            portServerRemote = config.get("port_server_remote").asInteger();
            serverRemoteUrlPrefix = config.get("server_remote_url_prefix").asString();
            forceHostNameResolution = config.get( "force_host_name_resolution").asBoolean();
            apiKey = config.get("api_key").asString();
            useSsl = config.get( "use_ssl").asBoolean();
            launchWebApiServer = config.get( "launch_web_api_server").asBoolean();
            webApiServerPort = config.get("web_api_server_port").asInteger();
            timezone = config.get("timezone").asString();
        }
    }

    @Setter
    @Getter
    public static class Scan {
        // region define variables
        String managerIp;
        Integer managerPort;
        String managerProtocol;
        String managerTimezone;
        Boolean useSSL;
        // endregion define variables

        public Scan(Json config) {
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            managerIp = config.get("manager_ip").asString();
            managerPort = config.get("manager_port").asInteger();
            managerProtocol = config.get("manager_protocol").asString();
            managerTimezone = config.get("manager_timezone").asString();
            useSSL = config.get( "use_ssl").asBoolean();
        }
    }

    @Setter
    @Getter
    public static class Status {
        // region define variables
        Boolean sendNotifications;
        Integer notificationsPeriod;
        // endregion define variables

        public Status(Json config) {
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            sendNotifications = config.get( "send_notifications").asBoolean();
            notificationsPeriod = config.get("notifications_period").asInteger();
        }
    }

    @Setter
    @Getter
    public static class NmapService {
        // region define variables
        Boolean debugMode;
        String debugDir;
        Boolean logMode;
        String logDir;
        // endregion define variables

        public NmapService(Json config) {
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            debugMode = config.get( "debug_mode").asBoolean();
            debugDir = config.get("debug_dir").asString();
            logMode = config.get( "log_mode").asBoolean();
            logDir = config.get("log_dir").asString();
        }
    }

    @Setter
    @Getter
    public static class CpeService {
        // region define variables
        String dictionaryPath;
        // endregion define variables

        public CpeService(Json config) {
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            dictionaryPath = config.get("dictionaryPath").asString();
        }
    }

    @Setter
    @Getter
    public static class Tap {
        // region define variables
        String localIpAddress;
        Integer localPort;
        String knowHostFilePath;
        // endregion define variables

        public Tap(Json config) {
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            localIpAddress = config.get("localIpAddr").asString();
            localPort = config.get("localPort").asInteger();
            knowHostFilePath = config.get("knowHostFilePath").asString();
        }
    }

    @Setter
    @Getter
    public static class Server {
        // region define variables
        @Setter
        Boolean on;
        Boolean verbose;
        Boolean debug;
        Integer webserverPort;
        String webRootdir;
        String webRootdir2;
        String webRootdir3;
        String datafileSubdir;
        Boolean coreCommands;
        Boolean varsCommands;
        Boolean modbusCommands;
        Boolean externalCommands;
        Boolean jsonCommands;
        Boolean jcmdCommands;
        Boolean databaseCommands;
        Boolean configFileCommands;
        Boolean mxCommands;
        Boolean sendAlerts;
        Boolean sendConsoleOutput;
        Boolean logJsonCommandsToFile;
        String logDir;
        String logPrefixFilename;
        Integer maxSizeOfLogFiles;
        Integer websocketTimeout;
        // endregion define variables

        public Server(Json config) {
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            on = config.get( "on").asBoolean();
            verbose = config.get( "verbose").asBoolean();
            debug = config.get( "debug").asBoolean();
            webserverPort = config.get("webserver_port").asInteger();
            webRootdir = config.get("web_rootdir").asString();
            webRootdir2 = config.get("web_rootdir2").asString();
            webRootdir3 = config.get("web_rootdir3").asString();
            datafileSubdir = config.get("datafile_subdir").asString();
            coreCommands = config.get( "core_commands").asBoolean();
            varsCommands = config.get( "vars_commands").asBoolean();
            modbusCommands = config.get( "modbus_commands").asBoolean();
            externalCommands = config.get( "external_commands").asBoolean();
            jsonCommands = config.get( "json_commands").asBoolean();
            jcmdCommands = config.get( "jcmd_commands").asBoolean();
            databaseCommands = config.get( "database_commands").asBoolean();
            configFileCommands = config.get( "config_file_commands").asBoolean();
            mxCommands = config.get( "mx_commands").asBoolean();
            sendAlerts = config.get( "send_alerts").asBoolean();
            sendConsoleOutput = config.get( "send_console_output").asBoolean();
            logJsonCommandsToFile = config.get( "logJsonCommandsToFile").asBoolean();
            logDir = config.get("log_dir").asString();
            logPrefixFilename = config.get("log_prefix_filename").asString();
            maxSizeOfLogFiles = config.get("max_size_of_log_files").asInteger();
            websocketTimeout = config.get("websocket_timeout").asInteger();
        }
    }

    @Setter
    @Getter
    public static class UdpServerConf {
        // region define variables
        @Setter
        Boolean on;
        Boolean verbose;
        Boolean traceAllMessages;
        String ip;
        Integer port;
        Integer maxInputQueues;
        Integer maxSizeOfInputQueues;
        // endregion define variables

        public UdpServerConf(Json config) {
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            on = config.get( "on").asBoolean();
            verbose = config.get( "verbose").asBoolean();
            traceAllMessages = config.get( "trace_all_messages").asBoolean();
            ip = config.get("ip").asString();
            port = config.get("port").asInteger();
            maxInputQueues = config.get("max_input_queues").asInteger();
            maxSizeOfInputQueues = config.get("max_size_of_input_queues").asInteger();
        }
    }

    @Setter
    @Getter
    public static class ModuleExec {
        // region define variables
        Integer samplingTime;
        String modulesPackageName;
        Integer numberOfExecLoops;
        Boolean autostart;
        // endregion define variables

        public ModuleExec(Json config) {
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            modulesPackageName = config.get("modules_package_name").asString();
            samplingTime = config.get("sampling_time").asInteger();
            numberOfExecLoops = config.get("number_of_exec_loops").asInteger();
            autostart = config.get( "autostart").asBoolean();
        }
    }

    @Setter
    @Getter
    public static class Module {
        // region define variables
        String name;
        String type;
        CSLModuleConfig config;
        // endregion define variables

        public Module(Json moduleConfig) {
            if (moduleConfig ==null) { throw new RuntimeException("Wrong configuration"); }
            name = moduleConfig.get("name").asString();
            type = moduleConfig.get("type").asString();
            config = new CSLModuleConfig(moduleConfig.get("config"));
        }

        public static List<Module> makeList(Json list) {
            if (list == null || list.isNull() || !list.isArray()) {
                throw new RuntimeException("wrong config");
            }

            List<Module> moduleList = new ArrayList<>();
            for (Json json : list.asJsonList()) {
                moduleList.add(new Module(json));
            }

            return moduleList;
        }

        @Setter
        @Getter
        public static class CSLModuleConfig {
            Boolean autostart;
            Integer execLoopNumber;
            Integer inputPriority;
            Integer stepPriority;
            Integer outputPriority;

            public CSLModuleConfig(Json config) {
                if (config ==null) { throw new RuntimeException("Wrong configuration"); }
                autostart = config.get( "autostart").asBoolean();
                execLoopNumber = config.get("exec_loop_number").asInteger();
                inputPriority = config.get("input_priority").asInteger();
                stepPriority = config.get("step_priority").asInteger();
                outputPriority = config.get("output_priority").asInteger();
            }
        }
    }

    @Setter
    @Getter
    public static class IdsConf {
        // region define variables
        Boolean on;
        Integer mode;
        String helpMode;
        Boolean relativeToCSLConfigDir;
        String idsconfDir;
        Boolean idsconfDirRelativeToDataDir;
        String packetsDirForDetectionOffline;
        String packetsDirForDetectionOfflineInfo;
        String packetsDirForRecording;
        String packetsDirForRecordingInfo;
        String packetsDirForLearning;
        String packetsDirForLearningInfo;
        Boolean validationAfterLearning;
        Boolean logToFile;
        String variablesPrefixFilename;
        String packetsPrefixFilename;
        String networkPrefixFilename;
        Integer maxSizeOfLogFiles;
        String subdirLearn;
        String helpSubdirLearn;
        String subdirOfflineDetect;
        String helpSubdirDetect;
        String rulesForDetection;
        String rulesForLearning;
        String rulesForSuricataBase;
        String rulesForSuricataLearned;
        String learnedModel;
        String newLearnedModel;
        String systemConfiguration;
        String currentIdsParamsFile;
        List<String> idstraceFlags;
        String general;
        String idstraceDir;
        Boolean idstraceOn;
        Boolean sendToConsole;
        Boolean sendToBrowser;
        Boolean showTicks;
        Boolean killPreviousInstance;
        String tapsDir;
        Integer historyLength;
        // endregion define variables

        public IdsConf(Json config) {
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            on = config.get( "on").asBoolean();
            mode = config.get("mode").asInteger();
            helpMode = config.get("help_mode").asString();
            relativeToCSLConfigDir = config.get( "relative_to_CSLConfig_dir").asBoolean();
            idsconfDir = config.get("idsconf_dir").asString();
            idsconfDirRelativeToDataDir = config.get( "idsconf_dir_relative_to_data_dir").asBoolean();
            packetsDirForDetectionOffline = config.get("packets_dir_for_detection_offline").asString();
            packetsDirForDetectionOfflineInfo = config.get("packets_dir_for_detection_offline_info").asString();
            packetsDirForRecording = config.get("packets_dir_for_recording").asString();
            packetsDirForRecordingInfo = config.get("packets_dir_for_recording_info").asString();
            packetsDirForLearning = config.get("packets_dir_for_learning").asString();
            packetsDirForLearningInfo = config.get("packets_dir_for_learning_info").asString();
            validationAfterLearning = config.get( "validation_after_learning").asBoolean();
            logToFile = config.get( "log_to_file").asBoolean();
            variablesPrefixFilename = config.get("variables_prefix_filename").asString();
            packetsPrefixFilename = config.get("packets_prefix_filename").asString();
            networkPrefixFilename = config.get("network_prefix_filename").asString();
            maxSizeOfLogFiles = config.get("max_size_of_log_files").asInteger();
            subdirLearn = config.get("subdir_learn").asString();
            helpSubdirLearn = config.get("help_subdir_learn").asString();
            subdirOfflineDetect = config.get("subdir_offline_detect").asString();
            helpSubdirDetect = config.get("help_subdir_detect").asString();
            rulesForDetection = config.get("rules_for_detection").asString();
            rulesForLearning = config.get("rules_for_learning").asString();
            rulesForSuricataBase = config.get("rules_for_suricata_base").asString();
            rulesForSuricataLearned = config.get("rules_for_suricata_learned").asString();
            learnedModel = config.get("learned_model").asString();
            newLearnedModel = config.get("new_learned_model").asString();
            systemConfiguration = config.get("system_configuration").asString();
            currentIdsParamsFile = config.get("current_idsparams_file").asString();
            idstraceFlags = config.get("idstrace_flags").asJsonList().stream().map(e->e.asString()).toList();
            idstraceDir = config.get("idstrace_dir").asString();
            idstraceOn = config.get( "idstrace_on").asBoolean();
            sendToConsole = config.get( "send_to_console").asBoolean();
            sendToBrowser = config.get( "send_to_browser").asBoolean();
            showTicks = config.get( "show_ticks").asBoolean();
            killPreviousInstance = config.get( "kill_previous_instance").asBoolean();
            tapsDir = config.get("taps_dir").asString();
            historyLength = config.get("history_length").asInteger();
        }
    }

    @Setter
    @Getter
    public static class AlertViewer {
        // region define variables
        String ip;
        Integer port;
        String name;
        Boolean logToFile;
        String logDir;
        String prefixFilename;
        Integer maxSizeOfLogFiles;
        Boolean alertToWeb;
        String alertJsonTag;
        Boolean alertToUdp;
        Boolean alertToDb;
        Boolean showAlerts;
        Boolean doNotResentSameAlert;
        Integer alertDuration;
        String filenameCurrentAlerts;
        String subdirBackupAlerts;
        // endregion define variables

        public AlertViewer(Json config) {
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            ip = config.get("ip").asString();
            port = config.get("port").asInteger();
            name = config.get("name").asString();
            logToFile = config.get( "log_to_file").asBoolean();
            logDir = config.get("log_dir").asString();
            prefixFilename = config.get("prefix_filename").asString();
            maxSizeOfLogFiles = config.get("max_size_of_log_files").asInteger();
            alertToWeb = config.get( "alert_to_web").asBoolean();
            alertJsonTag = config.get("alert_json_tag").asString();
            alertToUdp = config.get( "alert_to_udp").asBoolean();
            alertToDb = config.get( "alert_to_db").asBoolean();
            showAlerts = config.get( "show_alerts").asBoolean();
            doNotResentSameAlert = config.get( "do_not_resent_same_alert").asBoolean();
            alertDuration = config.get("alert_duration").asInteger();
            filenameCurrentAlerts = config.get("filename_current_alerts").asString();
            subdirBackupAlerts = config.get("subdir_backup_alerts").asString();
        }
    }

    @Setter
    @Getter
    public static class Autocrypt {
        // region define variables
        String ip;
        Integer port;
        Integer syncFrequency;
        // endregion define variables

        public Autocrypt(Json config) {
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            ip = config.get("ip").asString();
            port = config.get("port").asInteger();
            syncFrequency = config.get("sync_frequency").asInteger();
        }
    }
}
