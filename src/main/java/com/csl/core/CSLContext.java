package com.csl.core;

import com.csl.alert.CSLAlertFactory;
import com.csl.alert.CSLAlertManager;
import com.csl.defaultclasses.FileStoreService;
import com.csl.ids.IDSParams;
import com.csl.ids.IDSRunner;
import com.csl.intercom.broker.CSLMqttBrokerHandler;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.intercom.status.StatusNotifier;
import com.csl.interfaces.ICSLContext;
import com.csl.interfaces.IIDSRunner;
import com.csl.interfaces.IModule;
import com.csl.logger.FileLogFactory;
import com.csl.modules.ModuleIDS;
import com.csl.web.CSLHttpServerJetty;
import com.csl.web.CSLUDPServer;
import com.ucsl.interfaces.*;
import com.wcsl.ids.IDSMainProcessorFactory;
import lombok.Getter;
import lombok.Setter;
import main.util.CSLRunningArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CSLContext implements ICSLContext {
    /**
     * Logger instance for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(CSLContext.class);

    /**
     * Version of {@link CSLContext}.
     */
    public static String VERSION = "0.1.0-alpha1";

    @Getter
    private String cslConfDir = "";

    /**
     * Singleton instance of the class.
     */
    public static CSLContext instance = new CSLContext();

    /**
     * Indicates if the context is running as a server (true), client (false), or uninitialized (null).
     */
    private Boolean isServer = null;

    /**
     * Instance of the alert manager.
     */
    private CSLAlertManager cslAlertManager = null;

    private IDSRunner idsRunner = null;

    @Getter
    private IDSParams idsParams = null;

    private CSLMqttBrokerHandler mqttBroker = null;

    private StatusNotifier statusNotifier = null;

    /**
     * Instance of the HTTP server.
     */
    private CSLHttpServerJetty cslHttpServer = null;

    /**
     * Instance of the UDP server.
     */
    private CSLUDPServer cslUDPServer = null;

    private boolean replayMode = false;
    private long lastSystemCurrentTimeMillis = 0;
    private int samplingTime = 100;

    @Getter
    private boolean exitCSL = false;

    private boolean autostart = false;
    private int nExecSteps = 0;
    private boolean showProgression = false;

    private int numberOfExecLoops = 1;

    private final Map<String, com.csl.core.ModuleContext> modules = new HashMap<>();
    private final List<com.csl.core.ModuleContext> inputExecList = new ArrayList<>();
    private final List<com.csl.core.ModuleContext> outputExecList = new ArrayList<>();
    private final List<com.csl.core.ModuleContext> stepExecList = new ArrayList<>();

    private final Map<String, Class<IModule>> moduleClassList = new HashMap<>();

    private ScheduledExecutorService scheduler = null;

    private long initialTime = 0;

    @Getter
    @Setter
    private boolean testMode = false;

    private IFileStoreService fileUtils;
    private IFileLogFactory fileLogFactory;
    private IIDSMainProcessor idsMainProcessor;

    /**
     * Private constructor for singleton pattern.
     */
    private CSLContext() {
        // Singleton instance
    }

    /**
     * Prints an error message to the standard error stream.
     *
     * @param message The error message to print.
     */
    public void printError(String message) {
        logger.error(message);
    }

    /**
     * Gets the user directory.
     *
     * @return The user directory as a string.
     */
    public String getUserDir() {
        return JServiceLoader.getUserDir();
    }

    /**
     * Gets the alert manager instance, logging a warning if not registered.
     *
     * @return The alert manager instance.
     */
    public IAlertManager getCSLAlertManager() {
        if (cslAlertManager == null) {
            logger.warn("Warning, no alertManager registered");
        }
        return cslAlertManager;
    }

    /**
     * Gets the file log factory instance.
     *
     * @return The file log factory instance.
     */
    public IFileLogFactory getFileLogFactory() {
        if (fileLogFactory == null) {
            fileLogFactory = new FileLogFactory();
        }
        return fileLogFactory;
    }

    /**
     * Gets the IDS runner instance, logging a warning if not registered.
     *
     * @return The IDS runner instance.
     */
    public IIDSRunner getIdsRunner() {
        if (idsRunner == null) {
            logger.warn("Warning, no idsRunner registered");
        }
        return idsRunner;
    }

    /**
     * Gets the HTTP server instance, logging a warning if not registered.
     *
     * @return The HTTP server instance.
     */
    public CSLHttpServerJetty getCslHttpServer() {
        if (cslHttpServer == null) {
            logger.warn("Warning, no CSL HTTP server registered");
        }
        return cslHttpServer;
    }

    /**
     * Sets the HTTP server instance.
     *
     * @param cslHttpServer The HTTP server instance to set.
     */
    private void setCslHttpServer(CSLHttpServerJetty cslHttpServer) {
        this.cslHttpServer = cslHttpServer;
    }

    /**
     * Gets the UDP server instance, logging a warning if not registered.
     *
     * @return The UDP server instance.
     */
    public CSLUDPServer getCslUDPServer() {
        if (cslUDPServer == null) {
            logger.warn("Warning, no CSL UDP server registered");
        }
        return cslUDPServer;
    }

    /**
     * Sets the UDP server instance.
     *
     * @param cslUDPServer The UDP server instance to set.
     */
    private void setCslUDPServer(CSLUDPServer cslUDPServer) {
        this.cslUDPServer = cslUDPServer;
    }

    /**
     * Builds the full path within the user directory.
     *
     * @param dir The directory to build the path for.
     * @return The full path as a string.
     */
    public String buildFullPathInUserDir(String dir) {
        return JServiceLoader.buildFullPathInUserDir(dir);
    }

    /**
     * Registers a module class with the given name.
     *
     * @param name The name of the module.
     * @param c    The class of the module.
     */
    public void registerModuleClass(String name, Class<?> c) {
        if (moduleClassList.get(name) != null) {
            logger.error("Module {} already registered", name);
            return;
        }
        moduleClassList.put(name, (Class<IModule>) c);
    }

    /**
     * Gets the time elapsed since the context was started.
     *
     * @return The time in milliseconds.
     */
    public long getTimeFromStartingTime() {
        return getSystemCurrentTimeMillis() - initialTime;
    }

    /**
     * Gets the current system time, accounting for replay mode if enabled.
     *
     * @return The current system time in milliseconds.
     */
    public long getSystemCurrentTimeMillis() {
        if (isReplayMode()) {
            return lastSystemCurrentTimeMillis;
        }
        return System.currentTimeMillis();
    }

    /**
     * Gets the configuration instance.
     *
     * @return The configuration instance.
     */
    public Config getConfig() {
        if (Config.instance.Server == null) {
            logger.error("Invalid config file, update to new format");
            System.exit(0);
        }
        return Config.instance;
    }

    /**
     * Sets the configuration file name and reloads the configuration.
     *
     * @param configFileName The configuration file name.
     * @return The configuration instance.
     */
    private Config setConfigFileName(String configFileName) {
        logger.debug("Reading configuration from {}", configFileName);
        Config.reload(configFileName);
        return getConfig();
    }

    /**
     * Sets the user directory.
     *
     * @param dir The user directory to set.
     */
    private void setUserDir(String dir) {
        JServiceLoader.setUserDir(dir);
    }

    /**
     * Updates the configuration based on the provided {@link CSLRunningArgs}.
     *
     * @param cslRunningArgs The arguments for CSL running.
     */
    public void updateConfigFromCSLRunningArgs(CSLRunningArgs cslRunningArgs) {
        // User directory
        if (!cslRunningArgs.isUserDirDefault()) {
            setUserDir(cslRunningArgs.getUserDir());
        } else {
            String s = "";
            if (!s.isEmpty()) {
                setUserDir(s);
            } else {
                setUserDir(cslRunningArgs.getUserDir()); // set default
            }
        }

        // Update other directories
        if (cslRunningArgs.hasDirForRecording()) {
            Config.instance.IdsConf.setPacketsDirForRecording(cslRunningArgs.getDirForRecording());
        }
        if (cslRunningArgs.hasDirForLearning()) {
            Config.instance.IdsConf.setPacketsDirForLearning(cslRunningArgs.getDirForLearning());
        }
        if (cslRunningArgs.hasDirForDetectionOffLine()) {
            Config.instance.IdsConf.setPacketsDirForDetectionOffline(cslRunningArgs.getDirForDetectionOffline());
        }
        if (cslRunningArgs.hasIdsMode()) {
            Config.instance.IdsConf.setMode(cslRunningArgs.getIdsMode());
        }

        // Log directory
        if (cslRunningArgs.hasLogDir()) {
            Config.instance.IdsConf.setIdstraceDir(cslRunningArgs.getLogDir());
            Config.instance.Server.setLogDir(cslRunningArgs.getLogDir());
            Config.instance.AlertViewer.setLogDir(cslRunningArgs.getLogDir());
        }
    }

    /**
     * Initializes the CSL context with the provided running arguments.
     *
     * @param cslRunningArgs The running arguments.
     */
    public void init(CSLRunningArgs cslRunningArgs) {
        if (cslRunningArgs.hasError()) {
            logger.error("Error: {}", cslRunningArgs.getError());
            System.exit(0);
        }

        setConfigFileName(cslRunningArgs.getConfigFile());
        getConfig();
        updateConfigFromCSLRunningArgs(cslRunningArgs);

        this.cslConfDir = buildFullPathInUserDir("cslconf");

        org.eclipse.jetty.util.log.Log.setLog(new com.csl.core.NoLogging());

        this.idsMainProcessor = IDSMainProcessorFactory.instance.createIDSMainProcessor(
                Config.instance.IdsConf, getCslConfDir());

        idsMainProcessor.setFileLogFactory(getFileLogFactory());
        fileUtils = new FileStoreService(getCslConfDir());
        idsMainProcessor.setFileStoreServices(fileUtils);

        cslAlertManager = new CSLAlertManager(idsMainProcessor, getConfig().AlertViewer);
        idsMainProcessor.setAlertManager(cslAlertManager);
        idsMainProcessor.setAlertFactory(new CSLAlertFactory());

        if (cslRunningArgs.isHasIdsRunner()) {
            idsParams = new IDSParams(idsMainProcessor);
            idsParams.initFromJson(getConfig(), cslRunningArgs.getDataDir(),
                    cslRunningArgs.isTestparam(), cslRunningArgs.isDoNotUseCurrentIDSParamsFileName());

            if (cslRunningArgs.hasIdsMode()) {
                idsParams.setIDSMode(cslRunningArgs.getIdsMode());
            }
            if (cslRunningArgs.hasDataSetForRecording()) {
                idsParams.setCurrentDataSetNameForRecording(cslRunningArgs.getDataSetForRecording());
            }
            if (cslRunningArgs.hasDataSetForLearning()) {
                idsParams.setCurrentDataSetNameForLearning(cslRunningArgs.getDataSetForLearning());
            }
            if (cslRunningArgs.hasDataSetForDetectionOffLine()) {
                idsParams.setCurrentDataSetNameForDetectionOffLine(cslRunningArgs.getDataSetForDetectionOffline());
            }
        }

        idsMainProcessor.setConsole(idsParams.getConsole());

        setCslHttpServer(new CSLHttpServerJetty());
        setCslUDPServer(new CSLUDPServer());
    }

    /**
     * Sets the API as a remote API to be accessible via the HTTP server.
     *
     * @param apiname The name of the API.
     */
    public void registerHttpEndpoint(String apiname) {
        if (getCslHttpServer() == null) return;
        getCslHttpServer().registerHttpEndpoint(apiname);
    }

    /**
     * Performs post-initialization tasks, setting up the server and/or client configurations.
     *
     * @param isServer Indicates if the context should operate as a server (true), client (false), or uninitialized (null).
     */
    public void postInit(Boolean isServer) {
        this.isServer = isServer;

        if (isServer == null) {
            logger.error("CSLContext not initialized as server or client");
            return;
        }

        if (isServer) {
            getCslHttpServer().initServer(Config.instance.Server);
            getStatusNotifier().setSendNotifications(false);
        } else {
            getCslUDPServer().initUDPServer(Config.instance.UdpServerConf);
            initDynamicModules();
            initTime();

            ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
            idsRunner = new IDSRunner(idsParams, ids);
        }
    }

    /**
     * Starts the HTTP and UDP servers.
     */
    public void startServers() {
        if (isServer == null) {
            logger.error("CSLContext not initialized as server or client, cannot start servers");
            return;
        }

        if (isServer) {
            getCslHttpServer().start();
        } else {
            getCslUDPServer().start();
            startExec();
        }

        JServiceLoader.getCSLInterModuleCommunicationManager().start();
    }

    /**
     * Stops the HTTP and UDP servers.
     */
    public void stopServers() {
        getCslHttpServer().stop();
        getCslUDPServer().stop();

        CSLContext.instance.stopExec();
        JServiceLoader.getCSLInterModuleCommunicationManager().stop();

        if (idsRunner != null) idsRunner.stop();
        if (mqttBroker != null) mqttBroker.close();
        if (statusNotifier != null) statusNotifier.close();
    }

    /**
     * Initializes the timing settings for module execution.
     */
    private void initTime() {
        initialTime = getSystemCurrentTimeMillis();
        samplingTime = Config.instance.ModuleExec.getSamplingTime();
    }

    /**
     * Gets the module context by name.
     *
     * @param name The name of the module.
     * @return The module context.
     */
    public com.csl.core.ModuleContext getModuleContext(String name) {
        return modules.get(name);
    }

    /**
     * Initializes internal modules, starting with {@link ModuleIDS}.
     */
    private void initInternalModules() {
        new ModuleIDS();
    }

    /**
     * Initializes the modules based on the configuration.
     */
    private void initDynamicModules() {
        String modulesPackageName = Config.instance.ModuleExec.getModulesPackageName();
        logger.debug("Loading modules");
        
        initInternalModules();

        numberOfExecLoops = Config.instance.ModuleExec.getNumberOfExecLoops();
        logger.debug("Running {} execution loops", numberOfExecLoops);

        for (Config.Module moduleDescriptor : Config.instance.Modules) {
            String mname = moduleDescriptor.getName();

            if (modules.get(mname) != null) {
                logger.error("A module with this name <{}> has already been declared", mname);
            } else {
                String type = moduleDescriptor.getType();
                Class<?> clazz = getModuleClass(type);

                if (clazz == null) {
                    logger.error("Cannot find modules of type <{}>", type);
                } else {
                    try {
                        IModule m = (IModule) clazz.getDeclaredConstructor().newInstance();
                        com.csl.core.ModuleContext mc = new com.csl.core.ModuleContext();
                        mc.setClazz((Class<IModule>) clazz);
                        mc.setModule(m);
                        mc.setName(mname);
                        mc.setConfig(moduleDescriptor.getConfig());

                        modules.put(mname, mc);
                        logger.info("Initialization of module {}[{}]", mname, type);
                    } catch (Exception e) {
                        logger.error("Error initializing module {}: {}", mname, e.getMessage(), e);
                    }
                }
            }
        }

        autostart = Config.instance.ModuleExec.getAutostart();
    }

    /**
     * Gets the module class by name.
     *
     * @param name The name of the module class.
     * @return The module class.
     */
    private Class<?> getModuleClass(String name) {
        return moduleClassList.get(name);
    }

    /**
     * Logs the result of a module execution.
     *
     * @param r The result to log.
     */
    private void logResult(IResult r) {
        if (r == null) {
            logger.warn("No result");
        } else if (!r.isOK()) {
            logger.error("Error {}: {}", r.getErrorCode(), r.getMessage());
        }
    }

    /**
     * Task that executes the modules in the defined order.
     */
    private final Runnable task = () -> {
        logger.trace("Sampling time: {}", getTimeFromStartingTime());

        if (showProgression && nExecSteps > 80) {
            nExecSteps = 0;
        } else {
            nExecSteps++;
            }

            try {
                for (int nloop = 0; nloop < numberOfExecLoops; nloop++) {
                logger.trace("Exec loop #{}", nloop);
                    for (com.csl.core.ModuleContext m : inputExecList) {
                        if (m.getLoopNumber() == nloop) {
                            logger.trace("  input for {}", m.getName());
                            IResult r = m.getModule().execInputPart(instance, m);
                            logResult(r);
                        }
                    }
                    for (com.csl.core.ModuleContext m : stepExecList) {
                        if (m.getLoopNumber() == nloop) {
                            logger.trace("  exec for {}", m.getName());
                            IResult r = m.getModule().execStepPart(instance, m);
                            logResult(r);
                        }
                    }
                    for (com.csl.core.ModuleContext m : outputExecList) {
                        if (m.getLoopNumber() == nloop) {
                            logger.trace("  output for {}", m.getName());
                            IResult r = m.getModule().execOutputPart(instance, m);
                            logResult(r);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error during execution: {}", e.getMessage(), e);
            }

            if (isExitCSL()) {
                scheduler.shutdown();
                System.exit(0);
            }
    };

    /**
     * Checks if replay mode is enabled.
     *
     * @return True if replay mode is enabled, false otherwise.
     */
    private boolean isReplayMode() {
        return replayMode;
    }

    /**
     * Starts the execution of the modules.
     */
    public void startExec() {
        logger.debug("Starting modules execution");

        scheduler = Executors.newSingleThreadScheduledExecutor();
        int delay = samplingTime;
        logger.info("Starting with sampling time: {} ms", samplingTime);
        scheduler.scheduleAtFixedRate(task, 0, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the execution of the modules.
     */
    public void stopExec() {
        scheduler.shutdown();
    }

    /**
     * Gets the IDS main processor.
     *
     * @return The IDS main processor.
     */
    public IIDSMainProcessor getIDSMainProcessor() {
        return idsMainProcessor;
    }

    /**
     * Gets the MQTT broker handler.
     *
     * @return The MQTT broker handler.
     */
    public CSLMqttBrokerHandler getMqttBroker() {
        if (mqttBroker == null) {
            mqttBroker = new CSLMqttBrokerHandler(getConfig());
        }
        return mqttBroker;
    }

    /**
     * Gets the status notifier.
     *
     * @return The status notifier.
     */
    public StatusNotifier getStatusNotifier() {
        if (statusNotifier == null) {
            statusNotifier = new StatusNotifier(false);
        }
        return statusNotifier;
    }
}
