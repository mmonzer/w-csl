package com.csl.core;

import com.csl.exceptions.WrongConfigurationException;
import com.csl.util.FileUtils;
import com.ucsl.json.Json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
    static String configFileName = "application.json";
    private static final String WRONG_CONFIGURATION = "Wrong configuration";
    Logger logger = LoggerFactory.getLogger(Config.class);
    Json jConfig;
    String separator = "__";
    public static Config instance = new Config(configFileName);

    public final Client Client;
    public final Config.Scan Scan;
    public final Config.Status Status;
    public final Tap TapService;
    public final Server Server;
    public final Config.UdpServerConf UdpServerConf;
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
        IdsConf = new IdsConf(jConfig.get("ids_conf"));
        AlertViewer = new AlertViewer(jConfig.get("alert_viewer"));
        Scan = new Scan(jConfig.get("discovery_service"));
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

            return FileUtils.readFile(reader);
        } catch (Exception e) {
            // e.printStackTrace();
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
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
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
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
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
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
            sendNotifications = config.get( "send_notifications").asBoolean();
            notificationsPeriod = config.get("notifications_period").asInteger();
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
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
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
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
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
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
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

        public Boolean getOn() {
            return on;
        }

        public void setOn(Boolean on) {
            this.on = on;
        }

        public Integer getMode() {
            return mode;
        }

        public void setMode(Integer mode) {
            this.mode = mode;
        }

        public String getHelpMode() {
            return helpMode;
        }

        public void setHelpMode(String helpMode) {
            this.helpMode = helpMode;
        }

        public Boolean getRelativeToCSLConfigDir() {
            return relativeToCSLConfigDir;
        }

        public void setRelativeToCSLConfigDir(Boolean relativeToCSLConfigDir) {
            this.relativeToCSLConfigDir = relativeToCSLConfigDir;
        }

        public String getIdsconfDir() {
            return idsconfDir;
        }

        public void setIdsconfDir(String idsconfDir) {
            this.idsconfDir = idsconfDir;
        }

        public Boolean getIdsconfDirRelativeToDataDir() {
            return idsconfDirRelativeToDataDir;
        }

        public void setIdsconfDirRelativeToDataDir(Boolean idsconfDirRelativeToDataDir) {
            this.idsconfDirRelativeToDataDir = idsconfDirRelativeToDataDir;
        }

        public String getPacketsDirForDetectionOffline() {
            return packetsDirForDetectionOffline;
        }

        public void setPacketsDirForDetectionOffline(String packetsDirForDetectionOffline) {
            this.packetsDirForDetectionOffline = packetsDirForDetectionOffline;
        }

        public String getPacketsDirForDetectionOfflineInfo() {
            return packetsDirForDetectionOfflineInfo;
        }

        public void setPacketsDirForDetectionOfflineInfo(String packetsDirForDetectionOfflineInfo) {
            this.packetsDirForDetectionOfflineInfo = packetsDirForDetectionOfflineInfo;
        }

        public String getPacketsDirForRecording() {
            return packetsDirForRecording;
        }

        public void setPacketsDirForRecording(String packetsDirForRecording) {
            this.packetsDirForRecording = packetsDirForRecording;
        }

        public String getPacketsDirForRecordingInfo() {
            return packetsDirForRecordingInfo;
        }

        public void setPacketsDirForRecordingInfo(String packetsDirForRecordingInfo) {
            this.packetsDirForRecordingInfo = packetsDirForRecordingInfo;
        }

        public String getPacketsDirForLearning() {
            return packetsDirForLearning;
        }

        public void setPacketsDirForLearning(String packetsDirForLearning) {
            this.packetsDirForLearning = packetsDirForLearning;
        }

        public String getPacketsDirForLearningInfo() {
            return packetsDirForLearningInfo;
        }

        public void setPacketsDirForLearningInfo(String packetsDirForLearningInfo) {
            this.packetsDirForLearningInfo = packetsDirForLearningInfo;
        }

        public Boolean getValidationAfterLearning() {
            return validationAfterLearning;
        }

        public void setValidationAfterLearning(Boolean validationAfterLearning) {
            this.validationAfterLearning = validationAfterLearning;
        }

        public Boolean getLogToFile() {
            return logToFile;
        }

        public void setLogToFile(Boolean logToFile) {
            this.logToFile = logToFile;
        }

        public String getVariablesPrefixFilename() {
            return variablesPrefixFilename;
        }

        public void setVariablesPrefixFilename(String variablesPrefixFilename) {
            this.variablesPrefixFilename = variablesPrefixFilename;
        }

        public String getPacketsPrefixFilename() {
            return packetsPrefixFilename;
        }

        public void setPacketsPrefixFilename(String packetsPrefixFilename) {
            this.packetsPrefixFilename = packetsPrefixFilename;
        }

        public String getNetworkPrefixFilename() {
            return networkPrefixFilename;
        }

        public void setNetworkPrefixFilename(String networkPrefixFilename) {
            this.networkPrefixFilename = networkPrefixFilename;
        }

        public Integer getMaxSizeOfLogFiles() {
            return maxSizeOfLogFiles;
        }

        public void setMaxSizeOfLogFiles(Integer maxSizeOfLogFiles) {
            this.maxSizeOfLogFiles = maxSizeOfLogFiles;
        }

        public String getSubdirLearn() {
            return subdirLearn;
        }

        public void setSubdirLearn(String subdirLearn) {
            this.subdirLearn = subdirLearn;
        }

        public String getHelpSubdirLearn() {
            return helpSubdirLearn;
        }

        public void setHelpSubdirLearn(String helpSubdirLearn) {
            this.helpSubdirLearn = helpSubdirLearn;
        }

        public String getSubdirOfflineDetect() {
            return subdirOfflineDetect;
        }

        public void setSubdirOfflineDetect(String subdirOfflineDetect) {
            this.subdirOfflineDetect = subdirOfflineDetect;
        }

        public String getHelpSubdirDetect() {
            return helpSubdirDetect;
        }

        public void setHelpSubdirDetect(String helpSubdirDetect) {
            this.helpSubdirDetect = helpSubdirDetect;
        }

        public String getRulesForDetection() {
            return rulesForDetection;
        }

        public void setRulesForDetection(String rulesForDetection) {
            this.rulesForDetection = rulesForDetection;
        }

        public String getRulesForLearning() {
            return rulesForLearning;
        }

        public void setRulesForLearning(String rulesForLearning) {
            this.rulesForLearning = rulesForLearning;
        }

        public String getRulesForSuricataBase() {
            return rulesForSuricataBase;
        }

        public void setRulesForSuricataBase(String rulesForSuricataBase) {
            this.rulesForSuricataBase = rulesForSuricataBase;
        }

        public String getRulesForSuricataLearned() {
            return rulesForSuricataLearned;
        }

        public void setRulesForSuricataLearned(String rulesForSuricataLearned) {
            this.rulesForSuricataLearned = rulesForSuricataLearned;
        }

        public String getLearnedModel() {
            return learnedModel;
        }

        public void setLearnedModel(String learnedModel) {
            this.learnedModel = learnedModel;
        }

        public String getNewLearnedModel() {
            return newLearnedModel;
        }

        public void setNewLearnedModel(String newLearnedModel) {
            this.newLearnedModel = newLearnedModel;
        }

        public String getSystemConfiguration() {
            return systemConfiguration;
        }

        public void setSystemConfiguration(String systemConfiguration) {
            this.systemConfiguration = systemConfiguration;
        }

        public String getCurrentIdsParamsFile() {
            return currentIdsParamsFile;
        }

        public void setCurrentIdsParamsFile(String currentIdsParamsFile) {
            this.currentIdsParamsFile = currentIdsParamsFile;
        }

        public List<String> getIdstraceFlags() {
            return idstraceFlags;
        }

        public void setIdstraceFlags(List<String> idstraceFlags) {
            this.idstraceFlags = idstraceFlags;
        }

        public String getGeneral() {
            return general;
        }

        public void setGeneral(String general) {
            this.general = general;
        }

        public String getIdstraceDir() {
            return idstraceDir;
        }

        public void setIdstraceDir(String idstraceDir) {
            this.idstraceDir = idstraceDir;
        }

        public Boolean getIdstraceOn() {
            return idstraceOn;
        }

        public void setIdstraceOn(Boolean idstraceOn) {
            this.idstraceOn = idstraceOn;
        }

        public Boolean getSendToConsole() {
            return sendToConsole;
        }

        public void setSendToConsole(Boolean sendToConsole) {
            this.sendToConsole = sendToConsole;
        }

        public Boolean getSendToBrowser() {
            return sendToBrowser;
        }

        public void setSendToBrowser(Boolean sendToBrowser) {
            this.sendToBrowser = sendToBrowser;
        }

        public Boolean getShowTicks() {
            return showTicks;
        }

        public void setShowTicks(Boolean showTicks) {
            this.showTicks = showTicks;
        }

        public Boolean getKillPreviousInstance() {
            return killPreviousInstance;
        }

        public void setKillPreviousInstance(Boolean killPreviousInstance) {
            this.killPreviousInstance = killPreviousInstance;
        }

        public String getTapsDir() {
            return tapsDir;
        }

        public void setTapsDir(String tapsDir) {
            this.tapsDir = tapsDir;
        }

        public Integer getHistoryLength() {
            return historyLength;
        }

        public void setHistoryLength(Integer historyLength) {
            this.historyLength = historyLength;
        }

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
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
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
            idstraceFlags = config.get("idstrace_flags").asJsonList().stream().map(Json::asString).toList();
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

    public static class AlertViewer {
        // region define variables
        String ip;
        Integer port;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getLogToFile() {
            return logToFile;
        }

        public void setLogToFile(Boolean logToFile) {
            this.logToFile = logToFile;
        }

        public String getLogDir() {
            return logDir;
        }

        public void setLogDir(String logDir) {
            this.logDir = logDir;
        }

        public String getPrefixFilename() {
            return prefixFilename;
        }

        public void setPrefixFilename(String prefixFilename) {
            this.prefixFilename = prefixFilename;
        }

        public Integer getMaxSizeOfLogFiles() {
            return maxSizeOfLogFiles;
        }

        public void setMaxSizeOfLogFiles(Integer maxSizeOfLogFiles) {
            this.maxSizeOfLogFiles = maxSizeOfLogFiles;
        }

        public Boolean getDoNotResentSameAlert() {
            return doNotResentSameAlert;
        }

        public void setDoNotResentSameAlert(Boolean doNotResentSameAlert) {
            this.doNotResentSameAlert = doNotResentSameAlert;
        }

        public Integer getAlertDuration() {
            return alertDuration;
        }

        public void setAlertDuration(Integer alertDuration) {
            this.alertDuration = alertDuration;
        }

        String name;
        Boolean logToFile;
        String logDir;
        String prefixFilename;
        Integer maxSizeOfLogFiles;
        Boolean doNotResentSameAlert;
        Integer alertDuration;
        // endregion define variables

        public AlertViewer(Json config) {
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
            ip = config.get("ip").asString();
            port = config.get("port").asInteger();
            name = config.get("name").asString();
            logToFile = config.get( "log_to_file").asBoolean();
            logDir = config.get("log_dir").asString();
            prefixFilename = config.get("prefix_filename").asString();
            maxSizeOfLogFiles = config.get("max_size_of_log_files").asInteger();
            doNotResentSameAlert = config.get( "do_not_resent_same_alert").asBoolean();
            alertDuration = config.get("alert_duration").asInteger();
        }
    }


    public static class Autocrypt {
        // region define variables
        String ip;

        public Integer getSyncFrequency() {
            return syncFrequency;
        }

        public void setSyncFrequency(Integer syncFrequency) {
            this.syncFrequency = syncFrequency;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        Integer port;
        Integer syncFrequency;
        // endregion define variables

        public Autocrypt(Json config) {
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
            ip = config.get("ip").asString();
            port = config.get("port").asInteger();
            syncFrequency = config.get("sync_frequency").asInteger();
        }
    }
}
