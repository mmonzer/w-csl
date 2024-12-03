package main.services;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.ids.Tap;
import com.csl.web.ApiHandler;
import com.jcraft.jsch.JSchException;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.extensions.SshUtils;
import main.services.endpoints.TapsEndpoints;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.NoRouteToHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.csl.util.FileUtils.readJsonFromFile;
import static com.csl.web.HTTPConstants.*;

public class TapsServices extends Service {
    private static final String SCRIPTS_DIR = "~/csl/scripts";
    private static final String START_TAPS = "cd " + SCRIPTS_DIR + " && sudo ./launchTap.sh & exit";
    private static final String STOP_TAPS = "cd " + SCRIPTS_DIR + " && sudo ./killTaps.sh";

    private static final String REPLAY = "cd " + SCRIPTS_DIR + " && sudo ./replay.sh ";

    private static final String STOP_SURICATA = "cd " + SCRIPTS_DIR + " && sudo ./killSuricata.sh";
    private static final String START_SURICATA = "cd " + SCRIPTS_DIR + " && sudo ./launchSuricata.sh";
    private static final String CLEAR_SURICATA_LOG = "sudo rm /var/log/suricata/suricata.log";
    private static final String SURICATA_CONF_DIR = "/opt/csl/configSuricata";
    private static final String REMOVE_ADDITIONAL_RULES = "cd " + SCRIPTS_DIR + " && sudo ./removeAdditionnalRules.sh";
    private static final String RELOAD_RULES = "cd " + SCRIPTS_DIR + " && sudo ./reloadSuricataRules.sh";

    static ArrayList<Json> configuredTaps;
    static HashMap<String, Tap> activeTaps = new HashMap<>();
    static String localIP;
    static Integer localPort;
//    static String knownHostFilePath;

    private final ApiHandler apiHandler;

    static String idsconf;

    public Json stopTap(Json params) {
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        activeTaps.get(name).sendCmd("/monitoring",
                Json.read("{\"cmd\":\"monitorStop\"}")).toJson();
        activeTaps.get(name).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataStop\"}")).toJson();
//        taps.get(name).sendCmd("/suricata",
//                Json.read("{\"cmd\":\"suricataStop\"}")).toJson();
        return JsonApiResponse.result(Json.read("{\"msg\":\"Tap " + name + " was successfully stopped\"}")).toJson();
    }

    public Json startTap(Json params) {
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        activeTaps.get(name).sendCmd("/monitoring",
                Json.read("{\"cmd\":\"monitorStart\"}")).toJson();
        activeTaps.get(name).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataStart\"}")).toJson();
