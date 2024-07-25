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

    public final CSLGlobal Global;
    public final CSLScan Scan;
    public final CSLStatus Status;
    public final CSLNmapService NmapService;
    public final CSLCpeService CpeService;
    public final CSLSshService SshService;
    public final CSLDatabaseServerConf DatabaseServerConf;
    public final CSLWebServerConf WebServerConf;
    public final CSLServiceLoader ServiceLoader;
    public final CSLUdpServerConf UdpServerConf;
    public final CSLModuleExec ModuleExec;
    public final List<CSLModule> Modules;
    public final CSLIdsConf IdsConf;
    public final CSLAlertViewer AlertViewer;
    public final CSLAutocrypt Autocrypt;

    private Config(String configFile) {
        jConfig = readConfig(configFile);

        Global = new CSLGlobal(jConfig.get("global"));
        Scan = new CSLScan(jConfig.get("discovery"));
        Status = new CSLStatus(jConfig.get("status"));
        NmapService = new CSLNmapService(jConfig.get("nmap_service"));
        CpeService = new CSLCpeService(jConfig.get("cpe_service"));
        SshService = new CSLSshService(jConfig.get("cve_service"));
        DatabaseServerConf = new CSLDatabaseServerConf(jConfig.get("ssh_service"));
        WebServerConf = new CSLWebServerConf(jConfig.get("web_server_conf"));
        ServiceLoader = new CSLServiceLoader(jConfig.get("service_loader"));
        UdpServerConf = new CSLUdpServerConf(jConfig.get("udp_server_conf"));
        ModuleExec = new CSLModuleExec(jConfig.get("module_exec"));
        Modules = CSLModule.makeList(jConfig.get("modules"));
        IdsConf = new CSLIdsConf(jConfig.get("ids_conf"));
        AlertViewer = new CSLAlertViewer(jConfig.get("alert_viewer"));
        Autocrypt = new CSLAutocrypt(jConfig.get("autocrypt"));
    }

    Json readConfig(String f) {

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

    @Getter
    public static class CSLGlobal {
        // region define variables
        String ipServerRemote;
        Integer portServerRemote;
        String serverRemoteUrlPrefix;
        Boolean forceHostNameResolution;
        String apiKey;
        Boolean useSsl;
        Boolean launchWebApiServer;
        Integer webApiServerPort;
        // endregion define variables

        public CSLGlobal(Json globalConfig) {
            ipServerRemote = JsonUtil.getStringFromJson(globalConfig, "ip_server_remote", "localhost");
            portServerRemote = JsonUtil.getIntFromJson(globalConfig, "port_server_remote", 8000);
            serverRemoteUrlPrefix = JsonUtil.getStringFromJson(globalConfig, "server_remote_url_prefix", "");
            forceHostNameResolution = JsonUtil.getBooleanFromJson(globalConfig, "force_host_name_resolution", false);
            apiKey = JsonUtil.getStringFromJson(globalConfig, "api_key", "");
            useSsl = JsonUtil.getBooleanFromJson(globalConfig, "use_ssl", true);
            launchWebApiServer = JsonUtil.getBooleanFromJson(globalConfig, "launch_web_api_server", false);
            webApiServerPort = JsonUtil.getIntFromJson(globalConfig, "web_api_server_port", 9900);
        }
    }

    @Getter
    public static class CSLScan {
        // region define variables
        String managerIp;
        Integer managerPort;
        String managerProtocol;
        String managerTimezone;
        // endregion define variables

        public CSLScan(Json globalConfig) {
            managerIp = JsonUtil.getStringFromJson(globalConfig, "manager_ip", "localhost");
            managerPort = JsonUtil.getIntFromJson(globalConfig, "manager_port", 8010);
            managerProtocol = JsonUtil.getStringFromJson(globalConfig, "manager_protocol", "http");
            managerTimezone = JsonUtil.getStringFromJson(globalConfig, "manager_timezone", "UTC");
        }
    }

    @Getter
    public static class CSLStatus {
        // region define variables
        Boolean sendNotifications;
        Integer notificationsPeriod;
        // endregion define variables

        public CSLStatus(Json globalConfig) {
            sendNotifications = JsonUtil.getBooleanFromJson(globalConfig, "send_notifications", false);
            notificationsPeriod = JsonUtil.getIntFromJson(globalConfig, "notifications_period", 1);
        }
    }

    @Getter
    public static class CSLNmapService {
        // region define variables
        Boolean debugMode;
        String debugDir;
        Boolean logMode;
        String logDir;
        // endregion define variables

        public CSLNmapService(Json globalConfig) {
            debugMode = JsonUtil.getBooleanFromJson(globalConfig, "debug_mode", true);
            debugDir = JsonUtil.getStringFromJson(globalConfig, "debug_dir", "./");
            logMode = JsonUtil.getBooleanFromJson(globalConfig, "log_mode", false);
            logDir = JsonUtil.getStringFromJson(globalConfig, "log_dir", "./");
        }
    }

    @Getter
    public static class CSLCpeService {
        // region define variables
        String dictionaryPath;
        // endregion define variables

        public CSLCpeService(Json globalConfig) {
            dictionaryPath = JsonUtil.getStringFromJson(globalConfig, "dictionaryPath", "src/main/resources/cslconf/cpe/CpeTree.json");
        }
    }

    @Getter
    public static class CSLSshService {
        // region define variables
        String localIpAddress;
        Integer localPort;
        String knowHostFilePath;
        // endregion define variables

        public CSLSshService(Json globalConfig) {
            localIpAddress = JsonUtil.getStringFromJson(globalConfig, "localIpAddr", "localhost");
            localPort = JsonUtil.getIntFromJson(globalConfig, "localPort", 8001);
            knowHostFilePath = JsonUtil.getStringFromJson(globalConfig, "knowHostFilePath", "~/.ssh/known_hosts");
        }
    }

    @Getter
    public static class CSLDatabaseServerConf {
        // region define variables
        String datafileSubdir;
        @Setter
        Boolean on;
        // endregion define variables

        public CSLDatabaseServerConf(Json globalConfig) {
            datafileSubdir = JsonUtil.getStringFromJson(globalConfig, "datafile_subdir", "datafile");
            on = JsonUtil.getBooleanFromJson(globalConfig, "on", true);
        }
    }

    @Getter
    public static class CSLServiceLoader {
        // region define variables
        Boolean traceServiceExecution;
        Boolean traceLibrarySearch;
        List<String> services;
        // endregion define variables

        public CSLServiceLoader(Json globalConfig) {
            traceServiceExecution = JsonUtil.getBooleanFromJson(globalConfig, "trace_service_execution", true);
            traceLibrarySearch = JsonUtil.getBooleanFromJson(globalConfig, "trace_library_search", true);
            services = JsonUtil.getListStrFromJson(globalConfig, "services", List.of("./service1.jar"));
        }
    }

    @Getter
    public static class CSLWebServerConf {
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

        public CSLWebServerConf(Json globalConfig) {
            on = JsonUtil.getBooleanFromJson(globalConfig, "on", true);
            verbose = JsonUtil.getBooleanFromJson(globalConfig, "verbose", false);
            debug = JsonUtil.getBooleanFromJson(globalConfig, "debug", false);
            webserverPort = JsonUtil.getIntFromJson(globalConfig, "webserver_port", 8000);
            webRootdir = JsonUtil.getStringFromJson(globalConfig, "web_rootdir", "");
            webRootdir2 = JsonUtil.getStringFromJson(globalConfig, "web_rootdir2", "./public3");
            webRootdir3 = JsonUtil.getStringFromJson(globalConfig, "web_rootdir3", "./public3");
            datafileSubdir = JsonUtil.getStringFromJson(globalConfig, "datafile_subdir", "datafile");
            coreCommands = JsonUtil.getBooleanFromJson(globalConfig, "core_commands", true);
            varsCommands = JsonUtil.getBooleanFromJson(globalConfig, "vars_commands", true);
            modbusCommands = JsonUtil.getBooleanFromJson(globalConfig, "modbus_commands", true);
            externalCommands = JsonUtil.getBooleanFromJson(globalConfig, "external_commands", true);
            jsonCommands = JsonUtil.getBooleanFromJson(globalConfig, "json_commands", true);
            databaseCommands = JsonUtil.getBooleanFromJson(globalConfig, "database_commands", true);
            configFileCommands = JsonUtil.getBooleanFromJson(globalConfig, "config_file_commands", true);
            mxCommands = JsonUtil.getBooleanFromJson(globalConfig, "mx_commands", true);
            sendAlerts = JsonUtil.getBooleanFromJson(globalConfig, "send_alerts", true);
            sendConsoleOutput = JsonUtil.getBooleanFromJson(globalConfig, "send_console_output", true);
            logJsonCommandsToFile = JsonUtil.getBooleanFromJson(globalConfig, "logJsonCommandsToFile", true);
            logDir = JsonUtil.getStringFromJson(globalConfig, "log_dir", "./idslogs/jcmds");
            logPrefixFilename = JsonUtil.getStringFromJson(globalConfig, "log_prefix_filename", "JCMD");
            maxSizeOfLogFiles = JsonUtil.getIntFromJson(globalConfig, "max_size_of_log_files", 10000);
            websocketTimeout = JsonUtil.getIntFromJson(globalConfig, "websocket_timeout", 10000);
        }
    }

    @Getter
    public static class CSLUdpServerConf {
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

        public CSLUdpServerConf(Json globalConfig) {
            on = JsonUtil.getBooleanFromJson(globalConfig, "on", false);
            verbose = JsonUtil.getBooleanFromJson(globalConfig, "verbose", true);
            traceAllMessages = JsonUtil.getBooleanFromJson(globalConfig, "trace_all_messages", false);
            ip = JsonUtil.getStringFromJson(globalConfig, "ip", "0.0.0.0");
            port = JsonUtil.getIntFromJson(globalConfig, "port", 8001);
            maxInputQueues = JsonUtil.getIntFromJson(globalConfig, "max_input_queues", 10);
            maxSizeOfInputQueues = JsonUtil.getIntFromJson(globalConfig, "max_size_of_input_queues", 100);
        }
    }

    @Getter
    public static class CSLModuleExec {
        // region define variables
        Integer samplingTime;
        String modulesPackageName;
        Integer numberOfExecLoops;
        Boolean autostart;
        // endregion define variables

        public CSLModuleExec(Json globalConfig) {
            modulesPackageName = JsonUtil.getStringFromJson(globalConfig, "modules_package_name", "main.modules");
            samplingTime = JsonUtil.getIntFromJson(globalConfig, "sampling_time", 1000);
            numberOfExecLoops = JsonUtil.getIntFromJson(globalConfig, "number_of_exec_loops", 2);
            autostart = JsonUtil.getBooleanFromJson(globalConfig, "autostart", true);
        }
    }

    @Getter
    public static class CSLModule {
        // region define variables
        String name;
        String type;
        CSLModuleConfig config;
        // endregion define variables

        public CSLModule(Json globalConfig) {
            name = JsonUtil.getStringFromJson(globalConfig, "name", "module_ids");
            type = JsonUtil.getStringFromJson(globalConfig, "type", "ModuleIDS");
            config = new CSLModuleConfig(globalConfig.get("config"));
        }

        public static List<CSLModule> makeList(Json list) {
            if (list == null || list.isNull() || !list.isArray()) {
                throw new RuntimeException("wrong config");
            }

            List<CSLModule> moduleList = new ArrayList<>();
            for (Json json : list.asJsonList()) {
                moduleList.add(new CSLModule(json));
            }

            return moduleList;
        }

        @Getter
        public static class CSLModuleConfig {
            Boolean autostart;
            Integer execLoopNumber;
            Integer inputPriority;
            Integer stepPriority;
            Integer outputPriority;

            public CSLModuleConfig(Json globalConfig) {
                autostart = JsonUtil.getBooleanFromJson(globalConfig, "autostart", true);
                execLoopNumber = JsonUtil.getIntFromJson(globalConfig, "exec_loop_number", 1);
                inputPriority = JsonUtil.getIntFromJson(globalConfig, "input_priority", 90);
                stepPriority = JsonUtil.getIntFromJson(globalConfig, "step_priority", 99);
                outputPriority = JsonUtil.getIntFromJson(globalConfig, "output_priority", 98);
            }
        }
    }

    @Getter
    public static class CSLIdsConf {
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

        public CSLIdsConf(Json globalConfig) {
            on = JsonUtil.getBooleanFromJson(globalConfig, "on", true);
            mode = JsonUtil.getIntFromJson(globalConfig, "mode", 1);
            helpMode = JsonUtil.getStringFromJson(globalConfig, "help_mode", "0:idle, 1:record only, 2: detect online 3: learning 4: detect offline");
            relativeToCSLConfigDir = JsonUtil.getBooleanFromJson(globalConfig, "relative_to_CSLConfig_dir", false);
            idsconfDir = JsonUtil.getStringFromJson(globalConfig, "idsconf_dir", "idsconf");
            idsconfDirRelativeToDataDir = JsonUtil.getBooleanFromJson(globalConfig, "idsconf_dir_relative_to_data_dir", false);
            packetsDirForDetectionOffline = JsonUtil.getStringFromJson(globalConfig, "packets_dir_for_detection_offline", "../zzidsdir/data/test/");
            packetsDirForDetectionOfflineInfo = JsonUtil.getStringFromJson(globalConfig, "packets_dir_for_detection_offline_info", "used for detection off line (mode 4) and mode 3 if validation after lerarning");
            packetsDirForRecording = JsonUtil.getStringFromJson(globalConfig, "packets_dir_for_recording", "../zzidsdir/data/test");
            packetsDirForRecordingInfo = JsonUtil.getStringFromJson(globalConfig, "packets_dir_for_recording_info", "used for mode record and detect on line with log (mode 1 and 2)");
            packetsDirForLearning = JsonUtil.getStringFromJson(globalConfig, "packets_dir_for_learning", "../zzidsdir/data/test");
            packetsDirForLearningInfo = JsonUtil.getStringFromJson(globalConfig, "packets_dir_for_learning_info", "used for learning  (mode 3)");
            validationAfterLearning = JsonUtil.getBooleanFromJson(globalConfig, "validation_after_learning", false);
            logToFile = JsonUtil.getBooleanFromJson(globalConfig, "log_to_file", true);
            variablesPrefixFilename = JsonUtil.getStringFromJson(globalConfig, "variables_prefix_filename", "VARIABLES");
            packetsPrefixFilename = JsonUtil.getStringFromJson(globalConfig, "packets_prefix_filename", "PACKETS");
            networkPrefixFilename = JsonUtil.getStringFromJson(globalConfig, "network_prefix_filename", "NETWORK");
            maxSizeOfLogFiles = JsonUtil.getIntFromJson(globalConfig, "max_size_of_log_files", 100000000);
            networkPrefixFilename = JsonUtil.getStringFromJson(globalConfig, "network_prefix_filename", "NETWORK");
            subdirLearn = JsonUtil.getStringFromJson(globalConfig, "subdir_learn", "");
            helpSubdirLearn = JsonUtil.getStringFromJson(globalConfig, "help_subdir_learn", "The data for learning are in a subdir of working_dir");
            subdirOfflineDetect = JsonUtil.getStringFromJson(globalConfig, "subdir_offline_detect", "test");
            helpSubdirDetect = JsonUtil.getStringFromJson(globalConfig, "help_subdir_detect", "The data for off line detect are in a subdir of working_dir");
            rulesForDetection = JsonUtil.getStringFromJson(globalConfig, "rules_for_detection", "RulesForDetection.txt");
            rulesForLearning = JsonUtil.getStringFromJson(globalConfig, "rules_for_learning", "RulesForLearning.txt");
            rulesForSuricataBase = JsonUtil.getStringFromJson(globalConfig, "rules_for_suricata_base", "RulesForSuricata.txt");
            rulesForSuricataLearned = JsonUtil.getStringFromJson(globalConfig, "rules_for_suricata_learned", "RulesForSuricataLearned.txt");
            learnedModel = JsonUtil.getStringFromJson(globalConfig, "learned_model", "LearnedRules.json");
            newLearnedModel = JsonUtil.getStringFromJson(globalConfig, "new_learned_model", "NewLearnedRules.json");
            systemConfiguration = JsonUtil.getStringFromJson(globalConfig, "system_configuration", "SystemConfiguration.json");
            currentIdsParamsFile = JsonUtil.getStringFromJson(globalConfig, "current_idsparams_file", "CurrentIDSParams.json");
            idstraceFlags = JsonUtil.getListStrFromJson(globalConfig, "idstrace_flags", Arrays.asList("general:1", "alert:0", "dpi:false", "syslearn:0", "setrisk:0", "XTERNAL_COMMANDS:1", "TEST_PKT_IDS:0"));
            idstraceDir = JsonUtil.getStringFromJson(globalConfig, "idstrace_dir", "./idslogs/trace");
            idstraceOn = JsonUtil.getBooleanFromJson(globalConfig, "idstrace_on", true);
            sendToConsole = JsonUtil.getBooleanFromJson(globalConfig, "send_to_console", false);
            sendToBrowser = JsonUtil.getBooleanFromJson(globalConfig, "send_to_browser", true);
            showTicks = JsonUtil.getBooleanFromJson(globalConfig, "show_ticks", false);
            killPreviousInstance = JsonUtil.getBooleanFromJson(globalConfig, "kill_previous_instance", true);
            tapsDir = JsonUtil.getStringFromJson(globalConfig, "taps_dir", "taps");
            historyLength = JsonUtil.getIntFromJson(globalConfig, "history_length", 60);
        }
    }

    @Getter
    public static class CSLAlertViewer {
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
        Boolean doNotResentSameAlert;
        Integer alertDuration;
        // endregion define variables

        public CSLAlertViewer(Json globalConfig) {
            ip = JsonUtil.getStringFromJson(globalConfig, "ip", "localhost");
            port = JsonUtil.getIntFromJson(globalConfig, "port", 4445);
            name = JsonUtil.getStringFromJson(globalConfig, "name", "My Alerts");
            logToFile = JsonUtil.getBooleanFromJson(globalConfig, "logToFile", true);
            logDir = JsonUtil.getStringFromJson(globalConfig, "log_dir", "./idslogs/alerts");
            prefixFilename = JsonUtil.getStringFromJson(globalConfig, "prefix_filename", "AL");
            maxSizeOfLogFiles = JsonUtil.getIntFromJson(globalConfig, "max_size_of_log_files", 10000);
            alertToWeb = JsonUtil.getBooleanFromJson(globalConfig, "alert_to_web", true);
            alertJsonTag = JsonUtil.getStringFromJson(globalConfig, "alert_json_tag", "alert");
            alertToUdp = JsonUtil.getBooleanFromJson(globalConfig, "alert_to_udp", true);
            doNotResentSameAlert = JsonUtil.getBooleanFromJson(globalConfig, "do_not_resent_same_alert", true);
            alertDuration = JsonUtil.getIntFromJson(globalConfig, "alert_duration", 10000);
        }
    }

    @Getter
    public static class CSLAutocrypt {
        // region define variables
        String ip;
        Integer port;
        Integer syncFrequency;
        // endregion define variables

        public CSLAutocrypt(Json globalConfig) {
            ip = JsonUtil.getStringFromJson(globalConfig, "ip", "localhost");
            port = JsonUtil.getIntFromJson(globalConfig, "port", 8002);
            syncFrequency = JsonUtil.getIntFromJson(globalConfig, "sync_frequency", 300);
        }
    }
}
