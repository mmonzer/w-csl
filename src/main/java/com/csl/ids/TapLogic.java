package com.csl.ids;

import com.csl.intercom.cslscan.ApiHandler;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.jcraft.jsch.JSchException;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.extensions.SshUtils;
import main.services.JsonApiResponse;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TapLogic {
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
    static String localPort;
    static String knownHostFilePath;

    private final ApiHandler apiHandler;

    static String idsconf;

    private static String readFile(String fileName) throws IOException {
        String jsonRaw = "";
        File fichierRegles = new File(fileName);
        InputStream lecteur = new BufferedInputStream(new FileInputStream(fichierRegles));
        InputStreamReader ipsr = new InputStreamReader(lecteur, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(ipsr);
        String ligne;
        while ((ligne = br.readLine()) != null) {
            jsonRaw += ligne + "\r\n";
        }
        br.close();
        return jsonRaw;
    }

    public static Json readJsonFile(String fileName) throws IOException {
        String jsonRaw = "";
        File fichierRegles = new File(fileName);
        InputStream lecteur = new BufferedInputStream(new FileInputStream(fichierRegles));
        InputStreamReader ipsr = new InputStreamReader(lecteur);
        BufferedReader br = new BufferedReader(ipsr);
        String ligne;
        while ((ligne = br.readLine()) != null) {
            jsonRaw += ligne + "\n";
        }
        br.close();
        return Json.read(jsonRaw);
    }

    public static Json stopTap(String name) {
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        activeTaps.get(name).sendCmd("/monitoring",
                Json.read("{\"cmd\":\"monitorStop\"}")).toJson();
//        taps.get(name).sendCmd("/suricata",
//                Json.read("{\"cmd\":\"suricataStop\"}")).toJson();
        return JsonApiResponse.result(Json.read("{\"msg\":\"Tap " + name + " was successfully stopped\"}")).toJson();
    }

    public static Json startTap(String name) {
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        activeTaps.get(name).sendCmd("/monitoring",
                Json.read("{\"cmd\":\"monitorStart\"}")).toJson();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
        s.deleteCharAt(s.length()-1);
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
            basicConf = readJsonFile(idsconf + "/taps/basicNetworkConf.json");
            basicConf.at("id", name);
            basicConf.at("csl_node_ip", localIP);
            basicConf.at("csl_node_port", localPort);
            writeToFile(basicConf.toString(), idsconf + "/taps/" + name + "/tapReseau.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            basicConf = readJsonFile(idsconf + "/taps/basicProcessConf.json");
            basicConf.at("id", name);
            for (Json jj : basicConf.at("modules").asJsonList()) {
                if (jj.at("name").asString().contentEquals("module2")) {
                    jj.at("config").at("host_target", localIP);
                    jj.at("config").at("port_target", localPort);
                }
            }

            writeToFile(basicConf.toString(), idsconf + "/taps/" + name + "/tapProcess.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            e.printStackTrace();
        }
    }

    /**
     * Changes the IP to contact the tap API. It changes the configuration if the CSL-Client side and
     * records it into a file.
     * @param name of the tap
     * @param ip new ip of the tap API
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
            if (activeTaps.get(name).sendCmd("/config", "{\"cmd\":\"getConfig\"}").getResult()==null) {
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
     * Gets the IDS logs of the given tap
     * @param name name of the tap
     * @return the last logs of the ids
     */
    public Json getSuricataLogs(String name) {
        return activeTaps.get(name).sendCmd("/suricata", "{\"cmd\":\"suricataGetLogs\"}").toJson().get("result");
    }

    /**
     * Gets the IDS logs of the given tap
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
                    e.printStackTrace();
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
    public static String getConfFromTap() {
        // TODO : change url and deal with answer
        HttpResponse<String> response = null;
        String url = "http://localhost:8888/config";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString("{\"cmd\":\"getConfig\"}"))
                .build();
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response.body();
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

    public static void sendConfToTap(String name, String file) {
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
                SshUtils ssh = new SshUtils(username, password, ip, port/*,knownHostFilePath*/);
                try {
                    switch (file) {
                        case "reseau":
                            ssh.sendFile(idsconf + "/taps/" + name + "/tapReseau.json", "/home/" + username + "/csl/tapReseau/conf.json");
                            break;
                        case "process":
                            ssh.sendFile(idsconf + "/taps/" + name + "/tapProcess.json", "/home/" + username + "/csl/tapProcess/CSLConfigTAPProcess.json");
                            break;
                        case "suricataconf":
                            ssh.sendFile(idsconf + "/taps/" + name + "/suricata.yaml", "/home/" + username + "/csl/configSuricata/suricata/suricata.yaml");
                            break;
                        case "genrules":
                            ssh.sendFile(idsconf + "/taps/" + name + "/genrules.rules", "/home/" + username + "/csl/configSuricata/suricata/rules/csl.rules");
                            break;
                        case "baserules":
                            ssh.sendFile(idsconf + "/taps/" + name + "/baserules.rules", "/home/" + username + "/csl/configSuricata/suricata/rules/cslbase.rules");
                            break;
                    }
                } catch (IOException | JSchException e) {
                    e.printStackTrace();
                }
                ssh.endConnection();

            }
        }
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
                    Json includesRaw = readJsonFile(idsconf + "/taps/includesConfiguration.json");
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
                    e.printStackTrace();
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
    public Json getInterfacesIdsTap (Json params) {
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
    public Json setInterfacesIdsTap (Json params) {
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
                Json.read("{\"cmd\":\"suricataSetInterfaces\",\"params\":{\"interfaces\":"+interfaces+"}}")).toJson().get("result");
    }

    /**
     * Starts the Suricata of the given TAP name
     *
     * @param name name of the TAP
     * @return the result of the starting
     */
    public Json startSuricata(String name) {
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        return activeTaps.get(name).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataStart\"}")).toJson();
    }

    /**
     * Stops the Suricata of the given TAP name
     *
     * @param name name of the TAP
     * @return the result of the stopping
     */
    public Json stopSuricata(String name) {
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        return activeTaps.get(name).sendCmd("/suricata",
                Json.read("{\"cmd\":\"suricataStop\"}")).toJson();
    }

    /**
     * Starts the Monitor of the given TAP name
     *
     * @param name name of the TAP
     * @return the result of the starting
     */
    public Json startMonitor(String name) {
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        return activeTaps.get(name).sendCmd("/monitoring",
                Json.read("{\"cmd\":\"monitorStart\"}")).toJson();
    }

    /**
     * Stops the Monitor of the given TAP name
     *
     * @param name name of the TAP
     * @return the result of the stopping
     */
    public Json stopMonitor(String name) {
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
            e.printStackTrace();
        }
        ssh.endConnection();

        return reloadRulesParseOutput(output);
    }

    /**
     * Default constructor of the Suricata service.
     */
    public TapLogic() {
        this("taps",
                "Service for managing the different CSL-Probes: adding, configuring, ...",
                "ssh_service");
    }

    /**
     * Generic constructor of the Suricata service.
     */
    public TapLogic(String name, String description, String configFileSectionName) {
        //super(name, description, configFileSectionName);
        apiHandler = new ApiHandler("CSL-Tap","http://localhost:8888");
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
    private @NotNull Json reloadAllTapsRules() {
        List<Json> allTapsOutputs = new ArrayList<>();
        boolean gotError = false;

        for (Tap tap : activeTaps.values()) {
            String output = tap.sendCmd("/suricata", Json.read("{\"cmd\":\"suricataReloadRulesNonBlocking\"}")).toString();

            Json result = Json.object();
            result.at("idname", tap.getName());
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

    /**
     * Sends one or more variables of the suricata configuration.
     *
     * @return the name of the tap and the result of the update
     */
    private @NotNull Json setSuricataConf(Json params) {
        if (!params.has("name")) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        if (!activeTaps.containsKey(params.get("name").asString())) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        Tap tap = activeTaps.get(params.get("name").asString());

        Json output = tap.sendCmd("/suricata",
                Json.read("{\"cmd\":\"updateConfig\", \"params\":" + params.toString() + "}")).toJson();
        output.atDel("status");
        Json result = Json.object();
        result.at("idname", tap.getName());
        result.at("result", output);

        if (output.has("([Ee]rror)|([Pp]roblem)")) {
            result.at("error", true);
        }

        return result;
    }

    /**
     * Get configuration yaml file.
     *
     * @return the name of the tap and the content of the yaml file
     */
    private @NotNull Json getSuricataConf(Json params) {
        if (!params.has("name")) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        if (!activeTaps.containsKey(params.get("name").asString())) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        Tap tap = activeTaps.get(params.get("name").asString());

        Json output = tap.sendCmd("/suricata",Json.read("{\"cmd\":\"suricataGetConfigurationFile\"}")).toJson();;
        Json result = Json.object();
        result.at("idname", tap.getName());
        try {
            result.at("result", output.get("result").get("result"));
        } catch (Exception e) {
            result.at("error", true);
        }

        return result;
    }
}
