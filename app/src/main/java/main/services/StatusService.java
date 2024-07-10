package main.services;

import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.csl.intercom.status.StatusNotifier;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;

/**
 * Control the status notifications, mostly allow to remotely control the sending of them.
 * Also provides a command to directly retrieve the status message.
 */
<<<<<<< HEAD
public class StatusService extends Service {
    /**
     * Status of the notifier
     */
=======
public class StatusService implements ICSLService {
    @Getter
    IApiCommands apiCommands= new ApiCommandsFactory().createApiCommands("status");
>>>>>>> origin/feature/refactor_code
    private StatusNotifier notifier = null;

    /**
     * Default constructor of the Status service
     */
    public StatusService() {
        this("status",
                "Service for checking and changing if we want to receive the notifications.",
                "status");
    }

    /**
     * Generic constructor of the Status service
     */
    public StatusService(String name, String description, String configFileSectionName) {
        super(name,
                description,
                configFileSectionName);
    }

    /**
     * Initialization of the Status service commands
     * @param jConfig the configuration section of the configuration file
     * @param cslDir the CSL directory
     * @return true if the initialization happened with no problems, false otherwise.
     */
    @Override
    public boolean init(Json jConfig, String cslDir) {
        System.out.println("Initializing status service ...");
        notifier = CSLContext.instance.getStatusNotifier();
        notifier.setSendNotifications(JsonUtil.getBooleanFromJson(jConfig, "send_notifications", false));

        addCmd("get_status", params -> notifier.getNotification(),
                new JsonCmdHelp().setDesc("Get a status notification, created for the occasion")
                        .setResult("A status notification's contents.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("set_should_send_notifications", params -> {
                    boolean shouldSend = JsonUtil.getBooleanFromJson(params, "send", true);
                    notifier.setSendNotifications(shouldSend);
                    return JsonApiResponse.success().toJson();
                },
                new JsonCmdHelp().setDesc("Change the sending behaviour of status notifications")
                        .setParam("send","true if notifications should be sent periodically, false otherwise.",IJsonCmdHelp.BOOL)
                        .setResult("<code>{ \"success\": true }</code> if the new behaviour was successfully applied.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );

        System.out.println("Status service operational");
        return true;
    }
<<<<<<< HEAD
=======

    @Override
    public String getConfigFileSectionName() {
        return "status";
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
>>>>>>> origin/feature/refactor_code
}
