package com.csl.intercom.jsoncmd;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

//import com.csl.core.CSLContext;
import com.csl.intercom.broker.CSLInterModuleCommunicationManager;
import com.csl.intercom.broker.MosquittoConfig;
import com.xcsl.interfaces.IApiCommands;
import com.xcsl.interfaces.ICSLService;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;






public class JServiceLoader {


	public static CSLInterModuleCommunicationManager cslInterModuleCommunicationManager =null;
	static String moduleName="XXX";

	static MosquittoConfig mosquittoConfig= new MosquittoConfig();
	

	static String userDir=System.getProperty("user.dir");
	static List<IApiCommands> listOfAPIToRegister = new ArrayList<IApiCommands>();
	static List<XApiCommands> listOfXAPIToRegister = new ArrayList<XApiCommands>();


	private static List<String> listOfServiceNames= new ArrayList<>();


	static public String getUserDir() {
		return userDir;
	}

	static public String setUserDir(String s) {
		userDir=s;
		return userDir;
	}


	public static MosquittoConfig getMosquittoConfig() {
		return mosquittoConfig;
	}

	public static void setMosquittoConfig(MosquittoConfig mosquittoConfig) {
		JServiceLoader.mosquittoConfig = mosquittoConfig;
	}

	static public boolean displayInfo(String d) {
		System.out.println("[********]"+d);
		return true;
	}

	static public long getSystemCurrentTimeMillis() {

		return System.currentTimeMillis();
	}

	public static String buildFullPathInUserDir(String dir) {


		if (dir==null) dir ="";

		if (dir.startsWith(getUserDir())) return dir;

		dir=dir.replace('\\','/');

		dir=clean(dir);

		if (dir.startsWith(".")) dir =dir.substring(1);
		if (dir.startsWith(File.separator)) dir =dir.substring(1);

		return getUserDir()+File.separator+dir;
	}

	private static String clean(String s) {
		String z="../";
		while (s.indexOf(z)>=0) {
			int n= s.indexOf(z);
			String s1=s.substring(0,n);
			String s2=s.substring(n+z.length(),s.length());
			s=s1+s2;
		}
		return s;
	}


