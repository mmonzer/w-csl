package main.services;

import com.csl.core.Config;
import com.jcraft.jsch.JSchException;
import com.ucsl.json.Json;
import com.csl.util.SshUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;

import com.csl.util.FileUtils;

public class SuricataServices extends Service {
    private static final Logger logger = LoggerFactory.getLogger(SuricataServices.class);
    static ArrayList<Json> configuredSuricata;
    static String localIP;
    static Integer localPort;
    static String knownHostFilePath;

    private static final String SURICATA_CONF_DIR = "~/csl/configSuricata";
    private static final String STOP_SURICATA = "sudo kill -9 `cat " + SURICATA_CONF_DIR + "/suricataPID`";
    private static final String RELOAD_RULES = "sudo kill -USR2 `cat " + SURICATA_CONF_DIR + "/suricataPID`";

    private static String startSuricataCommand(String ip, String port) {
        return "cd " + SURICATA_CONF_DIR + " && sudo java -jar ProxyUnixStream.jar /etc/suricata/log/socket " + localIP + " " + localPort + " & " +
                "sudo suricata -D -c " + SURICATA_CONF_DIR + "/suricata/suricata.yaml -i enp0s3 --pidfile " + SURICATA_CONF_DIR + "/suricataPID";
    }

    private static String startSuricataCommand(String ip, Integer port) {
        return "cd " + SURICATA_CONF_DIR + " && sudo java -jar ProxyUnixStream.jar /etc/suricata/log/socket " + localIP + " " + localPort + " & " +
                "sudo suricata -D -c " + SURICATA_CONF_DIR + "/suricata/suricata.yaml -i enp0s3 --pidfile " + SURICATA_CONF_DIR + "/suricataPID";
    }

    // Not used anymore, its TapsServices.java's counterpart is
    public static Json startSuricata(String id, String password, String name) {
        String ip = null;
        int port = 22;
        for (Json j : configuredSuricata) {
            if (j.at("id").asString().contentEquals(name)) {
                ip = j.at("ip").asString();
                try {
                    port = j.at("port").asInteger();
                } catch (NullPointerException e) {
                    logger.debug("Using default SSH port (22)");
                }
            }
        }
        SshUtils ssh = new SshUtils(id, password, ip, port/*,knownHostFilePath*/);
        String command = startSuricataCommand(localIP, localPort);
        String output = null;
        try {
            output = ssh.remoteExec(command);
        } catch (JSchException | IOException e) {
            // e.printStackTrace();
        }
        Json out = Json.object();
        out.at("result", output);
        return out;
    }

    public static Json stopSuricata(String id, String password, String name) {
        String ip = null;
        int port = 22;
        for (Json j : configuredSuricata) {
            if (j.at("id").asString().contentEquals(name)) {
                ip = j.at("ip").asString();
                try {
                    port = j.at("port").asInteger();
                } catch (NullPointerException e) {
                    logger.debug("Using default SSH port (22)");
                }
            }
        }
        SshUtils ssh = new SshUtils(id, password, ip, port);
        String output = null;
        try {
            output = ssh.remoteExec(STOP_SURICATA);
        } catch (JSchException | IOException e) {
            // e.printStackTrace();
        }
        Json out = Json.object();
        out.at("result", output);
        return out;
    }

    public static Json reloadRules(String id, String password, String name) {
        String ip = null;
        int port = 22;
        for (Json j : configuredSuricata) {
            if (j.at("id").asString().contentEquals(name)) {
                ip = j.at("ip").asString();
                try {
                    port = j.at("port").asInteger();
                } catch (NullPointerException e) {
                    logger.debug("Using default SSH port (22)");
                }
            }
        }
        SshUtils ssh = new SshUtils(id, password, ip, port);
        String output = null;
        try {
            output = ssh.remoteExec(RELOAD_RULES);
        } catch (JSchException | IOException e) {
            // e.printStackTrace();
        }
        Json out = Json.object();
        out.at("result", output);
        return out;
    }