//        taps.get(name).sendCmd("/suricata",
//                Json.read("{\"cmd\":\"suricataStart\"}")).toJson();
        return JsonApiResponse.result(Json.read("{\"msg\":\"Tap " + name + " was successfully started\"}")).toJson();
    }

    public static Json startReplay(String name, String pcap) {
        String id = "", password = "";
        String ip = null;
        int port = 22;

        for (Json j : configuredTaps) {
            if (j.at("idname").asString().contentEquals(name)) {
                name = j.get("idname").asString();
                ip = j.at("ip").asString();
                id = j.at("username").asString();
                password = j.at("password").asString();
                try {
                    port = j.at("port").asInteger();
                } catch (NullPointerException e) {
                    System.out.println("Using default SSH port (22)");
                }
            }
        }
        System.out.println("Start script replay <" + pcap + "> on tap :" + name + " " + ip);
        SshUtils ssh = new SshUtils(id, password, ip, port/*,knownHostFilePath*/);
        String command = REPLAY + pcap;
        System.out.println("Command :" + command);
        String output = null;
        try {
            output = ssh.remoteExecNoWait(command);
        } catch (JSchException | IOException e) {
            // e.printStackTrace();
        }
        ssh.endConnection();

        Json out = Json.object();
        out.at("result", output);
        return out;
    }

    public static Json clearLogs(String name) {
        String id = "", password = "";
        String ip = null;
        int port = 22;
        for (Json j : configuredTaps) {
            if (j.at("idname").asString().contentEquals(name)) {
                ip = j.at("ip").asString();
                id = j.at("username").asString();
                password = j.at("password").asString();
                try {
                    port = j.at("port").asInteger();
                } catch (NullPointerException e) {
                    System.out.println("Using default SSH port (22)");
                }
            }
        }
        SshUtils ssh = new SshUtils(id, password, ip, port/*,knownHostFilePath*/);
        String command = CLEAR_SURICATA_LOG;
        String output = null;
        try {
            output = ssh.remoteExec(command);
        } catch (JSchException | IOException e) {
            // e.printStackTrace();
        }
        ssh.endConnection();

        Json out = Json.object();
        out.at("result", output);
        return out;
    }

    private static void writeToFile(String s, String path) throws IOException {
        try (FileWriter myWriter = new FileWriter(path)) {
            myWriter.write(s);
        }
    }

    private static void writeToFile(HashMap<String, Tap> t, String path) throws IOException {
        StringBuilder s = new StringBuilder("[");
        for (Tap tap : t.values()) {
            s.append("{");
            s.append("\"idname\":\"").append(tap.getName()).append("\",");
            s.append("\"id\":\"").append(tap.getId()).append("\",");
            s.append("\"ip\":\"").append(tap.getIp()).append("\",");
            s.append("\"port\":\"").append(tap.getPort()).append("\",");
            s.append("\"includes\":").append(tap.getIncludes());
            s.append("},");
        }
        s.deleteCharAt(s.length() - 1);
        s.append("]");
        writeToFile(s.toString(), path);
    }

    public static void newTap(String name) {
        File theDir = new File(idsconf + "/taps/" + name);
        if (!theDir.exists()) {
            theDir.mkdirs();
        }
        Json j = Json.object();
        j.at("idname", name);
        j.at("id", name);
        j.at("includes", new ArrayList<>());

        configuredTaps.add(j);

        Json basicConf = null;
        try {
            basicConf = readJsonFromFile(idsconf + "/taps/basicNetworkConf.json");
            basicConf.at("id", name);
            basicConf.at("csl_node_ip", localIP);
            basicConf.at("csl_node_port", localPort);
            writeToFile(basicConf.toString(), idsconf + "/taps/" + name + "/tapReseau.json");
        } catch (IOException e) {
            // e.printStackTrace();
        }

        try {
            basicConf = readJsonFromFile(idsconf + "/taps/basicProcessConf.json");
            basicConf.at("id", name);
            for (Json jj : basicConf.at("modules").asJsonList()) {
                if (jj.at("name").asString().contentEquals("module2")) {
                    jj.at("config").at("host_target", localIP);
                    jj.at("config").at("port_target", localPort);
                }
            }

            writeToFile(basicConf.toString(), idsconf + "/taps/" + name + "/tapProcess.json");
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    /**
     * Adds new tap to the list of active Taps. If its present, it modifies it
     *
     * @param params parameters with the information of the new Tap
     * @return the new tap
     */
    public Json newTap(Json params) {
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        int port;
        if (!params.has("port") || !params.get("port").isNumber()) {
            port = 8888;
        } else {
            port = params.get("port").asInteger();
        }
        String ip;
        if (!params.has("ip") || !params.get("ip").isString()) {
            ip = "127.0.0.1";
        } else {
            ip = params.get("ip").asString();
        }

        Tap newTap;
        if (activeTaps.values().stream().anyMatch(t -> t.getId().equals(name))) {
            newTap = activeTaps.get("name");
            newTap.setIp(ip);
            newTap.setPort(port);
            configuredTaps.removeIf((e) -> e.get("idname").asString().equals(name));
        } else {
            newTap = new Tap(name, name, ip, port, new ArrayList<>());
        }

        configuredTaps.add(newTap.toJson());

        try {
            writeToFile(configuredTaps.toString(), idsconf + "/taps/TapsConfiguration.json");
        } catch (IOException ignored) {
        }

        activeTaps.put(newTap.getId(), newTap);

        return newTap.toJson();
    }

    /**
     * Deletes new tap to the list of active Taps
     *
     * @param params parameters with the information of the new Tap
     * @return the new tap
     */
    public Json deleteTap(Json params) {
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();

        configuredTaps.removeIf((e) -> e.get("idname").asString().equals(name));

        try {
            writeToFile(configuredTaps.toString(), idsconf + "/taps/TapsConfiguration.json");
        } catch (IOException ignored) {
        }

        activeTaps.remove(name);


        return JsonApiResponse.result(Json.make(configuredTaps)).toJson();
    }

    public static void deleteTap(String name) {
        ArrayList<Json> tapsclone = (ArrayList<Json>) configuredTaps.clone();
        for (Json j : configuredTaps) {
            if (j.at("idname").asString().contentEquals(name)) {
                tapsclone.remove(j);
            }
        }
        configuredTaps = tapsclone;
        try {
            FileUtils.deleteDirectory(new File(idsconf + "/taps/" + name));
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    /**
     * Changes the IP to contact the tap API. It changes the configuration if the CSL-Client side and
     * records it into a file.
     *
     * @param name of the tap
     * @param ip   new ip of the tap API
     * @return an API response with the new tap configuration if the field result.
     */
    public Json setIp(String name, String ip) {
        // Check name in active taps
        if (!activeTaps.containsKey(name)) {
            return Json.object();
        }


        // TODO  deal better with wrong IP
        // Update the ip in the tap, and roll up if unreachable.
//        String previousIp = activeTaps.get(name).getIp();
        activeTaps.get(name).setIp(ip);
//        try {
//            if (activeTaps.get(name).sendCmd("/config", "{\"cmd\":\"getConfig\"}").getResult()==null) {
//                throw new NoRouteToHostException();
//            }
//        } catch (Exception e) {
//            activeTaps.get(name).setIp(previousIp);
//            return JsonApiResponse.error("Unreachable IP. Very likely: the IP is wrong, firewall is blocking the connexion or an intermediate server is down.").toJson();
//        }


        // Save configuration to file
        try {
            writeToFile(activeTaps, idsconf + "/taps/TapsConfiguration.json");
            return JsonApiResponse.result(activeTaps.get(name).toJson()).toJson();
        } catch (IOException ignore) {
            return JsonApiResponse.error("Configuration file not found.").toJson();
        }
//        Json tap = configuredTaps.stream().filter(t -> t.get("idname").asString().equals(name)).findFirst().orElse(null);
//        try {
//            tap.set("ip", ip);
//            Json write = Json.object();
//            write.at("write", configuredTaps);
//            writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
//            return tap;
//        } catch (NullPointerException |IOException ignore) {
//            return JsonApiResponse.error("Wrong tap name").toJson();
//        }
    }

    /**
     * Changes the IP to contact the tap API. It changes the configuration if the CSL-Client side and
     * records it into a file.
     *
     * @param name of the tap
     * @param port new port to contact the tap API
     * @return an API response with the new tap configuration if the field result.
     */
    public Json setPort(String name, int port) {
        // Check name in active taps
        if (!activeTaps.containsKey(name)) {
            return Json.object();
        }

        // Update the ip in the tap, and roll up if unreachable.
        int previousPort = activeTaps.get(name).getPort();
        activeTaps.get(name).setPort(port);
        try {
            if (activeTaps.get(name).sendCmd("/config", "{\"cmd\":\"getConfig\"}").getResult() == null) {
                throw new NoRouteToHostException();
            }
        } catch (Exception e) {
            activeTaps.get(name).setPort(previousPort);
            return JsonApiResponse.error("Unreachable port. Very likely: the port is wrong, firewall is blocking the connexion or an intermediate server is down.").toJson();
        }

        // Save configuration to file
        try {
            writeToFile(activeTaps, idsconf + "/taps/TapsConfiguration.json");
            return JsonApiResponse.result(activeTaps.get(name).toJson()).toJson();
        } catch (IOException ignore) {
            return JsonApiResponse.error("Configuration file not found.").toJson();
        }
    }

    /**
     * Gets the file containing the IDS logs of the given tap
     *
     * @param params general params of the request
     * @return the logs file of the ids
     */
    public Json getSuricataLogs(Json params) {
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured tap").toJson();
        }
        return activeTaps.get(name).getFile("/suricata",
                Json.read("{\"cmd\":\"suricataGetLogFile\"}"));
    }

    /**
     * Gets the IDS logs of the given tap
     *
     * @param name name of the tap
     * @return the last logs of the ids
     */
    public Json clearSuricataLogs(String name) {
        return activeTaps.get(name).sendCmd("/suricata", "{\"cmd\":\"suricataClearLogs\"}").toJson().get("result").get("result");
    }

    public static Json getConfFromtap(String name, String file) {
        String username = "", password = "";
        int port = 22;

        Json result = Json.object();
        for (Json j : configuredTaps) {
            System.out.println(j);
            if (j.at("idname").asString().contentEquals(name)) {
                String ip = j.at("ip").asString();
                username = j.at("username").asString();
                password = j.at("password").asString();
                try {
                    port = j.at("port").asInteger();
                } catch (NullPointerException e) {
                    System.out.println("Using default SSH port (22)");
                }
                System.out.println("id " + username + " password " + password + " ip " + ip);
                SshUtils ssh = new SshUtils(username, password, ip, port/*,knownHostFilePath*/);
                try {
                    switch (file) {
                        case "reseau":
                            ssh.getFile("/home/" + username + "/csl/tapReseau/conf.json", idsconf + "/taps/" + name + "/tapReseau.json");
                            break;
                        case "process":
                            ssh.getFile("/home/" + username + "/csl/tapProcess/CSLConfigTAPProcess.json", idsconf + "/taps/" + name + "/tapProcess.json");
                            break;
                        case "suricataconf":
                            ssh.getFile("/home/" + username + "/csl/configSuricata/suricata/suricata.yaml", idsconf + "/taps/" + name + "/suricata.yaml");
                            break;
                        case "genrules":
                            ssh.getFile("/home/" + username + "/csl/configSuricata/suricata/rules/csl.rules", idsconf + "/taps/" + name + "/genrules.rules");
                            break;
                        case "baserules":
                            ssh.getFile("/home/" + username + "/csl/configSuricata/suricata/rules/cslbase.rules", idsconf + "/taps/" + name + "/baserules.rules");
                            break;
                    }
                } catch (IOException | JSchException e) {
                    // e.printStackTrace();
                }
                ssh.endConnection();
            }
        }


        return result;
    }

    /**
     * Gets the configuration of the only TAP
     *
     * @return the configuration of the TAP
     */
    public Json getConfFromTap(Json params) {
        // Check if name of tap ok
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }

        return activeTaps.get(name).sendCmd("/config",
                Json.read("{\"cmd\":\"getConfig\"}")).toJson().get("result");
    }

    /**
     * Gets the configuration of the one tap. It's the configuration tof CSLClient to connect the tap
     *
     * @return the configuration of the tap
     */
    public Json getTapConf(Json params) {
//        try {
//            return readJsonFromFile(idsconf + "/taps/TapsConfiguration.json");
//        } catch (IOException e) {
//            // e.printStackTrace();
//        }
//        return Json.object();

        // Check if name of tap ok
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }

        return activeTaps.get(name).toJson();
    }

    /**
     * Gets the configuration of the one tap. It's the configuration tof CSLClient to connect the tap
     *
     * @return the configuration of the tap
     */
    public Json getTapConf_old(Json params) {
        try {
            return readJsonFromFile(idsconf + "/taps/TapsConfiguration.json");
        } catch (IOException e) {
            // e.printStackTrace();
        }
        return Json.object();
    }

    /**
     * Gets the configuration of the all tap. It's the configuration tof CSL-Client to connect the tap
     *
     * @return the configuration of the taps
     */
    public Json getTapsConf(Json params) {
        Json response = Json.object();
        for (Tap tap : activeTaps.values()) {
            response.at(tap.getId(), tap.toJson());
        }

        return response;
    }

    public Json sendConfToTap(Json params) {
        // Check if name of tap ok
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        Tap tap = activeTaps.get(name);

        params.delAt("name");
        Json output = tap.sendCmd("/config",
                Json.read("{\"cmd\":\"updateConfig\", \"params\":" + params.toString() + "}")).toJson();
        Json result = Json.object();
        result.at("idname", tap.getName());
        result.at("id", tap.getId());
        result.at("result", output);

        if (output.has("([Ee]rror)|([Pp]roblem)")) {
            result.at("error", true);
        }

        return result;
    }

    public static void sendIncludes(String name) {
        String username = "", password = "";
        int port = 22;

        for (Json j : configuredTaps) {
            if (j.at("idname").asString().contentEquals(name)) {
                String ip = j.at("ip").asString();
                username = j.at("username").asString();
                password = j.at("password").asString();
                try {
                    port = j.at("port").asInteger();
                } catch (NullPointerException e) {
                    System.out.println("Using default SSH port (22)");
                }
                // Declaration
                try {
                    Json includes = j.at("includes");
                    ArrayList<String> ruleFiles = new ArrayList<String>();
                    Json includesRaw = readJsonFromFile(idsconf + "/taps/includesConfiguration.json");
                    for (Json jj : includesRaw.asJsonList()) {
                        for (Json jjj : includes.asJsonList()) {
                            if (jjj.asInteger() == jj.at("id").asInteger()) {
                                for (Json jjjj : jj.at("files").asJsonList()) {
                                    ruleFiles.add(jjjj.asString());
                                }
                            }
                        }
                    }
                    SshUtils ssh = new SshUtils(username, password, ip, port/*,knownHostFilePath*/);
                    // ssh.remoteExec("sudo rm /home/"+username+"/configSuricata/suricata/rules/additionnalRules/*.rules");
                    ssh.remoteExec(REMOVE_ADDITIONAL_RULES);
                    ssh.endConnection();
                    String yamlFile = "";
                    yamlFile += "%YAML 1.1\r\n---\r\n- csl.rules\r\n- cslbase.rules\r\n";
                    for (String ruleFile : ruleFiles) {
                        System.out.println("Sending " + ruleFile);
                        yamlFile += "- /home/" + username + "/configSuricata/suricata/rules/additionnalRules/" + ruleFile + "\r\n";
                        ssh = new SshUtils(username, password, ip, port/*,knownHostFilePath*/);
                        ssh.sendFile(idsconf + "/taps/rules/" + ruleFile, "/home/" + username + "/configSuricata/suricata/rules/additionnalRules/" + ruleFile);
                        ssh.endConnection();
                    }
                    writeToFile(yamlFile, idsconf + "/taps/" + name + "/includes.yaml");
                    ssh = new SshUtils(username, password, ip, port/*,knownHostFilePath*/);
                    ssh.sendFile(idsconf + "/taps/" + name + "/includes.yaml", "/home/" + username + "/configSuricata/suricata/includes.yaml");
                    ssh.endConnection();
                } catch (IOException | JSchException e) {
                    // e.printStackTrace();
                }
            }
        }
    }

    public static void setUsernamePassword(String name, String username, String password) {
        for (Json j : configuredTaps) {
            if (j.at("idname").asString().contentEquals(name)) {
                j.set("password", password);
                j.set("username", username);
            }
        }
    }

    public static void setNetworkName(String name, String networkName) {
        for (Json j : configuredTaps) {
            if (j.at("idname").asString().contentEquals(name)) {
                j.at("networkName", networkName);
            }
        }
    }

    /**
     * Get the interfaces sniffed by Suricata in the given TAP name
     *
     * @param params params of the request
     * @return the result of the change
     */
    public Json getInterfacesIdsTap(Json params) {
        // Check if name of tap ok
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }

        return activeTaps.get(name).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataGetInterfaces\"}")).toJson().get("result");
    }

    /**
     * Changes the interfaces sniffed by Suricata in the given TAP name
     *
     * @param params params of the request
     * @return the result of the change
     */
    public Json setInterfacesIdsTap(Json params) {
        // Check if name of tap ok
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        // Check if interfaces ok
        if (!params.has("interfaces")) {
            return JsonApiResponse.error("List of new interfaces must be included as 'interfaces'").toJson();
        }
        Json interfaces = params.get("interfaces");
        if (!interfaces.isArray()) {
            return JsonApiResponse.error("Interfaces must be a list").toJson().get("result");
        }

        return activeTaps.get(name).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataSetInterfaces\",\"params\":{\"interfaces\":" + interfaces + "}}")).toJson().get("result");
    }

    /**
     * Starts the Suricata of the given TAP name
     *
     * @param params parameters of the request
     * @return the result of the starting
     */
    public Json startSuricata(Json params) {
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        return JsonApiResponse.result(activeTaps.get(name).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataStart\"}")).getResult().get("result")).toJson();
    }

    /**
     * Stops the Suricata of the given TAP name
     *
     * @param params parameters of the request
     * @return the result of the stopping
     */
    public Json stopSuricata(Json params) {
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        return JsonApiResponse.result(activeTaps.get(name).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataStop\"}")).getResult().get("result")).toJson();
    }

    /**
     * Starts the Monitor of the given TAP name
     *
     * @param params name of the TAP
     * @return the result of the starting
     */
    public Json startMonitor(Json params) {
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        return activeTaps.get(name).sendCmd("/monitoring",
                Json.read("{\"cmd\":\"monitorStart\"}")).toJson();
    }

    /**
     * Stops the Monitor of the given TAP name
     *
     * @param params name of the TAP
     * @return the result of the stopping
     */
    public Json stopMonitor(Json params) {
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        return activeTaps.get(name).sendCmd("/monitoring",
                Json.read("{\"cmd\":\"monitorStop\"}")).toJson();
    }

    private static Json reloadRulesParseOutput(String output) {
        if (output == null) {
            return Json.object();
        }
        Json out = Json.object();
        if (output.startsWith("{")) {
            Json result = Json.read(output);
            out.at("result", result);
            if (!result.at("return").asString().equals("OK")) {
                out.at("error", true);
            }
        } else {
            Json result = Json.object();
            result.at("return", "NOK");
            result.at("message", output);
            out.at("result", result);
            out.at("error", true);
        }
        return out;
    }

    public static Json reloadRules(String name) {
        String id = "", password = "";
        int port = 22;

        String ip = null;
        for (Json j : configuredTaps) {
            if (j.at("idname").asString().contentEquals(name)) {
                ip = j.at("ip").asString();
                id = j.at("username").asString();
                password = j.at("password").asString();
                try {
                    port = j.at("port").asInteger();
                } catch (NullPointerException e) {
                    System.out.println("Using default SSH port (22)");
                }
            }
        }
        SshUtils ssh = new SshUtils(id, password, ip, port/*,knownHostFilePath*/);
        String command = RELOAD_RULES;
        String output = null;
        try {
            output = ssh.remoteExec(command);
        } catch (JSchException | IOException e) {
            // e.printStackTrace();
        }
        ssh.endConnection();

        return reloadRulesParseOutput(output);
    }

    /**
     * Default constructor of the Suricata service.
     */
    public TapsServices() {
        this("taps",
                "Service for managing the different CSL-Probes: adding, configuring, ...",
                "ssh_service");
    }

    /**
     * Generic constructor of the Suricata service.
     */
    public TapsServices(String name, String description, String configFileSectionName) {
        super(name, description, configFileSectionName);
        apiHandler = new ApiHandler("CSL-Tap", "localhost",8888, false);
    }

    public String getTapName(Json j) {

        String n = JsonUtil.getStringFromJson(j, "name", "");
        return n;
    }

    public String tapNameHasError(Json j) {

        String n = JsonUtil.getStringFromJson(j, "name", "");

        if (n.isEmpty()) return "No tap name";

        String x = idsconf + "/taps/" + n;

        File file = new File(x);

        if (!file.isDirectory()) return "No directory for tap " + n;

        return "";
    }

    public String missingParams(Json j, String... params) {

        String e = "";
        for (String s : params) {
            if (!j.has(s)) {
                if (!e.isEmpty()) s = s + ",";
            }
        }

        if (!e.isEmpty()) e = "Missing params:" + e;
        return e;
    }

    public static Json downloadFile2(String serverUrl, String outputFilePath) throws Exception {
        // Create and start the HttpClient
        HttpClient httpClient = new HttpClient();
        httpClient.start();

        Json responseJson = Json.object();
        try {
            // Create a POST request to the server
            Request request = httpClient.newRequest(serverUrl)
                    .method(HttpMethod.POST)
                    .content(new StringContentProvider("{\"cmd\":\"suricataGetLogFile\"}"), JSON_FORMAT);

            // Send the request and get the response
            InputStreamResponseListener listener = new InputStreamResponseListener();
            request.send(listener);

            Response response = listener.get(30, TimeUnit.SECONDS); // Wait for the response
            if (response.getHeaders().contains(CONTENT_TYPE)) {
                responseJson.at(CONTENT_TYPE, response.getHeaders().getField(CONTENT_TYPE).getValue());
            }
            if (response.getHeaders().contains(CONTENT_DISPOSITION)) {
                responseJson.at(CONTENT_DISPOSITION, response.getHeaders().getField(CONTENT_DISPOSITION).getValue());
            }
            String strResponse = "";
            // Check if the response status is OK (200)
            if (response.getStatus() == 200) {
                try (InputStream inputStream = listener.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        strResponse += new String(buffer);
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    responseJson.at("Content", strResponse);
                }
            } else {
                System.err.println("Failed to download file: " + response.getStatus());
                responseJson.at("success", "false");
                responseJson.at("error", "Failed to download the file");
            }
        } finally {
            httpClient.stop();
        }
        return responseJson;
    }

    /**
     * Configures the TAP entities from file. It saves the information in a configuredTaps json list (DEPRECATED) and
     * in a activeTaps hashmap.
     * @return the active Taps hashmap
     */
    public static HashMap<String, Tap> configureTaps() {
        idsconf = CSLContext.getInstance().getCslConfDir();
        Json conf;
        Tap tap;
        HashMap<String, Tap> activeTaps = new HashMap<>();
        try {
            conf = readJsonFromFile(idsconf + "/taps/TapsConfiguration.json");
            if (conf.isArray()) {
                configuredTaps = (ArrayList<Json>) conf.asJsonList();
            } else {
                configuredTaps = new ArrayList<>();
            }
            for (Json j : configuredTaps) {
                tap = new Tap(j.at("idname").asString(),
                        j.at("tap_id").asString(),
                        j.at("ip").asString(),
                        j.at("port").asInteger(),
                        j.at("includes").asJsonList()
                );
                activeTaps.put(tap.getId(), tap);
            }
        } catch (IOException e1) {
            System.err.println("No tap config found");
            configuredTaps = new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Unable to parse conf or No tap config found in " + idsconf + "/taps/TapsConfiguration.json");
            configuredTaps = new ArrayList<>();
        }
        return activeTaps;
    }

    /**
     * Initialization of the TAPs commands
     *
     * @return true if the initialization happened with no problems, false otherwise.
     */
    @Override
    public boolean init() {
        Config.Tap config = Config.instance.TapService;
        activeTaps = configureTaps();
//        knownHostFilePath = jConfig.at("knowHostFilePath").asString();
//        localIP = jConfig.at("localIpAddr").asString(););
        localIP = config.getLocalIpAddress();
//        localPort = jConfig.at("localPort").asString();
        localPort = config.getLocalPort();

        addCmd(TapsEndpoints.NEW_TAP, this::newTap);

        addCmd(TapsEndpoints.NEW_TAP_DEPRECATED, this::newTap);

        addCmd(TapsEndpoints.TAP_NUMBER, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                Json j = Json.object();
                j.at("number", configuredTaps.size());
                return j;
            }
        });

        addCmd(TapsEndpoints.TAP_NUMBER_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                Json j = Json.object();
                j.at("number", configuredTaps.size());
                return j;
            }
        });

        addCmd(TapsEndpoints.DELETE_TAP, this::deleteTap);

        addCmd(TapsEndpoints.DELETE_TAP_DEPRECATED, this::deleteTap);

        addCmd(TapsEndpoints.SET_IP, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                if (!params.has("name")) {
                    return JsonApiResponse.error("Tap's name missing from params").toJson();
                }
                if (!params.has("ip")) {
                    return JsonApiResponse.error("Tap's new ip missing from params").toJson();
                }
                return setIp(params.at("name").asString(), params.at("ip").asString());
            }
        });

        addCmd(TapsEndpoints.SET_IP_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                if (!params.has("name")) {
                    return JsonApiResponse.error("Tap's name missing from params").toJson();
                }
                if (!params.has("ip")) {
                    return JsonApiResponse.error("Tap's new ip missing from params").toJson();
                }
                return setIp(params.at("name").asString(), params.at("ip").asString());
//                    Json write = Json.object();
//                    write.at("write", configuredTaps);
//                    try {
//                        writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
//                    } catch (IOException e) {
//                        // e.printStackTrace();
//                    }
//                    return Json.object();
            }
        });

        addCmd(TapsEndpoints.SET_PORT, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                if (!params.has("name")) {
                    return JsonApiResponse.error("Tap's name missing from params").toJson();
                }
                if (!params.has("port")) {
                    return JsonApiResponse.error("Tap's new port missing from params").toJson();
                }
                return setPort(params.at("name").asString(), params.at("port").asInteger());
            }
        });

        addCmd(TapsEndpoints.SET_PORT_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                if (!params.has("name")) {
                    return JsonApiResponse.error("Tap's name missing from params").toJson();
                }
                if (!params.has("port")) {
                    return JsonApiResponse.error("Tap's new port missing from params").toJson();
                }
                return setPort(params.get("name").asString(), params.get("port").asInteger());
            }
        });

        addCmd(TapsEndpoints.SET_USERNAME_PASSWORD, new IJsonCmd() {
            @Override
            public Json exec(Json params) {

                setUsernamePassword(params.at("name").asString(), params.at("username").asString(), params.at("password").asString());
                Json write = Json.object();
                write.at("write", configuredTaps);
                try {
                    writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.SET_USERNAME_PASSWORD_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {

                setUsernamePassword(params.at("name").asString(), params.at("username").asString(), params.at("password").asString());
                Json write = Json.object();
                write.at("write", configuredTaps);
                try {
                    writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.SET_NETWORK_NAME, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                setNetworkName(params.at("name").asString(), params.at("networkName").asString());
                Json write = Json.object();
                write.at("write", configuredTaps);
                try {
                    writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.SET_NETWORK_NAME_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                setNetworkName(params.at("name").asString(), params.at("networkName").asString());
                Json write = Json.object();
                write.at("write", configuredTaps);
                try {
                    writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.GET_CONF_FROM_TAP, this::getConfFromTap);

        addCmd(TapsEndpoints.GET_CONF_FROM_TAP_DEPRECATED, this::getConfFromTap);

        addCmd(TapsEndpoints.SEND_CONF_TO_TAP, this::sendConfToTap);

        addCmd(TapsEndpoints.SEND_CONF_TO_TAP_DEPRECATED, this::sendConfToTap);

        addCmd(TapsEndpoints.GET_TAP_CNX_CONF, this::getTapConf);

        addCmd(TapsEndpoints.GET_TAP_CNX_CONF_DEPRECATED, this::getTapConf_old);

        addCmd(TapsEndpoints.GET_ALL_TAP_CNX_CONF, this::getTapsConf);

        addCmd(TapsEndpoints.GET_ALL_TAP_CNX_CONF_DEPRECATED, this::getTapsConf);

        addCmd(TapsEndpoints.GET_TAP_STATE, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                Json j = Json.object();
                j.at("state", "IDLE");
                return j;
            }
        });

        addCmd(TapsEndpoints.GET_TAP_STATE_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                Json j = Json.object();
                j.at("state", "IDLE");
                return j;
            }
        });

        addCmd(TapsEndpoints.START_TAP, this::startTap);

        addCmd(TapsEndpoints.START_TAP_DEPRECATED, this::startTap);

        addCmd(TapsEndpoints.STOP_TAP, this::stopTap);

        addCmd(TapsEndpoints.STOP_TAP_DEPRECATED, this::stopTap);

        addCmd(TapsEndpoints.RESTART_TAP, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                stopTap(params);
                return startTap(params);
            }
        });

        addCmd(TapsEndpoints.RESTART_TAP_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                stopTap(params);
                return startTap(params);
            }
        });

        addCmd(TapsEndpoints.GET_POSSIBLE_INTERFACES_TAP, this::getPossibleInterfaces);

        addCmd(TapsEndpoints.SET_INTERFACES_IDS_TAP, this::setInterfacesIdsTap);

        addCmd(TapsEndpoints.GET_INTERFACES_IDS_TAP, this::getInterfacesIdsTap);

        addCmd(TapsEndpoints.REPLAY, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                String name = JsonUtil.getStringFromJson(params, "name", "???");
                String pcap = JsonUtil.getStringFromJson(params, "pcap_file", "???");

                return startReplay(name, pcap);
            }
        });

        addCmd(TapsEndpoints.START_IDS, this::startSuricata);

        addCmd(TapsEndpoints.START_IDS_DEPRECATED, this::startSuricata);

        addCmd(TapsEndpoints.STOP_IDS, this::stopSuricata);

        addCmd(TapsEndpoints.STOP_IDS_DEPRECATED, this::stopSuricata);

        addCmd(TapsEndpoints.START_MONITOR, this::startMonitor);

        addCmd(TapsEndpoints.STOP_MONITOR, this::stopMonitor);

        addCmd(TapsEndpoints.RELOAD_ALL_TAPS_RULES, this::reloadAllTapsRules);

        addCmd(TapsEndpoints.RELOAD_ALL_TAPS_RULES_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {

                List<Json> allTapsOutputs = new ArrayList<>();
                boolean gotError = false;

                for (Json j : configuredTaps) {
                    String output = "";

                    output = apiHandler.sendPost("/suricata",
                            Json.read("{\"cmd\":\"suricataReloadRules\"}")).toString();

                    Json result = Json.object();
                    result.at("idname", j.at("idname").asString());
                    result.at("result", reloadRulesParseOutput(output));

                    if (result.at("result").has("error")) {
                        gotError = true;
                    }

                    allTapsOutputs.add(result);
                }

                Json out = Json.object();
                out.at("result", allTapsOutputs);
                if (gotError) {
                    out.at("error", true);
                }
                return out;
            }
        });

        addCmd(TapsEndpoints.RELOAD_RULES, this::reloadRules);

        addCmd(TapsEndpoints.RELOAD_RULES_DEPRECATED, this::reloadRules);
        // Setter et getter de l'édition de Json graphique
        addCmd(TapsEndpoints.GET_PROCESS_JSON, new IJsonCmd() {
                    @Override
                    public Json exec(Json params) {
                        try {
                            return readJsonFromFile(idsconf + "/taps/" + params.at("name").asString() + "/tapProcess.json");
                        } catch (IOException e) {
                            // e.printStackTrace();
                        }
                        return Json.object();
                    }
                }
        );
        // Setter et getter de l'édition de Json graphique
        addCmd(TapsEndpoints.GET_PROCESS_JSON_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                try {
                    return readJsonFromFile(idsconf + "/taps/" + params.at("name").asString() + "/tapProcess.json");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.GET_NETWORK_JSON, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                try {
                    return readJsonFromFile(idsconf + "/taps/" + params.at("name").asString() + "/tapReseau.json");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.GET_NETWORK_JSON_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                // try {

                Json result = getSuricataConf(params);
                result.set("result", result.at("result").asString().substring(0, 63550));
                return result;
                // return readJsonFromFile(idsconf + "/taps/" + params.at("name").asString() + "/tapReseau.json");
                   /* } catch (IOException e) {
                        // e.printStackTrace();
                    }
                    return Json.object();*/
            }
        });

        addCmd(TapsEndpoints.SET_PROCESS_JSON, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                try {
                    writeToFile(params.at("conf").toString(), idsconf + "/taps/" + params.at("name").asString() + "/tapProcess.json");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.SET_PROCESS_JSON_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                try {
                    writeToFile(params.at("conf").toString(), idsconf + "/taps/" + params.at("name").asString() + "/tapProcess.json");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.SET_NETWORK_JSON, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                try {
                    writeToFile(params.at("conf").toString(), idsconf + "/taps/" + params.at("name").asString() + "/tapReseau.json");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.SET_NETWORK_JSON_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                try {
                    writeToFile(params.at("conf").toString(), idsconf + "/taps/" + params.at("name").asString() + "/tapReseau.json");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.GET_SURICATA_STATUS, this::getSuricataStatus);

        addCmd(TapsEndpoints.GET_SURICATA_STATUS_DEPRECATED, this::getSuricataStatus);

        addCmd(TapsEndpoints.GET_SURICATA_CONF, this::getSuricataConf);

        addCmd(TapsEndpoints.GET_SURICATA_CONF_DEPRECATED, this::getSuricataConf);

        addCmd(TapsEndpoints.SET_SURICATA_CONF, this::setSuricataConf);

        addCmd(TapsEndpoints.SET_SURICATA_CONF_DEPRECATED, this::setSuricataConf);

        addCmd(TapsEndpoints.RESET_SURICATA_CONF, this::resetSuricataConf);

        addCmd(TapsEndpoints.GET_BASE_RULES, this::getBaseRules);

        addCmd(TapsEndpoints.GET_BASE_RULES_DEPRECATED, this::getBaseRules);

        addCmd(TapsEndpoints.GET_GENERATED_RULES, this::getGenRules);

        addCmd(TapsEndpoints.GET_GENERATED_RULES_DEPRECATED, this::getGenRules);

        addCmd(TapsEndpoints.MODIFY_GENERATED_RULES, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                if (!params.has("name")) {
                    return JsonApiResponse.error("Tap's name missing from params").toJson();
                }
                if (!params.has("rules")) {
                    return JsonApiResponse.error("Rules are missing from params").toJson();
                }
                if (!activeTaps.containsKey(params.get("name").asString())) {
                    return JsonApiResponse.error("Tap's name does not correspond to a configured tap").toJson();
                }
                return activeTaps.get(params.get("name").asString()).sendCmd("/suricata",
                        Json.read("{\"cmd\":\"suricataModifyCustomRules\",\"params\":" + params + "}")).toJson();
            }
        });

        addCmd(TapsEndpoints.MODIFY_GENERATED_RULES_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                return apiHandler.sendPost("/suricata",
                        Json.read("{\"cmd\":\"suricataModifyCustomRules\",\"params\":" + params + "}")).toJson();
            }
        });

        addCmd(TapsEndpoints.MODIFY_BASE_RULES, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                if (!params.has("name")) {
                    return JsonApiResponse.error("Tap's name missing from params").toJson();
                }
                if (!params.has("rules")) {
                    return JsonApiResponse.error("Rules are missing from params").toJson();
                }
                if (!activeTaps.containsKey(params.get("name").asString())) {
                    return JsonApiResponse.error("Tap's name does not correspond to a configured tap").toJson();
                }
                return activeTaps.get(params.get("name").asString()).sendCmd("/suricata",
                        Json.read("{\"cmd\":\"suricataModifyBaseRules\",\"params\":" + params + "}")).toJson();
            }
        });

        addCmd(TapsEndpoints.MODIFY_BASE_RULES_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                if (!params.has("name")) {
                    return JsonApiResponse.error("Tap's name missing from params").toJson();
                }
                if (!params.has("rules")) {
                    return JsonApiResponse.error("Rules are missing from params").toJson();
                }
                if (!activeTaps.containsKey(params.get("name").asString())) {
                    return JsonApiResponse.error("Tap's name does not correspond to a configured tap").toJson();
                }
                return activeTaps.get(params.get("name").asString()).sendCmd("/suricata",
                        Json.read("{\"cmd\":\"suricataModifyBaseRules\",\"params\":" + params + "}")).toJson();
            }
        });

        addCmd(TapsEndpoints.REPLACE_BASE_RULES, this::replaceBaseRules);

        addCmd(TapsEndpoints.REPLACE_GENERATED_RULES, this::replaceGenRules);

        addCmd(TapsEndpoints.ADD_BASE_RULES, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                if (!params.has("name")) {
                    return JsonApiResponse.error("Tap's name missing from params").toJson();
                }
                return setRules("Base", params);
            }
        });

        addCmd(TapsEndpoints.ADD_BASE_RULES_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                return apiHandler.sendPost("/suricata",
                        Json.read("{\"cmd\":\"suricataAddBaseRules\",\"params\":" + params.toString() + "}")).toJson();
            }
        });

        addCmd(TapsEndpoints.ADD_GENERATED_RULES, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                if (!params.has("name")) {
                    return JsonApiResponse.error("Tap's name missing from params").toJson();
                }
                return setRules("Custom", params);
            }
        });

        addCmd(TapsEndpoints.ADD_GENERATED_RULES_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                return apiHandler.sendPost("/suricata",
                        Json.read("{\"cmd\":\"suricataAddCustomRules\",\"params\":" + params.toString() + "}")).toJson();
            }
        });
        /*
         * Update suricata rules in all taps
         */
        addCmd(TapsEndpoints.UPDATE_ALL_TAPS_RULES, this::updateAllTapsSuricataRules);

        addCmd(TapsEndpoints.UPDATE_ALL_TAPS_RULES_DEPRECATED, this::updateAllTapsSuricataRules);

        addCmd(TapsEndpoints.GET_SURICATA_LOGS, this::getSuricataLogs);

        addCmd(TapsEndpoints.GET_SURICATA_LOGS_DEPRECATED, this::getSuricataLogs);

        addCmd(TapsEndpoints.CLEAR_SURICATA_LOGS, this::clearSuricataLogs);

        addCmd(TapsEndpoints.CLEAR_SURICATA_LOGS_DEPRECATED, this::clearSuricataLogs);

        addCmd(TapsEndpoints.SEND_INCLUDES, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                sendIncludes(params.at("name").asString());
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.SEND_INCLUDES_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                sendIncludes(params.at("name").asString());
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.SET_INCLUDES, new IJsonCmd() {
            @Override
            public Json exec(Json params) {

                ArrayList<Integer> includeList = new ArrayList<>();
                for (Json j : params.at("include").asJsonList()) {
                    if (j.at("checked").asBoolean()) {
                        includeList.add(j.at("id").asInteger());
                    }
                }
                for (Json good : configuredTaps) {
                    if (good.at("idname").asString().contentEquals(params.at("name").asString())) {
                        good.atDel("includes");
                        good.at("includes", includeList);
                    }
                }
                Json write = Json.object();
                write.at("write", configuredTaps);
                try {
                    writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.SET_INCLUDES_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {

                ArrayList<Integer> includeList = new ArrayList<>();
                for (Json j : params.at("include").asJsonList()) {
                    if (j.at("checked").asBoolean()) {
                        includeList.add(j.at("id").asInteger());
                    }
                }
                for (Json good : configuredTaps) {
                    if (good.at("idname").asString().contentEquals(params.at("name").asString())) {
                        good.atDel("includes");
                        good.at("includes", includeList);
                    }
                }
                Json write = Json.object();
                write.at("write", configuredTaps);
                try {
                    writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.GET_INCLUDES, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                try {
                    Json includes = Json.object();
                    Json taps = readJsonFromFile(idsconf + "/taps/TapsConfiguration.json");

                    for (Json k : taps.asJsonList()) {
                        Json includesRaw = Json.object();
                        ArrayList<Json> includesRawClone = new ArrayList<Json>();
                        includesRaw = readJsonFromFile(idsconf + "/taps/includesConfiguration.json");
                        for (Json j : includesRaw.asJsonList()) {
                            for (Json r : k.at("includes").asJsonList()) {
                                if (j.at("id").asInteger() == r.asInteger()) {
                                    j.at("checked", true);
                                }
                            }
                            includesRawClone.add(j);
                        }
                        for (Json j : includesRawClone) {
                            if (JsonUtil.getJson(j, "checked") == null)
                                j.at("checked", false);
                        }
                        includes.at(k.at("idname").asString(), includesRawClone);
                    }

                    return includes;
                } catch (IOException e1) {
                    // e1.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.GET_INCLUDES_DEPRECATED, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                try {
                    Json includes = Json.object();
                    Json taps = readJsonFromFile(idsconf + "/taps/TapsConfiguration.json");

                    for (Json k : taps.asJsonList()) {
                        Json includesRaw = Json.object();
                        ArrayList<Json> includesRawClone = new ArrayList<Json>();
                        includesRaw = readJsonFromFile(idsconf + "/taps/includesConfiguration.json");
                        for (Json j : includesRaw.asJsonList()) {
                            for (Json r : k.at("includes").asJsonList()) {
                                if (j.at("id").asInteger() == r.asInteger()) {
                                    j.at("checked", true);
                                }
                            }
                            includesRawClone.add(j);
                        }
                        for (Json j : includesRawClone) {
                            if (JsonUtil.getJson(j, "checked") == null)
                                j.at("checked", false);
                        }
                        includes.at(k.at("idname").asString(), includesRawClone);
                    }


                    return includes;
                } catch (IOException e1) {
                    // e1.printStackTrace();
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.TEST, new IJsonCmd() {
            @Override
            public Json exec(Json params) {
                try {
                    return readJsonFromFile("./datafile/world.json");
                } catch (IOException e) {
                }
                return Json.object();
            }
        });

        addCmd(TapsEndpoints.TEST_STREAMING, (params) -> {
            if (!params.has("name") || !params.get("name").isString()) {
                return JsonApiResponse.error("Tap's name missing from params").toJson();
            }
            String name = params.get("name").asString();
            if (!activeTaps.containsKey(name)) {
                return JsonApiResponse.error("Tap's name does not correspond to a configured tap").toJson();
            }
            return activeTaps.get(name).getFile("/suricata",
                    Json.read("{\"cmd\":\"suricataGetLogFile\"}"));
        });

        System.out.println("API commands operationnals");

        return true;
    }

    /**
     * Reloads the rules of the given IDS
     *
     * @param params general parameters of the request
     * @return whether the rules were updated
     */
    private Json reloadRules(Json params) {
        if (!params.has("name")) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        if (!activeTaps.containsKey(params.get("name").asString())) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured tap").toJson();
        }

        return JsonApiResponse.result(activeTaps.get(params.get("name").asString()).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataReloadRules\"}")).getResult().get("result")).toJson();
    }

    /**
     * Replaces all the base rules of the given IDS
     *
     * @param params general parameters of the request
     * @return the base rules of the given IDS
     */
    private Json replaceBaseRules(Json params) {
        if (!params.has("name")) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        if (!params.has("rules")) {
            return JsonApiResponse.error("Rules are missing from params").toJson();
        }
        if (!activeTaps.containsKey(params.get("name").asString())) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured tap").toJson();
        }
        return activeTaps.get(params.get("name").asString()).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataReplaceBaseRules\",\"params\":" + params + "}")).toJson();
    }

    /**
     * Replaces all the generated rules of the given IDS
     *
     * @param params general parameters of the request
     * @return the generated rules of the given IDS
     */
    private Json replaceGenRules(Json params) {
        if (!params.has("name")) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        if (!params.has("rules")) {
            return JsonApiResponse.error("Rules are missing from params").toJson();
        }
        if (!activeTaps.containsKey(params.get("name").asString())) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured tap").toJson();
        }
        return activeTaps.get(params.get("name").asString()).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataReplaceCustomRules\",\"params\":" + params + "}")).toJson();
    }

    /**
     * Gets the base rules of the given IDS
     *
     * @param params general parameters of the request
     * @return the base rules of the given IDS
     */
    private Json getBaseRules(Json params) {
        if (!params.has("name")) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        if (!activeTaps.containsKey(params.get("name").asString())) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured tap").toJson();
        }
        return activeTaps.get(params.get("name").asString()).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataGetBaseRules\"}")).toJson();
    }

    /**
     * Gets the generated rules of the given IDS
     *
     * @param params general parameters of the request
     * @return the generated rules of the given IDS
     */
    private Json getGenRules(Json params) {
        if (!params.has("name")) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        if (!activeTaps.containsKey(params.get("name").asString())) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured tap").toJson();
        }
        return activeTaps.get(params.get("name").asString()).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataGetCustomRules\"}")).toJson();
    }

    /**
     * Gets the status of the IDS
     *
     * @param params general parameters of the request
     * @return the status of the IDS
     */
    private Json getSuricataStatus(Json params) {
        if (!params.has("name")) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        if (!activeTaps.containsKey(params.get("name").asString())) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured tap").toJson();
        }
        return JsonApiResponse.result(
                activeTaps.get(params.get("name").asString()).sendCmd("/suricata", "{\"cmd\":\"suricataStatus\"}").getResult().get("result")).toJson();
    }

    private Json clearSuricataLogs(Json params) {
        JsonApiResponse response;
        // Check if name of tap in params
        if (!params.has("name")) {
            response = JsonApiResponse.error("Taps name must be included");
            return response.toJson();
        }
        Json nameJ = params.get("name");
        // Check name is a string
        if (!nameJ.isString()) {
            response = JsonApiResponse.error("Taps name must be a string");
            return response.toJson();
        }
        String name = params.get("name").asString();
        // Check taps name in taps
        if (!activeTaps.containsKey(name)) {
            response = JsonApiResponse.error("Taps name is not configured");
            return response.toJson();
        }
        return clearSuricataLogs(name);
    }

    /**
     * Set the rules of a given tap
     *
     * @param type   type of rule
     * @param params name of the tap
     * @return the new rules
     */
    private static Json setRules(String type, Json params) {
        if (!activeTaps.containsKey(params.get("name").asString())) {
            return JsonApiResponse.error("This tap is not configured").toJson();
        }
        if (!params.has("rules")) {
            return JsonApiResponse.error("Rules must be passed in params").toJson();
        }
        if (!type.matches("(Base)|(Custom)")) {
            return JsonApiResponse.error("Unknown type of rule").toJson();
        }
        return activeTaps.get(params.get("name").asString()).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataAdd" + type + "Rules\",\"params\":{\"rules\":" + params.get("rules") + "}}")).toJson();
    }

    /**
     * Replaces all custom suricata rules for all the taps at once. Old rules will be lost.
     *
     * @param params json with a key "rules", which contains the list of the new rules.
     * @return the
     */
    private @Nullable Json updateAllTapsSuricataRules(Json params) {
        if (!params.has("rules")) {
            return JsonApiResponse.error("rules is missing from params").toJson();
        }
        Json response = Json.read("[]");
        for (Tap tap : activeTaps.values()) {
            response = tap.sendCmd("/suricata",
                    Json.read("{\"cmd\":\"suricataReplaceCustomRules\"," +
                            "\"params\":{\"rules\":" + params.get("rules").asJsonList() + "}}")).toJson();
        }
        return response;
    }

    /**
     * Sends the query to reload all taps rules (non-blocking).
     *
     * @return the output of every reload
     */
    private @NotNull Json reloadAllTapsRules(Json params) {
        List<Json> allTapsOutputs = new ArrayList<>();

        for (Tap tap : activeTaps.values()) {
            JsonApiResponse response = tap.sendCmd("/suricata", Json.read("{\"cmd\":\"suricataReloadRulesNonBlocking\"}"));

            Json result = Json.object();
            result.at("idname", tap.getName());
            result.at("id", tap.getId());
            if (response.isSuccess()) {
                if (response.getResult().has("result") && response.getResult().get("result").has("reloadRules")) {
                    result.at("result", response.getResult().get("result").get("reloadRules").asString());
                } else {
                    result.at("result", response.getResult().get("result"));
                }
            } else {
                result.at("result", "failed");
            }

            allTapsOutputs.add(result);
        }

        Json out = Json.object();
        out.at("result", allTapsOutputs);

        return out;
    }

    /**
     * Sends one or more variables of the suricata configuration.
     *
     * @return the name of the tap and the result of the update
     */
    private @NotNull Json setSuricataConf(Json params) {
        // Verify tap ok
        if (!params.has("name") && !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        // Check if configured
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        Tap tap = activeTaps.get(name);
        // Check if contains file
        if (!params.has("file") && !params.get("file").isString()) {
            return JsonApiResponse.error("Could not read the configuration file").toJson();
        }
        params.delAt("name");

        JsonApiResponse result = tap.sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataSetConfigurationFile\", \"params\":" + params.toString() + "}"));

        if (result.isSuccess() && result.getResult().has("Result")) {
            return JsonApiResponse.result(result.getResult().get("Result")).toJson();
        } else {
            return result.toJson();
        }
    }

    /**
     * Resets the configuration file of Suricata IDS.
     *
     * @return the name of the tap and the result of the reset
     */
    private @NotNull Json resetSuricataConf(Json params) {
        // Verify tap ok
        JsonUtil.getStringFromJson(params, "name", "");
        if (!params.has("name") && !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        // Check if configured
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        Tap tap = activeTaps.get(name);

        JsonApiResponse result = tap.sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataResetConfigurationFile\"}"));

        if (result.isSuccess() && result.getResult().has("Result")) {
            return JsonApiResponse.result(result.getResult().get("Result")).toJson();
        } else {
            return result.toJson();
        }
    }

    /**
     * Get configuration yaml file.
     *
     * @return the name of the tap and the content of the yaml file
     */
    private @NotNull Json getSuricataConf(Json params) {
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured tap").toJson();
        }
        return activeTaps.get(name).getFile("/suricata",
                Json.read("{\"cmd\":\"suricataGetConfigurationFile\"}"));
    }

    /**
     * Get last part of configuration yaml file.
     *
     * @return the name of the tap and the content of the yaml file
     */
    private @NotNull Json getSuricataConf_prototype(Json params) {
        if (!params.has("name")) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        if (!activeTaps.containsKey(params.get("name").asString())) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        Tap tap = activeTaps.get(params.get("name").asString());

        Json output = tap.sendCmd("/suricata", Json.read("{\"cmd\":\"suricataGetConfigurationFile\"}")).toJson();
        ;
        Json result = Json.object();
        result.at("idname", tap.getName());
        result.at("id", tap.getId());
        if (output.has("result") && output.get("result").has("result")) {
            result.at("result", output.get("result").get("result"));
        } else {
            result.at("error", true);
        }

        return result;
    }

    /**
     * Get the possible interfaces at the given tap.
     *
     * @return the name of the tap and the content of the yaml file
     */
    private @NotNull Json getPossibleInterfaces(Json params) {
        if (!params.has("name")) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        if (!activeTaps.containsKey(params.get("name").asString())) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        Tap tap = activeTaps.get(params.get("name").asString());

        Json output = tap.sendCmd("/config", Json.read("{\"cmd\":\"getPossibleInterfaces\"}")).toJson();
        ;
        Json result = Json.object();
        result.at("idname", tap.getName());
        result.at("id", tap.getId());
        if (output.has("result") && output.get("result").has("result")) {
            result.at("result", output.get("result").get("result"));
        } else {
            result.at("error", true);
        }

        return result;
    }
}
