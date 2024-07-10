package com.csl.ids;

//import java.io.File;
//import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;


import com.ucsl.interfaces.IFileStoreService;
import com.ucsl.interfaces.IIDSMainProcessor;
import com.ucsl.interfaces.IIDSMainProcessorParams;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import com.ucsl.util.IDSUtil;


public class IDSTapManager {
	
	String tapsConfigFileName="TapsConfiguration.json";
	String tapsDir="taps";
	
	String suricataConfigFileName="suricata.yaml";
	String tapConfigFileName="config.ini";
	
	

	static public int SURICATA_RULES_BASE=3;
	static public int SURICATA_RULES_LEARNED=4;
	static public int SURICATA_CONFIG=5;
	static public int TAP_CONFIG=6;
	
	
	private String rulesForSuricataBaseFileName="RulesForSuricata.txt";
	private String rulesForSuricataLearnedFileName="RulesForSuricataLearned.txt";


	String rootDir="./idsconf";
	List<String> listTapIds= new ArrayList<>();

	boolean initialized=false;


	// operations : create, delete, rename, select, copy

	public static String ADD="add";
	public static String DEL="del";
	public static String RENAME="rename";
	public static String UPDATE="update";
	public static String LIST="list";

	public static String LOAD="load";
	public static String SAVE="save";
		

	Json jTaps=Json.array();


	IIDSMainProcessorParams idsParams;
	private IFileStoreService fileUtils;
	private IIDSMainProcessor idsMainProcessor;

	
	public IDSTapManager(IIDSMainProcessorParams idsParams) {
		this.idsParams=idsParams;
		this.idsMainProcessor= idsParams.getIdsMainProcessor();
		this.fileUtils=idsParams.getFileUtils();
	}

	
	public IDSTapManager() {
		
	
	}
	
	
	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	void init() {
		if (idsParams!=null) {
			setRulesForSuricataBaseFileName(idsParams.getRulesForSuricataBaseFileName());
			setRulesForSuricataLearnedFileName(idsParams.getRulesForSuricataBaseFileName());
			setRootDir(idsParams.getIdsModelDir());
			setTapsIDs(idsParams.getTapsIDs());
			
		}
	
		loadTapsConfig();
		setInitialized(true);
	}
	
	public String getRootDir() {
		return rootDir;
	}

	public void setRootDir(String rootDir) {
		this.rootDir = rootDir;
	}

	public String getTapsDir() {
		return getRootDir()+IDSUtil.fileSeparator+tapsDir;
	}
	


	public List<String> getTapsIDs() {
		return listTapIds;
	}


	public void setTapsIDs(List<String> listTapIds) {
		this.listTapIds = listTapIds;
	}
	
	
	public void saveTapsConfig() {
		String dir=		getTapsDir();
		String fileName=	tapsConfigFileName;
		Json j= Json.object();
		fileUtils.saveJsonToFile(dir, fileName, jTaps);
		
	}
	
	
	public void loadTapsConfig() {
		
		String dir=		getTapsDir();
		System.out.println("TAP dir="+dir);
		if (fileUtils.fileExists(dir,tapsConfigFileName))
			jTaps=fileUtils.readJsonFromFile(dir, tapsConfigFileName);
		else
			jTaps = Json.array();
		
		System.out.println("Reading TAPS conf:"+jTaps);
		
	}
	
	

	public String getRulesForSuricataBaseFileName() {
		return rulesForSuricataBaseFileName;
	}
	public void setRulesForSuricataBaseFileName(String rulesForSuricataBaseFileName) {
		this.rulesForSuricataBaseFileName = rulesForSuricataBaseFileName;
	}
	public String getRulesForSuricataLearnedFileName() {
		return rulesForSuricataLearnedFileName;
	}
	public void setRulesForSuricataLearnedFileName(String rulesForSuricataLearnedFileName) {
		this.rulesForSuricataLearnedFileName = rulesForSuricataLearnedFileName;
	}
	

