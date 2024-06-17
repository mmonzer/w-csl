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
import com.csl.logger.CSLLogger;
import com.csl.logger.FileLogFactory;
import com.csl.modules.ModuleIDS;
import com.csl.web.CSLHttpServer;
import com.csl.web.CSLUDPServer;
import com.csl.web.database.DataBaseServer;
import com.ucsl.interfaces.*;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import com.wcsl.ids.IDSMainProcessorFactory;
import lombok.Getter;
import lombok.Setter;
import main.util.CSLRunningArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class CSLContext implements ICSLContext, ICSLLogger {
    private static final Logger logger = LoggerFactory.getLogger(CSLContext.class);

    public static String VERSION = "0.1.0-alpha1";

    private static final String DEFAULT_CONFIG_PATH = System.getProperty("user.dir");
    private static final String DEFAULT_CONFIG_FILE = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "runconfig" + File.separator + "CSLConfigIDS.json";
    private String configFileName = DEFAULT_CONFIG_PATH + File.separator + DEFAULT_CONFIG_FILE;

    @Getter
    private String cslConfDir = "";

    public static CSLContext instance = new CSLContext();
    public static CSLLogger cslLogger = CSLLogger.instance;

    boolean client = false, server = false;

    CSLAlertManager cslAlertManager = null;
    private IDSRunner idsRunner = null;

    @Getter
    private IDSParams idsParams = null;

    private CSLMqttBrokerHandler mqttBroker = null;
    private StatusNotifier statusNotifier = null;

    DataBaseServer databaseServer = null;

    CSLHttpServer cslHttpServer = null;
    ;
    CSLUDPServer cslUDPServer = null;
	
	
	/*config

		httpserver
		udpserver
		modulerunner
		interprocess

		database
		servicemanager
	 */

    boolean replayMode = false; // run faster for test (not in real time)

    long lastSystemCurrentTimeMillis = 0;

    @Getter
    int samplingTime = 100;

    @Setter
    @Getter
    private boolean exitCSL = false;

    @Getter
    private boolean debug = true;

    @Setter
    @Getter
    private boolean verbose = false;

    @Getter
    @Setter
    private boolean testMode = false;

    private IFileStoreService fileUtils;

    private IFileLogFactory fileLogFactory;

    private IIDSMainProcessor idsMainProcessor;


    boolean autostart = false;

    int nExecSteps = 0;

    boolean showProgression = false;

    int numberOfExecLoops = 1; // can run modules in several loop

    Json jConfig = null;

    Map<String, ModuleContext> modules = new HashMap<String, ModuleContext>();
    List<ModuleContext> inputExecList = new ArrayList<ModuleContext>();
    List<ModuleContext> outputExecList = new ArrayList<ModuleContext>();
    List<ModuleContext> stepExecList = new ArrayList<ModuleContext>();

    Map<String, Class<IModule>> moduleClassList = new HashMap<String, Class<IModule>>();

    ScheduledExecutorService scheduler = null;

    private long initialTime = 0;

    private CSLContext() {


    }


    public void setDebug(boolean debug) {
        this.debug = debug;
        CSLLogger.instance.setDebug(debug);
    }

    public void printError(String s) {
        System.err.println(s);
    }

    @Override
    public void logError(String msg) {
        // TODO Auto-generated method stub
        cslLogger.error(msg);
    }

    @Override
    public void logInfo(String msg) {
        // TODO Auto-generated method stub
        cslLogger.info(msg);
    }

    @Override
    public void logFatal(String msg) {
        // TODO Auto-generated method stub
        cslLogger.fatal(msg);
    }

    @Override
    public void logWarn(String msg) {
        // TODO Auto-generated method stub
        cslLogger.warn(msg);
    }

    @Override
    public void logDebug(String msg) {
        // TODO Auto-generated method stub
        cslLogger.debug(msg);
    }

    @Override
    public void setLogLevel(int v) {
        // TODO Auto-generated method stub
        cslLogger.setLogLevel(v);
    }

    public String getUserDir() {
        return JServiceLoader.getUserDir();
    }

    public IAlertManager getCSLAlertManager() {
        if (cslAlertManager == null) System.err.println("Warning, no alertManager registered");

        return cslAlertManager;
    }

    public IFileLogFactory getFileLogFactory() {
        if (fileLogFactory == null) fileLogFactory = new FileLogFactory();
        return fileLogFactory;
    }

    public IIDSRunner getIdsRunner() {
        if (idsRunner == null) System.err.println("Warning, no idsRunner registered");
        return idsRunner;
    }

//	public void setIdsRunner(IIDSRunner idsRunner) {
//		this.idsRunner = idsRunner;
//	}
    public DataBaseServer getDatabaseServer() {
        if (idsRunner == null) System.err.println("Warning, no Database server registered");

        return databaseServer;
    }

    private void setDatabaseServer(DataBaseServer databaseServer) {
        this.databaseServer = databaseServer;
    }

    public CSLHttpServer getCslHttpServer() {
        if (cslHttpServer == null) System.err.println("Warning, no CSL HTTP server registered");

        return cslHttpServer;
    }

    private void setCslHttpServer(CSLHttpServer cslHttpServer) {
        this.cslHttpServer = cslHttpServer;
    }

    public CSLUDPServer getCslUDPServer() {
        if (cslUDPServer == null) System.err.println("Warning, no CSL UDP server registered");

        return cslUDPServer;
    }

    private void setCslUDPServer(CSLUDPServer cslUDPServer) {
        this.cslUDPServer = cslUDPServer;
    }

    public String buildFullPathInUserDir(String dir) {

        return JServiceLoader.buildFullPathInUserDir(dir);
    }

    public void registerModuleClass(String name, Class c) {

        System.out.println("JMFXXXX__register:" + name + " --> " + c.getName());
        if (moduleClassList.get(name) != null) {
            logger.error("Module {} already registered", name);
            return;
        }
        moduleClassList.put(name, c);

    }

    public long getTimeFromStartingTime() {

        return getSystemCurrentTimeMillis() - initialTime;
    }

    public long getTimeSystemCurrent() {

        return getSystemCurrentTimeMillis();
    }

    public long getSystemCurrentTimeMillis() {
        if (isReplayMode()) {
            return lastSystemCurrentTimeMillis;
        }
        return System.currentTimeMillis();
    }

    public static final String EOL = System.getProperty("line.separator");

    private static String readFile(String filename) throws IOException {
        BufferedReader br = null;
        FileReader fr = null;

        try {
            fr = new FileReader(filename);
            br = new BufferedReader(fr);
            String nextLine = "";
            StringBuilder sb = new StringBuilder();
            while ((nextLine = br.readLine()) != null) {
                sb.append(nextLine); // note: BufferedReader strips the EOL character
                //   so we add a new one!
                sb.append(EOL);
            }
            return sb.toString();
        } finally {
            if (br != null) br.close();
            if (fr != null) fr.close();
        }
    }

    void readConfig(String f) {

        String content = "{}";
        try {
            content = readFile(f);
        } catch (IOException e) {
            logger.error("Cannot read config file :{}", f, e);
        }

        jConfig = Json.read(content);

        // Check the environment for existing variables
        Map<String, String> env = System.getenv();

        for (String section : jConfig.asJsonMap().keySet()) {
            if (jConfig.at(section).isObject()) {
                for (String var : jConfig.at(section).asJsonMap().keySet()) {
                    String separator = ".";
                    String env_var = "CSL" + separator + section.toUpperCase() + separator + var.toUpperCase();
                    if (env.containsKey(env_var)) {
                        jConfig.at(section).set(var, env.get(env_var));
                    }
                }
            }
        }
    }

    public Json getConfig() {

        if (jConfig == null) readConfig(configFileName);
        if (jConfig.get("web_server_conf") == null) {
            System.out.println("Invalid config file, update to new format");
            System.exit(0);
            ;
        }
        return jConfig;
    }

    private void setConfigFileName(String configFileName) {
        if (isVerbose()) System.out.println("Reading configuration from " + configFileName);
        this.configFileName = configFileName;

        getConfig();
    }

    public String getConfigFileName() {
        return this.configFileName;
    }

    private void setUserDir(String dir) {

        JServiceLoader.setUserDir(dir);
    }

    private Json safeGet(Json j, String name) {
        if (!j.has(name)) {
            j.set(name, Json.object());
        }
        return j.get(name);
    }

    public void updateConfigFromCSLRunningArgs(CSLRunningArgs cslRunningArgs) {


        // userdir
        // take the dir from config file if defined and no command line
        if (!cslRunningArgs.isUserDirDefault()) {
            setUserDir(cslRunningArgs.getUserDir());
        } else {
            String s = JsonUtil.getStringFromJson(jConfig, "csl/cslconf", "");
            if (!s.isEmpty())
                setUserDir(s);
            else {
                setUserDir(cslRunningArgs.getUserDir());  // set default
            }
        }


        // datadir (set in IDSRunner)
        if (cslRunningArgs.hasDataDir()) {
            //	safeztGet
        }

        // dirs data
        if (cslRunningArgs.hasDirForRecording()) {
            safeGet(getConfig(), IDSParams.IDS_CONF).set(IDSParams.PACKETS_DIR_FOR_RECORDING,
                    cslRunningArgs.getDirForRecording());
        }
        if (cslRunningArgs.hasDirForLearning()) {
            safeGet(getConfig(), IDSParams.IDS_CONF).set(IDSParams.PACKETS_DIR_FOR_LEARNING,
                    cslRunningArgs.getDirForLearning());
        }
        if (cslRunningArgs.hasDirForDetectionOffLine()) {
            safeGet(getConfig(), IDSParams.IDS_CONF).set(IDSParams.PACKETS_DIR_FOR_DETECTION_OFFLINE,
                    cslRunningArgs.getDirForDetectionOffline());
        }
        if (cslRunningArgs.hasIdsMode()) {
            safeGet(getConfig(), IDSParams.IDS_CONF).set(IDSParams.MODE,
                    cslRunningArgs.getIdsMode());
        }

        // logdir
        if (cslRunningArgs.hasLogDir()) {
            safeGet(getConfig(), "ids_conf").set("idstrace_dir", cslRunningArgs.getLogDir());
            // jcmd logs
            safeGet(getConfig(), "web_server_conf").set("log_dir", cslRunningArgs.getLogDir());
            // alerts
            safeGet(getConfig(), "alert_viewer").set("log_dir", cslRunningArgs.getLogDir());
        }

        // databasedir
        if (cslRunningArgs.hasDatabaseDir()) {

            safeGet(getConfig(), "database_server_conf").set("datafile_subdir", cslRunningArgs.getDatabasedir());
        }
    }

    public void init(CSLRunningArgs cslRunningArgs) {

        if (cslRunningArgs.hasError()) {
            System.out.println("Error :" + cslRunningArgs.getError());
            System.exit(0);
        }

        setConfigFileName(cslRunningArgs.getConfigFile());


        getConfig();

        updateConfigFromCSLRunningArgs(cslRunningArgs);

        String cslConf = JsonUtil.getStringFromJson(getConfig(), "csl/cslconf", "cslconf");
        this.cslConfDir = buildFullPathInUserDir(cslConf);

        setVerbose(cslRunningArgs.isVerbose());
        setDebug(cslRunningArgs.isDebug());

        setTestMode(cslRunningArgs.isTestparam());

        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());


        this.idsMainProcessor = IDSMainProcessorFactory.instance.createIDSMainProcessor(
                getConfig().get("ids_conf"),
                getCslConfDir(),
                this);

        idsMainProcessor.setFileLogFactory(getFileLogFactory());

        fileUtils = new FileStoreService(getCslConfDir());
        idsMainProcessor.setFileStoreServices(fileUtils);


        cslAlertManager = new CSLAlertManager(idsMainProcessor, getConfig().get("alert_viewer"));
        idsMainProcessor.setAlertManager(cslAlertManager);

        idsMainProcessor.setAlertFactory(new CSLAlertFactory());

        if (cslRunningArgs.isHasIdsRunner()) {
            idsParams = new IDSParams(idsMainProcessor);

            idsParams.initFromJson(getConfig(), cslRunningArgs.getDataDir(),
                    cslRunningArgs.isTestparam(), cslRunningArgs.isDoNotUseCurrentIDSParamsFileName()); // pworkingDir);


            if (cslRunningArgs.hasIdsMode())
                idsParams.setIDSMode(cslRunningArgs.getIdsMode());
            if (cslRunningArgs.hasDataSetForRecording())
                idsParams.setCurrentDataSetNameForRecording(cslRunningArgs.getDataSetForRecording());
            if (cslRunningArgs.hasDataSetForLearning())
                idsParams.setCurrentDataSetNameForLearning(cslRunningArgs.getDataSetForLearning());
            if (cslRunningArgs.hasDataSetForDetectionOffLine())
                idsParams.setCurrentDataSetNameForDetectionOffLine(cslRunningArgs.getDataSetForDetectionOffline());


        }

        idsMainProcessor.setConsole(idsParams.getConsole());

        setDatabaseServer(new DataBaseServer());

        setCslHttpServer(new CSLHttpServer());
        setCslUDPServer(new CSLUDPServer());
    }

    /***
     * Set as distant API (to be called using socket --> CSLWebSocketForJcmd.execJCmd)
     * (Tells the CslHttpServer that this is a remote API)
     * @param apiname
     */
    public void setApiRemote(String apiname) {
        if (getCslHttpServer() == null) return;

        getCslHttpServer().addRemoteApi(apiname);

    }

    public void postInit(boolean server, boolean client) {
        this.server = server;
        this.client = client;

        if (client)
            getDatabaseServer().init(getConfig().get("database_server_conf"));

        if (server)
            getCslHttpServer().initServer(getConfig().get("web_server_conf"));

        if (client)
            getCslUDPServer().initUDPServer(getConfig().get("udp_server_conf"));

        // The server should not send status notifications
        if (server)
            getStatusNotifier().setSendNotifications(false);

        if (client) {

            initModules();  // by default only ModuleIDS
            initTime();

            ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();

            idsRunner = new IDSRunner(idsParams, ids, this);
        }
    }

    public void start() {
        if (server) getCslHttpServer().start();

        if (client) getCslUDPServer().start();
        if (client) startExec();
        JServiceLoader.getCSLInterModuleCommunicationManager().start();

    }

    public void stop() {
        getCslHttpServer().stop();
        getCslUDPServer().stop();

        CSLContext.instance.stopExec();
        JServiceLoader.getCSLInterModuleCommunicationManager().stop();

        if (idsRunner != null) idsRunner.stop();
        if (mqttBroker != null) mqttBroker.close();
        if (statusNotifier != null) statusNotifier.close();
    }

    private void initTime() {
        initialTime = getSystemCurrentTimeMillis(); //  System.currentTimeMillis();
        samplingTime = JsonUtil.getIntFromJson(getConfig(), "module_exec/sampling_time", 100);
    }

    public ModuleContext getModuleContext(String name) {
        return modules.get(name);
    }

    private void initInternalModules() {

        new ModuleIDS();

    }

    private void initModules() {

        boolean notfound = true;

        String modulesPackageName =
                JsonUtil.getStringFromJson(getConfig(), "module_exec/modules_package_name", "modules");

        //test if csl.jar ?

        if (isVerbose()) System.out.println("Loading modules");
        initInternalModules();

        numberOfExecLoops = JsonUtil.getIntFromJson(getConfig(), "module_exec/number_of_exec_loops", 1);

        if (isVerbose()) System.out.println("Running " + numberOfExecLoops + " execution loops");

        Json j = getConfig().get("modules");

        Iterator<Json> itr = j.iterator();

        while (itr.hasNext()) {
            Json moduleDescriptor = itr.next();
            CSLContext.instance.logInfo(moduleDescriptor + " ");

            String mname = moduleDescriptor.get("name").asString();

            if (modules.get(mname) != null) {
                logger.error("A module with this name <" + mname + "> has already been declared");
                //existe dejae
            } else {
                String type = moduleDescriptor.get("type").asString();
                Class clazz = getModuleClass(type);

                if (clazz == null) {
                    logger.error("Cannot find modules of type <" + type + "> ");

                } else {
                    try {
                        IModule m = (IModule) clazz.newInstance();
                        ModuleContext mc = new ModuleContext();
                        mc.setClazz(clazz);
                        mc.setModule(m);
                        mc.setName(mname);
                        Json mconfig = moduleDescriptor.get("config");
                        mc.setConfig(mconfig);

                        modules.put(mname, mc);
                        CSLContext.instance.logInfo("Module:" + mc);
                        logger.info("Initialization of module {}[{}]", mname, type);
                    } catch (InstantiationException | IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }


        }


        // do init

        for (ModuleContext m : modules.values()) {

            if (m.getConfig().get("input_priority") != null) {
                int n = m.getConfig().get("input_priority").asInteger();
                m.setInputPriority(n);
            } else
                logger.warn("input priority undefined for module {}", m.getName());

            if (m.getConfig().get("step_priority") != null) {
                int n = m.getConfig().get("step_priority").asInteger();
                m.setStepPriority(n);
            } else
                logger.warn("step priority undefined for module {}", m.getName());

            if (m.getConfig().get("output_priority") != null) {
                int n = m.getConfig().get("output_priority").asInteger();
                m.setOutputPriority(n);
            } else
                logger.warn("output priority undefined for module {}", m.getName());

            if (m.getConfig().get("exec_loop_number") != null) {
                int n = m.getConfig().get("exec_loop_number").asInteger();
                if (n < 0) n = 0;
                if (n > numberOfExecLoops - 1) n = numberOfExecLoops - 1;
                m.setLoopNumber(n);
            }


            // do the module init
            m.getModule().init(this, m);

            inputExecList.add(m);
            outputExecList.add(m);
            stepExecList.add(m);
        }

        inputExecList.sort(new Comparator<ModuleContext>() {
            @Override
            public int compare(ModuleContext o1, ModuleContext o2) {
                return -o1.getInputPriority() + o2.getInputPriority();
            }
        });
        outputExecList.sort(new Comparator<ModuleContext>() {
            @Override
            public int compare(ModuleContext o1, ModuleContext o2) {
                return -o1.getOutputPriority() + o2.getOutputPriority();
            }
        });
        stepExecList.sort(new Comparator<ModuleContext>() {
            @Override
            public int compare(ModuleContext o1, ModuleContext o2) {
                return -o1.getStepPriority() + o2.getStepPriority();
            }
        });

        autostart = JsonUtil.getBooleanFromJson(getConfig(), "module_exec/autostart", true);
    }

    Class getModuleClass(String name) {
        return moduleClassList.get(name);
    }

    private void logResult(IResult r) {
        if (r == null)
            cslLogger.warn("No result");
        if (!r.isOK())
            cslLogger.error("Error " + r.getErrorCode() + ":" + r.getMessage());
    }

    Runnable task = new Runnable() {

        public void run() {

            cslLogger.debug("Sampling time:" + getTimeFromStartingTime());

            if (showProgression) {
                if (nExecSteps > 80) {
                }//System.out.println("");nExecSteps=0;}}
                else nExecSteps++;
            }

            try {
                for (int nloop = 0; nloop < numberOfExecLoops; nloop++) {
                    cslLogger.info("Exec loop #" + nloop);
                    for (ModuleContext m : inputExecList) {
                        if (m.getLoopNumber() == nloop) {
                            cslLogger.info("  input for " + m.getName());
                            IResult r = m.getModule().execInputPart(instance, m);
                            logResult(r);
                        }
                    }
                    for (ModuleContext m : stepExecList) {
                        if (m.getLoopNumber() == nloop) {
                            cslLogger.info("  exec for " + m.getName());
                            IResult r = m.getModule().execStepPart(instance, m);
                            logResult(r);
                        }
                    }
                    for (ModuleContext m : outputExecList) {
                        if (m.getLoopNumber() == nloop) {
                            cslLogger.info("  output for " + m.getName());
                            IResult r = m.getModule().execOutputPart(instance, m);
                            logResult(r);
                        }
                    }
                }
            } catch (Exception e) {
                //System.err.println(e);
                for (StackTraceElement z : e.getStackTrace()) {
                    System.err.println("" + z);
                }

            }
            if (isExitCSL()) {
                scheduler.shutdown();
                System.exit(0);
            }
        }

    };

    private boolean isReplayMode() {
        return replayMode;
    }

    public void startExec() {

        if (CSLContext.instance.isVerbose()) System.out.println("Starting modules execution");

        scheduler
                = Executors.newSingleThreadScheduledExecutor();


        int delay = samplingTime;
        CSLContext.instance.logInfo("Starting with sampling time :" + samplingTime + " ms");
        scheduler.scheduleAtFixedRate(task, 0, delay, TimeUnit.MILLISECONDS);

    }

    public void stopExec() {

        scheduler.shutdown();
    }

    public IIDSMainProcessor getIDSMainProcessor() {
        // TODO Auto-generated method stub
        return idsMainProcessor;
    }

    public CSLMqttBrokerHandler getMqttBroker() {
        if (mqttBroker == null) {
            mqttBroker = new CSLMqttBrokerHandler(getConfig());
        }
        return mqttBroker;
    }

    public StatusNotifier getStatusNotifier() {
        if (statusNotifier == null) {
            statusNotifier = new StatusNotifier(false);
        }
        return statusNotifier;
    }
}

