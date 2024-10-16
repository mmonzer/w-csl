package main.services;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.csl.intercom.status.StatusNotifier;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.JsonUtil;

/**
 * Control the status notifications, mostly allow to remotely control the sending of them.
 * Also provides a command to directly retrieve the status message.
 */
public class StatusService extends Service {
    /**
     * Status of the notifier
     */
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
     *
     * @return true if the initialization happened with no problems, false otherwise.
     */
    @Override
    public boolean init() {
        System.out.println("Initializing status service ...");
        notifier = CSLContext.instance.getStatusNotifier();
//        notifier.setSendNotifications(JsonUtil.getBooleanFromJson(jConfig, "send_notifications", false));
        notifier.setSendNotifications(Config.instance.Status.getSendNotifications());

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
}
