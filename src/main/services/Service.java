package main.services;

import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.ucsl.interfaces.*;
import com.ucsl.json.Json;
import main.services.endpoints.Endpoint;

/**
 * Generic Service with a name, description, a configuration file and an API command.
 */
public abstract class Service implements ICSLService {
    /**
     * Name of service
     */
    protected String name;
    /**
     * Name of service description
     */
    protected String description;
    /**
     * Name of service configuration file
     */
    protected String configFileSectionName;
    /**
     * apiCommand for the current Service
     */
    protected IApiCommands apiCommands;

    /**
     * Constructor of the generic Service with a name, a description and a configuration filename
     *
     * @param name                  name of the service
     * @param description           description of the service
     * @param configFileSectionName name of corresponding configuration file
     */
    public Service(String name, String description, String configFileSectionName) {
        this(name, description, configFileSectionName, "");
    }

    /**
     * Constructor of the generic Service with a name, a description, a configuration filename
     * and a root for the api commands
     *
     * @param name                  name of the service
     * @param description           description of the service
     * @param configFileSectionName name of corresponding configuration file
     * @param rootAPI               root for the api commands
     */
    public Service(String name, String description, String configFileSectionName, String rootAPI) {
        this.name = name;
        this.description = description;
        this.configFileSectionName = configFileSectionName;
        this.apiCommands = new ApiCommandsFactory().createApiCommands(rootAPI);
        this.apiCommands.setName(name);
        this.apiCommands.setDescription(description);
    }

    /**
     * Constructor of the generic Service with a name and a configuration filename
     *
     * @param name                  name of the service
     * @param configFileSectionName name of corresponding configuration file
     */
    public Service(String name, String configFileSectionName) {
        this(name, name + " description", configFileSectionName);
    }

    /**
     * Generic function where one will add the custom commands
     *
     * @param jConfig the configuration section of the configuration file
     * @param cslDir  the CSL directory
     * @return true if the initialization happened with no problems, false otherwise.
     */
    public abstract boolean init(Json jConfig, String cslDir);

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

    /**
     * Register an API command.
     *
     * @param endpoint endpoint information: cmd and help
     * @param cmd      The callback to be executed when the command is invoked.
     * @return A {@link String}
     */
    public String addCmd(Endpoint endpoint, IJsonCmd cmd) {
        return addCmd(endpoint.cmd(), cmd, endpoint.help());
    }

    /**
     * Gives the name of the service
     *
     * @return the name of the service in format {@link String}
     */
    public String getName() {
        return name;
    }

    /**
     * Gives the name of the configuration file
     *
     * @return the name of the file in format {@link String}
     */
    public String getConfigFileSectionName() {
        return configFileSectionName;
    }

    /**
     * Gives the api command object with the name, description and commands of the service
     *
     * @return the api command object with the name, description and commands of the service
     */
    @Override
    public IApiCommands getApiCommands() {
        return apiCommands;
    }

    /**
     * Function called when stopping the service
     *
     * @return false as default
     */
    @Override
    public boolean terminate() {
        return false;
    }

    /**
     * Returns the error if a variable is not found in the body
     *
     * @return error json
     */
    public Json errorVariableNotFound(String variableName) {
        return JsonApiResponse.error(variableName + " is missing from body").toJson();
    }
}
