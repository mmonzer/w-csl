
package com.csl.intercom.jsoncmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

import spark.Request;
import spark.Response;
import spark.Route;

public class ApiCommands {

	static boolean debug =true;

	boolean initialized = false;
	HashMap<String, JsonCmd> listOfCommands = new HashMap<String, JsonCmd>();
	HashMap<String, JsonCmdHelp> listOfCommandHelps = new HashMap<String, JsonCmdHelp>();
	private String path = "";

	List<String> listOfCommandNames= new ArrayList<String>();
	
	public void setName(String name) {
		// TODO Auto-generated method stub
		this.path=name;
	}
	
	public Json exec(String name, Json params) {

		JsonCmd j = listOfCommands.get(name);

		if (j == null) {
			if (name.compareToIgnoreCase("help")==0) {
				
				return getHelp(params);
			}
			
			Json jresult = Json.object();
			jresult.set("error", "Command <" + name + "> not found");
			return jresult;
		}

		return j.exec(params);
	}

	public String registerCmd(String name, JsonCmd j) {
		if (listOfCommands.get(name) != null)
			return "Command with this name already registered :"+name;
		listOfCommands.put(name, j);
		listOfCommandNames.add(name);
		return "ok";
	}
	
	public String registerCmd(String name, JsonCmd j, JsonCmdHelp jh) {
		if (listOfCommands.get(name) != null)
			return "Command with this name already registered :"+name;
		listOfCommands.put(name, j);
		listOfCommandNames.add(name);
		if (jh!=null) listOfCommandHelps.put(name, jh.setName(name));
		return "ok";
	}
	
	private String formatPath(String proxyPath) {
		if (proxyPath == null)
			proxyPath = "/";
		if (!proxyPath.endsWith("/"))
			proxyPath += "/";
		return proxyPath;
	}
	
	public String getPathFilter() {
		return formatPath(path)+"*";
	}
	
	public String getPathName() {
		if (path == null)
			return "";
		if (path.endsWith("/"))
			return path.substring(0, path.length() - 1);
		return path;
	}

	public String getPathFilterForGet() {
		String s=getPathFilter();
		if (!s.startsWith("/")) s="/"+s;
		return s;
	}
	
	public String getPathNameForPost() {
		String s=getPathName();
		if (!s.startsWith("/")) s="/"+s;
		return s;
	}
	
	public String getCleanApiName() {
		
		String s=path;
		if (s.endsWith("/"))
			s= s.substring(0,s.length() - 1);
		
		if (s.startsWith("/")) s=s.substring(1);
		
		return s;
	}
	
	public String getName() {
	
		return getCleanApiName();
	}
	
	/*
	 * path : /test (for example)
	 */