	public Json findTapDescriptor(String idname) {
		
		for (Json j:jTaps.asJsonList()) {
			String s=JsonUtil.getStringFromJson(j, "idname","");
			if (s.compareTo(idname)==0) return j;
		}
		return null;
	}
	
	public boolean isExistingTapDescriptor(String idname) {
		return findTapDescriptor(idname)!=null;
	}
	
	public Json emptyFileError(String error) {
		Json result=Json.object();
		result.set("text", "");
		result.set("error", error);
		return result;
	}
	
	public Json emptyFile() {
		Json result=Json.object();
		result.set("text", "");
		result.set("error","");
		return result;
	}
	
	public Json loadSuricataRulesBase(String idname) {
		if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found "+idname);
		String filename=
				getTapsDir()+IDSUtil.fileSeparator+idname+IDSUtil.fileSeparator+getRulesForSuricataBaseFileName();
		
		if (!fileUtils.fileExists( filename)) return emptyFile();
		return fileUtils.readFileInAJsonText(filename);
	}
	
	// jcontents.text 
	public Json saveSuricataRulesBase(String idname,String text) {
		if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found "+idname);
		String filename=
				getTapsDir()+IDSUtil.fileSeparator+idname+IDSUtil.fileSeparator+getRulesForSuricataBaseFileName();
		
		return fileUtils.writeFileFromText(filename, text);
		
	}
	
	
	public Json loadSuricataRulesLearned(String idname) {
		if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found "+idname);
		String filename=
				getTapsDir()+IDSUtil.fileSeparator+idname+IDSUtil.fileSeparator+getRulesForSuricataLearnedFileName();
		
		if (!fileUtils.fileExists( filename)) return emptyFile();	
		return fileUtils.readFileInAJsonText(filename);
	}
	
	// jcontents.text 
	public Json saveSuricataRulesLearned(String idname, String text) {
		if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found "+idname);
		String filename=
				getTapsDir()+IDSUtil.fileSeparator+idname+IDSUtil.fileSeparator+getRulesForSuricataLearnedFileName();
		
		return fileUtils.writeFileFromText(filename, text);
		
	}
	
	public Json loadSuricataConfig(String idname) {
		if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found "+idname);
		String filename=
				getTapsDir()+IDSUtil.fileSeparator+idname+IDSUtil.fileSeparator+suricataConfigFileName;
		if (!fileUtils.fileExists( filename)) return emptyFile();
		return fileUtils.readFileInAJsonText(filename);
	}
	
	// jcontents.text 
	public Json saveSuricataConfig(String idname, String text) {
		if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found "+idname);
		String filename=
				getTapsDir()+IDSUtil.fileSeparator+idname+IDSUtil.fileSeparator+suricataConfigFileName;
		
		return fileUtils.writeFileFromText(filename, text);
		
	}
	
	public Json loadTapConfig(String idname) {
		if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found "+idname);
		String filename=
				getTapsDir()+IDSUtil.fileSeparator+idname+IDSUtil.fileSeparator+tapConfigFileName;
		if (!fileUtils.fileExists( filename)) return emptyFile();
		return fileUtils.readFileInAJsonText(filename);
	}
	
	// jcontents.text 
	public Json saveTapConfig(String idname, String text) {
		if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found "+idname);
		String filename=
				getTapsDir()+IDSUtil.fileSeparator+idname+IDSUtil.fileSeparator+tapConfigFileName;
		
		return fileUtils.writeFileFromText(filename, text);
		
	}

	public Json addTap(String idname,Json value) {
		Json result= Json.object();
		
		if (!isValidName(idname)) return Json.object().set("error", "Invalid type name "+idname);
		if (isExistingTapDir(idname)||isExistingTapDescriptor(idname)) return Json.object().set("error", "Existing tap name "+idname);
		
		Json j= Json.object();
		j.set("idname", idname);
		j.set("ip", JsonUtil.getStringFromJson(value, "ip",""));
		j.set("username", JsonUtil.getStringFromJson(value, "username",""));
		j.set("password", JsonUtil.getStringFromJson(value, "password",""));
		
		jTaps.add(j);
		createTapDir(idname);
		saveTapsConfig();
		
		return result;
	}
	
