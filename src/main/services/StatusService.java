package main.services;

import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.csl.intercom.status.StatusNotifier;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

/**
 * Control the status notifications, mostly allow to remotely control the sending of them.
 * Also provides a command to directly retrieve the status message.
 */
public class StatusService implements ICSLService {
    private String name = "status";
    private String configFileSectionName = "status";
    private IApiCommands apiCommands = new ApiCommandsFactory().createApiCommands("");
    private StatusNotifier notifier = null;


    @Override
    public boolean init(Json jConfig, String cslDir) {
        System.out.println("Initializing status service ...");
        notifier = CSLContext.instance.getStatusNotifier();
        notifier.setSendNotifications(JsonUtil.getBooleanFromJson(jConfig, "send_notifications", false));

        addCmd("get_status", params -> notifier.getNotification());
        addCmd("set_should_send_notifications", params -> {
            boolean shouldSend = JsonUtil.getBooleanFromJson(params, "send", true);
            notifier.setSendNotifications(shouldSend);
            return Json.object("result", "OK");
        });

        System.out.println("Status service operational");
        return true;
    }

    @Override
    public String getConfigFileSectionName() {
        return configFileSectionName;
    }

    @Override
    public IApiCommands getApiCommands() {
        apiCommands.setName(name);
        return apiCommands;
    }

    @Override
    public boolean terminate() {
        return false;
    }

    /**
     * Register an API command.
     *
     * @param name The name of the command.
     * @param cmd  The callback to be executed when the command is invoked.
     * @return A {@link String}
     */
    public String addCmd(String name, IJsonCmd cmd) {
        return apiCommands.registerCmd(name, cmd);
    }

    /**
     * Register an API command.
     *
     * @param name The name of the command.
     * @param cmd  The callback to be executed when the command is invoked.
     * @param help The helper to display in the '/apihelp' page.
     * @return A {@link String}
     */
    public String addCmd(String name, IJsonCmd cmd, IJsonCmdHelp help) {
        return apiCommands.registerCmd(name, cmd, help);
    }
}
