package main.util;

import java.io.File;

import com.csl.ids.IDSParams;
import com.ucsl.json.Json;

public class CSLRunningArgs {
	
	
	String configFile="runconfig"+File.separator+"CSLConfigIDS.json";
	//String pworkingDir="";
	String error="";
	
	
	boolean debug=false;
	boolean verbose=false;
	
	boolean startIdsRunner=true;
	boolean startCSLHttpServer=true;
	boolean startCSLUDPServer=true;
	boolean startCSLDatabaseServer=true;
	
	
	boolean doNotUseCurrentIDSParamsFileName=false;
			
	String userDir0= System.getProperty("user.dir");
	String userDir=userDir0;
	boolean userDirDefault=true;

	
	String dataDir="";
	private boolean testparam=false;
	
	
	public String logDir="";
	
	public String dataSetForLearning="";
	public String dataSetForRecording="";
	public String dataSetForDetectionOffline="";
	
	public String dirForLearning="";
	public String dirForRecording="";
	public String dirForDetectionOffline ="";
	
					
	
	public String databasedir="";
	
	
	public int idsMode=-1;
	
	//public static CSLRunningArgs instance =  new CSLRunningArgs();

	public CSLRunningArgs() { 
		
		this.userDir=System.getProperty("user.dir");

		this.dataDir=System.getProperty("user.dir")+File.separator+"idsdata";

			
	}

	
	
		
	public String validTestModeparam(String s) {
	
		s=s.toUpperCase();
		
		String z="";
		for (int i=0; i<IDSParams.idsModeAsString.length;i++) {
			if (IDSParams.idsModeAsString[i].compareTo(s)==0) return s;
			if (!z.isEmpty()) z=z+",";
			z=z+IDSParams.idsModeAsString[i];
		}
	
		
		System.err.println("Invalid test mode, should be "+z);
		return IDSParams.idsModeAsString[2];  // default detect online
		
	}
	
	

	public boolean isUserDirDefault() {
		return userDirDefault;
	}




	public void setUserDirDefault(boolean userDirDefault) {
		this.userDirDefault = userDirDefault;
	}




	public boolean isHasIdsRunner() {
		return startIdsRunner;
	}




	public CSLRunningArgs setHasIdsRunner(boolean hasIdsRunner) {
		this.startIdsRunner = hasIdsRunner;
		return this;
	}




	public boolean isStartIdsRunner() {
		return startIdsRunner;
	}
	public CSLRunningArgs setStartIdsRunner(boolean startIdsRunner) {
		this.startIdsRunner = startIdsRunner;
		return this;
	}




	public boolean isStartCSLHttpServer() {
		return startCSLHttpServer;
	}
	public CSLRunningArgs setStartCSLHttpServer(boolean startCSLHttpServer) {
		this.startCSLHttpServer = startCSLHttpServer;
		return this;
	}




	public boolean isStartCSLUDPServer() {
		return startCSLUDPServer;
	}
	public CSLRunningArgs setStartCSLUDPServer(boolean startCSLUDPServer) {
		this.startCSLUDPServer = startCSLUDPServer;
		return this;
	}




	public boolean isStartCSLDatabaseServer() {
		return startCSLDatabaseServer;
	}
	public CSLRunningArgs setStartCSLDatabaseServer(boolean startCSLDatabaseServer) {
		this.startCSLDatabaseServer = startCSLDatabaseServer;
		return this;
	}



	public boolean hasDatabaseDir() {
		return databasedir!="";
	}
	public String getDatabasedir() {
		return databasedir;
	}
	public CSLRunningArgs setDatabasedir(String databasedir) {
		this.databasedir = databasedir;
		return this;
	}



	public boolean hasIdsMode() {
		return idsMode>=0;
	}

	
	public int getIdsMode() {
		return idsMode;
	}
	public CSLRunningArgs setIdsMode(int idsMode) {
		this.idsMode = idsMode;
		return this;
	}




	public String getUserDir() {
		return userDir;
	}




