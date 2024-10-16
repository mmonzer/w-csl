package com.csl.intercom.jsoncmd;

import com.csl.util.JCmd;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdWithFiles;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import main.services.JsonApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.csl.web.jcmdoversocket.CSLWebSocketForJcmd.COMMAND;

public class ApiCommands {
//    public class ApiCommands implements IApiCommands {

    static private final Logger logger = LoggerFactory.getLogger(ApiCommands.class);
    static boolean debug = true;

    HashMap<String, IJsonCmd> listOfCommands = new HashMap<String, IJsonCmd>();
    HashMap<String, IJsonCmdWithFiles> listOfCommandsWithFiles = new HashMap<String, IJsonCmdWithFiles>();
    HashMap<String, JsonCmdHelp> listOfCommandHelps = new HashMap<String, JsonCmdHelp>();
    List<String> listOfCommandNames = new ArrayList<String>();
    Map<String, JsonCmdPrivilegeFamily> listOfCommandPrivileges = new HashMap<>();
    @Getter
    private String path = "";
    private String description = "";

    private ApiCommands(String path) {
        this.path = path;
    }

    public ApiCommands() {

    }

    public Json exec(String name, Json params, Json files) {
        if (listOfCommands.get(name) == null && listOfCommandsWithFiles.get(name)== null) {
            if (name.compareToIgnoreCase("help") == 0) {

                return getHelp(params);
            }

            Json jsonresult = Json.object();
            jsonresult.set("error", "Command <" + name + "> not found");
            return jsonresult;
        }
        if (listOfCommands.get(name) != null) {
            return wrapperForAPIRequest(listOfCommands.get(name), params);
        } else {
            return wrapperForAPIRequest(listOfCommandsWithFiles.get(name), params, files);
        }
    }

//    @Override
    public String registerCmd(String name, IJsonCmd jsonCommand) {
        return registerCmd(name, jsonCommand, null, null);
    }

//    @Override
    public String registerCmd(String name, IJsonCmd jsonCommand, JsonCmdHelp jsonCommandHelp) {
        return registerCmd(name, jsonCommand, jsonCommandHelp, null);
    }

//    @Override
    public String registerCmd(String name, IJsonCmd jsonCommand, JsonCmdPrivilegeFamily privilegeFamily) {
        return registerCmd(name, jsonCommand, null, privilegeFamily);
    }

//    @Override
    public String registerCmd(String name, IJsonCmd jsonCommand, JsonCmdHelp jsonCommandHelp, JsonCmdPrivilegeFamily privilegeFamily) {
        if (listOfCommands.get(name) != null)
            return "Command with this name already registered :" + name;
        listOfCommands.put(name, jsonCommand);
        listOfCommandNames.add(name);
        if (jsonCommandHelp != null) {listOfCommandHelps.put(name, jsonCommandHelp.setName(name));}
        if (privilegeFamily!=null) {
            listOfCommandPrivileges.put(name, privilegeFamily);
        }
        return "ok";
    }

    public String registerCmd(String name, IJsonCmdWithFiles jsonCommand) {
        return registerCmd(name, jsonCommand, null, null);
    }

    public String registerCmd(String name, IJsonCmdWithFiles jsonCommand, JsonCmdHelp jsonCommandHelp) {
        return registerCmd(name, jsonCommand, jsonCommandHelp, null);
    }

    public String registerCmd(String name, IJsonCmdWithFiles jsonCommand, JsonCmdPrivilegeFamily privilegeFamily) {
        return registerCmd(name, jsonCommand, null, privilegeFamily);
    }

    public String registerCmd(String name, IJsonCmdWithFiles jsonCommand, JsonCmdHelp jsonCommandHelp, JsonCmdPrivilegeFamily privilegeFamily) {
        if (listOfCommands.get(name) != null)
            return "Command with this name already registered :" + name;
        listOfCommandsWithFiles.put(name, jsonCommand);
        listOfCommandNames.add(name);
        if (jsonCommandHelp != null) {listOfCommandHelps.put(name, jsonCommandHelp.setName(name));}
        if (privilegeFamily!=null) {
            listOfCommandPrivileges.put(name, privilegeFamily);
        }
        return "ok";
    }

    private String formatPath(String proxyPath) {
        if (proxyPath == null)
            proxyPath = "/";
        if (!proxyPath.endsWith("/"))
            proxyPath += "/";
        return proxyPath;
    }

    public String getPathFilter() {
        return formatPath(path) + "*";
    }

    public String getPathName() {
        if (path == null)
            return "";
        if (path.endsWith("/"))
            return path.substring(0, path.length() - 1);
        return path;
    }

    private String getCleanApiName() {
        String s = path;
        if (s == null) s = "";
        if (s.endsWith("/"))
            s = s.substring(0, s.length() - 1);

        if (s.startsWith("/")) s = s.substring(1);

        return s;
    }

    /*
     * path : /test (for example)
     */

//    @Override
    public String getName() {
        return getCleanApiName();
    }

