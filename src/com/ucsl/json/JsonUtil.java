package com.ucsl.json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.ucsl.interfaces.EvaluationException;


public class JsonUtil {


	static public  String replaceSingleQuote(String s) {

		return s.replaceAll("'", "\"");
	}


	static public boolean setJsonField(Json j,List<JsonSelectorElement> selector,String value) {



		return false;
	}


	static public Json getJsonField(Json j,List<JsonSelectorElement> selector) {
		for (JsonSelectorElement sel:selector) {
			if (sel.isIndex())
				j=j.at(sel.getIndex());
			else
				j=j.at(sel.getField());
			////System.out.println("Found json subobject:"+j);
			if (j==null) {
				//throw new EvaluationException("Invalid field or index:"+sel);
				return null;
			}
		}
		return j;

	}


	public static void setElement(Json j, String s , Json value) throws JsonException {

		setElement(j, s, value,false);
	}

	public static void setElement(Json j, String s , Json value, boolean createMissingProps) throws JsonException {

		JsonSelector js= new JsonSelector().parse(s);

		if (js.size()==0) throw new JsonException("Invalid selector:"+s);

		setElement(j, js.get(0), value, createMissingProps);
	}

	public static void setElement(Json j, JsonSelector js , Json value,boolean createMissingProps) throws JsonException {
		if (js.size()==0) return;
		setElement(j, js.get(0), value,createMissingProps);
	}

	public static void setElement(Json j, JsonSelectorElement jse, Json value, boolean createMissingProps) throws JsonException {

		//System.out.println("j="+j);

		if (jse.isIndex()) {
			if (!j.isArray())  throw new JsonException("Expecting array for "+jse.getParent().getName());
			int len=j.asJsonList().size();
			if (jse.getIndex()<len) {
				if (jse.getChild()==null) {
					j.set(jse.getIndex(), value);
					return ;
				}

				setElement(j.at(jse.getIndex()), jse.getChild(),value,createMissingProps);
			}
			else
			 throw new JsonException("Index out of bounds "+jse.getParent().getName()+" index:"+jse.getIndex());
		}
		else if (jse.isStarSelector()  ) {
			if (!jse.getParent().isArray())  throw new JsonException("Expecting array for "+jse.getParent().getName());
			if (jse.getChild()==null) {
				j.add(value);
			}
			else {
				Json jj;
				if (jse.getChild().isIndex()||jse.getChild().isStarSelector()) {
					jj=Json.array();
				}
				else {
					jj=Json.object();
				}
				j.add(jj);
				setElement(jj, jse.getChild(),value,createMissingProps);
			}

		}
		else {
			if (j.isArray()) {
				throw new JsonException("Index expected :"+jse.getField());
			}
			if (jse.getChild()==null) {
				j.set(jse.getField(),value);
			}
			else {
				//System.out.println("Field="+jse.getField());
				if (j.has(jse.getField())) {
					setElement(j.get(jse.getField()), jse.getChild(),value,createMissingProps);
				}
				else {
					if (!createMissingProps)
						throw new JsonException("Invalid prop  <"+jse.getField()+"> in "+ jse.getParent().getName());

					Json jj;
					if (jse.getChild().isIndex()||jse.getChild().isStarSelector()) {
						jj=Json.array();
					}
					else {
						jj=Json.object();
					}
					j.set(jse.getField(),jj);
					setElement(jj, jse.getChild(),value,createMissingProps);

				}
			}
		}




	}

	static public Json getElement(Json j, String s) throws JsonException {
		JsonSelector js= new JsonSelector().parse(s);

		if (js.size()==0) throw new JsonException("Invalid selector:"+s);

		return getElement(j, js.get(0));


	}

	static public Json getElement(Json j, JsonSelectorElement jse) throws JsonException {

		//System.out.println("j="+j);

		if (jse.isIndex()) {
			if (!j.isArray())  throw new JsonException("Expecting array for "+jse.getParent().getName());
			int len=j.asJsonList().size();
			if (jse.getIndex()<len) {
				if (jse.getChild()==null) {
					//	j.set(jse.getIndex(), value);
					return j.at(jse.index);
				}

				return getElement(j.at(jse.getIndex()), jse.getChild());
			}
			throw new JsonException("Index out of bounds "+jse.getParent().getName()+" index:"+jse.getIndex());
		}
		else if (jse.isStarSelector()  ) {
			throw new JsonException("Invalid index value "+jse.getParent().getName()+" *");

		}
		else {
			if (jse.getChild()==null) {
				return j.get(jse.getField());
			}
			else {
				//System.out.println("Field="+jse.getField());
				if (j.has(jse.getField())) {
					return getElement(j.get(jse.getField()), jse.getChild());
				}
				else {
					throw new JsonException("Invalid prop name "+jse.getField()+" in "+ jse.getParent().getName());
				}
			}
		}




	}


	static public String getAsString(Json j, String pname) {

		Json x = j.get(pname);
		if (x==null) return null;
		if (x.getValue()==null) return null;
		return x.asString();

	}

	static public  String getStringFromJson(Json j, String propName,String defaultValue) {
		//Json v=j.get(propName);
		Json v=findChild(j,propName);
		if (v==null) return defaultValue;
		return v.asString();
	}

	static public  List<String> getListStrFromJson(Json j, String propName, List<String> defaultValue) {
		Json v=findChild(j,propName);
		if (v==null) return defaultValue;
		List<String> list = new ArrayList<>();
		for (Json val: v.asJsonList()) {
			if (val.isString()) { list.add(val.asString()); }
		}
		return list;
	}


