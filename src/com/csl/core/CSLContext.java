package com.csl.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.csl.alert.CSLAlertFactory;
import com.csl.alert.CSLAlertManager;
import com.csl.defaultclasses.FileStoreService;
import com.csl.ids.IDSParams;
import com.csl.ids.IDSRunner;
import com.csl.intercom.broker.CSLMqttBrokerHandler;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.interfaces.ICSLContext;
import com.csl.interfaces.IIDSRunner;
import com.csl.interfaces.IModule;
import com.csl.logger.CSLLogger;
import com.csl.logger.FileLogFactory;
import com.csl.modules.ModuleIDS;
//import com.csl.variables.ParamDescriptor;
import com.csl.web.CSLHttpServer;
import com.csl.web.CSLUDPServer;
import com.csl.web.database.DataBaseServer;
import com.ucsl.interfaces.IIDSMainProcessor;
//mport com.xcsl.ids.IDSTrace;
import com.ucsl.interfaces.IAlertManager;
import com.ucsl.interfaces.ICSLLogger;
import com.ucsl.interfaces.IFileLogFactory;
import com.ucsl.interfaces.IFileStoreService;
import com.ucsl.interfaces.IResult;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import com.wcsl.ids.IDSMainProcessorFactory;

import main.util.CSLRunningArgs;


public class CSLContext implements ICSLContext, ICSLLogger {

	public static String VERSION="0.1.0-alpha1";

	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


	private static String DEFAULT_CONFIG_PATH=System.getProperty("user.dir");
	private static String DEFAULT_CONFIG_FILE="runconfig"+File.separator+"CSLConfigIDS.json";
	private String configFileName= DEFAULT_CONFIG_PATH+File.separator+DEFAULT_CONFIG_FILE;

	//private String userDir=System.getProperty("user.dir");
	private String cslConfDir="";

	public static CSLContext instance = new CSLContext();
	public static CSLLogger cslLogger= CSLLogger.instance;


	boolean client=false, server=false;

	CSLAlertManager cslAlertManager=null;
	private IDSRunner idsRunner=null;
	private IDSParams idsParams=null;
	private CSLMqttBrokerHandler mqttBroker = null;

	
	DataBaseServer databaseServer=null;
	

	CSLHttpServer cslHttpServer=null;;
	CSLUDPServer cslUDPServer=null;
	
	
	/*config

		httpserver
		udpserver
		modulerunner
		interprocess

		database
		servicemanager
	 */
	//private boolean CSLServerStarted=false;

	//private CSLConfigFileManager cslConfigFileManager=new CSLConfigFileManager();
	//	private CSLFileServer cslFileServer=null;  // created if port>0 and declard in configfile


	boolean replayMode=false; // run faster for test (not in real time)
	// time is set with setNewSystemCurre
	//   setSystemCurrentTimeMillis() {
	long lastSystemCurrentTimeMillis=0;
	long currentSamplingTime=0;

	int samplingTime=100;
	private boolean exitCSL=false;
	boolean autostart=false;
	int nExecSteps=0;
	boolean showProgression=false;

	int numberOfExecLoops =1 ; // can run modules in several loop
	//  on each loop the order is :  input exec output
	//	loops are executed one after the other (in fact there is one loop for control & data treatement 
	// and one loop for process simulation

	//private String config_path= DEFAULT_CONFIG_PATH;
	//HashMap<String,ParamDescriptor> listOfGlobalParamsDescriptors=new HashMap<String,ParamDescriptor>();



	Json jConfig=null;
	//List<VariablesTable> localVariablesTableList= new ArrayList<VariablesTable>();


	Map<String, ModuleContext> modules=new HashMap<String,ModuleContext>();
	List<ModuleContext> inputExecList= new ArrayList<ModuleContext>();
	List<ModuleContext> outputExecList= new ArrayList<ModuleContext>();
	List<ModuleContext> stepExecList= new ArrayList<ModuleContext>();


	Map<String, Class<IModule>> moduleClassList= new HashMap<String,Class<IModule>>();


	ScheduledExecutorService scheduler=null;

	private int currentPortForUCP=9001;
	//private int currentPortForWEB=9000;
	//private int currentPortForFileserver=-1;

	private long initialTime=0;

	private static int PORTMAX=9999;

	private static int CSL_ID=1234;

	private boolean debug=true;

	private Boolean openBrowser;
	String homePageName="";


	//private String[] args;
	private boolean verbose=false;
	private boolean testMode=false;

	private IFileStoreService fileUtils;

	private IFileLogFactory fileLogFactory;

