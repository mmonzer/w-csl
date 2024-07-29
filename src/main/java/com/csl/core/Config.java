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

import com.ucsl.json.JsonException;
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
            ipServerRemote = JsonUtil.getStringFromJson(config, "ip_server_remote", "localhost");
            portServerRemote = JsonUtil.getIntFromJson(config, "port_server_remote", 8000);
            serverRemoteUrlPrefix = JsonUtil.getStringFromJson(config, "server_remote_url_prefix", "");
            forceHostNameResolution = JsonUtil.getBooleanFromJson(config, "force_host_name_resolution", false);
            apiKey = JsonUtil.getStringFromJson(config, "api_key", "");
            useSsl = JsonUtil.getBooleanFromJson(config, "use_ssl", true);
            launchWebApiServer = JsonUtil.getBooleanFromJson(config, "launch_web_api_server", false);
            webApiServerPort = JsonUtil.getIntFromJson(config, "web_api_server_port", 9900);
            timezone = JsonUtil.getStringFromJson(config, "timezone", "Europe/Paris");
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
            managerIp = JsonUtil.getStringFromJson(config, "manager_ip", "localhost");
            managerPort = JsonUtil.getIntFromJson(config, "manager_port", 8010);
            managerProtocol = JsonUtil.getStringFromJson(config, "manager_protocol", "http");
            managerTimezone = JsonUtil.getStringFromJson(config, "manager_timezone", "UTC");
            useSSL = JsonUtil.getBooleanFromJson(config, "use_ssl", false);
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
            sendNotifications = JsonUtil.getBooleanFromJson(config, "send_notifications", false);
            notificationsPeriod = JsonUtil.getIntFromJson(config, "notifications_period", 1);
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
            debugMode = JsonUtil.getBooleanFromJson(config, "debug_mode", true);
            debugDir = JsonUtil.getStringFromJson(config, "debug_dir", "./");
            logMode = JsonUtil.getBooleanFromJson(config, "log_mode", false);
            logDir = JsonUtil.getStringFromJson(config, "log_dir", "./");
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
            dictionaryPath = JsonUtil.getStringFromJson(config, "dictionaryPath", "src/main/resources/cslconf/cpe/CpeTree.json");
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
            localIpAddress = JsonUtil.getStringFromJson(config, "localIpAddr", "localhost");
            localPort = JsonUtil.getIntFromJson(config, "localPort", 8001);
            knowHostFilePath = JsonUtil.getStringFromJson(config, "knowHostFilePath", "~/.ssh/known_hosts");
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
            on = JsonUtil.getBooleanFromJson(config, "on", true);
            verbose = JsonUtil.getBooleanFromJson(config, "verbose", false);
            debug = JsonUtil.getBooleanFromJson(config, "debug", false);
            webserverPort = JsonUtil.getIntFromJson(config, "webserver_port", 8000);
            webRootdir = JsonUtil.getStringFromJson(config, "web_rootdir", "");
            webRootdir2 = JsonUtil.getStringFromJson(config, "web_rootdir2", "./public3");
            webRootdir3 = JsonUtil.getStringFromJson(config, "web_rootdir3", "./public3");
            datafileSubdir = JsonUtil.getStringFromJson(config, "datafile_subdir", "datafile");
            coreCommands = JsonUtil.getBooleanFromJson(config, "core_commands", true);
            varsCommands = JsonUtil.getBooleanFromJson(config, "vars_commands", true);
            modbusCommands = JsonUtil.getBooleanFromJson(config, "modbus_commands", true);
            externalCommands = JsonUtil.getBooleanFromJson(config, "external_commands", true);
            jsonCommands = JsonUtil.getBooleanFromJson(config, "json_commands", true);
            jcmdCommands = JsonUtil.getBooleanFromJson(config, "jcmd_commands", true);
            databaseCommands = JsonUtil.getBooleanFromJson(config, "database_commands", true);
            configFileCommands = JsonUtil.getBooleanFromJson(config, "config_file_commands", true);
            mxCommands = JsonUtil.getBooleanFromJson(config, "mx_commands", true);
            sendAlerts = JsonUtil.getBooleanFromJson(config, "send_alerts", true);
            sendConsoleOutput = JsonUtil.getBooleanFromJson(config, "send_console_output", true);
            logJsonCommandsToFile = JsonUtil.getBooleanFromJson(config, "logJsonCommandsToFile", true);
            logDir = JsonUtil.getStringFromJson(config, "log_dir", "./idslogs/jcmds");
            logPrefixFilename = JsonUtil.getStringFromJson(config, "log_prefix_filename", "JCMD");
            maxSizeOfLogFiles = JsonUtil.getIntFromJson(config, "max_size_of_log_files", 10000);
            websocketTimeout = JsonUtil.getIntFromJson(config, "websocket_timeout", 10000);
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
            on = JsonUtil.getBooleanFromJson(config, "on", false);
            verbose = JsonUtil.getBooleanFromJson(config, "verbose", true);
            traceAllMessages = JsonUtil.getBooleanFromJson(config, "trace_all_messages", false);
            ip = JsonUtil.getStringFromJson(config, "ip", "0.0.0.0");
            port = JsonUtil.getIntFromJson(config, "port", 8001);
            maxInputQueues = JsonUtil.getIntFromJson(config, "max_input_queues", 10);
            maxSizeOfInputQueues = JsonUtil.getIntFromJson(config, "max_size_of_input_queues", 100);
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
            modulesPackageName = JsonUtil.getStringFromJson(config, "modules_package_name", "main.modules");
            samplingTime = JsonUtil.getIntFromJson(config, "sampling_time", 100);
            numberOfExecLoops = JsonUtil.getIntFromJson(config, "number_of_exec_loops", 2);
            autostart = JsonUtil.getBooleanFromJson(config, "autostart", true);
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
            if (config ==null) { throw new RuntimeException("Wrong configuration"); }
            name = JsonUtil.getStringFromJson(moduleConfig, "name", "module_ids");
            type = JsonUtil.getStringFromJson(moduleConfig, "type", "ModuleIDS");
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
                autostart = JsonUtil.getBooleanFromJson(config, "autostart", true);
                execLoopNumber = JsonUtil.getIntFromJson(config, "exec_loop_number", 1);
                inputPriority = JsonUtil.getIntFromJson(config, "input_priority", 90);
                stepPriority = JsonUtil.getIntFromJson(config, "step_priority", 99);
                outputPriority = JsonUtil.getIntFromJson(config, "output_priority", 98);
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
            on = JsonUtil.getBooleanFromJson(config, "on", true);
            mode = JsonUtil.getIntFromJson(config, "mode", 1);
            helpMode = JsonUtil.getStringFromJson(config, "help_mode", "0:idle, 1:record only, 2: detect online 3: learning 4: detect offline");
            relativeToCSLConfigDir = JsonUtil.getBooleanFromJson(config, "relative_to_CSLConfig_dir", false);
            idsconfDir = JsonUtil.getStringFromJson(config, "idsconf_dir", "idsconf");
            idsconfDirRelativeToDataDir = JsonUtil.getBooleanFromJson(config, "idsconf_dir_relative_to_data_dir", false);
            packetsDirForDetectionOffline = JsonUtil.getStringFromJson(config, "packets_dir_for_detection_offline", "../zzidsdir/data/test/");
            packetsDirForDetectionOfflineInfo = JsonUtil.getStringFromJson(config, "packets_dir_for_detection_offline_info", "used for detection off line (mode 4) and mode 3 if validation after lerarning");
            packetsDirForRecording = JsonUtil.getStringFromJson(config, "packets_dir_for_recording", "../zzidsdir/data/test");
            packetsDirForRecordingInfo = JsonUtil.getStringFromJson(config, "packets_dir_for_recording_info", "used for mode record and detect on line with log (mode 1 and 2)");
            packetsDirForLearning = JsonUtil.getStringFromJson(config, "packets_dir_for_learning", "../zzidsdir/data/test");
            packetsDirForLearningInfo = JsonUtil.getStringFromJson(config, "packets_dir_for_learning_info", "used for learning  (mode 3)");
            validationAfterLearning = JsonUtil.getBooleanFromJson(config, "validation_after_learning", false);
            logToFile = JsonUtil.getBooleanFromJson(config, "log_to_file", true);
            variablesPrefixFilename = JsonUtil.getStringFromJson(config, "variables_prefix_filename", "VARIABLES");
            packetsPrefixFilename = JsonUtil.getStringFromJson(config, "packets_prefix_filename", "PACKETS");
            networkPrefixFilename = JsonUtil.getStringFromJson(config, "network_prefix_filename", "NETWORK");
            maxSizeOfLogFiles = JsonUtil.getIntFromJson(config, "max_size_of_log_files", 100000000);
            networkPrefixFilename = JsonUtil.getStringFromJson(config, "network_prefix_filename", "NETWORK");
            subdirLearn = JsonUtil.getStringFromJson(config, "subdir_learn", "");
            helpSubdirLearn = JsonUtil.getStringFromJson(config, "help_subdir_learn", "The data for learning are in a subdir of working_dir");
            subdirOfflineDetect = JsonUtil.getStringFromJson(config, "subdir_offline_detect", "test");
            helpSubdirDetect = JsonUtil.getStringFromJson(config, "help_subdir_detect", "The data for off line detect are in a subdir of working_dir");
            rulesForDetection = JsonUtil.getStringFromJson(config, "rules_for_detection", "RulesForDetection.txt");
            rulesForLearning = JsonUtil.getStringFromJson(config, "rules_for_learning", "RulesForLearning.txt");
            rulesForSuricataBase = JsonUtil.getStringFromJson(config, "rules_for_suricata_base", "RulesForSuricata.txt");
            rulesForSuricataLearned = JsonUtil.getStringFromJson(config, "rules_for_suricata_learned", "RulesForSuricataLearned.txt");
            learnedModel = JsonUtil.getStringFromJson(config, "learned_model", "LearnedRules.json");
            newLearnedModel = JsonUtil.getStringFromJson(config, "new_learned_model", "NewLearnedRules.json");
            systemConfiguration = JsonUtil.getStringFromJson(config, "system_configuration", "SystemConfiguration.json");
            currentIdsParamsFile = JsonUtil.getStringFromJson(config, "current_idsparams_file", "CurrentIDSParams.json");
            idstraceFlags = JsonUtil.getListStrFromJson(config, "idstrace_flags", Arrays.asList("general:1", "alert:0", "dpi:false", "syslearn:0", "setrisk:0", "XTERNAL_COMMANDS:1", "TEST_PKT_IDS:0"));
            idstraceDir = JsonUtil.getStringFromJson(config, "idstrace_dir", "./idslogs/trace");
            idstraceOn = JsonUtil.getBooleanFromJson(config, "idstrace_on", true);
            sendToConsole = JsonUtil.getBooleanFromJson(config, "send_to_console", false);
            sendToBrowser = JsonUtil.getBooleanFromJson(config, "send_to_browser", true);
            showTicks = JsonUtil.getBooleanFromJson(config, "show_ticks", true);
            killPreviousInstance = JsonUtil.getBooleanFromJson(config, "kill_previous_instance", true);
            tapsDir = JsonUtil.getStringFromJson(config, "taps_dir", "taps");
            historyLength = JsonUtil.getIntFromJson(config, "history_length", 60);
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
            ip = JsonUtil.getStringFromJson(config, "ip", "localhost");
            port = JsonUtil.getIntFromJson(config, "port", 4445);
            name = JsonUtil.getStringFromJson(config, "name", "My Alerts");
            logToFile = JsonUtil.getBooleanFromJson(config, "log_to_file", true);
            logDir = JsonUtil.getStringFromJson(config, "log_dir", "./idslogs/alerts/logs");
            prefixFilename = JsonUtil.getStringFromJson(config, "prefix_filename", "alert");
            maxSizeOfLogFiles = JsonUtil.getIntFromJson(config, "max_size_of_log_files", 10000);
            alertToWeb = JsonUtil.getBooleanFromJson(config, "alert_to_web", true);
            alertJsonTag = JsonUtil.getStringFromJson(config, "alert_json_tag", "alert");
            alertToUdp = JsonUtil.getBooleanFromJson(config, "alert_to_udp", true);
            alertToDb = JsonUtil.getBooleanFromJson(config, "alert_to_db", true);
            showAlerts = JsonUtil.getBooleanFromJson(config, "show_alerts", true);
            doNotResentSameAlert = JsonUtil.getBooleanFromJson(config, "do_not_resent_same_alert", false);
            alertDuration = JsonUtil.getIntFromJson(config, "alert_duration", 500);
            filenameCurrentAlerts = JsonUtil.getStringFromJson(config, "filename_current_alerts", "current_alerts");
            subdirBackupAlerts = JsonUtil.getStringFromJson(config, "subdir_backup_alerts", "alerts");
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
            ip = JsonUtil.getStringFromJson(config, "ip", "localhost");
            port = JsonUtil.getIntFromJson(config, "port", 8002);
            syncFrequency = JsonUtil.getIntFromJson(config, "sync_frequency", 300);
        }
    }
}