    public static void sendRules(String name, String username, String password) {
        for (Json j : configuredSuricata) {
            if (j.at("id").asString().contentEquals(name)) {
                String ip = j.at("ip").asString();
                int port = 22;
                try {
                    port = j.at("port").asInteger();
                } catch (NullPointerException e) {
                    logger.debug("Using default SSH port (22)");
                }
                SshUtils ssh = new SshUtils(username, password, ip, port);
                try {
                    ssh.sendFile("./datafile/suricataRules/" + name + ".rules", "/home/" + username + "/configSuricata/suricata/rules/csl.rules");
                } catch (IOException | JSchException e) {
                    // e.printStackTrace();
                }
            }
        }
    }

    public static Json getRules(String name, String username, String password) {
        String resultat = "";
        for (Json j : configuredSuricata) {
            if (j.at("id").asString().contentEquals(name)) {
                String ip = j.at("ip").asString();
                int port = 22;
                try {
                    port = j.at("port").asInteger();
                } catch (NullPointerException e) {
                    logger.debug("Using default SSH port (22)");
                }
                SshUtils ssh = new SshUtils(username, password, ip, port);
                try {
                    ssh.getFile("/home/" + username + "/configSuricata/suricata/rules/csl.rules", "./datafile/suricataRules/" + name + ".rules");
                    resultat = FileUtils.readFile("./datafile/suricataRules/" + name + ".rules");
                } catch (IOException | JSchException e) {
                    // e.printStackTrace();
                }
            }
        }
        Json result = Json.object();
        result.at("result", resultat);
        return result;
    }