	static public  Boolean getBooleanFromJson(Json j, String propName, Boolean defaultValue) {
		//Json v=j.get(propName);
		Json v=findChild(j,propName);
		if (v==null) return defaultValue;
		return v.asBoolean();
	}


	public static Long getLongFromJson(Json j, String propName, long defaultValue) {
		//Json v=j.get(propName);
		Json v=findChild(j,propName);
		if (v==null) return defaultValue;
		return v.asLong();
	}

	public static Integer getIntFromJson(Json j, String propName, Integer defaultValue) {
		//Json v=j.get(propName);
		Json v=findChild(j,propName);
		if (v==null) return defaultValue;
		return v.asInteger();
	}


	public static Double getDoubleFromJson(Json j, String propName, Double defaultValue) {
		//Json v=j.get(propName);
		Json v=findChild(j,propName);
		if (v==null) return defaultValue;
		return v.asDouble();
	}

	public static Json getJson(Json j, String propName) {
		Json v=findChild(j,propName);
		return v;
	}

	public static Json findChild(Json j, String path) {

		if (j==null) return null;

		path=path.replace(".", "/");
		
		String[] paths=path.split("/");
		for (int i=0;i<paths.length;i++) {
			j=j.get(paths[i]);
			if (j==null) return null;
		}
		return j;
	}



	public static final String EOL = System.getProperty("line.separator");

//	private static String readFile(String filename) throws IOException {
//		BufferedReader br = null;
//		FileReader fr = null;
//
//		try {
//			fr = new FileReader(filename);
//			br = new BufferedReader(fr);
//			String nextLine = "";
//			StringBuilder sb = new StringBuilder();
//			while ((nextLine = br.readLine()) != null) {
//				sb.append(nextLine); // note: BufferedReader strips the EOL character
//				//   so we add a new one!
//				sb.append(EOL);
//			}
//			return sb.toString();
//		}
//		finally {
//			if (br != null) br.close();
//			if (fr != null) fr.close();
//		}
//	}

//	public static Json readFileAsJson(String f)  {
//		//path, String fileName) {
//
//
//		//URL  url= new URL("file:///"+fileName);
//		String content="{}";
//		//String f=path+ IDSUtil.fileSeparator+fileName;
//		try {
//			content = readFile(f);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			//CSLLogger.instance.error("Cannot read file :"+f);
//			IDSMainProcessor.cslLogger().printError(" Read file as Json : cannot read file <"+f+">");
//			return Json.object();
//		}
//
//		return Json.read(content);
//	}


	static public String prettyPrint(Json j) {
		return prettyPrint2(j);
	}
	
	static public String prettyPrint1(Json j) {
		ScriptEngineManager factory = new ScriptEngineManager();
		// create JavaScript engine
		ScriptEngine engine = factory.getEngineByName("JavaScript");

		String result="{\"msg\":\"Invalid json\"}";

		try {


			Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE); 
			bindings.clear(); 

			bindings.put("jsonString",j.toString());

			engine.eval("result = JSON.stringify(JSON.parse(jsonString),null,2)");
			result = (String)bindings.get("result"); 
			//System.out.println(result);

		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}


	public static Json concat(Json p1, Json p2) {
		// TODO Auto-generated method stub
		Json result= Json.object();
		
		for (String key:p1.asJsonMap().keySet()) {
			result.set(key,  p1.get(key));
		}
		
		
		for (String key:p2.asJsonMap().keySet()) {
			result.set(key,  p2.get(key));
		}
		
		return result;
	}
	
	
	static public String prettyPrint2(Json j) {
		return object2str(j,"", "", false);
	}
	
	
	static public boolean compare(Json j1, Json j2) {
		String s1= object2str(j1,"", "", false);
		String s2= object2str(j2,"", "", false);
		
		return s1.compareTo(s2)==0;
	}
	
	static public String object2str(Json j, String decal, String name, boolean addComma) {
		
		String result="";
		String quote="\"";
		if (j.isArray()) {
			int n=j.asJsonList().size(); int i=1;
			if (!name.isEmpty()) 
				result = result+decal+name+": [";
			else 
				result = decal+"[";
			if (n==0) {
				if (addComma) return result+"],\n"; else return result+"]\n";
				}
			result=result+"\n";
			
			String se=",\n";
			for (Json je:j.asJsonList()) {
				if (je.isObject()||je.isArray()) {
					boolean add=(i!=n);
					result=result+object2str(je, decal+"  ","",add);
				}
				else {
					if (i==n) se="\n";
					result=result+decal+"  "+je.toString()+se;
				}
				i++;
			}
			if (addComma) result = result+decal+"],\n";
			else result = result+decal+"]\n";
			
		}
		else if (j.isObject()) {
			result ="";
			Map<String, Json> map = j.asJsonMap();
			int n=map.size(); int i=1; String se=",\n";
			
			if (!name.isEmpty()) 
				result = result+decal+name+": {";
			else 
				result = decal+"{";
			
			if (n==0) {
				if (addComma) return result+"},\n"; else return result+"}\n";
				}
			result=result+"\n";
			
			
			
			for (Map.Entry<String, Json> entry : map.entrySet()) {
				String key=entry.getKey();
				Json je=entry.getValue();
				if (je.isObject()||je.isArray()) {
					boolean add=(i!=n);
					result=result+object2str(je, decal+"  ",quote+key+quote,add);
				}
				else {
					
					if (i==n) se="\n";
					result=result+decal+"  "+quote+key+quote+":"+je.toString()+se;
					
				}
				i++;
				
				}
			if (addComma) result = result+decal+"},\n";
			else result = result+decal+"}\n";
		}
		else result= j.toString();
		return result;
	}
}
