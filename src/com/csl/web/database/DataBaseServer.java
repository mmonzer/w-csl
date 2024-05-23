package com.csl.web.database;

import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.web.CSLHttpServer;
import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import spark.Request;
import spark.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class DataBaseServer {

	private static   String datafiledir = System.getProperty("user.dir")+File.separatorChar+"datafile";

	public  DataBaseServer() {

	}


	public  void init(Json j) {
		boolean on=JsonUtil.getBooleanFromJson(j, "on",true);
		if (!on) return;
		
		String datafilesubdir="datafile";

		if (j!=null)
			datafilesubdir=JsonUtil.getStringFromJson(j, "datafile_subdir","datafile");


		datafiledir = CSLContext.instance.buildFullPathInUserDir(datafilesubdir);
		JServiceLoader.displayInfo(datafiledir);

		File f=new File(datafiledir);
		f.mkdirs();

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

	private  void sendEventFileModified(String fileName,String uuid) {

		Json j=Json.object();
		j.set("action","modified");
		j.set("name",fileName);
		j.set("uuid", uuid);
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

	public Json saveJsonAsDataFile(String fileName,Json data, boolean pretty) {
		fileName=cleanDataFileName(fileName);

		String result="";
		Json j=Json.object();

		{

			Json contents=data.get("contents");

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


	public  void main(String[] args) {

		System.out.println(cleanDataFileName("c://teata/frq.$!!&&$$jkj.jk.json"));

	}

}