    public static void newSuricata(String name) {
        Json j = Json.object();
        j.at("id", name);
        j.at("rules_file", "./datafile/suricataRules/" + name + ".rules");

        configuredSuricata.add(j);
        try {
            FileUtils.writeToFile("./datafile/suricataRules/" + name + ".rules", "");
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    public static void deleteSuricata(String name) {
        ArrayList<Json> suricataClone = (ArrayList<Json>) configuredSuricata.clone();
        for (Json j : configuredSuricata) {
            if (j.at("id").asString().contentEquals(name)) {
                suricataClone.remove(j);
            }
        }
        configuredSuricata = suricataClone;
    }

    public static void setIp(String name, String ip) {
        ArrayList<Json> suricataClone = (ArrayList<Json>) configuredSuricata.clone();
        for (Json j : configuredSuricata) {
            if (j.at("id").asString().contentEquals(name)) {
                suricataClone.remove(j);
                j.at("ip", ip);
                suricataClone.add(j);
            }
        }
        configuredSuricata = suricataClone;
    }

    public static void renameSuricata(String name, String newName) {
        ArrayList<Json> suricataClone = (ArrayList<Json>) configuredSuricata.clone();
        for (Json j : configuredSuricata) {
            if (j.at("id").asString().contentEquals(name)) {
                suricataClone.remove(j);
                j.delAt("id");
                j.delAt("rules_file");
                j.at("id", newName);
                j.at("rules_file", "./datafile/suricataRules/" + newName + ".rules");
                suricataClone.add(j);
                String oldRules = "";
                try {
                    oldRules = FileUtils.readFile("./datafile/suricataRules/" + name + ".rules");
                    FileUtils.writeToFile("./datafile/suricataRules/" + newName + ".rules", oldRules);
                } catch (IOException e) {
                    // e.printStackTrace();
                }
            }
        }
        configuredSuricata = suricataClone;
    }

    /**
     * Default constructor of the Suricata service. Name, description and configuration file are given here.
     */
    public SuricataServices() {
        this("suricata",
                "Service for dealing with suricata configuration.",
                "ssh_service");
    }

    /**
     * Generic constructor of the Suricata service.
     */
    public SuricataServices(String name, String description, String configFileSectionName) {
        super(name, description, configFileSectionName);
    }

    @Override
    public boolean init() {
        defineServiceEndpoints();
        return true;
    }

    /**
     * Initialization of the Suricata commands
     *
     * @return true if the initialization happened with no problems, false otherwise.
     */
    public void defineServiceEndpoints() {
        Config.Tap config = Config.INSTANCE.tapService;
        logger.debug("Initializing SSH suricata commands ..");
        try {
            Json conf = FileUtils.readJsonFromFile("./datafile/configuredSuricata.json");
            if (conf.isArray())
                configuredSuricata = (ArrayList<Json>) conf.asJsonList();
            else
                configuredSuricata = new ArrayList<Json>();
        } catch (IOException e1) {
            System.err.println("No tap config found");
            configuredSuricata = new ArrayList<Json>();
        }

        knownHostFilePath = config.getKnowHostFilePath();
        localIP = config.getLocalIpAddress();
        localPort = config.getLocalPort();


        addCmd("newSuricata", params -> {
            logger.debug("paramètres de newSuricata : {}", params.toString());
            logger.debug("nom utilisé : {}", params.at("name").asString());

            newSuricata(params.at("name").asString());
            Json write = Json.object();
            write.at("write", configuredSuricata);
            try {
                FileUtils.writeToFile("./datafile/configuredSuricata.json", write.at("write").toString());
            } catch (IOException e) {
                // e.printStackTrace();
            }
            return Json.object();
        });

        addCmd("renameSuricata", params -> {
            renameSuricata(params.at("name").asString(), params.at("newName").asString());
            Json write = Json.object();
            write.at("write", configuredSuricata);
            try {
                FileUtils.writeToFile("./datafile/configuredSuricata.json", write.at("write").toString());
            } catch (IOException e) {
                // e.printStackTrace();
            }
            return Json.object();
        });

        addCmd("deleteSuricata", params -> {
            deleteSuricata(params.at("name").asString());
            Json write = Json.object();
            write.at("write", configuredSuricata);
            try {
                FileUtils.writeToFile("./datafile/configuredSuricata.json", write.at("write").toString());
            } catch (IOException e) {
                // e.printStackTrace();
            }
            return Json.object();
        });

        addCmd("setSuricataIp", params -> {
            setIp(params.at("name").asString(), params.at("ip").asString());
            Json write = Json.object();
            write.at("write", configuredSuricata);
            try {
                FileUtils.writeToFile("./datafile/configuredSuricata.json", write.at("write").toString());
            } catch (IOException e) {
                // e.printStackTrace();
            }
            return Json.object();
        });

        addCmd("getSuricataRules", params -> {
            Json result = getRules(params.at("name").asString(), params.at("username").asString(), params.at("password").asString());
            Json write = Json.object();
            write.at("write", configuredSuricata);
            try {
                FileUtils.writeToFile("./datafile/configuredSuricata.json", write.at("write").toString());
            } catch (IOException e) {
                // e.printStackTrace();
            }
            return result;
        });

        addCmd("sendSuricataRules", params -> {
            sendRules(params.at("name").asString(), params.at("username").asString(), params.at("password").asString());
            Json write = Json.object();
            write.at("write", configuredSuricata);
            try {
                FileUtils.writeToFile("./datafile/configuredSuricata.json", write.at("write").toString());
            } catch (IOException e) {
                // e.printStackTrace();
            }
            return Json.object();
        });

        addCmd("getConfiguredSuricata", params -> {
            try {
                return FileUtils.readJsonFromFile("./datafile/configuredSuricata.json");
            } catch (IOException e) {
                // e.printStackTrace();
            }
            return Json.object();
        });

        addCmd("getSuricataState", params -> {
            Json j = Json.object();
            j.at("state", "IDLE");
            return j;
        });


        addCmd("startSuricata", params -> startSuricata(params.at("username").asString(), params.at("password").asString(), params.at("name").asString()));

        addCmd("stopSuricata", params -> stopSuricata(params.at("username").asString(), params.at("password").asString(), params.at("name").asString()));

        addCmd("reloadRules", params -> reloadRules(params.at("username").asString(), params.at("password").asString(), params.at("name").asString()));
        logger.debug("SSH commands operationnal");
    }
}
