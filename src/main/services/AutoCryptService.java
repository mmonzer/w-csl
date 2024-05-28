package main.services;

import com.csl.autocrypt.AutoCrypt;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;

public class AutoCryptService extends Service {
    private AutoCrypt manager;

    /**
     * Initialization of the TAPs commands
     *
     * @param config the configuration section of the configuration file
     * @param configFile the CSL directory
     * @return true if the initialization happened with no problems, false otherwise.
     */
    @Override
    public boolean init(Json config, String configFile) {
        manager.setIp(config.at("ip").asString());
        manager.setPort(config.at("port").asInteger());
        manager.reinitApiHandler();

        addCmd("command_to_change",
                new IJsonCmd() {
                    @Override
                    public Json exec(Json params) {
                        Json payload = Json.object();
                        payload.at("cmd", "command_to_change");
                        payload.at("params", params);
                        return manager.sendCmd("/config", payload.toString()).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Dummy api point for module")
                        .setParam("No params", "", IJsonCmdHelp.JSON)
                        .setResult("Dummy result", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );

        return true;
    }



    /**
     * Default constructor of the Suricata service.
     */
    public AutoCryptService() {
        this("autocrypt",
                "Service for managing the different certificates",
                "auto_crypt");
    }

    /**
     * Generic constructor of the Suricata service.
     */
    public AutoCryptService(String name, String description, String configFileSectionName) {
        super(name, description, configFileSectionName);
        manager = new AutoCrypt();
    }
}
