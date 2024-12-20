package com.csl.intercom.jsoncmd;

import com.csl.util.JCmd;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdWithFiles;
import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;
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
    private static final Logger logger = LoggerFactory.getLogger(ApiCommands.class);
    public static final String IS_MISSING_FROM_BODY = " is missing from body";
    static boolean debug = true;

    HashMap<String, IJsonCmd> listOfCommands = new HashMap<>();
    HashMap<String, IJsonCmdWithFiles> listOfCommandsWithFiles = new HashMap<>();
    HashMap<String, JsonCmdHelp> listOfCommandHelps = new HashMap<>();
    List<String> listOfCommandNames = new ArrayList<>();
    @Getter
    Map<String, JsonCmdPrivilegeFamily> listOfCommandPrivileges = new HashMap<>();
    @Getter
    private String path = "";
    @Setter
    @Getter
    private String description = "";

    public Json exec(String name, Json params, Json files) {
        if (listOfCommands.get(name) != null) {
            return wrapperForAPIRequest(listOfCommands.get(name), params);
        } else {
            return wrapperForAPIRequest(listOfCommandsWithFiles.get(name), params, files);
        }
    }

    public String registerCmd(String name, IJsonCmd jsonCommand) {
        return registerCmd(name, jsonCommand, null, null);
    }

    public String registerCmd(String name, IJsonCmd jsonCommand, JsonCmdHelp jsonCommandHelp) {
        return registerCmd(name, jsonCommand, jsonCommandHelp, null);
    }

    public String registerCmd(String name, IJsonCmd jsonCommand, JsonCmdPrivilegeFamily privilegeFamily) {
        return registerCmd(name, jsonCommand, null, privilegeFamily);
    }

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

    private String getCleanApiName() {
        String s = path;
        if (s == null) s = "";
        if (s.endsWith("/"))
            s = s.substring(0, s.length() - 1);

        if (s.startsWith("/")) s = s.substring(1);

        return s;
    }

    public String getName() {
        return getCleanApiName();
    }

    public void setName(String name) {
        this.path = name;
    }

    public List<String> getListOfCommands() {
        return new ArrayList<>(listOfCommands.keySet());
    }

    public Json execJcmd(Json jCmd) {
        Json cmd = jCmd.get(JCmd.CMD);
        Json params = jCmd.get(JCmd.PARAMETERS);
        Json files = jCmd.get("files");

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
            return JsonApiResponse.error(e.getMessage()+ IS_MISSING_FROM_BODY).toJson();
        }
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        for (Entry<String, IJsonCmd> entry : listOfCommands.entrySet()) {
            String name = entry.getKey();
            if (!s.isEmpty()) s.append(",");
            s.append(name);
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
            return JsonApiResponse.error(e.getMessage() + IS_MISSING_FROM_BODY).toJson();
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
            return JsonApiResponse.error(e.getMessage() + IS_MISSING_FROM_BODY).toJson();
        }
    }

}
