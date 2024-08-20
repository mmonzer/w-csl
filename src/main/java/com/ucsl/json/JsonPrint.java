package com.ucsl.json;

import java.io.StringWriter;
import java.util.Map;



public class JsonPrint {

	static StringWriter s= new StringWriter();
	
	public static void write(Json j,String indent,String sep ) {
		if (j.isObject()) {
			s.write(indent+"{\n");
			Map<String, Json> elements = j.asJsonMap();
			int len=j.asJsonMap().size();int n=1;
			for (Map.Entry x : elements.entrySet()) {
				  s.write(indent+x.getKey()+":");
				  if (n<len) sep=","; else sep="\n";n++;					  
		          write((Json)x.getValue()," "+indent,sep);
		          //if (n<len) 
		        	  s.write("\n");
		        }
			s.write(indent+"}"+sep);
		}
		else if (j.isArray()) {
			s.write("[\n");
			int len =j.asJsonList().size();int n=1;
			for (Json x:j.asJsonList()) {
				if (n<len) sep=","; else sep="\n";n++;			
				write(x," "+indent,sep);
		        }
			s.write(indent+"]"+sep);
		}
		else if (j.isString()) s.write(indent+j.asString()+sep);
		else if (j.isNumber()) s.write(indent+j.toString()+sep);
		else if (j.isBoolean()) s.write(indent+j.toString()+sep);
		else s.write(indent+j.toString()+sep);
		
	}
	
	  public static String toString(Json j) {
		  write(j, "","");
		  return s.toString();
	     
	    }
	  
	  
	 
}