	public CSLRunningArgs setUserDir(String userDir) {
		this.userDir = userDir;
		return this;
	}



	public boolean hasDataDir() {
		return dataDir!="";
	}
	public String getDataDir() {
		return dataDir;
	}
	public CSLRunningArgs setDataDir(String dataDir) {
		this.dataDir = dataDir;
		return this;
	}




	public boolean isDebug() {
		return debug;
	}
	public CSLRunningArgs setDebug(boolean debug) {
		this.debug = debug;
		return this;
	}




	public boolean isVerbose() {
		return verbose;
	}
	public CSLRunningArgs setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	public boolean getTestParam() {
		return testparam;
	}

	public CSLRunningArgs setTestParam(boolean testparam) {
		this.testparam = testparam;
		return this;
	}


	public boolean hasLogDir() {
		return logDir!="";
	}
	
	public String getLogDir() {
		return logDir;
	}

	public CSLRunningArgs setLogDir(String logDir) {
		this.logDir = logDir;
		return this;
	}

	
	public boolean hasDataSetForLearning() {
		return dataSetForLearning!="";
	}
	public String getDataSetForLearning() {
		return dataSetForLearning;
	}
	public CSLRunningArgs setDataSetForLearning(String dataSet) {
		this.dataSetForLearning = dataSet;
		return this;
	}
	
	public boolean hasDirForLearning() {
		return dirForLearning!="";
	}
	public String getDirForLearning() {
		return dirForLearning;
	}
	public CSLRunningArgs setDirForLearning(String dir) {
		this.dirForLearning = dir;
		return this;
	}
	
	
	

	public boolean hasDataSetForRecording() {
		return dataSetForRecording!="";
	}
	public String getDataSetForRecording() {
		return dataSetForRecording;
	}
	public CSLRunningArgs setDataSetForRecording(String dataSet) {
		this.dataSetForRecording = dataSet;
		return this;
	}
	
	public boolean hasDirForRecording() {
		return dirForRecording!="";
	}
	public String getDirForRecording() {
		return dirForRecording;
	}
	public CSLRunningArgs setDirForRecording(String dir) {
		this.dirForRecording = dir;
		return this;
	}
	
	
	public boolean hasDataSetForDetectionOffLine() {
		return dataSetForDetectionOffline!="";
	}
	public String getDataSetForDetectionOffLine() {
		return dataSetForDetectionOffline;
	}
	public CSLRunningArgs setDataSetForDetectionOffLine(String dataSet) {
		this.dataSetForDetectionOffline = dataSet;
		return this;
	}

	public boolean hasDirForDetectionOffLine() {
		return dirForDetectionOffline!="";
	}
	public String getDirForDetectionOffLine() {
		return dirForDetectionOffline;
	}
	public CSLRunningArgs setDirForDetectionOffLine(String dir) {
		this.dirForDetectionOffline = dir;
		return this;
	}

	
	
	
	
	
	
	
	
	
	
	public boolean isDoNotUseCurrentIDSParamsFileName() {
		return doNotUseCurrentIDSParamsFileName;
	}




	public void setDoNotUseCurrentIDSParamsFileName(boolean doNotUseCurrentIDSParamsFileName) {
		this.doNotUseCurrentIDSParamsFileName = doNotUseCurrentIDSParamsFileName;
	}




	public String getConfigFile() {
		return configFile;
	}
	
	public CSLRunningArgs setConfigFile(String s) {
		this.configFile=s;
		return this;
	}
	
	
	public String getPathOfConfigFile() {
		File f= new File(configFile);
		String s= f.getParentFile().toString();
		if (s==null) return "";
		return s;
	}

	//public String getworkingDir() {
	//	return pworkingDir;
	//}

	public String getError() {
		return error;
	}

	public boolean hasError() {
		return !error.isEmpty();
	}



	
	
	
	
	

