package com.csl.web.database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ICSLService;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.intercom.jsoncmd.JsonCmd;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.csl.web.websockets.CSLWebSocket;
import com.xcsl.ids.IDSTrace;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

public class CSLServiceJsonDataBase implements ICSLService {

	
	private static   String datafiledir = System.getProperty("user.dir")+File.separatorChar+"datafile";




	static boolean debug =true;
	//static CSLAlertManager cslAlertManager = new CSLAlertManager("Intrusion detection");
	ApiCommands apiCommands=new ApiCommands("/"+"dbjson");
	
	
	
	@Override
	public ApiCommands getApiCommands() {
		// TODO Auto-generated method stub
		return apiCommands;
	}

	@Override
	public String getConfigFileSectionName() {
		// TODO Auto-generated method stub
		return "database_server_conf";
	}

	@Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return true;
	}


	
	public boolean init(Json j, String userDir) {

		
		
		String datafilesubdir="datafile";





		if (j!=null)
			datafilesubdir=JsonUtil.getStringFromJson(j, "datafile_subdir","datafile");


		datafiledir = CSLContext.instance.buildFullPathInUserDir(datafilesubdir);
		//System.getProperty("user.dir")+File.separatorChar+datafilesubdir;
		JServiceLoader.displayInfo(datafiledir);

		//datafiledir = IDSRunner.instance.getUserDir()+File.separatorChar+datafilesubdir;


		File f=new File(datafiledir);
		f.mkdirs();

		
		
		/*
		 * user : Json describing the user (for logging of actions)
		 * modeip the identifier is ip if true, uuid if false
		 * op
		 * id  (ip or uuid
		 * path
		 * newParams
		 * oldP
		 */
		

		// api : let x = await asyncExecApiCmd("devdb","op",

	
		
		
		//CSLHttpServer.addApiCommands(apiCommands);
	
		//JServiceLoader.addApi(apiCommands);

	
	
		
		apiCommands.registerCmd("dir_jsonfile", new JsonCmd() {
			@Override
			public Json exec(Json params) {
				
				Json jresult =listJsonFilesAsJson();

				
				return jresult;
			}
		},
				new JsonCmdHelp()
				.setDesc("list of json file")
				.setResult("return a Json array file the list of filenames",JsonCmdHelp.JSON)
				);
		
		apiCommands.registerCmd("save_jsonfile", new JsonCmd() {
			@Override
			public Json exec(Json params) {
				
				boolean pretty=false;
				String fileName=JsonUtil.getStringFromJson(params, "name", "");
				if (fileName.isEmpty()) {
					return Json.object().set("error","no filename");
					
				}
				if (!params.has("contents")) {
					return Json.object().set("error","no contents");
					
				}
				if (params.has("pretty")) {
					pretty=JsonUtil.getBooleanFromJson(params, "pretty", false);
					
				}
				fileName=cleanDataFileName(fileName);
				
				Json r=saveJsonAsDataFile(fileName, params, pretty);
				
				return r;
			}
		},
				new JsonCmdHelp()
				.setDesc("load json file")
				.setParam("name", "name of file",JsonCmdHelp.STR)
				.setParam("contents", "contents of file (Json object)",JsonCmdHelp.JSON)
				
				.setResult("return error if any",JsonCmdHelp.JSON)
				);
		
		apiCommands.registerCmd("load_jsonfile", new JsonCmd() {
			@Override
			public Json exec(Json params) {
				
				String fileName=JsonUtil.getStringFromJson(params, "name", "");
				if (fileName.isEmpty()) {
					return Json.object().set("error","no filename");
					
				}
				fileName=cleanDataFileName(fileName);
				
				return loadDataFileAsJson(fileName);
			}
		},
				new JsonCmdHelp()
				.setDesc("load json file")
				.setParam("name", "name of file",JsonCmdHelp.STR)
				.setResult("return the contents of file as json",JsonCmdHelp.JSON)
				);
		
		
		
		return true;
	}
	
	
	
	public  String cleanDataFileName(String filename) {




		String ext="";
		if (filename.indexOf(".") > 0) {
			ext=filename.substring(filename.lastIndexOf(".")+1,filename.length());
			filename = filename.substring(0, filename.lastIndexOf("."));
		}
		int index = filename.lastIndexOf("\\");
		filename = filename.substring(index + 1);

		index = filename.lastIndexOf("/");
		filename = filename.substring(index + 1);

		filename = filename.replaceAll("^[^a-zA-Z_$]|[^\\w$]", "_");
		ext = ext.replaceAll("^[^a-zA-Z_$]|[^\\w$]", "_");

		if (!ext.isEmpty()) filename=filename+'.'+ext;
		return filename;
	}


	private  String writeDataFile(String path, String content) {

		path=datafiledir+File.separator+path;
		try {

			Files.write(Paths.get(path), content.getBytes());
			return "ok";
		} catch (IOException e) {
			e.printStackTrace();
			return "error";
		}

	}


	private  String deleteDataFile(String path) {

		path=datafiledir+File.separator+path;

		boolean ok=new File(path).delete();
		if (ok) return "ok"; else return "error";


	}

	private  Json readDataFile(String path) 
	{


		path=datafiledir+File.separator+path;
		System.out.println("READING_FILE:"+datafiledir);

		String content = "";

		try
		{
			content = new String ( Files.readAllBytes( Paths.get(path) ) );
			Json z=Json.read(content);
			return Json.object().set("contents",z);
		} 
		catch (IOException e) 
		{
			//e.printStackTrace();
			//return "{\"Error\":\"File not found:"+e.getMessage()+"\"}";
			Json z=Json.object();
			z.set("contents",Json.object());
			z.set("error","Object not found ("+e.getMessage()+")");

			return z;

		}

		//return content;
	}


	private  void sendEventFileModified(String fileName,String uuid) {

		Json j=Json.object();
		j.set("action","modified");
		j.set("name",fileName);
		j.set("uuid", uuid);

//		CSLWebSocketForDatabase.broadcastMessageJson("database",j );
		CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_DATABASE,j );
	}


	

	private  List<String> listJsonFiles() {

		String dir=datafiledir;

		List<String> listOfFiles = new ArrayList<String>();

		File[] list = new File(dir).listFiles();
		for (int i=0;i<list.length;i++) {
			if (list[i].isFile()) {
				String filename=list[i].getName();
				if (filename.toLowerCase().endsWith(".json")) {
					String name = filename.substring(0, filename.lastIndexOf("."));
					listOfFiles.add(name);
				}

			}
		}

		return listOfFiles;
	}

	public  Json listJsonFilesAsJson() {


		Json j=Json.array();

		List<String> listOfFiles = listJsonFiles();

		for (String s:listOfFiles) {
			j.add(s);
		}



		return j;
	}




	public Json saveJsonAsDataFile(String fileName,Json data) {

		
	//	System.out.println("JMFJMF : WRITE JSON TO"+fileName);
		return saveJsonAsDataFile(fileName, data,false);
	}

	public Json saveJsonAsDataFile(String fileName,Json data, boolean pretty) {



		fileName=cleanDataFileName(fileName);
		IDSTrace.log(IDSTrace.WEB_DATABASE,
				"Save jsonfile:"+fileName);

		String result="";
		Json j=Json.object();


		{

			Json contents=data.get("contents");
			IDSTrace.log(IDSTrace.WEB_DATABASE,
					"Save Contents="+data.get("contents"));

			String uuid=JsonUtil.getStringFromJson(data, "uuid","");


			String s="";
			if (pretty)
				s=JsonUtil.prettyPrint(contents);
			else
				s=contents.toString();



			if ((fileName!=null)&&(contents!=null)) {
				result=writeDataFile(fileName+".json", s);
				sendEventFileModified(fileName, uuid);
				j.set("result","ok");


			} else {
				if (fileName==null) j.set("error", "Invalid file name ");
				if (contents==null) j.set("error","Invalid contents ");

			}
		}



		return j;
	}





	public boolean  docExists(String name) {
		String fileName=cleanDataFileName(name);
		String path=datafiledir+File.separator+fileName+".json";

		return new File(path).isFile();


	}


	public Json loadDataFileAsJson(String name) {


		String fileName=cleanDataFileName(name);
		IDSTrace.log(IDSTrace.WEB_DATABASE,
				"Load jsonfile:"+fileName);


		String result="";
		Json j=Json.object();

		if (fileName!=null) {
			//result=DataBaseServer.readDataFile(fileName+".json");
			j=readDataFile(fileName+".json");
			//Json z=Json.read(result);
			//j.set("contents",z);
			IDSTrace.log(IDSTrace.WEB_DATABASE,
					"File Contents="+result);

		} else {
			j.set("contents",Json.object());
			j.set("error", "No file with name:"+name);
			IDSTrace.log(IDSTrace.WEB_DATABASE,
					"File Load error="+j.toString());

		}

		return j;
	}



	public Json deleteDataFileJson(String name) {
		String fileName=cleanDataFileName(name);
		IDSTrace.log(IDSTrace.WEB_DATABASE,
				"Delete jsonfile:"+fileName);


		String result=deleteDataFile(fileName+".json");
		Json j=Json.object();
		j.set("result", result);
		return j;

	}

		

}
