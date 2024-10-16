package com.csl.intercom.jsoncmd;

import java.util.List;
import java.util.Map.Entry;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

/**
 * Helper class that builds the apihelp HTML page.
 */
public class ApiGetHelp {
	/**
	 * Method that builds the body of HTML page from the api data.
	 * @param apiNames list of names of the api endpoints
	 * @param apiDescriptions list of descriptions of the api endpoints
	 * @param params parameters passed through the request
	 * @return the HTML page in f{@link String} format.
	 */
	public String getHelp(List<String> apiNames, List<String> apiDescriptions, Json params) {

		String s="";
		String api="";
		if (params.has("api")) {
			api=params.get("api").asString();
		}
		for (int i=0; i<apiNames.size();i++) {
			if (!api.isEmpty()) {
				if (api.compareTo(apiNames.get(i))==0) 	{
					
					s=s+getHelp(apiNames.get(i), apiDescriptions.get(i), params);
				}
			}
			else {
				if (!params.has("all"))params.set("all", "");
				s=s+getHelp(apiNames.get(i), apiDescriptions.get(i), params);
			}
			
		}

		
			s=generatePage("<table class=\"helptable\">"+s+"</table>",params);
		return s;

	}

	/**
	 * Method that builds the HTML page from the api data.
	 * @param sbody body of the HTML page
	 * @param params parameters passed through the request
	 * @return the HTML page in f{@link String} format.
	 */
	private String generatePage(String sbody, Json params) {

		String ns="<!DOCTYPE html>\n" + 
				"<html>\n" + 
				"<head>\n" + 
				"<style>\n" +
				getStyle(params)+

				"</style>\n" + 
				"</head>\n" + 
				"<body>\n" + 
				sbody+
				"</body>\n" + 
				"</html>\n" + 
				"";
		return ns;
	}

	/**
	 * Method that builds the part of an API endpoint.
	 * @param apiName name of the api endpoint
	 * @param apiDescription description of the api endpoint
	 * @param params parameters passed through the request
	 * @return the HTML page in f{@link String} format.
	 */
	public String getHelp(String apiName, String apiDescription, Json params) {

		String styletr=" style =\"width:100 mm\" ";
		String size="3";
		if (params.has("status")) size="4";
				
		
		boolean showExample=params.has("ex");
		
		String s="";
		s="<tr width=\"100mm\"><td colspan=\""+size+"\" class=\"apiname\">"+apiName+"</td></tr>";
		s+="<tr width=\"100mm\"><td colspan=\""+size+"\" class=\"apidescription\">"+apiDescription+"</td></tr>";

		if (params.has("api")&&params.has("cmd")&params.has("url")) {
			
			String urlMain=params.get("url").asString()+"?ex";
			if (params.has("print")) urlMain=urlMain+"&print";
			if (!params.has("all")) urlMain=urlMain+"&api="+params.get("api").asString();
			
			s=s+"<tr><td colspan=\""+size+"\" >"+
					"<a href=\""+urlMain+" \" target=\"_blank\" >main page</a>"+"</td></tr>";

		}
		
		
		Json j= getHelpInfoAsJson(apiName,  params);

		String cmd="";
		if (params.has("cmd")) {
			cmd=params.get("cmd").asString();
		}
		
		if (!j.isArray()) {
			if (j.has("error")) {
				s=s+"<tr width=\"100mm\"><td colspan=\"\"+size+\"\" >"+"Not available</td></tr>";
			}
			else {
				s = s + "<tr><td colspan=\"\"+size+\"\" >" + "Not available</td></tr>";
			}
		}
		else {

			s=s+"<tr " +styletr+">" + 
					"    <th >Cmd</th>\n" + 
					//"    <th>Description</th>\n" + 
					"    <th>Params</th>\n" + 
					"    <th>Result</th>\n" ;
			if (params.has("status")) s=s+"    <th>Status</th>\n"; 
			
			s=s+		"  </tr>";
			
			for (Json jrow:j.asJsonList()) {
				
				boolean okcmd=true;
				boolean full=false;
				String url="";
				String jcmd=jrow.get("cmd").asString();
				if (params.has("api")&&params.has("cmd")) {
					full=true;
				}
				else {
					if (params.has("url")) url=params.get("url").asString();
					url=url+"?api="+apiName+"&cmd="+jcmd+"&ex";
					if (params.has("all")) url=url+"&all";
					if (params.has("print")) url=url+"&print";
				}
				if (!cmd.isEmpty()) okcmd=cmd.compareTo(jcmd)==0;
				
				String row="";
				//System.out.println(jrow);
				row=row+"<td><b>"+jrow.get("cmd").asString()+"</b><br><i>"+jrow.get("desc").asString()+ "</td>";
				//row=row+"<td>"+jrow.get("desc").asString()+"</td>";
				if (jrow.has("params")) {
					String sx="";
					
					//for (Json jp:jrow.get("params").asJsonList()) {
					for (Entry<String, Json> entry : jrow.get("params").asJsonMap().entrySet()) {
						sx=sx+"&bull; "+entry.getKey()+" : "+""+entry.getValue().asString()+"<br>";
					}
					row=row+"<td>"+sx+"</td>";
					
				}
				else {
					row=row+"<td> </td>";
				}
				if (jrow.has("result")) {
					row=row+"<td>"+jrow.get("result").asString()+"</td>";
				}
				else {
					row=row+"<td> </td>";
				}
				if (jrow.has("status")) {
					row=row+"<td>"+jrow.get("status").asString()+"</td>";
				}
				
				if (okcmd)	s=s+"<tr>"+row+"</tr>";
			
				if (showExample&okcmd) {
					row="";
					String exstyle="style=\" font-family: 'Courier New', monospace; font-size: small; \"";
					String ex_params="";
					if (jrow.has("ex_params")) ex_params=formatJson(jrow.get("ex_params"), true, url); 
					String ex_result="";
					if (jrow.has("ex_result")) ex_result=formatJson(jrow.get("ex_result"), full,url);
					
					boolean empty= ex_params.isEmpty()&&ex_params.isEmpty();
					
					if (empty) {
						row=row+"<td style=\"text-align: right; font-size: small;\"> </td>";
								
					}
					else {
						row=row+"<td style=\"text-align: right; font-size: small;\"><i>Example </td>";
					}
					row=row+"<td colspan=\"1 \" "+exstyle+">"+ex_params+"</td>";
					row=row+"<td colspan=\"1 \" "+exstyle+">"+ex_result+"</td>";
					
					if (jrow.has("status")) {
						row=row+"<td>"+"</td>";
					}
					s=s+"<tr>"+row+"</tr>";
					
				}
			}
		}
		return s;
	}