	public CSLRunningArgs parseArgs(String[] args) {
		
		
		boolean firstConfig=false;

		//String OPTION_DIR = "datadir:";
		String USER_DIR = "-u:";
		String DATA_DIR = "-d:";
		
	//	String pworkingDir = ""; // data dir for learn and detect offline, log dir for record and detect on-line

	//	String configFile="";

		for (int i = 0; i < args.length; i++) {
		//	System.out.println(" #" + i + ":" + args[i]);
			if (args[i].toLowerCase().endsWith(".json")) {
				if (firstConfig) {
					configFile = args[i];firstConfig=false;
				}
				else
					System.err.println("Warning : multiple paramsfile " + args[i]);
			//} else if (args[i].toLowerCase().startsWith(OPTION_DIR)) {
			//	pworkingDir = args[i].toLowerCase().substring(OPTION_DIR.length());
			} else if (args[i].toLowerCase().startsWith(USER_DIR)) {
				userDir = args[i].toLowerCase().substring(USER_DIR.length());
				userDirDefault=false;
			} else if (args[i].toLowerCase().startsWith(DATA_DIR)) {
				dataDir = args[i].toLowerCase().substring(DATA_DIR.length());
			} else if (args[i].compareTo("-verbose")==0) {
					setVerbose(true);
			} else if (args[i].compareTo("-debug")==0) {
					setDebug(true);
			} else if (args[i].toLowerCase().startsWith("--testmode")) {
				boolean test=true;
				//IDSRunner.instance.getIdsParams().setTestMode(true);
				String sx=args[i].toLowerCase().substring("--testmode".length());
				if (sx.startsWith(":")) sx=sx.substring(1);
				System.out.println(sx);
				sx=validTestModeparam(sx);
				//IDSRunner.instance.getIdsParams().setTestParam(sx);
				if (sx.compareToIgnoreCase("false")==0) test=false;
				if (sx.compareToIgnoreCase("0")==0) test=false;
				setTestParam(test);
				
				String z="";
				sx="<"+sx+">";
				for (int j=0;j<sx.length();j++) z=z+"=";
				System.err.println( "!====================="+z+"!\n"+
									"!WARNING : TEST MODE "+sx+" !\n"+
									"!====================="+z+"!");
			} 
			
			else {
				System.out.println("Invalid parameter:" + args[i]);
			}
		}
		
		
		if (dataDir.startsWith(".")) {
			dataDir=dataDir.substring(1);
			if (dataDir.isEmpty())
				dataDir=userDir;
			else
				dataDir=userDir+File.separator+dataDir;
		}
		
		
			
		
		// LIRE LA CONFIG DE CONTEXT
		String path = System.getProperty("user.home");

		if (configFile.isEmpty())
			configFile= getUserDir() + File.separator + "runconfig/CSLConfigIDS.json";
		
		
		//String paramsFile = ""; // "params.json";

		File file = new File(configFile);

		if (!file.exists()) {
			
			System.out.println("Cannot not find config file :"+file.getAbsolutePath());
			
			String fname=getUserDir() + File.separator + configFile;
			
			file = new File(fname);
			if (!file.exists()) {
				System.out.println("Cannot not find config file :"+file.getAbsolutePath());
				
				fname=getUserDir() + File.separator + "runconfig" + File.separator +configFile;
					
				file = new File(fname);
				if (!file.exists()) {
					System.out.println("Cannot find config file " + file.getAbsolutePath());
					//System.exit(0);
					error="Cannot find config file " + file.getAbsolutePath();
				}
				else
					configFile=fname;
				
			}
			else {
				configFile= fname;
				
			}
			
			
		}
		
		
		System.out.println("Config file :"+file.getAbsolutePath()+" in "+getPathOfConfigFile());
		System.out.println("User dir :"+getUserDir());
		System.out.println("Data dir :"+getDataDir());
			System.out.println();
		
	
		return this;
	}
	
	
	
	
	public Json getAsJson() {
		
		Json result = Json.object();
		//result.set("workingDir", "");
		//result.set("configFile", "");
		//result.set("error", "");
	
		result.set("user_dir",userDir);
		result.set("data_dir",dataDir);
		
		result.set("configFile",configFile);
		result.set("error", error);
		
		return result;
	}
		
	
}