    /**
     * Gives the description of the API service
     * @return the description of the service
     */
//    @Override
    public String getDescription() {
        return description;
    }

//    @Override
    public void setDescription(String description) {
        this.description = description;
    }

//    @Override
    public void setName(String name) {
        this.path = name;
    }

    public List<String> getListOfCommands() {
        return new ArrayList<String>(listOfCommands.keySet());
    }

    public IJsonCmd getJCmd(String name) {
        return listOfCommands.get(name);
    }

//    @Override
    public Json execJcmd(Json jCmd) {
        Json data = jCmd;
        Json cmd = data.get(JCmd.CMD);
        Json params = data.get(JCmd.PARAMETERS);
        Json files = data.get("files");

        if (cmd == null) {
            logger.warn("Invalid JSON command: null");
        }
        MDC.put(COMMAND, cmd.asString());
        if (params == null) {
            params = Json.object();
        }

        if (debug) {logger.trace("Exec {} {}", cmd, params);}

        try {
            return exec(cmd.asString(), params, files);
        } catch (IllegalArgumentException e) {
            return JsonApiResponse.error(e.getMessage()+ " is missing from body").toJson();
        }
    }

    public Json getHelp(Json params) {
        Json jlist = Json.array();

        boolean showExamples = false;
        Json jExamples = Json.array();
        if (params.has("ex")) {
            jExamples = loadExamples(getName());
            showExamples = true;
        }

        for (String name : listOfCommandNames) {
            if (listOfCommandHelps.get(name) != null) {
                JsonCmdHelp z = listOfCommandHelps.get(name);
                if (!z.isHidden()) {
                    Json jz = z.toJson(params);
                    if (jz == null) continue;
                    if (jz.isArray()) {
                        for (Json jjj : jz.asJsonList()) jlist.add(jjj);
                    } else {
                        if (showExamples) {
                            Json ex = findExemple(jExamples, name);
                            if (ex != null) {
                                if (ex.has(JCmd.PARAMETERS)) jz.set("ex_params", ex.get(JCmd.PARAMETERS));
                                if (ex.has("result")) jz.set("ex_result", ex.get("result"));
                            }
                        }
                        jlist.add(jz);
                    }
                }
            } else {
                if (name.compareToIgnoreCase("help") != 0) {
                    Json r = Json.object().set(JCmd.CMD, name).set("desc", "no info");
                    if (params.has("status")) r.set("status", "");
                    jlist.add(r);
                }
            }
        }
        return jlist;
    }


    private Json execCmd(String api, String cmd, Json jparams) {
        Json j = Json.object();

        j.set(JCmd.CMD, cmd);
        j.set(JCmd.PARAMETERS, jparams);
        return JServiceLoader.cslInterModuleCommunicationManager.executeCommand(api, j);
    }

    public Json readObjectFromDatabase(String name) {
        Json p = Json.object();
        p.set("name", name);

        String api = "dbjson";

        //ici
        return execCmd(api, "load_jsonfile", p);
    }

    public Json loadExamples(String apiname) {
        Json result = readObjectFromDatabase("helpex_" + apiname);

        if (result.has("contents")) return result.get("contents");
        if (result.has("error")) return Json.array();

        return Json.array();

    }

    public Json findExemple(Json examples, String cmd) {
        for (Json j : examples) {
            if (j.has(JCmd.CMD)) {
                String c = JsonUtil.getStringFromJson(j, JCmd.CMD, "");
                if (cmd.compareTo(c) == 0) return j;
            }
        }

        return null;
    }

    public Map<String, JsonCmdPrivilegeFamily> getListOfCommandPrivileges() {
        return listOfCommandPrivileges;
    }

    public String toString() {
        String s = "";
        for (Entry<String, IJsonCmd> entry : listOfCommands.entrySet()) {
            String name = entry.getKey();
            if (!s.isEmpty()) s = s + ",";
            s = s + name;
        }

        return getCleanApiName() + ":[" + s + "]";
    }


    /**
     * Wrapper to catch the exceptions and format a correct answer
     * @param cmd generic cmd to execute
     * @param params params to execute the cmd
     * @return the response of the cmd
     */
    public static Json wrapperForAPIRequest(IJsonCmd cmd, Json params) {
        try {
            return cmd.exec(params);
        } catch (IllegalArgumentException e) {
            return JsonApiResponse.error(e.getMessage() + " is missing from body").toJson();
        }
    }
    /**
     * Wrapper to catch the exceptions and format a correct answer
     * @param cmd generic cmd to execute
     * @param params params to execute the cmd
     * @return the response of the cmd
     */
    public static Json wrapperForAPIRequest(IJsonCmdWithFiles cmd, Json params, Json files) {
        try {
            return cmd.exec(params, files);
        } catch (IllegalArgumentException e) {
            return JsonApiResponse.error(e.getMessage() + " is missing from body").toJson();
        }
    }

}
