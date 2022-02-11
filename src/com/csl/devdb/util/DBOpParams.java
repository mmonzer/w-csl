package com.csl.devdb.util;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;


public class DBOpParams {

	Json user=null;
	String error="";

	String op="";
	boolean modeIp=true;
	String id=null;
	String id2=null;
	String path=null;

	Json value=null;
	Json selector=null;

	public DBOpParams(Json params) {


//test ici
		
		//  {op:operation,user:user, id:id,id2:id2,path:path,value:value,mode_ip:mode_ip})
		
		

		System.out.println("OP DB PARAMS:"+JsonUtil.prettyPrint(params));

		user=params.get("user");
		if (user==null) {
			setError("No user specified");

		}
		else {
			op= JsonUtil.getStringFromJson(params, "op","");
			
			
			if (params.has("ip")) {
				id=JsonUtil.getStringFromJson(params,"ip","");
				modeIp=true;
				
			}
			else if (params.has("uuid")) {
				id=JsonUtil.getStringFromJson(params,"uuid","");
				modeIp=false;
			}
			else {
				modeIp= JsonUtil.getBooleanFromJson(params, "mode_ip",true);				
				id=JsonUtil.getStringFromJson(params,"id","");
			
			}
			
			path=JsonUtil.getStringFromJson(params,"path","");
			value=params.get("value");
			if (params.has("props")&&!params.has("value")) value=params.get("props");

			if (params.has("id2")) { 
				id2=JsonUtil.getStringFromJson(params,"id2","");			
			} else {
				id2=JsonUtil.getStringFromJson(params,"ip2","");			
			}
			
		
				
				
		if (params.has("selector")) selector=params.get("selector");
			
		


		if (params.has("ip")&&(params.has("id")||params.has("mode_ip"))) {
			setError("Specify ip or id and modeip");
		}
		if (params.has("uuid")&&(params.has("id")||params.has("mode_ip"))) {
			setError("Specify uuid or id and modeip");
		}
		if (params.has("ip")&&params.has("uuid")) {
			setError("Specify i or uuid (not both)");
		}
		}
		
	}


	public String addString(String result, String txt,String s ) {
		if (s!=null) result=result+txt+"="+s+" ";
		return result;
	}
	
	
	
	public String addBoolean(String result, String txt,boolean b ) {
		result=result+txt+"="+b+" ";
		return result;
	}
	public String addJson(String result, String txt,Json j ) {
		if (j!=null) result=result+txt+"="+j+" ";
		return result;
	}
	
	public String toString() {
		String result="";
		result=addString(result,"op", op);
		result=addBoolean(result,"modeIP", modeIp);
		result=addString(result,"id", id);
		result=addString(result,"id2", id2);
		result=addString(result,"path",path);
		result=addJson(result,"value",value);
		result=addJson(result,"selector",selector);
		
		
		return result;
	}



	public boolean hasError() {
		return !error.isEmpty();
	}


	public String getError() {
		return error;
	}


	public void setError(String error) {
		this.error = error;
	}


	public Json getUser() {
		return user;
	}


	public String getOp() {
		return op;
	}


	public boolean getModeIp() {
		return modeIp;
	}


	public String getId() {
		return id;
	}


	public String getId2() {
		return id2;
	}


	public String getPath() {
		return path;
	}


	public Json getValue() {
		return value;
	}


	public Json getSelector() {
		return selector;
	}


	public boolean hasId() {
		// TODO Auto-generated method stub
		if (getId()==null) return false;
		if (getId().isEmpty()) return false;
		return true;
	}


	

}
