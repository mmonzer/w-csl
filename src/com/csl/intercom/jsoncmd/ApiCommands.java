package com.csl.intercom.jsoncmd;

import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;


public class ApiCommands implements IApiCommands {

    static private final Logger logger = LoggerFactory.getLogger(ApiCommands.class);
    static boolean debug = true;

    HashMap<String, IJsonCmd> listOfCommands = new HashMap<String, IJsonCmd>();
    HashMap<String, IJsonCmdHelp> listOfCommandHelps = new HashMap<String, IJsonCmdHelp>();
    List<String> listOfCommandNames = new ArrayList<String>();
    private String path = "";

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

        return j.exec(params);
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
        if (jh != null) listOfCommandHelps.put(name, jh.setName(name));
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

    @Override
    public String getName() {
        return getCleanApiName();
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub
        this.path = name;
    }

    public String getPath() {
        return path;
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

        if (debug) logger.debug("Exec {} {}", cmd, params);

        return exec(cmd.asString(), params);
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

    public String toString() {
        String s = "";
        for (Entry<String, IJsonCmd> entry : listOfCommands.entrySet()) {
            String name = entry.getKey();
            if (!s.isEmpty()) s = s + ",";
            s = s + name;
        }

        return getCleanApiName() + ":[" + s + "]";
    }


}
