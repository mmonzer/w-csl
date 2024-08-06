package com.csl.intercom.jsoncmd;

import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import main.services.JsonApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ApiCommands implements IApiCommands {

    static private final Logger logger = LoggerFactory.getLogger(ApiCommands.class);
    static boolean debug = true;

    HashMap<String, IJsonCmd> listOfCommands = new HashMap<String, IJsonCmd>();
    HashMap<String, IJsonCmdHelp> listOfCommandHelps = new HashMap<String, IJsonCmdHelp>();
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

    public Json exec(String name, Json params) {
        IJsonCmd j = listOfCommands.get(name);

        if (j == null) {
            if (name.compareToIgnoreCase("help") == 0) {

                return getHelp(params);
            }

            Json jresult = Json.object();
            jresult.set("error", "Command <" + name + "> not found");
            return jresult;
        }

        return wrapperForAPIRequest(j,params);
    }

    @Override
    public String registerCmd(String name, IJsonCmd j) {
        if (listOfCommands.get(name) != null)
            return "Command with this name already registered :" + name;
        listOfCommands.put(name, j);
        listOfCommandNames.add(name);
        return "ok";
    }

    @Override
    public String registerCmd(String name, IJsonCmd j, IJsonCmdHelp jh) {
        if (listOfCommands.get(name) != null)
            return "Command with this name already registered :" + name;
        listOfCommands.put(name, j);
        listOfCommandNames.add(name);
        if (jh != null) {listOfCommandHelps.put(name, jh.setName(name));}
        return "ok";
    }

    @Override
    public String registerCmd(String name, IJsonCmd j, JsonCmdPrivilegeFamily privilegeFamily) {
        String result = registerCmd(name, j);
        if ("ok".equals(result)) {
            listOfCommandPrivileges.put(name, privilegeFamily);
        }
        return result;
    }

    @Override
    public String registerCmd(String name, IJsonCmd j, IJsonCmdHelp jh, JsonCmdPrivilegeFamily privilegeFamily) {
        String result = registerCmd(name, j, jh);
        if ("ok".equals(result)) {
            listOfCommandPrivileges.put(name, privilegeFamily);
        }
        return result;
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

    @Override
    public String getName() {
        return getCleanApiName();
    }

    /**
     * Gives the description of the API service
     * @return the description of the service
     */
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setName(String name) {
        this.path = name;
    }

    public List<String> getListOfCommands() {
        return new ArrayList<String>(listOfCommands.keySet());
    }

    public IJsonCmd getJCmd(String name) {
        return listOfCommands.get(name);
    }

    @Override
    public Json execJcmd(Json jCmd) {
        Json data = jCmd;
        Json cmd = data.get("cmd");
        Json params = data.get("params");

        if (cmd == null) {
            logger.warn("Invalid jcmd: null");
        }
        if (params == null) {
            params = Json.object();
        }

        if (debug) {logger.trace("Exec {} {}", cmd, params);}

        try {
            return exec(cmd.asString(), params);
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
                IJsonCmdHelp z = listOfCommandHelps.get(name);
                if (!z.isHidden()) {
                    Json jz = z.toJson(params);
                    if (jz == null) continue;
                    if (jz.isArray()) {
                        for (Json jjj : jz.asJsonList()) jlist.add(jjj);
                    } else {
                        if (showExamples) {
                            Json ex = findExemple(jExamples, name);
                            if (ex != null) {
                                if (ex.has("params")) jz.set("ex_params", ex.get("params"));
                                if (ex.has("result")) jz.set("ex_result", ex.get("result"));
                            }
                        }
                        jlist.add(jz);
                    }
                }
            } else {
                if (name.compareToIgnoreCase("help") != 0) {
                    Json r = Json.object().set("cmd", name).set("desc", "no info");
                    if (params.has("status")) r.set("status", "");
                    jlist.add(r);
                }
            }
        }
        return jlist;
    }


    private Json execCmd(String api, String cmd, Json jparams) {
        Json j = Json.object();

        j.set("cmd", cmd);
        j.set("params", jparams);
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
            if (j.has("cmd")) {
                String c = JsonUtil.getStringFromJson(j, "cmd", "");
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

}
