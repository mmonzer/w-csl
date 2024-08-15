package com.csl.intercom.jsoncmd;

import com.csl.intercom.broker.CSLInterModuleCommunicationManager;
import com.csl.intercom.broker.MosquittoConfig;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * JServiceLoader is responsible for loading services offered by CSL-Client.
 * Services offer an API, in the form of a set of commands that can be called by other services and through a web API.
 */
public class JServiceLoader {

    private static final Logger logger = LoggerFactory.getLogger(JServiceLoader.class);

    public static CSLInterModuleCommunicationManager cslInterModuleCommunicationManager = null;
    
    @Getter
    static MosquittoConfig mosquittoConfig = new MosquittoConfig();

    @Getter
    static String userDir = System.getProperty("user.dir");  // Global user directory path

    static String moduleName = "XXX";  // Global module name

    static List<IApiCommands> listOfAPIToRegister = new ArrayList<>();
    private static final List<String> listOfServiceNames = new ArrayList<>();

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
     * Sets the Mosquitto configuration.
     *
     * @param mosquittoConfig The Mosquitto configuration to set.
     */
    public static void setMosquittoConfig(MosquittoConfig mosquittoConfig) {
        JServiceLoader.mosquittoConfig = mosquittoConfig;
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
     * Finds classes within a JAR file that implement specific services.
     *
     * @param config   The configuration as JSON.
     * @param jarPath  The path to the JAR file.
     * @return A list of classes found in the JAR file.
     */
    public static List<Class> findClasses(Json config, String jarPath) {
        List<Class> classes = new ArrayList<>();
        File file = new File(jarPath);

        boolean traceLibrarySearch = JsonUtil.getBooleanFromJson(config, "service_loader/trace_library_search", false);

        try {
            URL jarfile = new URL("jar", "", "file:" + file.getAbsolutePath() + "!/");
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{jarfile});

            try (JarInputStream jarStream = new JarInputStream(new FileInputStream(jarPath))) {
            JarEntry jarEntry;
                while ((jarEntry = jarStream.getNextJarEntry()) != null) {
                    if (jarEntry.getName().endsWith(".class")) {
                        String className = jarEntry.getName();
                        if (traceLibrarySearch) {
                            logger.trace("Found {}", jarEntry.getName().replaceAll("/", "\\."));
                    }

                        if (!className.contains("$") && className.contains("main/services")) {
                            if (traceLibrarySearch) {
                                logger.trace("Loading {}", className);
                            }

                            className = className.replaceAll("/", "\\.");
                            className = className.substring(0, className.length() - 6); // remove .class

                            Class<?> loadedClass = classLoader.loadClass(className);
                        classes.add(loadedClass);

                            Object instance = loadedClass.getDeclaredConstructor().newInstance();

                            if (instance instanceof ICSLService) {
                                registerService((ICSLService) instance, config);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error while loading classes", e);
        }

        return classes;
    }

    /**
     * Loads all modules specified in the configuration.
     *
     * @param config The configuration as JSON.
     */
    public static void loadAllModules(Json config) {
        boolean traceLibrarySearch = JsonUtil.getBooleanFromJson(config, "service_loader/trace_library_search", false);

        Json serviceLoaderConfig = config.get("service_loader");
        if (serviceLoaderConfig == null) serviceLoaderConfig = Json.object();

        Json servicesArray = serviceLoaderConfig.get("services");
        String separator = System.getProperty("path.separator");

        String classpathExtensions = "";
        if (servicesArray != null) {
            for (Json servicePath : servicesArray.asJsonList()) {
                String path = servicePath.asString();
                if (traceLibrarySearch) logger.trace("Adding jar: {}", path);
                if (!classpathExtensions.isEmpty()) classpathExtensions += separator;
                classpathExtensions += path;
            }
        }

        String classPath = System.getProperty("java.class.path");
        classPath = classPath + separator + classpathExtensions;

        String[] classPathEntries = classPath.split(separator);

        for (String path : classPathEntries) {
            if (traceLibrarySearch) logger.trace("Find library: {}", path);
            if (path.endsWith(".jar")) {
                findClasses(config, path);
            }
        }
    }

    /**
     * Adds an API command to the list of commands to be registered.
     *
     * @param api The API command to add.
     */
    public static void addApiCommands(IApiCommands api) {
        logger.debug("Registering API for HTTP: {}", api);
        listOfAPIToRegister.add(api);
    }

    /**
     * Returns the list of registered API commands.
     *
     * @return The list of API commands.
     */
    public static List<IApiCommands> getApiCommandsList() {
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
     * @param config the configuration as json
     * @return
     */
    public static boolean registerService(ICSLService cslService, Json config) {
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
     * Sets the module name and configuration, and initializes the communication manager with these settings.
     *
     * @param moduleName The name of the module.
     * @param config     The Mosquitto configuration to set.
     */
    public static void init(String moduleName, MosquittoConfig config) {
        JServiceLoader.moduleName = moduleName;
        setMosquittoConfig(config);
        getCSLInterModuleCommunicationManager().setModuleName(moduleName);
    }

    /**
     * Gets the CSLInterModuleCommunicationManager instance, initializing it if necessary.
     *
     * @return The CSLInterModuleCommunicationManager instance.
     */
    public static CSLInterModuleCommunicationManager getCSLInterModuleCommunicationManager() {
        if (cslInterModuleCommunicationManager == null) {
            cslInterModuleCommunicationManager = new CSLInterModuleCommunicationManager(moduleName, getMosquittoConfig());
        }
        return cslInterModuleCommunicationManager;
    }
}