	private IIDSMainProcessor idsMainProcessor;


	
	private CSLContext() {
		
		
		
	}
	
	
	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
		CSLLogger.instance.setDebug(debug);
	}

	public void printError(String s) {
		System.err.println(s);
	}

	public boolean isExitCSL() {
		return exitCSL;
	}

	public void setExitCSL(boolean exitCSL) {
		this.exitCSL = exitCSL;
	}

	//@Override
	public Json takeObjectFromInputQueue(int n) {
		// TODO Auto-generated method stub
		return null;
	}

	//@Override
	public void putObjectToOutputQueue(int n, Json j) {
		// TODO Auto-generated method stub

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
		if (cslAlertManager==null) System.err.println("Warning, no alertManager registered");

		return cslAlertManager;
	}

	

	public IFileLogFactory getFileLogFactory() {
		if (fileLogFactory==null) fileLogFactory= new FileLogFactory();
		return fileLogFactory;
	}

	public IIDSRunner getIdsRunner() {
		if (idsRunner==null) System.err.println("Warning, no idsRunner registered");
		return idsRunner;
	}

	
	
	
	
//	public void setIdsRunner(IIDSRunner idsRunner) {
//		this.idsRunner = idsRunner;
//	}

	public DataBaseServer getDatabaseServer() {
		if (idsRunner==null) System.err.println("Warning, no Database server registered");
		
		return databaseServer;
	}

	private void setDatabaseServer(DataBaseServer databaseServer) {
		this.databaseServer = databaseServer;
	}

	public CSLHttpServer getCslHttpServer() {
		if (cslHttpServer==null) System.err.println("Warning, no CSL HTTP server registered");
		
		return cslHttpServer;
	}

	private void setCslHttpServer(CSLHttpServer cslHttpServer) {
		this.cslHttpServer = cslHttpServer;
	}

	public CSLUDPServer getCslUDPServer() {
		if (cslHttpServer==null) System.err.println("Warning, no CSL UDP server registered");
		
		return cslUDPServer;
	}

	private void setCslUDPServer(CSLUDPServer cslUDPServer) {
		this.cslUDPServer = cslUDPServer;
	}

	public String buildFullPathInUserDir(String dir) {

		return JServiceLoader.buildFullPathInUserDir(dir);
//		if (dir==null) dir ="";
//
//		if (dir.startsWith(getUserDir())) return dir;
//
//		dir=dir.replace('\\','/');
//
//		dir=clean(dir);
//
//		if (dir.startsWith(".")) dir =dir.substring(1);
//		if (dir.startsWith(File.separator)) dir =dir.substring(1);
//
//		return getUserDir()+File.separator+dir;
	}

	
	public String buildFullPathInConfDir(String dir) {

		if (dir==null) dir ="";
		dir=dir.replace('\\','/');

		dir=clean(dir);

		if (dir.startsWith(".")) dir =dir.substring(1);
		if (dir.startsWith(File.separator)) dir =dir.substring(1);

		return getCslConfDir()+File.separator+dir;
	}

	private String clean(String s) {
		String z="../";
		while (s.indexOf(z)>=0) {
			int n= s.indexOf(z);
			String s1=s.substring(0,n);
			String s2=s.substring(n+z.length(),s.length());
			s=s1+s2;
		}
		return s;
	}





	//	public int getCurrentPortForWEB() {
	//		return currentPortForWEB;
	//	}
	//
	//	private void setCurrentPortForWEB(int currentPortForWEB) {
	//		this.currentPortForWEB = currentPortForWEB;
	//	}






	//	public int getCurrentPortForFileserver() {
	//		return currentPortForFileserver;
	//	}
	//
	//	public void setCurrentPortForFileserver(int currentPortForFileserver) {
	//		this.currentPortForFileserver = currentPortForFileserver;
	//	}

	public void registerModuleClass(String name,Class c) {

		System.out.println("JMFXXXX__register:"+name+" --> "+c.getName());
		if (moduleClassList.get(name)!=null) {
			cslLogger.error("Module "+name+" already registered");
			return;
		}
		moduleClassList.put(name,c);

	}
	//==


	public long getTimeFromStartingTime() {

		//return System.currentTimeMillis()-initialTime;
		return getSystemCurrentTimeMillis()-initialTime;
	}

	public long getTimeSystemCurrent() {

		//return System.currentTimeMillis()-initialTime;
		return getSystemCurrentTimeMillis();
	}


	public long getSystemCurrentTimeMillis() {
		if (isReplayMode()) {
			return lastSystemCurrentTimeMillis;
		}
		return System.currentTimeMillis();
	}

	public String getSystemCurrentTimeMillisAsFormattedString() {
		return sdf.format(getSystemCurrentTimeMillis());
	}


	public void setSystemCurrentTimeMillis(long t) {

		//System.out.print("TRY TO SET TIME TO :"+t+"   ");

		//System.out.println("SET TIME :"+t+" :"+(sdf.format(t)));
		//System.out.println("  LAST   :"+t+" :"+(sdf.format(lastSystemCurrentTimeMillis)));
		//System.out.println("  CURRENT:"+t+" :"+(sdf.format(currentSamplingTime)));

		if (t<lastSystemCurrentTimeMillis) {
			//System.err.println("Invalid time "+t+" last="+lastSystemCurrentTimeMillis+" delta ="+(lastSystemCurrentTimeMillis-t));
			return;
		}
		while (currentSamplingTime<=t) {
			currentSamplingTime=currentSamplingTime+samplingTime;
			lastSystemCurrentTimeMillis=currentSamplingTime;

			//System.out.println(".");
			//idsMainProcessor.log(IDSTrace.EXEC, "Exec:"+getTimeFromStartingTime()+"   Time="+(sdf.format(lastSystemCurrentTimeMillis)));

			execOneRunCycle();
			//System.out.println("  Cycle :"+currentSamplingTime);
		}
		lastSystemCurrentTimeMillis=t;
		//System.out.println("SET TIME TO :"+t);
	}


	public int getSamplingTime() {
		return samplingTime;
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
		}
		finally {
			if (br != null) br.close();
			if (fr != null) fr.close();
		}
	}

	void readConfig(String f) {
		//path, String fileName) {


		//URL  url= new URL("file:///"+fileName);
		String content="{}";
		//String f=path+ File.separator+fileName;
		try {
			content = readFile(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			CSLContext.cslLogger.error("Cannot read config file :"+f);
		}

		jConfig=Json.read(content);

		// Check the environment for existing variables
		Map<String,String> env = System.getenv();

		for (String section : jConfig.asJsonMap().keySet())
		{
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

	//	private Json loadConfigFile(String f) {
	//		//path, String fileName) {
	//
	//		this.configFileName=f;
	//
	//		
	//
	//		jConfig=getConfig();
	//		return jConfig;
	//	}



	public Json getConfig() {

		if (jConfig==null) readConfig(configFileName);
		if (jConfig.get("web_server_conf")==null) {
			System.out.println("Invalid config file, update to new format");
			System.exit(0);;
		}
		//DEFAULT_CONFIG_PATH, DEFAULT_CONFIG_FILE);
		return jConfig;
	}





	//	VariablesTable getLocalVariablesTable(String moduleName) {
	//		//return globalVariablesTable;
	//		
	//	}
	//
	//	Json getModuleConfig(String moduleName) {
	//		
	//	}

	private Json setConfigFileName(String configFileName)  {

		if (isVerbose()) System.out.println("Reading configuration from "+configFileName);
		this.configFileName=configFileName;


		return getConfig();

	}

	public String getConfigFileName() {
		return this.configFileName;
	}

	public String getCslConfDir() {
		return cslConfDir;
	}

	//	public Json setConfigFileName(String configFileName, String userDir)  {
	//
	//		this.userDir=userDir;
	//		
	//		return setConfigFileName(configFileName);
	//		
	//	}

	private void setUserDir(String dir) {
		
		JServiceLoader.setUserDir(dir);
	}

	//	public Json setConfigFileName(String configFileName,String userDir, String[] args)  {
	//
	//		//setArgs(args);
	//		if (isVerbose()) System.out.println("Reading configuration from "+configFileName);
	//		
	//		return setConfigFileName(configFileName, userDir);
	//	}


	//	public void initWithConfigFile(String configFileName)  {
	//
	//		setConfigFileName(configFileName);
	//		
	//		init();
	//		
	//	}
	//	
	//	public void initWithConfigFileAndArgs(String configFileName,String[] args)  {
	//
	//		
	//		setConfigFileName(configFileName, System.getProperty("user.dir"), args);
	//		init();
	//		
	//	}

	
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
			}
		else {
			String s=JsonUtil.getStringFromJson(jConfig, "csl/cslconf", "");
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
					cslRunningArgs.getDirForDetectionOffLine());
		}
		if (cslRunningArgs.hasIdsMode()) {
			safeGet(getConfig(), IDSParams.IDS_CONF).set(IDSParams.MODE,
					cslRunningArgs.getIdsMode());
		}
		
		// logdir
		if (cslRunningArgs.hasLogDir()) {
			safeGet(getConfig(),"ids_conf").set("idstrace_dir", cslRunningArgs.getLogDir());
			// jcmd logs
			safeGet(getConfig(),"web_server_conf").set("log_dir", cslRunningArgs.getLogDir());
			// alerts
			safeGet(getConfig(),"alert_viewer").set("log_dir",cslRunningArgs.getLogDir());
		}
		
		// databasedir
		if (cslRunningArgs.hasDatabaseDir() ) {
			
			safeGet(getConfig(), "database_server_conf").set("datafile_subdir",cslRunningArgs.getDatabasedir());
			
		}
		
		
		//System.out.println(JsonUtil.prettyPrint(getConfig()));
		
	}

	

	public String getDefaultIdsDataDir() {
		return CSLContext.instance.getUserDir()+File.separator+"idsdata";
		
	}
	
	public void init(CSLRunningArgs cslRunningArgs) {


		if (cslRunningArgs.hasError()) {
			System.out.println("Error :"+cslRunningArgs.getError());
			System.exit(0);
		}

		setConfigFileName(cslRunningArgs.getConfigFile());


		getConfig();

		updateConfigFromCSLRunningArgs(cslRunningArgs);
		
		//System.out.println(JsonUtil.prettyPrint(jConfig));

		//IDSTrace.log(IDSTrace.GENERAL, "test");

		


		String cslConf=JsonUtil.getStringFromJson(getConfig(), "csl/cslconf", "cslconf");
		this.cslConfDir = buildFullPathInUserDir(cslConf);

		setVerbose(cslRunningArgs.isVerbose());
		setDebug(cslRunningArgs.isDebug());

		setTestMode(cslRunningArgs.getTestParam());

		org.eclipse.jetty.util.log.Log.setLog(new NoLogging());
		
		
		
		//idsMainProcessor.setLogger(this);
		
		//this.idsMainProcessor= new IDSMainProcessor(
		this.idsMainProcessor = 	IDSMainProcessorFactory.instance.createIDSMainProcessor(
				getConfig().get("ids_conf"),
				getCslConfDir(),
				this);
		
		idsMainProcessor.setFileLogFactory(getFileLogFactory());
		
		fileUtils= new FileStoreService(getCslConfDir());
		idsMainProcessor.setFileStoreServices(fileUtils);
		
		
		//jConfig=null;  // forces reread
		cslAlertManager= new CSLAlertManager(idsMainProcessor,    getConfig().get("alert_viewer"));
		idsMainProcessor.setAlertManager(cslAlertManager);
		
		idsMainProcessor.setAlertFactory(new CSLAlertFactory());

	
		//idsMainProcessor.setDefaultIdsDataDir(getDefaultIdsDataDir());
		
		//Json j=getConfig();
		if (cslRunningArgs.isHasIdsRunner()) {
			//idsRunner= new IDSRunner(jConfig, cslRunningArgs, moduleIDS, this);
			idsParams= new IDSParams(idsMainProcessor );
					//CSLContext.instance.getUserDir()+File.separator+"idsdata");
			
			
			idsParams.initFromJson(getConfig(), cslRunningArgs.getDataDir(), 
					cslRunningArgs.getTestParam(),cslRunningArgs.isDoNotUseCurrentIDSParamsFileName()); // pworkingDir);
			
			
			if (cslRunningArgs.hasIdsMode())
				idsParams.setIDSMode(cslRunningArgs.getIdsMode());
			if (cslRunningArgs.hasDataSetForRecording())
				idsParams.setCurrentDataSetNameForRecording(cslRunningArgs.getDataSetForRecording());
			if (cslRunningArgs.hasDataSetForLearning())
				idsParams.setCurrentDataSetNameForLearning(cslRunningArgs.getDataSetForLearning());
			if (cslRunningArgs.hasDataSetForDetectionOffLine())
				idsParams.setCurrentDataSetNameForDetectionOffLine(cslRunningArgs.getDataSetForDetectionOffLine());
			
		
			
		}	
		
		idsMainProcessor.setConsole(idsParams.getConsole());
		
		setDatabaseServer(new DataBaseServer());
		
		setCslHttpServer(new CSLHttpServer());
		setCslUDPServer(new CSLUDPServer());
		
		//IDSTrace.updateConfig(jConfig.get("ids_conf"));
		

	}


	/***
	 * Set as distant API (to be called using socket --> CSLWebSocketForJcmd.execJCmd)
	 * (Tells the CslHttpServer that this is a remote API)
	 * @param apiname
	 */
	public void setApiRemote(String apiname) {
		if (getCslHttpServer()==null) return;
		
		getCslHttpServer().addRemoteApi(apiname);
		
	}
	
	public void postInit(boolean server, boolean client)   {


		this.server=server;
		this.client=client;
		
		//	 private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LogExample.class);


		//org.slf4j.Logger logger =  org.slf4j.LoggerFactory.getLogger("csl");


		//not used for IDS
		//initGlobalParameters();



		// not in user by default (use api is bettre)
		//ExternalCommands.instance.addMonitorCommands(
		//		CSLContext.context.getConfig().get("external_commands"));

		

		//CSLConfigFileServer.instance.initConfigFileManager(getConfig().get("config_file_manager_conf"));

		//CSLContext.instance.getDatabaseServer().init(getConfig().get("database_server_conf"));
		if (client)
			getDatabaseServer().init(getConfig().get("database_server_conf"));

		//CSLContext.instance.getCslHttpServer().initServer(getConfig().get("web_server_conf"));
		if (server)
			getCslHttpServer().initServer(getConfig().get("web_server_conf"));
		
		//CSLContext.instance.getCslUDPServer().initUDPServer(getConfig().get("udp_server_conf"));
		if (client)
			getCslUDPServer().initUDPServer(getConfig().get("udp_server_conf"));



		// not in used by default
		//CSLFileServer.instance.initFileServer(getConfig().get("file_server_conf"));



		if (client) {
			
			initModules();  // by default only ModuleIDS

		initTime();

		//boolean vars_commands= JsonUtil.getBooleanFromJson(getConfig(),"web_server_conf/vars_commands", false);
		//if (vars_commands) initWebsocketListener();

		ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
		
		idsRunner=new IDSRunner(idsParams, ids, this);
		}

	}
	
	
	public void start() {
		
		if (server) getCslHttpServer().start();
		
		if (client) getCslUDPServer().start();
		if (client) startExec();
		JServiceLoader.getCSLInterModuleCommunicationManager().start();
		
	}

	public void stop() {

		//CSLContext.instance.getCslHttpServer().stop();
		//CSLContext.instance.getCslUDPServer().stop();
		
		getCslHttpServer().stop();
		getCslUDPServer().stop();
		
		
		CSLContext.instance.stopExec();
		JServiceLoader.getCSLInterModuleCommunicationManager().stop();

		if (idsRunner!=null) idsRunner.stop();
		if (mqttBroker!=null) mqttBroker.close();


	}

	private void initTime() {

		initialTime=getSystemCurrentTimeMillis(); //  System.currentTimeMillis();
		samplingTime=JsonUtil.getIntFromJson(getConfig(), "module_exec/sampling_time", 100);


	}



	///
	//	
	//	public CSLConfigFileManager getCSLConfigFileManager() {
	//		return cslConfigFileManager;
	//	}

	/// == Modbus



	



	

	public ModuleContext getModuleContext(String name) {
		return modules.get(name);
	}




	//===============================================================
	// Global param managament
	//===============================================================
