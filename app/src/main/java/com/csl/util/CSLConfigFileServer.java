package com.csl.util;


import com.csl.core.CSLContext;
import com.csl.interfaces.IFileModificationViaUploadListener;
import com.csl.logger.LogToString;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import spark.Request;
import spark.Response;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class CSLConfigFileServer {
	
	//static public CSLConfigFileServer  instance= new CSLConfigFileServer();
	static boolean debug=false;

	private String dirOfLocalFiles=System.getProperty("user.dir");
	List<String> listOfLocalFiles = new ArrayList<String>();
	int maxNumberOfFileVersion=99;

	List<IFileModificationViaUploadListener> fileModificationViaUploadListeners= new ArrayList<IFileModificationViaUploadListener>();

	private boolean initialized=false;

	private boolean running=false;
	private boolean verbose=false;
	
	
	
	
/*
 "config_file_manager": {
	"on":true,
	"verbose":true,
	"port":8011,
	"show_modified_files":true,
	"root_conf_dir":"./idsconf",
	"conf_files":["NewLearnedRules.json", "RulesForDetection.txt","RulesForLearning.txt","LearnedRules.json","SystemConfiguration.json"],
	"max_number_of_version":10
}
 */
	public void initConfigFileManager(Json j) {
		if (j==null) return;
		
		running = JsonUtil.getBooleanFromJson(j,"on", false);
		if (!running) return;
		
		if (initialized) {
			System.err.println("already initialized");
			System.exit(0);
		}
		
		//Json j=CSLContext.context.getConfig().get("file_server");
		Json jarray=j.get("conf_files");
		
		for (Json je:jarray.asJsonList()) {
			String name=je.asString();
			addLocalFileTransferableViaHttp(name);
			if (debug) System.out.println("Adding autorisation for transferable file:"+name);
		}
		
		int max_number_of_version= JsonUtil.getIntFromJson(j, "max_number_of_version",5);
		setMaxNumberOfLocalFileVersion(max_number_of_version); // default 99, -1 keep all
		
		String dir=CSLContext.instance.buildFullPathInUserDir(
				JsonUtil.getStringFromJson(j, "root_conf_dir", ""));
		if (dir.isEmpty()) dir=CSLContext.instance.getUserDir();
		
		setDirOfLocalFiles(dir);
		
		boolean show_modified_files= JsonUtil.getBooleanFromJson(j, "show_modified_files",false);
		
		if (show_modified_files) {
			addFileModificationViaUploadListener(new IFileModificationViaUploadListener() {
			
			@Override
			public void modifying(String filename) {
				// TODO Auto-generated method stub
				System.out.println("File "+filename+" has been modified");
			}
		});
		}
		
		
		
		
		
	
		
		verbose = JsonUtil.getBooleanFromJson(j,"verbose", false);
		debug=JsonUtil.getBooleanFromJson(j,"debug", false);
		
		//int port=JsonUtil.getIntFromJson(j,"port", -1);
		
		
		if (verbose) {
			System.out.println("Config File server configuration:");
			System.out.println("=================================");
			
			System.out.println(" running:"+running);
			System.out.println(" debug  :"+debug);
			System.out.println(" file_directory:"+dirOfLocalFiles);
			
		}
	
		
		
	}
	
	
	//================================================================================================================

	/*
	 * 
	 * Config File transfer
	 * 
	 */
	
	private  String renderGetFile(Request req, Response res) {

		Set<String> paramKeys= req.queryParams();
		
		String sresponse = req.body();
		//System.out.println("\n"+sresponse);
		//System.out.println("path:"+req.pathInfo()); 
		String s=req.pathInfo();
		if (s.length()>1) s=s.substring(1);
		
		
		List<String> varNames= new ArrayList<String>(paramKeys);
		String result="";
		LogToString l= new LogToString();

		CSLContext.instance.cslLogger.addListener(l);
		
		
		String value = req.queryParams("filename");
		if (value!=null) {
			result=getFile(value);
		} else {
			result="No filename";
		}
		
		
		CSLContext.instance.cslLogger.removeListener(l);

		if (CSLContext.instance.isDebug())
			result=result+"\n"+l.getContents();
		res.body(result);
		return result;
	}

	
	private  String renderReverseFile(Request req, Response res) {

		Set<String> paramKeys= req.queryParams();
		
		String sresponse = req.body();
		//System.out.println("\n"+sresponse);
		//System.out.println("path:"+req.pathInfo()); 
		String s=req.pathInfo();
		if (s.length()>1) s=s.substring(1);
		
		
		List<String> varNames= new ArrayList<String>(paramKeys);
		String result="";
		LogToString l= new LogToString();

		CSLContext.instance.cslLogger.addListener(l);
		
		
		String value = req.queryParams("filename");
		if (value!=null) {
			result=reverseToPreviousFileVersion(value);
		} else {
			result="No filename";
		}
		
		
		CSLContext.instance.cslLogger.removeListener(l);

		if (CSLContext.instance.isDebug())
			result=result+"\n"+l.getContents();
		res.body(result);
		return result;
	}

	


	private  String renderSetFile(Request req, Response res) {

		Set<String> paramKeys= req.queryParams();
		
		//System.out.println("POST : test");
		String sresponse = req.body();
		//System.out.println("\n"+sresponse);
		//System.out.println("path:"+req.pathInfo()); 
		String s=req.pathInfo();
		if (s.length()>1) s=s.substring(1);
		
		
		LogToString l= new LogToString();
		CSLContext.instance.cslLogger.addListener(l);
		String result="";
		
		if (s.compareToIgnoreCase("setfile")==0) {
			Json inputJson = Json.read(sresponse);
			//System.out.println("Object="+inputJson);
			
			String fileName=inputJson.get("filename").asString();
			String contents=inputJson.get("contents").asString();
			
			if ((fileName!=null)&&(contents!=null)) {
				result=setFile(fileName, contents);
			
			} else {
				if (fileName==null) result=result+"Invalid file name ";
				if (contents==null) result=result+"Invalid contents ";
		
			}
		}
		else {
			result="Invalid command:<"+s+">";
		}
		
		
			
		
//		List<String> varNames= new ArrayList<String>(paramKeys);
//		String result="";
//
//	
//		
//		HashMap<String, String> keyvalues = new HashMap<String, String>();
//
//		for (String name:varNames) {
//			if (!result.isEmpty()) result=result+";";
//			String type="string";
//			////System.out.println(name);
//			String value = req.queryParams(name);
//			keyvalues.put(name,  value);
//			int idx=name.indexOf(':');
//			if (idx>=0) {
//				type=name.substring(idx+1);
//				name=name.substring(0, idx);
//			}
//			VariableValueType vtype=CSLVariable.getVariableTypeFromString(type);
//			////System.out.println("SETVAR:"+name+ " type:"+type+" vtype:"+vtype+" to "+value);
//		}
		
	
		CSLContext.instance.cslLogger.removeListener(l);

		if (CSLContext.instance.isDebug())
			result=result+"\n"+l.getContents();

		
		res.body(result);
		return result;
	}


	

	
	//================================================================================================================
	
	public String setFile(String filename,String contents) {
		//System.out.println("setFile:"+filename+"\nContents:\n"+contents);
		boolean ok=isValidLocalFileTransferableViaHttp(filename);
		if (!ok) {
			return "Invalid file name <"+filename+ ">: no rights to uploaded this file";
		}
		String path =dirOfLocalFiles+File.separatorChar+filename;
		
		renameFileWithTimeStamp(dirOfLocalFiles+File.separatorChar+filename, "");
		deleteFiles(path,maxNumberOfFileVersion);
		
		deleteReverseFile(dirOfLocalFiles+File.separatorChar+filename);
		
		
		writeFile(path, contents);
		
		for (IFileModificationViaUploadListener l:fileModificationViaUploadListeners) {
			l.modifying(filename);
		}
		
		return "ok";
	}

	public String getFile(String filename) {
		//System.out.println("getFile:"+filename);
		boolean ok=isValidLocalFileTransferableViaHttp(filename);
		if (!ok) {
			return "Invalid file name <"+filename+ ">: no rights to read this file";
		}
		String path =dirOfLocalFiles+File.separatorChar+filename;
		
		return readAFile(path);
		//return "not found";
	}

	public String reverseToPreviousFileVersion(String filename) {
		//System.out.println("reverseFile:"+filename);
		
		boolean ok=isValidLocalFileTransferableViaHttp(filename);
		if (!ok) {
			return "Invalid file name <"+filename+ ">: no rights to reverse this file";
		}
		String path =dirOfLocalFiles+File.separatorChar+filename;
		
		return doReverseFile(path);
		
		//return "ok";
	}
	
	
	public String cancelReverseToPreviousFileVersion(String filename) {
		//System.out.println("reverseFile:"+filename);
		
		boolean ok=isValidLocalFileTransferableViaHttp(filename);
		if (!ok) {
			return "Invalid file name <"+filename+ ">: no rights to reverse this file";
		}
		String path =dirOfLocalFiles+File.separatorChar+filename;
		
		
		renameFileWithTimeStamp(dirOfLocalFiles+File.separatorChar+filename, "");
		deleteFiles(path,maxNumberOfFileVersion);
	
		
		return doCancelReverseFile(path);
		
		//return "ok";
	}
	
	
	
	// ===
	//== Local Files ===//
	
		public String getDirOfLocalFiles() {
			return dirOfLocalFiles;
		}

		public void setDirOfLocalFiles(String dirOfLocalFiles) {
			this.dirOfLocalFiles = dirOfLocalFiles;
		}

		public void addLocalFileTransferableViaHttp(String fileName) {
			listOfLocalFiles.add(fileName);
		}
		
		public void removeLocalFileTransferableViaHttp(String fileName) {
			listOfLocalFiles.remove(fileName);
		}

		public void setListOfLocalFiles(List<String> listOfLocalFiles) {
			this.listOfLocalFiles = listOfLocalFiles;
		}
		
		public boolean isValidLocalFileTransferableViaHttp(String fileName) {
			return listOfLocalFiles.indexOf(fileName)>=0;
		}
		
		public int getMaxNumberOfLocalFileVersion() {
			return maxNumberOfFileVersion;
		}

		public void setMaxNumberOfLocalFileVersion(int maxNumberOfFileVersion) {
			this.maxNumberOfFileVersion = maxNumberOfFileVersion;
		}

		public void addFileModificationViaUploadListener(IFileModificationViaUploadListener l) {
			fileModificationViaUploadListeners.add(l);
		}
	
		public void removeFileModificationViaUploadListener(IFileModificationViaUploadListener l) {
			fileModificationViaUploadListeners.remove(l);
		}
		
	// ====
	
	private  void writeFile(String path, String content) {

		try {

			Files.write(Paths.get(path), content.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	
	private String readAFile(String filePath) 
	{
		String content = "";

		try
		{
			content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return "Error:"+e.getMessage();
		}

		return content;
	}


	
	
	/*
	 * Rename a file in adding a time stamp :
	 * 		name.ext --> name_timestamp.ext
	 * 
	 */
	
	private   static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

	private void renameFileWithTimeStamp(String filePath, String moreInfo) {

		File f = new File(filePath); 

		Path path = Paths.get(filePath); 

		// call getFileName() and get FileName path object 
		Path fileName = path.getFileName(); 
		Path parentPath = path.getParent();
		

		
		String fs=fileName.getFileName().toString();
		int pos=fs.lastIndexOf(".");
		String fileNameExtension="",fileNameWithoutExtension="";
		if (pos!=-1) {
			fileNameExtension = fs.substring(pos);
			fileNameWithoutExtension=fs.substring(0, pos);
		}
		else {
			fileNameWithoutExtension=fs;
		}
		
		Date date = new Date();
		String dir=parentPath.toString(); //System.getProperty("user.dir");
		String newFileName=dir+File.separatorChar+fileNameWithoutExtension+'_'+sdf.format(date.getTime())+fileNameExtension+moreInfo;

		
		if (f.exists()&&!f.isDirectory()) {
			File f2= new File(newFileName);
			boolean success = f.renameTo(f2);
		}
		else {
		//	System.out.println("File Does not Exists"); 
		}

	}

	
	
	
	// get the list of file with the path and name given in filePath and a time extensioon
	
	private  File[]  getListOfFilesWithTimeStamp(String filePath,String moreInfo ) {
		Path path = Paths.get(filePath); 

		
		Path fileName = path.getFileName(); 
		Path parentPath = path.getParent();
		
		
		String fs=fileName.getFileName().toString();
		int pos=fs.lastIndexOf(".");
		String fileNameExtension="",fileNameWithoutExtension="";
		if (pos!=-1) {
			fileNameExtension = fs.substring(pos);
			fileNameWithoutExtension=fs.substring(0, pos);
			//CSLFileManager.instance.reverseFile(filePath);
		}
		else {
			fileNameWithoutExtension=fs;
		}
		
		
		File f = new File(parentPath.toString());
		final String ext=fileNameExtension+moreInfo;
		final String fwext=fileNameWithoutExtension;
		
		
		FilenameFilter filter = new FilenameFilter() {
		        @Override
		        public boolean accept(File f, String name) {
		            return name.endsWith(ext) && name.startsWith(fwext+"_");
		        }
		    };

		
		File[] files = f.listFiles(filter);
		
		Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
		
		return files;
	}
	
	/*
	 * 
	 * delete backup files if there is more files than max
	*/
	
	
	private void deleteFiles(String filePath, int max) {

		

		File[] files=getListOfFilesWithTimeStamp(filePath,"");
		
		int i=0;
		for (File file:files) {
			i=i+1;
			//System.out.println(i+")"+file);
			if (i>max) {
				//System.out.println("--> to del");
				boolean ok=file.delete();
				//System.out.println("--> deleted");
				
			}
		}
		



	}
	
	
	
	private void deleteReverseFile(String filePath) {
		File[] files2=getListOfFilesWithTimeStamp(filePath,".reversed");
		for (File file:files2) {
			//System.out.println("Deleting "+file);
			boolean ok=file.delete();
			}
	}
	
	// put back the previous version of a file
	
	
	
	
	private String doReverseFile(String filePath) {

		

		File[] files=getListOfFilesWithTimeStamp(filePath,"");
		File f=null;
		
		if (files.length>0) {
			f=files[0];
		} else {
			return "No previous version ";
		}
		
		deleteReverseFile(filePath);
		
		
		
		if (f!=null) {
			renameFileWithTimeStamp(filePath,".reversed"); // rename current file
		}
		
		if (f.exists()&&!f.isDirectory()) {
			//System.out.println("Exists"); 
			File f2= new File(filePath);
			boolean success = f.renameTo(f2);
			//System.out.println("success:"+success);
		}
		else {
			//System.out.println("Does not Exists"); 
		}
		
		return "ok";
		
	}
	
	private String doCancelReverseFile(String filePath) {

		
	

		File[] files=getListOfFilesWithTimeStamp(filePath,".reversed");
		File f=null;
		
		if (files.length>0) {
			f=files[0];
		} else {
			return "No reversed version ";
		}
		
		deleteReverseFile(filePath);
		
		
		
		if (f!=null) {
			renameFileWithTimeStamp(filePath,".reversed"); // rename current file
		}
		
		if (f.exists()&&!f.isDirectory()) {
			//System.out.println("Exists"); 
			File f2= new File(filePath);
			boolean success = f.renameTo(f2);
			//System.out.println("success:"+success);
		}
		else {
			//System.out.println("Does not Exists"); 
		}
		
		return "ok";
		
	}

	
	
	



	public static void main(String[] args) {
		//FileUtils.writeFile("test.txt", "zazaa\ncontent");
		
		
		String filePath="test.txt";
		
		//FileUtils.renameFileWithTimeStamp(filePath,"");
		//FileUtils.deleteFiles(filePath,6);
		//CSLFileManager.instance.reverseFile(filePath);
		CSLConfigFileServer cslFileManager= new CSLConfigFileServer();
		
		cslFileManager.setMaxNumberOfLocalFileVersion(3); // default 99, -1 keep all
		
		cslFileManager.addLocalFileTransferableViaHttp("testfile.txt");
		cslFileManager.addFileModificationViaUploadListener(new IFileModificationViaUploadListener() {
			
			@Override
			public void modifying(String filename) {
				// TODO Auto-generated method stub
				System.out.println("File "+filename+" has been modified");
			}
		});
		
		//String s=cslFileManager.setFile("testfile.txt", "ligne 1");
		String s=cslFileManager.reverseToPreviousFileVersion("testfile.txt");
		System.out.println(s);
		
		
	}
}
