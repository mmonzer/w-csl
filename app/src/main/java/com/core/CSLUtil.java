package com.csl.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.ucsl.json.Json;

public class CSLUtil {

	public static int getConfigIntegerValue(Json config,String name,int defaulValue) {

		if (config==null) return defaulValue;
				
		Json obj=config.get(name);
		if (obj==null) return defaulValue;
		return obj.asInteger();
	}
	
	public static long getConfigLongValue(Json config,String name,int defaulValue) {

		if (config==null) return defaulValue;
				
		Json obj=config.get(name);
		if (obj==null) return defaulValue;
		return obj.asLong();
	}
	
	public static double getConfigDoubleValue(Json config,String name,double defaulValue) {

		if (config==null) return defaulValue;
		Json obj=config.get(name);
		if (obj==null) return defaulValue;
		return obj.asDouble();
	}

	public static String getConfigStringValue(Json config,String name,String defaulValue) {

		if (config==null) return defaulValue;
		Json obj=config.get(name);
		if (obj==null) return defaulValue;
		return obj.asString();

	}

	public static Boolean getConfigBooleanValue(Json config,String name,boolean defaulValue) {

		if (config==null) return defaulValue;
		Json obj=config.get(name);
		if (obj==null) return new Boolean(defaulValue);
		return obj.asBoolean();

	}


	private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
		List<Class> classes = new ArrayList<Class>();
		if (!directory.exists()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				assert !file.getName().contains(".");
				classes.addAll(findClasses(file, packageName + "." + file.getName()));
			} else if (file.getName().endsWith(".class")) {
				classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
			}
		}
		return classes;
	}

	public static  ArrayList<Class> getClasses(String packageName)
			throws ClassNotFoundException, IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<File>();
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			dirs.add(new File(resource.getFile()));
		}
		ArrayList<Class> classes = new ArrayList<Class>();
		for (File directory : dirs) {
			classes.addAll(findClasses(directory, packageName));
		}
		return classes;
	}

	public static List<Class> findClasses(String pathToJar) {
		List<Class> classes = new ArrayList<Class>();

		File file = new File(pathToJar);   
		URL jarfile;


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
					//		            	if (debug)
					//		            		//System.out.println("Found "
					//		            				+ jarEntry.getName().replaceAll("/", "\\."));
					if (jarEntry.getName().contains("main/modules")) {
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

					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}



		return classes;
	}
	
	
	static public void loadAllModules() {
//	 ClassLoader cl = ClassLoader.getSystemClassLoader();

//     URL[] urls = ((URLClassLoader)cl).getURLs();

	 
		String classPath = System.getProperty("java.class.path");
		String sep=System.getProperty("path.separator") ;
		String[] listpath=classPath.split(sep);
		
		for (String s:listpath) {
			////System.out.println("XXXX_LOOK:"+s);
			if (s.endsWith(".jar")) {
	     		findClasses(s);
	     	}
		}
	
//     for(URL url: urls){
//     	//System.out.println(url.getFile());
//     	String s=url.getFile();
//     	if (s.endsWith(".jar")) {
//     		findClasses(s);
//     	}
//     	
//     }
     
	}
      

}
	
//	static public void findPage(String path) {
////		 ClassLoader cl = ClassLoader.getSystemClassLoader();
//
////	     URL[] urls = ((URLClassLoader)cl).getURLs();
//
//		 
//			String classPath = System.getProperty("java.class.path");
//			String sep=System.getProperty("path.separator") ;
//			String[] listpath=classPath.split(sep);
//			
//			for (String s:listpath) {
//				////System.out.println("XXXX_LOOK:"+s);
//				if (s.endsWith(".jar")) {
//					findFilesInPublic(s);
//		     	}
//			}
//		
////	     for(URL url: urls){
////	     	//System.out.println(url.getFile());
////	     	String s=url.getFile();
////	     	if (s.endsWith(".jar")) {
////	     		findClasses(s);
////	     	}
////	     	
////	     }
//	     
//		}
	      
	
//	public static List<Class> findFilesInPublic(String pathToJar) {
//		List<Class> classes = new ArrayList<Class>();
//
//		File file = new File(pathToJar);   
//		URL jarfile;
//
//
//		ArrayList classes2 = new ArrayList();
//		boolean debug=true;
//
//		try {
//
//			jarfile = new URL("jar", "","file:" + file.getAbsolutePath()+"!/");
//			URLClassLoader cl = URLClassLoader.newInstance(new URL[] {jarfile });   
//
//			JarInputStream jarFile = new JarInputStream(new FileInputStream(
//					pathToJar));
//			JarEntry jarEntry;
//
//			while (true) {
//				jarEntry = jarFile.getNextJarEntry();
//				if (jarEntry == null) {
//					break;
//				}
//				System.out.println("Found "
//						+ jarEntry.getName());
//				//if (jarEntry.getName().endsWith(".class")) {
//					//		            	if (debug)
//					//		            		System.out.println("Found "
//					//		            				+ jarEntry.getName().replaceAll("/", "\\."));
//					if (jarEntry.getName().contains("public")) {
//						String className=jarEntry.getName().replaceAll("/", "\\.");
//
//						classes2.add(jarEntry.getName().replaceAll("/", "\\."));	
//						//System.out.println("Found "
//								+ jarEntry.getName().replaceAll("/", "\\."));
//						     
//						//Class c = cl.loadClass(className);
//						className=className.substring(0, className.length() - 6);
//						
//						Class loadedClass = cl.loadClass(className);
//						classes.add(loadedClass);
//						Object o=loadedClass.newInstance();
//
//					}
//
//				//}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//
//
//		return classes;
//	}
//	
//	
//	
//}
///**
// * 
//* ClassLoader loader = URLClassLoader.newInstance(
//   new URL[] { jarFileURL },
//    getClass().getClassLoader()
//);
//Class<?> clazz = Class.forName("mypackage.MyClass", true, loader);**/