//	void initGlobalParameters() {
//
//		listOfGlobalParamsDescriptors.clear();
//
//		Json params=getConfig().get("global_params");
//		if (params==null) return ;
//
//
//		Iterator<Json> itr=params.iterator();
//
//		while(itr.hasNext()) {
//			Json jv = itr.next();
//			////System.out.println(jv);
//			ParamDescriptor mvd= new ParamDescriptor(jv);
//			listOfGlobalParamsDescriptors.put(mvd.getName(),mvd);
//			////System.out.println("XXXXX_INIT_global_params="+mvd);
//
//		}
//
//	}

//	public ParamDescriptor getParamDescriptor(String name) {
//		return listOfGlobalParamsDescriptors.get(name);
//	}
//
//	public String getParamAsString(String name,String defaultValue) {
//		ParamDescriptor z = listOfGlobalParamsDescriptors.get(name);
//		if (z==null) return defaultValue;
//		return z.getValue();
//	}
//
//	public int getParamAsInteger(String name,int defaultValue) {
//		ParamDescriptor z = listOfGlobalParamsDescriptors.get(name);
//		if (z==null) return defaultValue;
//		return z.asInteger(defaultValue);
//	}
//
//	public double getParamAsDouble(String name,double defaultValue) {
//		ParamDescriptor z = listOfGlobalParamsDescriptors.get(name);
//		if (z==null) return defaultValue;
//		return z.asDouble(defaultValue);
//	}
//
//	public boolean getParamAsBoolean(String name,boolean defaultValue) {
//		ParamDescriptor z = listOfGlobalParamsDescriptors.get(name);
//		if (z==null) return defaultValue;
//		return z.asBoolean(defaultValue);
//	}
//
//	public String paramsToString() {
//		String s="Parameters:";
//
//		String l="";
//		for( Map.Entry<String,ParamDescriptor> e : listOfGlobalParamsDescriptors.entrySet() ) {
//			if (!s.isEmpty()) s=s+'\n';
//			l="";
//			l=l+"  "+e.getValue().getName()+"="+e.getValue().getValue();
//			int n=Math.max(0, 20-l.length());
//			l=l+"                     ".substring(0,n );
//			l=l+"//"+e.getValue().getDescription();
//			s=s+l;
//		}
//		return s+"\n";
//
//	}
	//===============================================================

	private void initInternalModules() {


		//new ModuleTAPProcess();   			// used by TapProcess
		//new ModuleVariablesLogger();		// idem

		new ModuleIDS();

	}

	public Json getConfigAsJsonOfModule(String nameOfModule) {

		Json j=getConfig().get("modules");


		Iterator<Json> itr=j.iterator();

		while(itr.hasNext()) {
			Json jj = itr.next();

			String name=JsonUtil.getStringFromJson(jj, "name","???");
			if (nameOfModule.compareTo(name)==0) return jj;


		}

		return null;
	}

	private void initModules()  {

		boolean notfound=true;


		//List<Class> zz = CSLUtil.findClasses("/Users/flausj/Documents/dev/_modbus/Simul.jar");

		String modulesPackageName=
				JsonUtil.getStringFromJson(getConfig(), "module_exec/modules_package_name", "modules");
		//		// forces class loading and registration
		//		try {
		//			ArrayList<Class> list = CSLUtil.getClasses(modulesPackageName);
		//			if (list.size()>0) notfound=false;
		//		} catch (ClassNotFoundException | IOException e1) {
		//			// TODO Auto-generated catch block
		//			e1.printStackTrace();
		//		}


		//String dir=System.getProperty("user.dir")

		//test if csl.jar ?

		if (isVerbose()) System.out.println("Loading modules");
		initInternalModules();
		//CSLUtil.loadAllModules();

		numberOfExecLoops=JsonUtil.getIntFromJson(getConfig(), "module_exec/number_of_exec_loops", 1);

		if (isVerbose()) System.out.println("Running "+numberOfExecLoops+" execution loops");

		Json j=getConfig().get("modules");


		Iterator<Json> itr=j.iterator();

		while(itr.hasNext()) {
			Json moduleDescriptor = itr.next();
			CSLContext.instance.logInfo(moduleDescriptor + " ");


			String mname=moduleDescriptor.get("name").asString();

			if (modules.get(mname)!=null) {
				CSLContext.cslLogger.error("A module with this name <"+mname+"> has already been declared");
				//existe dejae
			}
			else {
				String type=moduleDescriptor.get("type").asString();
				Class clazz=getModuleClass(type);

				if (clazz==null) {
					CSLContext.cslLogger.error("Cannot find modules of type <"+type+"> ");

				}
				else {
					try {
						IModule m=(IModule)clazz.newInstance();
						ModuleContext mc= new ModuleContext();
						mc.setClazz(clazz);
						mc.setModule(m);
						mc.setName(mname);
						Json mconfig=moduleDescriptor.get("config");
						mc.setConfig(mconfig);

						modules.put(mname,mc);
						CSLContext.instance.logInfo("Module:"+mc);
						//if (isVerbose()) 
						System.out.println("Initialization of module "+mname+"["+type+"]");
					} catch (InstantiationException | IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}


		}


		// readparameters
		for (ModuleContext m : modules.values()) {
		//	m.initParameters();
		}
		// do init

		for (ModuleContext m : modules.values()) {

			if (m.getConfig().get("input_priority")!=null) {
				int n=m.getConfig().get("input_priority").asInteger();
				m.setInputPriority(n);
			}
			else
				cslLogger.warn("input priority undefined for module "+m.getName());

			if (m.getConfig().get("step_priority")!=null) {
				int n=m.getConfig().get("step_priority").asInteger();
				m.setStepPriority(n);
			}
			else
				cslLogger.warn("step priority undefined for module "+m.getName());

			if (m.getConfig().get("output_priority")!=null) {
				int n=m.getConfig().get("output_priority").asInteger();
				m.setOutputPriority(n);
			}
			else
				cslLogger.warn("output priority undefined for module "+m.getName());

			if (m.getConfig().get("exec_loop_number")!=null) {
				int n=m.getConfig().get("exec_loop_number").asInteger();
				if (n<0) n=0;
				if (n>numberOfExecLoops-1) n=numberOfExecLoops-1;
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
				return -o1.getInputPriority()+o2.getInputPriority();
			}
		});
		outputExecList.sort(new Comparator<ModuleContext>() {
			@Override
			public int compare(ModuleContext o1, ModuleContext o2) {
				return -o1.getOutputPriority()+o2.getOutputPriority();
			}
		});
		stepExecList.sort(new Comparator<ModuleContext>() {
			@Override
			public int compare(ModuleContext o1, ModuleContext o2) {
				return -o1.getStepPriority()+o2.getStepPriority();
			}
		});



		autostart=JsonUtil.getBooleanFromJson(getConfig(), "module_exec/autostart", true);
		//showProgression=CSLUtil.getConfigBooleanValue(getConfig(), "show_progression", false);

		//openBrowser=CSLUtil.getConfigBooleanValue(getConfig(), "openbrowser", true);
		//homePageName=CSLUtil.getConfigStringValue(getConfig(), "homepage_name", "");



	}



	Class getModuleClass(String name) {
		//		if (name.compareTo("Module1")==0) {
		//			return Module1.class;
		//		}
		return moduleClassList.get(name);

		//return null;
	}



//	// websocket var listener
//	public void initWebsocketListener()  {
//
//		if (CSLContext.instance.isVerbose()) System.out.println("Starting web socket listener");
//
//		Json j=getConfig().get("websocket_variables_listener");
//
//		if (j==null) return;
//		Json jnames=j.get("names");
//
//		Iterator<Json> itr=jnames.iterator();
//
//		while(itr.hasNext()) {
//			Json jname = itr.next();
//			String vname=jname.asString();
//			CSLContext.instance.logInfo(jname + " "+vname);
//			CSLVariable var = getGlobalVariablesTable().get(vname);
//			if (var!=null) {
//				CSLContext.instance.logInfo(vname+" --> adding websocket listener");
//
//				IVariableModificationListener ivl = new IVariableModificationListener() {
//					
//					@Override
//					public void modifying(String name, String path, String oldValue, String newValue) {
//						// TODO Auto-generated method stub
//						String msg ="{ \"name\":"+name+",\"old\":"+oldValue+",\"new\":"+newValue+"}";
//						Json jj=Json.object();
//						jj.at("time",CSLContext.instance.getTimeFromStartingTime());
//						jj.at("name",name);
//						//jj.at("oldValue",oldValue);
//						jj.at("newValue",newValue);
//						
//						
//						////System.out.println("Modif: "+jj.toString());
//						CSLWebSocketForVariables.broadcastMessage(jj.toString());
//					}
//				};
//				
//				var.addListener(ivl);
//			}
//
//		}
//	}


	//== periodic exec

	private void  logResult(IResult r) {
		if (r==null)
			cslLogger.warn("No result");
		if (!r.isOK()) 
			cslLogger.error("Error "+r.getErrorCode()+":"+r.getMessage());
	}

	Runnable task = new Runnable() {

		public void run() {

			cslLogger.debug("Sampling time:"+getTimeFromStartingTime());

			if (showProgression) {
				//System.out.print('.');
				if (nExecSteps>80) 
				{}//System.out.println("");nExecSteps=0;}}
				else nExecSteps++;
			}

			//cslLogger.debug(getGlobalVariablesTable().toString());

			try {
				for (int nloop=0;nloop<numberOfExecLoops;nloop++) {
					cslLogger.info("Exec loop #"+nloop);
					////System.err.println("TODEBUG Exec loop #"+nloop);
					for (ModuleContext m:inputExecList) {
						if (m.getLoopNumber()==nloop) {
							cslLogger.info("  input for "+m.getName());
							IResult r=m.getModule().execInputPart(instance, m);
							logResult(r);
						}
					}
					for (ModuleContext m:stepExecList) {
						if (m.getLoopNumber()==nloop) {
							cslLogger.info("  exec for "+m.getName());
							IResult r=m.getModule().execStepPart(instance, m);
							logResult(r);
						}
					}
					for (ModuleContext m:outputExecList) {
						if (m.getLoopNumber()==nloop) {
							cslLogger.info("  output for "+m.getName());
							IResult r=m.getModule().execOutputPart(instance, m);
							logResult(r);
						}
					}
				}
			}
			catch (Exception e) {
				//System.err.println(e);
				for (StackTraceElement z:e.getStackTrace()) {
					System.err.println(""+z);
				}

			}
			if 	(isExitCSL()) {
				scheduler.shutdown();
				//System.out.println("\nEnd of run");
				System.exit(0);
			}
		}

	};



	public void execOneRunCycle() {
		if (isReplayMode() ) {
			task.run();
		}

	}



	private boolean isReplayMode() {
		return replayMode;
	}

	private void setReplayMode(boolean testingMode) {
		this.replayMode = testingMode;
	}


	public void startExecInReplayMode(long initialTimeForTest) {
		setReplayMode(true);
		execOneRunCycle();
		initialTime=initialTimeForTest;
		lastSystemCurrentTimeMillis=initialTimeForTest;
		currentSamplingTime=initialTimeForTest;



	}

	public void startExec() {

		if (CSLContext.instance.isVerbose()) System.out.println("Starting modules execution");

		scheduler
		= Executors.newSingleThreadScheduledExecutor();


		int delay = samplingTime;
		CSLContext.instance.logInfo("Starting with sampling time :"+samplingTime+" ms");
		scheduler.scheduleAtFixedRate(task, 0, delay, TimeUnit.MILLISECONDS);

	}



	public void stopExec() {



		scheduler.shutdown();

	}

	public void restartExec() {

		if (scheduler==null) 
		{
			startExec();return;
		}
		stopExec();
		startExec();

	}

	public boolean isAutostart() {
		return autostart;
	}

	public boolean isOpenBrowser() {
		return openBrowser;
	}

	public String getHomePageName() {
		return homePageName;
	}

	private StringBuffer printToString(StringBuffer z, String s) {
		z.append(s);
		z.append("\n");
		return z;
	}


	public  String dump() {
		// TODO Auto-generated method stub

		StringBuffer z=new StringBuffer();

		printToString(z,"************");
		printToString(z,"* CSL Core *");
		printToString(z,"************");
		printToString(z,"Time:"+getTimeFromStartingTime()+"  (initial time in system time:"+initialTime+")");

		String s="";
		for (Entry<String, Class<IModule>> entry : moduleClassList.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (!s.isEmpty()) s=s+",";
			s=s+key;

		}
		printToString(z,"Module types:"+s);

		

		s="";
		for (int nLoop=0;nLoop<numberOfExecLoops;nLoop++) {
			s="Loop #"+nLoop;
			printToString(z,s);

			s="  Input Exec order: ";
			for (ModuleContext m:inputExecList) {
				if (m.getLoopNumber()==nLoop)
					s=s+m.getName()+"("+m.getInputPriority()+") ";
			}
			printToString(z,s);
			s="  Step Exec order: ";
			for (ModuleContext m:stepExecList) {
				if (m.getLoopNumber()==nLoop)
					s=s+m.getName()+":"+m.getStepPriority()+") ";
			}
			printToString(z,s);
			s="  Output Exec order: ";
			for (ModuleContext m:outputExecList) {
				if (m.getLoopNumber()==nLoop)
					s=s+m.getName()+"("+m.getOutputPriority()+") ";
			}
			printToString(z,s);

		}

		//printToString(z,s);

		printToString(z,"");
		printToString(z,"Modules");
		printToString(z,"=======");

		for (Entry<String, ModuleContext> entry : modules.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			printToString(z,value.toString());
		}


		return z.toString();
	}



	public String moduleListtoJsonString() {
		String retval = "";
		String delta = "  ";//offset.length == 0 ? "" : offset[0];

		//List<String> names=new ArrayList<String>hmap.keySet();


		for( Entry<String, ModuleContext> e : modules.entrySet() ) {

			if (retval.length()>0) retval=retval+",";
			retval +="{";
			retval += delta + "\"name\":"+"\""+e.getKey()+"\"";
			//CSLVariable value = modules.get(e.getKey());
			//retval += delta + "\"value\":"+"\"";

			//
			retval += /*((CSLVariable)value).toString() +*/ "}";

		}
		return "["+retval+"]\n";
	}





	//	public void setArgs(String[] args) {
	//		// TODO Auto-generated method stub
	//		this.args=args;
	//		for (int i=0; i<args.length;i++) {
	//			if (args[i].compareTo("-verbose")==0) setVerbose(true);
	//			if (args[i].compareTo("-debug")==0) setDebug(true);
	//			if (!args[i].startsWith("-")) {
	//				File f= new File(args[i]);
	//				if (f.exists()) this.configFileName=args[i];
	//			}
	//		}
	//	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setTestMode(boolean testMode) {
		this.testMode=testMode;
	}

	public boolean isTestMode() {
		return testMode;
	}

	

	public IDSParams getIdsParams() {
		// TODO Auto-generated method stub
		return idsParams;
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

}