	public Json delTap(String idname) {
		Json result= Json.object();
		
		Json j=findTapDescriptor(idname);
		if (j==null) return Json.object().set("error", "Cannot find descriptor for "+idname);
		
		Json jarray= Json.array();
		for (Json jj:jTaps.asJsonList()) {
			if (idname.compareTo(jj.get("idname").asString())!=0) jarray.add(jj);
		}
		jTaps=jarray;
		saveTapsConfig();
		
		deleteTapDir(idname);
		saveTapsConfig();
		
		return result;
	}
	
	public Json updateTap(String idname,Json value) {
		Json result= Json.object();
		
		Json j=findTapDescriptor(idname);
		if (j==null) return Json.object().set("error", "Cannot find descriptor for "+idname);
		
		//j.set("idname", idname);
		if (value.has("ip")) j.set("ip", JsonUtil.getStringFromJson(value, "ip",""));
		if (value.has("username")) j.set("username", JsonUtil.getStringFromJson(value, "username",""));
		if (value.has("password")) j.set("password", JsonUtil.getStringFromJson(value, "password",""));
		
		
		saveTapsConfig();
		
		return result;
	}
	
	public Json renameTap(String idname,Json value) {
		Json result= Json.object();
		
		Json j=findTapDescriptor(idname);
		if (j==null) return Json.object().set("error", "Cannot find descriptor for "+idname);
	
		String newName=JsonUtil.getStringFromJson(value, "new_idname","");
		if (!isValidName(newName)) return Json.object().set("error", "Invalid type name "+newName);
		if (isExistingTapDir(newName)||isExistingTapDescriptor(newName)) return Json.object().set("error", "Existing tap name "+newName);
	
		if (renameTapDir(idname, newName)) {
			j.set("idname", newName);
				
		}
		saveTapsConfig();
		return result;
	}
	
	public Json tapsList() {
		
		return jTaps;
	}
	
	
	
	
	// return {} if ok, {"error":"vvxvx"} otherwise
	public Json exec(String cmd, String idname, Json value, String text) {
			//int category, String name, int targetCategory, String targetName, boolean overwrite) {

		String s="";

		if (!isInitialized()) init();
		
		cmd=cmd.toLowerCase();
		if (ADD.compareTo(cmd)==0) {
			return addTap(idname, value);
		
		}
		else if (DEL.compareTo(cmd)==0) {
			return delTap(idname);
		}
		else if (RENAME.compareTo(cmd)==0) {
			return renameTap(idname, value);
		}
		else if (UPDATE.compareTo(cmd)==0) {
			return updateTap(idname, value);
		}
		
		else if (LIST.compareTo(cmd)==0) {
			return tapsList();
			
		}
		else if (LOAD.compareToIgnoreCase(cmd)==0) {
			int type = JsonUtil.getIntFromJson(value, "type", 0);
			if (type==SURICATA_CONFIG) return loadSuricataConfig(idname);
			else if (type==TAP_CONFIG) return loadTapConfig(idname);
			else if (type==SURICATA_RULES_BASE) return loadSuricataRulesBase(idname);
			else if (type==SURICATA_RULES_LEARNED) return loadSuricataRulesLearned(idname);
			else
				return  Json.object().set("error","Invalid type "+type+" in load");
		}
		else if (SAVE.compareToIgnoreCase(cmd)==0) {
			int type = JsonUtil.getIntFromJson(value, "type", 0);
			if (type==SURICATA_CONFIG) return saveSuricataConfig(idname,text);
			else if (type==TAP_CONFIG) return saveTapConfig(idname,text);
			else if (type==SURICATA_RULES_BASE) return saveSuricataRulesBase(idname,text);
			else if (type==SURICATA_RULES_LEARNED) return saveSuricataRulesLearned(idname,text);
		
			else
				return  Json.object().set("error","Invalid type "+type+" in save");
		}
		else {
			return Json.object().set("error","Invalid operation:"+cmd);
		}
	

		
	}