	static public  List<Class> findClasses(Json jConfig, String pathToJar) {
		List<Class> classes = new ArrayList<Class>();

		File file = new File(pathToJar);   
		URL jarfile;

		boolean trace_library_search= JsonUtil.getBooleanFromJson(jConfig,
				"service_loader/trace_library_search",false);

		boolean trace_service_execution= JsonUtil.getBooleanFromJson(jConfig,
				"service_loader/trace_service_execution",false);



		ArrayList classes2 = new ArrayList();
		boolean debug=true;

		try {

			jarfile = new URL("jar", "","file:" + file.getAbsolutePath()+"!/");
			URLClassLoader cl = URLClassLoader.newInstance(new URL[] {jarfile });   

			JarInputStream jarFile = new JarInputStream(new FileInputStream(
					pathToJar));
			JarEntry jarEntry;

			while (true) {
				jarEntry = jarFile.getNextJarEntry();
				if (jarEntry == null) {
					break;
				}
				if (jarEntry.getName().endsWith(".class")) {
					//if (debug) 
					String sn=jarEntry.getName();
					if (sn.contains("services"))
					{
						if (trace_library_search) System.out.println("Found "
								+ jarEntry.getName().replaceAll("/", "\\."));


					}


					if (!sn.contains("$")&&sn.contains("main/services")) {
						if (trace_library_search)  System.out.println("Loading "+sn);
						String className=jarEntry.getName().replaceAll("/", "\\.");

						classes2.add(jarEntry.getName().replaceAll("/", "\\."));	
						//						//System.out.println("Found "
						//								+ jarEntry.getName().replaceAll("/", "\\."));

						//Class c = cl.loadClass(className);
						className=className.substring(0, className.length() - 6);

						Class loadedClass = cl.loadClass(className);
						classes.add(loadedClass);
						//System.out.println(loadedClass.getName());
						Object o=loadedClass.newInstance();


						if (o instanceof ICSLService)
							registerService((ICSLService) o,jConfig, trace_service_execution);

					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}



		return classes;
	}

	static public void loadAllModules(Json jConfig) {
		//		 ClassLoader cl = ClassLoader.getSystemClassLoader();

		//	     URL[] urls = ((URLClassLoader)cl).getURLs();


		boolean trace_library_search= JsonUtil.getBooleanFromJson(jConfig,
				"service_loader/trace_library_search",false);

		Json j=jConfig.get("service_loader");
		if (j==null) j=Json.object();
		Json jarray=j.get("services");
		String sep=System.getProperty("path.separator") ;

		String s="";
		if (jarray!=null) {
			for (Json je:jarray.asJsonList()) {
				String name=je.asString();

				if (trace_library_search) System.out.println("Adding jar :"+name);
				if (!s.isEmpty()) s=s+sep;
				s=s+name;
			}
		}


		String classPath = System.getProperty("java.class.path");

		//System.out.println(classPath);
		classPath=classPath+sep+s;

		String[] listpath=classPath.split(sep);

		//System.out.println(classPath);
		for (String sl:listpath) {
			if (trace_library_search) System.out.println("Find library :"+sl);
			if ( /*s.contains("service") && */(sl.endsWith(".jar")) )  {
				//System.out.println("Looking for class);
				findClasses(jConfig,sl);
			}
		}

		//	     for(URL url: urls){
		//	     	//System.out.println(url.getFile());
		//	     	String s=url.getFile();
		//	     	if (s.endsWith(".jar")) {
		//	     		findClasses(s);
		//	     	}
		//	     	
		//	     }

	}



	static public void addApiCommands(IApiCommands api) {
		// TODO Auto-generated method stub
		System.out.println("Register api for http:"+api);
		listOfAPIToRegister.add(api);
		//System.out.println(listOfAPIToRegister);
	}

	static public List<IApiCommands> getApiCommandsList() {
		return listOfAPIToRegister;
	}


	static public void addXApiCommands(XApiCommands api) {
		// TODO Auto-generated method stub
		System.out.println("Register api for http:"+api);
		listOfXAPIToRegister.add(api);
		//System.out.println(listOfAPIToRegister);
	}

	static public String getApiHelpPage(Json params) {

		return  ""+new ApiGetHelp().getHelp(listOfServiceNames, params);

	}



	/*ajouter 
	registerExternalService

	utiliser ca pour le hhtpserveur et le help


	faire une lib avec les fcts csl pour l'intercomme te les service

	(à exporter ds zcsl sec)*/

	static public boolean  registerService (ICSLService cslService, Json j, boolean trace) {


		String name=cslService.getApiCommands().getName();
		listOfServiceNames.add(name);

		Json jc=j.get(cslService.getConfigFileSectionName());

		if (jc==null) jc=Json.object();
		boolean ok=cslService.init(jc, getUserDir());

		if (trace) 
			System.out.println("Initializing service "+name);




		if (ok) {

			if (trace) System.out.println("Starting service "+name);
		//if (trace) System.out.println(" with config:"+jc);

			//ApiCommands z = new ApiCommands("/"+cslService.getName());

			addApiCommands(cslService.getApiCommands());

			/*List<String> list=new ArrayList<String>(cslService.getCmds().keySet());

			for (String cname:list ) {
				JsonCmd jcmd= cslService.getCmds().get(cname);
				z.registerCmd(cname, jcmd);

			};*/

			getCSLInterModuleCommunicationManager().registerAPI(cslService.getApiCommands());
		}


		return ok;
	}


	static public boolean  registeExternalService (XApiCommands xapi, boolean trace) {


		String name=xapi.getName();






		if (trace) System.out.println("Registering external service "+name);

		addXApiCommands(xapi);



		getCSLInterModuleCommunicationManager().registerExternalAPI(xapi);



		return true;
	}
	public static void setModuleName(String string, MosquittoConfig config) {
		// TODO Auto-generated method stub
		moduleName=string;
		setMosquittoConfig(config);
		getCSLInterModuleCommunicationManager().setModuleName(moduleName);
	}

	public static CSLInterModuleCommunicationManager getCSLInterModuleCommunicationManager() {

		if ( cslInterModuleCommunicationManager ==null) 
			cslInterModuleCommunicationManager= new CSLInterModuleCommunicationManager(moduleName, getMosquittoConfig());
		return cslInterModuleCommunicationManager;
	}

	/*	public static void addApi(ApiCommands apiCommands) {
		// TODO Auto-generated method stub
		CSLInterModuleCommunicationManager.instance.registerAPI(apiCommands);
	}
	 */



}
