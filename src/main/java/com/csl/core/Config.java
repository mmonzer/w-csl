package com.csl.core;

import com.csl.util.FileUtils;
import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

public class Config {
    private static final String CONFIG_FILE = "application.json";

    private static final String WRONG_CONFIGURATION = "Wrong configuration";
    private static final String SEPARATOR = "__";
    public static final Config INSTANCE = new Config(CONFIG_FILE);

    public final Client client;
    public final Config.Scan scan;
    public final Config.Status status;
    public final Tap tapService;
    public final Server server;
    public final Config.UdpServerConf udpServerConf;
    public final Config.IdsConf idsConf;
    public final Config.AlertViewer alertViewer;
    public final Config.Autocrypt autocrypt;

    private Config(String configFile) {
        Json jsonConfiguration = readConfig(configFile);

        server = new Server(assertAndGetFrom(jsonConfiguration, "server"));
        client = new Client(assertAndGetFrom(jsonConfiguration, "client"));
        status = new Status(assertAndGetFrom(jsonConfiguration, "status"));
        udpServerConf = new UdpServerConf(assertAndGetFrom(jsonConfiguration, "udp_server_conf"));
        idsConf = new IdsConf(assertAndGetFrom(jsonConfiguration, "ids_conf"));
        alertViewer = new AlertViewer(assertAndGetFrom(jsonConfiguration, "alert_viewer"));
        scan = new Scan(assertAndGetFrom(jsonConfiguration, "discovery_service"));
        tapService = new Tap(assertAndGetFrom(jsonConfiguration, "tap_service"));
        autocrypt = new Autocrypt(assertAndGetFrom(jsonConfiguration, "autocrypt_service"));
    }

    private static @NotNull Json assertAndGetFrom(@Nullable Json jsonConfiguration, @NotNull String property) {
        assert jsonConfiguration != null : WRONG_CONFIGURATION;
        assert jsonConfiguration.has(property) : WRONG_CONFIGURATION;
        assert jsonConfiguration.get(property) != null : WRONG_CONFIGURATION;

        return jsonConfiguration.get(property);
    }

    private Json readConfig(String f) {
        Json jsonConfig = Json.read(readFile(f));

        // Check the environment for existing variables
        Map<String, String> env = System.getenv();

        for (String section : jsonConfig.asJsonMap().keySet()) {
            if (jsonConfig.at(section).isObject()) {
                for (String variable : jsonConfig.at(section).asJsonMap().keySet()) {
                    String envVariable = "CSL" + SEPARATOR + section.toUpperCase() + SEPARATOR + variable.toUpperCase();
                    if (env.containsKey(envVariable)) {
                        jsonConfig.at(section).set(variable, env.get(envVariable));
                    }
                }
            }
        }
        return jsonConfig;
    }

    private static String readFile(String filename) {
        try {
            return FileUtils.readFile(Config.class.getClassLoader().getResource(filename).getPath());
        } catch (IOException e) {
            return null;
        }
    }

    @Getter
    public static class Client {
        // region define variables
        String ipServerRemote;
        Integer portServerRemote;
        String serverRemoteUrlPrefix;
        boolean forceHostNameResolution;
        String apiKey;
        boolean useSsl;
        boolean launchWebApiServer;
        Integer webApiServerPort;
        // endregion define variables

        public Client(Json config) {
            ipServerRemote = config.get("ip_server_remote").asString();
            portServerRemote = config.get("port_server_remote").asInteger();
            serverRemoteUrlPrefix = config.get("server_remote_url_prefix").asString();
            forceHostNameResolution = config.get("force_host_name_resolution").asBoolean();
            apiKey = config.get("api_key").asString();
            useSsl = config.get("use_ssl").asBoolean();
            launchWebApiServer = config.get("launch_web_api_server").asBoolean();
            webApiServerPort = config.get("web_api_server_port").asInteger();
        }
    }

    @Getter
    public static class Scan {
        // region define variables
        @Setter
        String managerIp;
        Integer managerPort;
        String managerProtocol;
        boolean useSsl;
        // endregion define variables

