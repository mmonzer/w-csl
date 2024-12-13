package com.csl.core;

import com.csl.exceptions.WrongConfigurationException;
import com.csl.util.FileUtils;
import com.ucsl.json.Json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
        }
    }

    @Getter
    public static class Scan {
        // region define variables
        @Setter
        String managerIp;
        Integer managerPort;
        String managerProtocol;
        Boolean useSSL;
        // endregion define variables

        public Scan(Json config) {
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
            managerIp = config.get("manager_ip").asString();
            managerPort = config.get("manager_port").asInteger();
            managerProtocol = config.get("manager_protocol").asString();
            useSSL = config.get( "use_ssl").asBoolean();
        }
    }

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

    @Getter
    public static class Server {
        // region define variables
        @Setter
        Boolean on;
        @Setter
        Boolean debug;
        Integer webserverPort;
        Boolean varsCommands;
        Boolean jcmdCommands;
        Boolean databaseCommands;
        Boolean sendAlerts;
        Boolean sendConsoleOutput;
        Integer websocketTimeout;
        // endregion define variables

        public Server(Json config) {
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
            on = config.get( "on").asBoolean();
            debug = config.get( "debug").asBoolean();
            webserverPort = config.get("webserver_port").asInteger();
            varsCommands = config.get( "vars_commands").asBoolean();
            jcmdCommands = config.get( "jcmd_commands").asBoolean();
            databaseCommands = config.get( "database_commands").asBoolean();
            sendAlerts = config.get( "send_alerts").asBoolean();
            sendConsoleOutput = config.get( "send_console_output").asBoolean();
            websocketTimeout = config.get("websocket_timeout").asInteger();
        }
    }

    @Getter
    public static class UdpServerConf {
        // region define variables
        @Setter
        Boolean on;
        @Setter
        String ip;
        @Setter
        Integer port;
        Integer maxInputQueues;
        Integer maxSizeOfInputQueues;
        // endregion define variables

        public UdpServerConf(Json config) {
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
            on = config.get( "on").asBoolean();
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
        Boolean on;
        @Setter
        String general;
        Boolean showTicks;
        Integer historyLength;
        // endregion define variables

        public IdsConf(Json config) {
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
            on = config.get( "on").asBoolean();
            showTicks = config.get( "show_ticks").asBoolean();
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
            if (config ==null) { throw new WrongConfigurationException(WRONG_CONFIGURATION); }
            ip = config.get("ip").asString();
            port = config.get("port").asInteger();
            syncFrequency = config.get("sync_frequency").asInteger();
        }
    }
}