	private String int2str(int n) {
		if (n<=9999) {
			String s="0000"+n;
			return s.substring(s.length()-4, s.length());
		}
		else {
			return ""+n;
		}
	}

	
	
	
	public String normalizeName(String s) {
		String z="";
		for (int i=0;i<s.length();i++) {
			char ch=s.charAt(i);
			if (Character.isLetterOrDigit(ch)) {
				z=z+ch;
			}
		}
		z=z.toLowerCase();
		if (z.length()>8) z=z.substring(0,8);
		return z;
	}

	
	public boolean isValidName(String s) {
		if (s==null) s="";
		if (s.isEmpty())  return false;
		return s.compareTo(normalizeName(s))==0;
	}

	
	public boolean isExistingTapDir(String s) {
		//s=normalizeName(s);
		String dirName= getTapsDir();
		//File file = new File(dirName+IDSUtil.fileSeparator+s);
		//return(file.exists());
		
		return idsMainProcessor.getFileStoreServices().dirExists(dirName+IDSUtil.fileSeparator+s);
	}

	
//	public String newTapName() {
//		String dirName= getTapsDir();
////		File file = new File(dirName);
////		String[] directories = file.list(new FilenameFilter() {
////			@Override
////			public boolean accept(File current, String name) {
////				return new File(current, name).isDirectory();
////			}
////		});
//
//		int n=0;
//
//		while (true) {
//			String s="tap"+int2str(n);
//			File f= new File(dirName+IDSUtil.fileSeparator+s);
//			if (!f.isDirectory()) return s;
//			n=n+1;
//		}
//
//	}

	public void createTapDir(String name) {
		
		String s=normalizeName(name);
		String dirName= getTapsDir();
		//File file = new File(dirName+IDSUtil.fileSeparator+s);
		idsMainProcessor.getFileStoreServices().createSubDir(dirName, s);
		//if (!file.exists()) file.mkdirs();
	
	}
	
	

	
	public void deleteTapDir(String name) {
		
		String s=normalizeName(name);
		String dirName= getTapsDir();
		//String s=dirName+IDSUtil.fileSeparator+s;
		idsMainProcessor.getFileStoreServices().deleteDirectory(dirName, s);
		//deleteDirectory(dirName+IDSUtil.fileSeparator+s);
	}
	
	
	public boolean renameTapDir(String src, String dst)  {
	    
		src=normalizeName(src);
		dst=normalizeName(dst);
		
		String dir=		getTapsDir()+IDSUtil.fileSeparator;
//		String fsrc=dir+src;
//		String fdst=dir+dst;
//		
//		File fileToMove = new File(fsrc);
//	    boolean isMoved = fileToMove.renameTo(new File(fdst));
//	    return isMoved;
	    
	    return
			  idsMainProcessor.getFileStoreServices().renameSubDir(dir, src, dst);
	}

	
	public static void main(String[] args) {
		
		IDSTapManager z = new IDSTapManager();
		
		String s=z.normalizeName("&z3DD wc");
		
		System.out.println(s);
		
		 s="";
		for (int i=1; i< 20; i++) {
			s=s+"e";
			System.out.println(z.normalizeName(s));
		}
		
		z.loadTapsConfig();
		//System.out.println(z.newTapName());
		
		System.out.println(z.exec("list", "",  Json.object(), ""));
		System.out.println(z.exec("add", "tap12",  Json.object(), ""));
		
		z.exec("save", "tap12", Json.object().set("type", 3), "essai");
		z.exec("save", "tap12", Json.object().set("type", 4), "essai learned");
		z.exec("save", "tap12", Json.object().set("type", 5), "sur config");
		z.exec("save", "tap12", Json.object().set("type", 6), "tap config");
		
		for (int i=3;i<7;i++) {
			System.out.println("File #"+i);
			System.out.println(z.exec("load", "tap12", Json.object().set("type", i), ""));
			
		}
		
		System.out.println(z.exec("rename", "tap12",  Json.object().set("idname", "tap13"), ""));
		
		
		
	}
	
}
