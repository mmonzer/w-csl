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
 * Class used to load the services offered by CSL-Client.
 * Services offer an API, in the form of a set of commands that can be called by other services and through a web API.
 */
public class JServiceLoader {
    private static final Logger logger = LoggerFactory.getLogger(JServiceLoader.class);
    public static CSLInterModuleCommunicationManager cslInterModuleCommunicationManager = null;
    static String moduleName = "XXX";

    @Getter
    static MosquittoConfig mosquittoConfig = new MosquittoConfig();

    @Getter
    static String userDir = System.getProperty("user.dir");
    static List<IApiCommands> listOfAPIToRegister = new ArrayList<IApiCommands>();
    static List<XApiCommands> listOfXAPIToRegister = new ArrayList<XApiCommands>();


    private static final List<String> listOfServiceNames = new ArrayList<>();


    static public String setUserDir(String s) {
        userDir = s;
        return userDir;
    }


    public static void setMosquittoConfig(MosquittoConfig mosquittoConfig) {
        JServiceLoader.mosquittoConfig = mosquittoConfig;
    }

    static public boolean displayInfo(String d) {
        System.out.println("[********]" + d);
        return true;
    }

    static public long getSystemCurrentTimeMillis() {

        return System.currentTimeMillis();
    }

    public static String buildFullPathInUserDir(String dir) {

        if (dir == null) dir = "";

        if (dir.startsWith(getUserDir())) return dir;

        dir = dir.replace('\\', '/');

        dir = clean(dir);

        if (dir.startsWith(".")) dir = dir.substring(1);
        if (dir.startsWith(File.separator)) dir = dir.substring(1);

        return getUserDir() + File.separator + dir;
    }

    private static String clean(String s) {
        String z = "../";
        while (s.contains(z)) {
            int n = s.indexOf(z);
            String s1 = s.substring(0, n);
            String s2 = s.substring(n + z.length());
            s = s1 + s2;
        }
        return s;
    }


    static public List<Class> findClasses(Json jConfig, String pathToJar) {
        List<Class> classes = new ArrayList<Class>();

        File file = new File(pathToJar);
        URL jarfile;

        boolean trace_library_search = JsonUtil.getBooleanFromJson(jConfig, "service_loader/trace_library_search", false);

        boolean trace_service_execution = JsonUtil.getBooleanFromJson(jConfig, "service_loader/trace_service_execution", false);


        ArrayList classes2 = new ArrayList();
        boolean debug = true;

        try {

            jarfile = new URL("jar", "", "file:" + file.getAbsolutePath() + "!/");
            URLClassLoader cl = URLClassLoader.newInstance(new URL[]{jarfile});

            JarInputStream jarFile = new JarInputStream(new FileInputStream(pathToJar));
            JarEntry jarEntry;

            while (true) {
                jarEntry = jarFile.getNextJarEntry();
                if (jarEntry == null) {
                    break;
                }
                if (jarEntry.getName().endsWith(".class")) {
                    String sn = jarEntry.getName();
                    if (sn.contains("services")) {
                        if (trace_library_search)
                            logger.trace("Found {}", jarEntry.getName().replaceAll("/", "\\."));
                    }

                    if (!sn.contains("$") && sn.contains("main/services")) {
                        if (trace_library_search) logger.trace("Loading {}", sn);
                        String className = jarEntry.getName().replaceAll("/", "\\.");

                        classes2.add(jarEntry.getName().replaceAll("/", "\\."));

                        className = className.substring(0, className.length() - 6);

                        Class loadedClass = cl.loadClass(className);
                        classes.add(loadedClass);
                        Object o = loadedClass.newInstance();


                        if (o instanceof ICSLService)
                            registerService((ICSLService) o, jConfig, trace_service_execution);

                    }

                }
            }
        } catch (Exception e) {
            logger.error("Error while loading classes", e);
        }

        return classes;
    }

    static public void loadAllModules(Json jConfig) {
        boolean trace_library_search = JsonUtil.getBooleanFromJson(jConfig, "service_loader/trace_library_search", false);

        Json j = jConfig.get("service_loader");
        if (j == null) j = Json.object();
        Json jarray = j.get("services");
        String sep = System.getProperty("path.separator");

        String s = "";
        if (jarray != null) {
            for (Json je : jarray.asJsonList()) {
                String name = je.asString();

                if (trace_library_search) logger.trace("Adding jar : {}", name);
                if (!s.isEmpty()) s = s + sep;
                s = s + name;
            }
        }

        String classPath = System.getProperty("java.class.path");

        classPath = classPath + sep + s;

        String[] listpath = classPath.split(sep);

        for (String sl : listpath) {
            if (trace_library_search) logger.trace("Find library : {}", sl);
            if ( (sl.endsWith(".jar"))) {
                findClasses(jConfig, sl);
            }
        }
    }


    static public void addApiCommands(IApiCommands api) {
        logger.info("Register api for http:" + api);
        listOfAPIToRegister.add(api);
    }

    static public List<IApiCommands> getApiCommandsList() {
        return listOfAPIToRegister;
    }


    static public void addXApiCommands(XApiCommands api) {
        logger.info("Register api for http:" + api);
        listOfXAPIToRegister.add(api);
    }

    static public String getApiHelpPage(Json params) {
        return new ApiGetHelp().getHelp(listOfServiceNames, listOfServiceNames, params);
    }



	/*ajouter 
	registerExternalService

	utiliser ca pour le hhtpserveur et le help


	faire une lib avec les fcts csl pour l'intercomme te les service

	(à exporter ds zcsl sec)*/

    /***
     * Create an api for the registered service with a set of commands
     * to do so: it do the following:
     * 	- Adds the service name to the list of services
     * 	- Calls the init function of the service
     * 	- Adds the service commands
     * 	- Registers the api in the CSLInterModuleCommunicationManager
     * @param cslService implements ICSLService
     * @param j the configuration as json
     * @param trace whether to print debugging logs
     * @return
     */
    static public boolean registerService(ICSLService cslService, Json j, boolean trace) {
        String name = cslService.getApiCommands().getName();
        listOfServiceNames.add(name);

        Json jc = j.get(cslService.getConfigFileSectionName());

        if (jc == null) jc = Json.object();
        boolean ok = cslService.init(jc, getUserDir());

        logger.info("Initializing service " + name);

        if (!ok) {
            logger.warn("cannot initialize {}", name);
        }

        if (ok) {
            logger.trace("Starting service {}", name);
            addApiCommands(cslService.getApiCommands());
            getCSLInterModuleCommunicationManager().registerAPI(cslService.getApiCommands());
        }

        return ok;
    }


    static public boolean registeExternalService(XApiCommands xapi, boolean trace) {
        String name = xapi.getName();

        logger.trace("Registering external service " + name);
        addXApiCommands(xapi);
        getCSLInterModuleCommunicationManager().registerExternalAPI(xapi);

        return true;
    }

    public static void setModuleName(String string, MosquittoConfig config) {
        moduleName = string;
        setMosquittoConfig(config);
        getCSLInterModuleCommunicationManager().setModuleName(moduleName);
    }

    public static CSLInterModuleCommunicationManager getCSLInterModuleCommunicationManager() {

        if (cslInterModuleCommunicationManager == null)
            cslInterModuleCommunicationManager = new CSLInterModuleCommunicationManager(moduleName, getMosquittoConfig());
        return cslInterModuleCommunicationManager;
    }
}
