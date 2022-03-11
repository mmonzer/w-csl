
package com.csl.intercom.jsoncmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.xcsl.interfaces.IApiCommands;
import com.xcsl.interfaces.IJsonCmd;
import com.xcsl.interfaces.IJsonCmdHelp;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

//import spark.Request;
//import spark.Response;
//import spark.Route;

public class ApiCommands implements IApiCommands {

	static boolean debug =true;

	//boolean initialized = false;
	HashMap<String, IJsonCmd> listOfCommands = new HashMap<String, IJsonCmd>();
	HashMap<String, IJsonCmdHelp> listOfCommandHelps = new HashMap<String, IJsonCmdHelp>();
	private String path = "";

	List<String> listOfCommandNames= new ArrayList<String>();
	
	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		this.path=name;
	}
	
	public Json exec(String name, Json params) {

		IJsonCmd j = listOfCommands.get(name);

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

	@Override
	public String registerCmd(String name, IJsonCmd j) {
		if (listOfCommands.get(name) != null)
			return "Command with this name already registered :"+name;
		listOfCommands.put(name, j);
		listOfCommandNames.add(name);
		return "ok";
	}
	
	@Override
	public String registerCmd(String name, IJsonCmd j, IJsonCmdHelp jh) {
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

	
	private  String getCleanApiName() {
		
		String s=path;
		if (s==null) s="";
		if (s.endsWith("/"))
			s= s.substring(0,s.length() - 1);
		
		if (s.startsWith("/")) s=s.substring(1);
		
		return s;
	}
	
	@Override
	public String getName() {
	
		return getCleanApiName();
	}
	
	/*
	 * path : /test (for example)
	 */

	private ApiCommands(String path) {
		// if (initialized) return;

		//CSLServer.addPostRoute(path, (req, res) -> execPostCommand(req, res));
		//CSLServer.addGetRoute(path, (req, res) -> renderGetCommand(req, res));

		//initialized = true;
		this.path = path;
	}

	public ApiCommands() {
		
	}
	
	
	public String getPath() {
		return path;
	}

	

	public List<String> getListOfCommands() {
		return new ArrayList<String>(listOfCommands.keySet());
	}

	public IJsonCmd getJCmd(String name) {
		return listOfCommands.get(name);
	}

	

	
	
	@Override
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
		    	IJsonCmdHelp z = listOfCommandHelps.get(name);
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
		for (Entry<String, IJsonCmd> entry : listOfCommands.entrySet()) {
		    //System.out.println(entry.getKey() + "/" + entry.getValue());
		    
		    String name=entry.getKey();
		    if (!s.isEmpty()) s=s+",";
		    s=s+name;
		    }
	
		return getCleanApiName()+":["+s+"]";
	}

	
	

}