	public ApiCommands(String path) {
		// if (initialized) return;

		//CSLServer.addPostRoute(path, (req, res) -> execPostCommand(req, res));
		//CSLServer.addGetRoute(path, (req, res) -> renderGetCommand(req, res));

		initialized = true;
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public Route getPostRoute() {
		return (req, res) -> execPostCommand(req, res);
	}

	public Route getGetRoute() {
		return (req, res) -> renderGetCommand(req, res);
	}

	public List<String> getListOfCommands() {
		return new ArrayList<String>(listOfCommands.keySet());
	}

	public JsonCmd getJCmd(String name) {
		return listOfCommands.get(name);
	}

	private String execPostCommand(Request req, Response res) {

		System.out.println("API POST : "+path);
		String sresponse = req.body();
		System.out.println("\n<" + sresponse+">");
		System.out.println("path:" + req.pathInfo());

		String result = "";

		// if (s.compareToIgnoreCase("setfile")==0)

		Json data = Json.read(sresponse);
		Json cmd = data.get("cmd");
		Json params = data.get("params");

		if (cmd == null) {
			System.out.println("Invalid jcmd:" + cmd);
		}
		if (params == null) {
			params = Json.object();
		}

		if (debug) System.out.println("Exec " + cmd + " " + params);

		
		String cresult = exec(cmd.asString(), params).toString();

		return cresult;
	}

	private String renderGetCommand(Request req, Response res) {

		//System.out.println("API GET : "+path);
		
		Set<String> paramKeys = req.queryParams();

		// String sresponse = req.body();

		String s = req.pathInfo();
		if (s.length() > 1)
			s = s.substring(1);

		//System.out.println("pathInfo="+s);
		String cmd = "???";

		List<String> varNames = new ArrayList<String>(paramKeys);

		Json params = Json.object();
		for (String name : varNames) {
			String value = req.queryParams(name);
			if (name.compareTo("cmd") == 0) {
				cmd = value;
			} 
			if (name.compareTo("exec_jsoncmd") == 0) {
				cmd = value;
			}
			else {
				params.set(name, value);

			}
		}

		System.out.println("Exec " + cmd + " " + params);

		String cresult = exec(cmd, params).toString();

		return cresult;
	}
	
	
	public Json execJcmd(Json jCmd) {

		
			Json data = jCmd;
			Json cmd = data.get("cmd");
			Json params = data.get("params");

			if (cmd == null) {
				System.out.println("Invalid jcmd:" + cmd);
			}
			if (params == null) {
				params = Json.object();
			}

			if (debug) System.out.println("Exec " + cmd + " " + params);

			return exec(cmd.asString(), params);

			
		}

	public Json getHelp(Json params) {
		// TODO Auto-generated method stub
		//String s="";
		
		Json jlist= Json.array();
		
		boolean showExamples=false;
		Json jExamples= Json.array();
		if (params.has("ex")) {
			jExamples=loadExamples(getName());
			showExamples=true;
		}
		
		for (String name:listOfCommandNames) {
		    //System.out.println(entry.getKey() + "/" + entry.getValue());
		    
		    //String name=entry.getKey();
		    if (listOfCommandHelps.get(name)!=null) {
		    	//s=s+name+'\n';
		    	JsonCmdHelp z = listOfCommandHelps.get(name);
		    	if (!z.isHidden()) {
		    		Json jz=z.toJson(params);
		    		if (showExamples) {
		    			Json ex=findExemple(jExamples, name);
		    			if (ex!=null) {
		    				if (ex.has("params")) jz.set("ex_params", ex.get("params"));
		    				if (ex.has("result")) jz.set("ex_result", ex.get("result"));
		    				
		    			}
		    		}	
		    		 jlist.add(jz);
		    //	if (!z.isHidden()) jlist.add(listOfCommandHelps.get(name).toJson(params));
		    	}
		    }
		    else {
		    	if (name.compareToIgnoreCase("help")!=0) {
		    		Json r= Json.object().set("cmd", name).set("desc", "no info");
		    		if (params.has("status")) r.set("status", "");
		    		jlist.add(r);
		    	}
		    }
		}
		return jlist;
	}
	
	
	private Json execCmd(String api,String cmd, Json jparams) {


		Json j= Json.object();

		j.set("cmd", cmd);
		j.set("params",jparams);
		return JServiceLoader.cslInterModuleCommunicationManager.executeCommand(api, j);
	}
	
	public Json readObjectFromDatabase(String name) {
		Json p= Json.object();
		p.set("name", name);
	
		String api="dbjson";
		return execCmd(api,"load_jsonfile", p);
	
	}
	
	public Json loadExamples(String apiname) {
		
		
		Json result=readObjectFromDatabase("helpex_"+apiname);
		
		//System.err.println("*** "+apiname+"  ex:"+result);
		if (result.has("contents")) return  result.get("contents");
		//if (result.isArray()) return  result;
		if (result.has("error")) return Json.array();
		
		
		return Json.array();
		
	}

	
	public Json findExemple(Json examples, String cmd) {
		
		for (Json j:examples) {
			if (j.has("cmd")) {
				String c=JsonUtil.getStringFromJson(j,"cmd","");
				if (cmd.compareTo(c)==0) return j;
			}
		}
		
		return null;
	}
	
	public String toString() {
		String s="";// ;
		for (Entry<String, JsonCmd> entry : listOfCommands.entrySet()) {
		    //System.out.println(entry.getKey() + "/" + entry.getValue());
		    
		    String name=entry.getKey();
		    if (!s.isEmpty()) s=s+",";
		    s=s+name;
		    }
	
		return getCleanApiName()+":["+s+"]";
	}
	

}
