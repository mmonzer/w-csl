package com.csl.intercom.jsoncmd;

import com.csl.intercom.broker.CSLInterModuleCommunicationManager;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.logger.CSLApplicativeLogger;
import com.ucsl.json.Json;
import lombok.Getter;
import main.services.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * JServiceLoader is responsible for loading services offered by CSL-Client.
 * Services offer an API, in the form of a set of commands that can be called by other services and through a web API.
 * I also automatically create the "/apihelp" web page to documents the available commands.
 *
 * This module is responsible allows to register the endpoints and APIs of the supported services.
 * These API could be forwarded to csl-client through a websocket or through MQTT (remote APIs)
 *  via the CSLInterModuleCommunicationManager
 * or called directly through a web api
 *
 * Note: The MQTT broker is not used in this version of the code.
 */
public class JServiceLoader {

    private static final CSLApplicativeLogger logger = CSLApplicativeLogger.getLogger(JServiceLoader.class);

    public static CSLInterModuleCommunicationManager cslInterModuleCommunicationManager = null;
    
    // TODO: The mosquitto configuration is not used in this version of the code
    // TODO: Add an option in the application.properties that decides whether to use the mosquitto broker
    //  (instead of the websocket for remote apis) or not
    // TODO: Read the mosquitto config from the application.json file
    static MosquittoConfig mosquittoConfig = new MosquittoConfig();

    @Getter
    static String userDir = System.getProperty("user.dir");  // Global user directory path
    // Todo: Get the mqtt client name from the application.properties file (in case of multiple clients)
    // The name of MQTT client to use
    static String moduleName = "XXX";

    // The endpoints to register
    private static final List<String> listOfServiceNames = new ArrayList<>();
    // The list of APIs for the endpoints to register
    private static List<ApiCommands> listOfAPIToRegister = new ArrayList<>();
    // The map of the api and the api commands
    public static final HashMap<String, ApiCommands> apiMap = new HashMap<>();

    /**
     * Sets the user directory.
     *
     * @param dir The directory to set as the user directory.
     * @return The updated user directory path.
     */
    public static String setUserDir(String dir) {
        userDir = dir;
        return userDir;
    }

    /**
     * Displays information as a log message.
     *
     * @param message The message to display.
     * @return true if the message was displayed.
     */
    public static boolean displayInfo(String message) {
        logger.info("[********] " + message);
        return true;
    }

    /**
     * Gets the current system time in milliseconds.
     *
     * @return The current system time in milliseconds.
     */
    public static long getSystemCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Builds the full path within the user directory.
     *
     * @param dir The directory to build the path for.
     * @return The full path as a string.
     */
    public static String buildFullPathInUserDir(String dir) {
        if (dir == null) dir = "";

        if (dir.startsWith(getUserDir())) return dir;

        dir = dir.replace('\\', '/');
        dir = clean(dir);

        if (dir.startsWith(".")) dir = dir.substring(1);
        if (dir.startsWith(File.separator)) dir = dir.substring(1);

        return getUserDir() + File.separator + dir;
    }

    /**
     * Cleans the directory path by removing unnecessary references.
     *
     * @param path The path to clean.
     * @return The cleaned path.
     */
    private static String clean(String path) {
        String parentDir = "../";
        while (path.contains(parentDir)) {
            int index = path.indexOf(parentDir);
            String beforeParent = path.substring(0, index);
            String afterParent = path.substring(index + parentDir.length());
            path = beforeParent + afterParent;
        }
        return path;
    }

    /**
     * Adds an API command to the list of commands to be registered.
     *
     * @param api The API command to add.
     */
    public static void addApiCommands(ApiCommands api) {
        logger.trace("Registering API for HTTP: {}", api);
        listOfAPIToRegister.add(api);
    }

    /**
     * Returns the list of registered API commands.
     *
     * @return The list of API commands.
     */
    public static List<ApiCommands> getApiCommandsList() {
        return listOfAPIToRegister;
    }

    /**
     * Generates a help page for the APIs based on the parameters provided.
     *
     * @param params Parameters used to generate the help page.
     * @return The help page content as a string.
     */
    public static String getApiHelpPage(Json params) {
        return new ApiGetHelp().getHelp(listOfServiceNames, listOfServiceNames, params);
    }

    /***
     * * Registers a service by adding its API commands to the list and initializing the service.
     * 
     * Create an api for the registered service with a set of commands
     * to do so: it do the following:
     * 	- Adds the service name to the list of services
     * 	- Calls the init function of the service
     * 	- Adds the service commands
     * 	- Registers the api in the CSLInterModuleCommunicationManager
     * @param cslService implements ICSLService
     * @return
     */
    public static boolean registerService(Service cslService) {
        String name = cslService.getApiCommands().getName();
        listOfServiceNames.add(name);

        logger.info("Initializing service {}", name);
        boolean isServiceInitializedCorrectly = cslService.init();

        if (isServiceInitializedCorrectly) {
            logger.trace("Starting service {}", name);
            addApiCommands(cslService.getApiCommands());
            getCSLInterModuleCommunicationManager().registerAPI(cslService.getApiCommands());
        } else {
            logger.warn("Cannot initialize {}", name);
        }

        return isServiceInitializedCorrectly;
    }

    /**
     * Gets the CSLInterModuleCommunicationManager instance, initializing it if necessary.
     *
     * @return The CSLInterModuleCommunicationManager instance.
     */
    public static CSLInterModuleCommunicationManager getCSLInterModuleCommunicationManager() {
        if (cslInterModuleCommunicationManager == null) {
            // The mosquitto configuration is not used in this version of the code
            cslInterModuleCommunicationManager = new CSLInterModuleCommunicationManager(moduleName, mosquittoConfig);
            cslInterModuleCommunicationManager.setModuleName(moduleName);
        }
        return cslInterModuleCommunicationManager;
    }


    public static void registerAPICommands(ApiCommands api) {
        String path = api.getName().toLowerCase();
        logger.info("Registering API: <{}>", path);
        apiMap.put(path, api);
    }
}