	private String formatJson(Json j, boolean full, String url) {
		
		boolean addlink=false;
		String s=JsonUtil.prettyPrint(j);
		
		String z="";
		String[] lines=s.split("\n");
		for (int i=0;i<lines.length;i++) {
			if (!full) {
				if (i<8) z=z+lines[i]+"<br>"; else addlink=true;
			}
			else
				z=z+lines[i]+"<br>";
		}
		if (addlink) z=z+"<a href=\""+url+" \" target=\"_blank\" >More ...</a>";
		return z;
	}

	private Json getHelpInfoAsJson(String apiName, Json jparams) {
		//Json jparams= Json.object();
		jparams.set("user", "user1");
		//jparams.set("op", "LST_DEVICES");

		Json r=JServiceLoader.getCSLInterModuleCommunicationManager().executeCommand(apiName, Json.object().set("cmd", "help").set("params", jparams));
		//System.out.println(JsonUtil.prettyPrint(r));

		return r;
	}

	/**
	 * Creates the style with a width depending on the parameters of the request.
	 * @param params parameters passed through the request
	 * @return the style in {@link String} format.
	 */
	private String getStyle(Json params) {
		return getStyle(params.has("print"));
	}

	/**
	 * Creates the style for the apihelp endpoint HTML webpage.
	 * @param fixedWidth if the width is fixed or not
	 * @return the style in format {@link String}.
	 */
	private String  getStyle(boolean fixedWidth) {
	
		String sw="width: 100%;";
		if (fixedWidth) sw="width:170mm; table-layout:fixed";
		
		String style2=
			".helptable {\n" + 
			"  font-family: Arial, Helvetica, sans-serif; "+ //font-size: 80%;\n" + 
			"  border-collapse: collapse;\n" + 
			//"  width: 100%;\n" + 
			sw+
			"}\n"+
			"\n" + 
			".helptable td, .helptable th {\n" + 
			"  border: 1px solid #ddd;\n" + 
			"  padding: 8px;vertical-align: top;" + 
			"}\n" + 
			"\n" + 
			".helptable tr:nth-child(odd){background-color: #f2f2f2;}\n" + 
			"\n" + 
			".helptable tr:hover {background-color: #ddd;}\n" +
			".helptable td.apiname {\n" +
			"  border: 1px solid #ddd;\n" +
			"  border-top: 6px solid #000;\n" +
			"  padding: 8px;\n" +
			"  background-color: #90D26D;\n" +
			"  font-size: 150%; text-align:center;"+
			"}\n" +
			""+
			".helptable td.apidescription {\n" +
			"  border: 1px solid #ddd;\n" +
			"  padding: 8px;\n" +
			"  background-color: #D9EDBF;\n" +
			"  font-size: 100%; text-align:center;"+
			"}\n" +
			""+
			".helptable th {\n" + 
			"  padding-top: 12px;\n" + 
			"  padding-bottom: 12px;\n" + 
			"  text-align: left;\n" + 
			"  background-color:  #2C7865;\n" +
			"  color: white;\n" + 
			"}";
	
	
		return style2;
	}
	
}
