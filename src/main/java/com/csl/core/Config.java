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
        String ipServerRemote;
        Integer portServerRemote;
        String serverRemoteUrlPrefix;
        Boolean forceHostNameResolution;
        String apiKey;
        Boolean useSsl;
        Boolean launchWebApiServer;
        Integer webApiServerPort;

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
        String managerIp;
        Integer managerPort;
        String managerProtocol;
        String managerTimezone;

        public CSLScan(Json globalConfig) {
            managerIp = JsonUtil.getStringFromJson(globalConfig, "manager_ip", "localhost");
            managerPort = JsonUtil.getIntFromJson(globalConfig, "manager_port", 8010);
            managerProtocol = JsonUtil.getStringFromJson(globalConfig, "manager_protocol", "http");
            managerTimezone = JsonUtil.getStringFromJson(globalConfig, "manager_timezone", "UTC");
        }
    }

    @Getter
    public static class CSLStatus {
        Boolean sendNotifications;
        Integer notificationsPeriod;
        String managerProtocol;
        String managerTimezone;

        public CSLStatus(Json globalConfig) {
            sendNotifications = JsonUtil.getBooleanFromJson(globalConfig, "send_notifications", false);
            notificationsPeriod = JsonUtil.getIntFromJson(globalConfig, "notifications_period", 1);
        }
    }

    @Getter
    public static class CSLNmapService {
        Boolean debugMode;
        String debugDir;
        Boolean logMode;
        String logDir;

        public CSLNmapService(Json globalConfig) {
            debugMode = JsonUtil.getBooleanFromJson(globalConfig, "debug_mode", true);
            debugDir = JsonUtil.getStringFromJson(globalConfig, "debug_dir", "./");
            logMode = JsonUtil.getBooleanFromJson(globalConfig, "log_mode", false);
            logDir = JsonUtil.getStringFromJson(globalConfig, "log_dir", "./");
        }
    }

    @Getter
    public static class CSLCpeService {
        String dictionaryPath;

        public CSLCpeService(Json globalConfig) {
            dictionaryPath = JsonUtil.getStringFromJson(globalConfig, "dictionaryPath", "src/main/resources/cslconf/cpe/CpeTree.json");
        }
    }

    @Getter
    public static class CSLSshService {
        String localIpAddress;
        Integer localPort;
        String knowHostFilePath;

        public CSLSshService(Json globalConfig) {
            localIpAddress = JsonUtil.getStringFromJson(globalConfig, "localIpAddr", "localhost");
            localPort = JsonUtil.getIntFromJson(globalConfig, "localPort", 8001);
            knowHostFilePath = JsonUtil.getStringFromJson(globalConfig, "knowHostFilePath", "~/.ssh/known_hosts");
        }
    }

    @Getter
    public static class CSLDatabaseServerConf {
        String datafileSubdir;
        Boolean on;

        public CSLDatabaseServerConf(Json globalConfig) {
            datafileSubdir = JsonUtil.getStringFromJson(globalConfig, "datafile_subdir", "datafile");
            on = JsonUtil.getBooleanFromJson(globalConfig, "on", true);
        }
    }

    @Getter
    public static class CSLServiceLoader {
        Boolean traceServiceExecution;
        Boolean traceLibrarySearch;
        List<String> services;

        public CSLServiceLoader(Json globalConfig) {
            traceServiceExecution = JsonUtil.getBooleanFromJson(globalConfig, "trace_service_execution", true);
            traceLibrarySearch = JsonUtil.getBooleanFromJson(globalConfig, "trace_library_search", true);
            services = JsonUtil.getListStrFromJson(globalConfig, "services", List.of("./service1.jar"));
        }
    }

    @Getter
    public static class CSLWebServerConf {
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
        Boolean on;
        Boolean verbose;
        Boolean traceAllMessages;
        String ip;
        Integer port;
        Integer maxInputQueues;
        Integer maxSizeOfInputQueues;

        public CSLUdpServerConf(Json globalConfig) {
            on = JsonUtil.getBooleanFromJson(globalConfig, "on", true);
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
        Integer sampling_time;
        String modulesPackageName;
        Integer numberOfExecLoops;
        Boolean autostart;

        public CSLModuleExec(Json globalConfig) {
            modulesPackageName = JsonUtil.getStringFromJson(globalConfig, "modules_package_name", "main.modules");
            sampling_time = JsonUtil.getIntFromJson(globalConfig, "sampling_time", 1000);
            numberOfExecLoops = JsonUtil.getIntFromJson(globalConfig, "number_of_exec_loops", 2);
            autostart = JsonUtil.getBooleanFromJson(globalConfig, "autostart", true);
        }
    }

    @Getter
    public static class CSLModule {
        String name;
        String type;
        CSLModuleConfig config;

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
            Integer exec_loop_number;
            Integer input_priority;
            Integer step_priority;
            Integer output_priority;

            public CSLModuleConfig(Json globalConfig) {
                autostart = JsonUtil.getBooleanFromJson(globalConfig, "autostart", true);
                exec_loop_number = JsonUtil.getIntFromJson(globalConfig, "exec_loop_number", 1);
                input_priority = JsonUtil.getIntFromJson(globalConfig, "input_priority", 90);
                step_priority = JsonUtil.getIntFromJson(globalConfig, "step_priority", 99);
                output_priority = JsonUtil.getIntFromJson(globalConfig, "output_priority", 98);
            }
        }
    }

    @Getter
    public static class CSLIdsConf {
        Boolean on;
        Integer mode;
        String help_mode;
        Boolean relative_to_CSLConfig_dir;
        String idsconf_dir;
        Boolean idsconf_dir_relative_to_data_dir;
        String packets_dir_for_detection_offline;
        String packets_dir_for_detection_offline_info;
        String packets_dir_for_recording;
        String packets_dir_for_recording_info;
        String packets_dir_for_learning;
        String packets_dir_for_learning_info;
        Boolean validation_after_learning;
        Boolean log_to_file;
        String variables_prefix_filename;
        String packets_prefix_filename;
        String network_prefix_filename;
        Integer max_size_of_log_files;
        String subdir_learn;
        String help_subdir_learn;
        String subdir_offline_detect;
        String help_subdir_detect;
        String rules_for_detection;
        String rules_for_learning;
        String rules_for_suricata_base;
        String rules_for_suricata_learned;
        String learned_model;
        String new_learned_model;
        String system_configuration;
        String current_idsparams_file;
        List<String> idstrace_flags;
        String general;
        String idstrace_dir;
        Boolean idstrace_on;
        Boolean send_to_console;
        Boolean send_to_browser;
        Boolean show_ticks;
        Boolean kill_previous_instance;
        String taps_dir;
        Integer history_length;

        public CSLIdsConf(Json globalConfig) {
            on = JsonUtil.getBooleanFromJson(globalConfig, "on", true);
            mode = JsonUtil.getIntFromJson(globalConfig, "mode", 1);
            help_mode = JsonUtil.getStringFromJson(globalConfig, "help_mode", "0:idle, 1:record only, 2: detect online 3: learning 4: detect offline");
            relative_to_CSLConfig_dir = JsonUtil.getBooleanFromJson(globalConfig, "relative_to_CSLConfig_dir", false);
            idsconf_dir = JsonUtil.getStringFromJson(globalConfig, "idsconf_dir", "idsconf");
            idsconf_dir_relative_to_data_dir = JsonUtil.getBooleanFromJson(globalConfig, "idsconf_dir_relative_to_data_dir", false);
            packets_dir_for_detection_offline = JsonUtil.getStringFromJson(globalConfig, "packets_dir_for_detection_offline", "../zzidsdir/data/test/");
            packets_dir_for_detection_offline_info = JsonUtil.getStringFromJson(globalConfig, "packets_dir_for_detection_offline_info", "used for detection off line (mode 4) and mode 3 if validation after lerarning");
            packets_dir_for_recording = JsonUtil.getStringFromJson(globalConfig, "packets_dir_for_recording", "../zzidsdir/data/test");
            packets_dir_for_recording_info = JsonUtil.getStringFromJson(globalConfig, "packets_dir_for_recording_info", "used for mode record and detect on line with log (mode 1 and 2)");
            packets_dir_for_learning = JsonUtil.getStringFromJson(globalConfig, "packets_dir_for_learning", "../zzidsdir/data/test");
            packets_dir_for_learning_info = JsonUtil.getStringFromJson(globalConfig, "packets_dir_for_learning_info", "used for learning  (mode 3)");
            validation_after_learning = JsonUtil.getBooleanFromJson(globalConfig, "validation_after_learning", false);
            log_to_file = JsonUtil.getBooleanFromJson(globalConfig, "log_to_file", true);
            variables_prefix_filename = JsonUtil.getStringFromJson(globalConfig, "variables_prefix_filename", "VARIABLES");
            packets_prefix_filename = JsonUtil.getStringFromJson(globalConfig, "packets_prefix_filename", "PACKETS");
            network_prefix_filename = JsonUtil.getStringFromJson(globalConfig, "network_prefix_filename", "NETWORK");
            max_size_of_log_files = JsonUtil.getIntFromJson(globalConfig, "max_size_of_log_files", 100000000);
            network_prefix_filename = JsonUtil.getStringFromJson(globalConfig, "network_prefix_filename", "NETWORK");
            subdir_learn = JsonUtil.getStringFromJson(globalConfig, "subdir_learn", "");
            help_subdir_learn = JsonUtil.getStringFromJson(globalConfig, "help_subdir_learn", "The data for learning are in a subdir of working_dir");
            subdir_offline_detect = JsonUtil.getStringFromJson(globalConfig, "subdir_offline_detect", "test");
            help_subdir_detect = JsonUtil.getStringFromJson(globalConfig, "help_subdir_detect", "The data for off line detect are in a subdir of working_dir");
            rules_for_detection = JsonUtil.getStringFromJson(globalConfig, "rules_for_detection", "RulesForDetection.txt");
            rules_for_learning = JsonUtil.getStringFromJson(globalConfig, "rules_for_learning", "RulesForLearning.txt");
            rules_for_suricata_base = JsonUtil.getStringFromJson(globalConfig, "rules_for_suricata_base", "RulesForSuricata.txt");
            rules_for_suricata_learned = JsonUtil.getStringFromJson(globalConfig, "rules_for_suricata_learned", "RulesForSuricataLearned.txt");
            learned_model = JsonUtil.getStringFromJson(globalConfig, "learned_model", "LearnedRules.json");
            new_learned_model = JsonUtil.getStringFromJson(globalConfig, "new_learned_model", "NewLearnedRules.json");
            system_configuration = JsonUtil.getStringFromJson(globalConfig, "system_configuration", "SystemConfiguration.json");
            current_idsparams_file = JsonUtil.getStringFromJson(globalConfig, "current_idsparams_file", "CurrentIDSParams.json");
            idstrace_flags = JsonUtil.getListStrFromJson(globalConfig, "idstrace_flags", Arrays.asList("general:1", "alert:0", "dpi:false", "syslearn:0", "setrisk:0", "XTERNAL_COMMANDS:1", "TEST_PKT_IDS:0"));
            idstrace_dir = JsonUtil.getStringFromJson(globalConfig, "idstrace_dir", "./idslogs/trace");
            idstrace_on = JsonUtil.getBooleanFromJson(globalConfig, "idstrace_on", true);
            send_to_console = JsonUtil.getBooleanFromJson(globalConfig, "send_to_console", false);
            send_to_browser = JsonUtil.getBooleanFromJson(globalConfig, "send_to_browser", true);
            show_ticks = JsonUtil.getBooleanFromJson(globalConfig, "show_ticks", false);
            kill_previous_instance = JsonUtil.getBooleanFromJson(globalConfig, "kill_previous_instance", true);
            taps_dir = JsonUtil.getStringFromJson(globalConfig, "taps_dir", "taps");
            history_length = JsonUtil.getIntFromJson(globalConfig, "history_length", 60);
        }
    }

    @Getter
    public static class CSLAlertViewer {
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
        String ip;
        Integer port;
        Integer sync_frequency;

        public CSLAutocrypt(Json globalConfig) {
            ip = JsonUtil.getStringFromJson(globalConfig, "ip", "localhost");
            port = JsonUtil.getIntFromJson(globalConfig, "port", 8002);
            sync_frequency = JsonUtil.getIntFromJson(globalConfig, "sync_frequency", 300);
        }
    }
}