        public Scan(Json config) {
            managerIp = config.get("manager_ip").asString();
            managerPort = config.get("manager_port").asInteger();
            managerProtocol = config.get("manager_protocol").asString();
            useSsl = config.get("use_ssl").asBoolean();
        }
    }

    @Getter
    public static class Status {
        // region define variables
        boolean sendNotifications;
        Integer notificationsPeriod;
        // endregion define variables

        public Status(Json config) {
            sendNotifications = config.get("send_notifications").asBoolean();
            notificationsPeriod = config.get("notifications_period").asInteger();
        }
    }

    @Getter
    public static class Tap {
        // region define variables
        String localIpAddress;
        Integer localPort;
        String knowHostFilePath;
        // endregion define variables

        public Tap(Json config) {
            localIpAddress = config.get("localIpAddr").asString();
            localPort = config.get("localPort").asInteger();
            knowHostFilePath = config.get("knowHostFilePath").asString();
        }
    }

    @Getter
    public static class Server {
        // region define variables
        @Setter
        boolean on;
        @Setter
        boolean debug;
        Integer webserverPort;
        boolean varsCommands;
        boolean jcmdCommands;
        boolean databaseCommands;
        boolean sendAlerts;
        boolean sendConsoleOutput;
        Integer websocketTimeout;
        // endregion define variables

        public Server(Json config) {
            on = config.get("on").asBoolean();
            debug = config.get("debug").asBoolean();
            webserverPort = config.get("webserver_port").asInteger();
            varsCommands = config.get("vars_commands").asBoolean();
            jcmdCommands = config.get("jcmd_commands").asBoolean();
            databaseCommands = config.get("database_commands").asBoolean();
            sendAlerts = config.get("send_alerts").asBoolean();
            sendConsoleOutput = config.get("send_console_output").asBoolean();
            websocketTimeout = config.get("websocket_timeout").asInteger();
        }
    }

    @Getter
    public static class UdpServerConf {
        // region define variables
        @Setter
        boolean on;
        @Setter
        String ip;
        @Setter
        Integer port;
        Integer maxInputQueues;
        Integer maxSizeOfInputQueues;
        // endregion define variables

        public UdpServerConf(Json config) {
            on = config.get("on").asBoolean();
            ip = config.get("ip").asString();
            port = config.get("port").asInteger();
            maxInputQueues = config.get("max_input_queues").asInteger();
            maxSizeOfInputQueues = config.get("max_size_of_input_queues").asInteger();
        }
    }

    @Getter
    public static class IdsConf {
        // region define variables
        @Setter
        boolean on;
        boolean showTicks;
        Integer historyLength;
        // endregion define variables

        public IdsConf(Json config) {
            on = config.get("on").asBoolean();
            showTicks = config.get("show_ticks").asBoolean();
            historyLength = config.get("history_length").asInteger();
        }
    }

    @Getter
    public static class AlertViewer {
        // region define variables
        @Setter
        String ip;
        @Setter
        Integer port;
        @Setter
        String name;
        boolean logToFile;
        String logDir;
        String prefixFilename;
        Integer maxSizeOfLogFiles;
        boolean doNotResentSameAlert;
        Integer alertDuration;
        // endregion define variables

        public AlertViewer(Json config) {
            ip = config.get("ip").asString();
            port = config.get("port").asInteger();
            name = config.get("name").asString();
            logToFile = config.get("log_to_file").asBoolean();
            logDir = config.get("log_dir").asString();
            prefixFilename = config.get("prefix_filename").asString();
            maxSizeOfLogFiles = config.get("max_size_of_log_files").asInteger();
            doNotResentSameAlert = config.get("do_not_resent_same_alert").asBoolean();
            alertDuration = config.get("alert_duration").asInteger();
        }
    }

    @Getter
    public static class Autocrypt {
        // region define variables
        @Setter
        String ip;
        @Setter
        Integer port;
        Integer syncFrequency;
        // endregion define variables

        public Autocrypt(Json config) {
            ip = config.get("ip").asString();
            port = config.get("port").asInteger();
            syncFrequency = config.get("sync_frequency").asInteger();
        }
    }
}
