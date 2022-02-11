package com.csl.web.database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.web.CSLHttpServer;
import com.csl.web.websockets.CSLWebSocket;
import com.xcsl.ids.IDSTrace;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

import spark.Request;
import spark.Response;


public class DataBaseServer {

	//static public DataBaseServer instance= new DataBaseServer();

	private static   String datafiledir = System.getProperty("user.dir")+File.separatorChar+"datafile";

	boolean initialized=false;
	//JCmdManager jCmdManager=null;


	

	public  DataBaseServer() {

		

	}


	public  void init(Json j) {
		// TODO Auto-generated method stub
		//Json jConfig=CSLContext.context.getConfig().get("ihm_server_conf");

		String datafilesubdir="datafile";





		if (j!=null)
			datafilesubdir=JsonUtil.getStringFromJson(j, "datafile_subdir","datafile");


		datafiledir = CSLContext.instance.buildFullPathInUserDir(datafilesubdir);
		//System.getProperty("user.dir")+File.separatorChar+datafilesubdir;
		JServiceLoader.displayInfo(datafiledir);

		//datafiledir = IDSRunner.instance.getUserDir()+File.separatorChar+datafilesubdir;


		File f=new File(datafiledir);
		f.mkdirs();

		//	try {
		//		DirWatcher.startWatcher(datafiledir);
		//	} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
		//		// TODO Auto-generated catch block
		//		e.printStackTrace();
		//	}



		// database
	/*	

	 */

	}

	public void addRoute(CSLHttpServer cslHttpServer ) {
		cslHttpServer.addPostRoute("/save_jsonfile/:file", (req, res) -> do_save_jsonfile(req,res));
		cslHttpServer.addGetRoute("/load_jsonfile/:file", (req, res) -> do_load_jsonfile(req,res));

		cslHttpServer.addGetRoute("/dir_jsonfile", (req, res) -> do_dir_jsonfile(req,res));
	}
	
	
	public  String getDataBaseDirectory() {
		return datafiledir;

	}


	private  String do_save_jsonfile(Request req, Response res) {



		String fileName=req.params(":file");
		String sresponse = req.body();
		Json data = Json.read(sresponse);


		Json jresult = saveJsonAsDataFile(fileName,data);


		fileName=cleanDataFileName(fileName);
		IDSTrace.log(IDSTrace.WEB_DATABASE,
				"Save jsonfile:"+fileName);

		return jresult.toString();
	}



	private  String do_load_jsonfile(Request req, Response res) {


		String fileName=req.params(":file");

		Json jresult =loadDataFileAsJson(fileName);


		String result=jresult.toString();

		res.body(result);
		return result;
	}

	private  String do_dir_jsonfile(Request req, Response res) {


		String fileName=req.params(":file");

		Json jresult =listJsonFilesAsJson();


		String result=jresult.toString();

		res.body(result);
		return result;
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

		String content = "";

		try
		{
			content = new String ( Files.readAllBytes( Paths.get(path) ) );
			Json z=Json.read(content);
			return z;
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


	private  void sendEventFileModifiedForAll() {

		Json j=Json.object();
		j.set("action","modified");
		j.set("uuid", "");

		List<String> listOfFiles = listJsonFiles();

		for (String fileName:listOfFiles) {
			j.set("name",fileName);
//			CSLWebSocketForDatabase.broadcastMessageJson("database",j );
			CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_DATABASE,j );
		}

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



//	private String prettyPrint(Json j) {
//		ScriptEngineManager factory = new ScriptEngineManager();
//		// create JavaScript engine
//		ScriptEngine engine = factory.getEngineByName("JavaScript");
//
//		String result="{\"msg\":\"Invalid json\"}";
//
//		try {
//
//
//			Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE); 
//			bindings.clear(); 
//
//			bindings.put("jsonString",j.toString());
//
//			engine.eval("result = JSON.stringify(JSON.parse(jsonString),null,2)");
//			result = (String)bindings.get("result"); 
//			//System.out.println(result);
//
//		} catch (ScriptException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return result;
//	}


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






	{
		//	init();

	}



	public  void main(String[] args) {

		System.out.println(cleanDataFileName("c://teata/frq.$!!&&$$jkj.jk.json"));

	}




}