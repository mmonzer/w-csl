package com.csl.util;
import java.util.Map;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

public class RulesUtil {


	/*
	 * 

{

ip :”x.x.x.x”,
name:””  (optionnel
links : [

{
 "protocol":"TCP",
 "app_protocol":"",
 "ip_dst":"10.0.208.16",
 "port_dst":8000,
 “permission”:”allowed”, “forbidden”, “allowed_with_alert_low”, “allowed_with_alert_high”
}


	 * 
	 */


	public Json generateTestData() {
		
		
		Json listDevices= Json.array();
		
		Json device=Json.object();
		listDevices.add(device);
		
		device.set("ip","1.1.12.1" );
		device.set("name","computer1" );
		
		
		Json links= Json.array();
		device.set("links",links);
		
		Json link=Json.object();
		links.add(link);
		
		link.set("protocol","TCP");
		link.set("app_protocol","modbus");
		
		link.set("ip_dst","10.0.208.16");		
		link.set("port_dst","502");
		link.set("permission","allowed" );  // default
		
		link=Json.object();
		links.add(link);
		
		link.set("protocol","TCP");
		link.set("app_protocol","modbus");
		
		link.set("ip_dst","10.0.208.18");		
		link.set("port_dst","502");
		link.set("permission","forbidden" );  // default
	
		
		
		return listDevices;
		
	}

	public String port2str(Json j,String name) {
		return port2str(j, name, false);
	}

	public String port2str(Json j,String name, Boolean extraSpace) {
		String space = "";
		if (extraSpace) {
			space = " ";
		}
		int p= JsonUtil.getIntFromJson(j,name, -1);
		if (p==-1) return space + "any" + space;
		return space + p + space;
	}
	
	
	public int permissionToRisk(String p) {
		
		String s=p.toLowerCase();
		if (s.compareTo("allowed")==0) return 0;
		if (s.compareTo("allowed_with_alert_low")==0) return 2;
		if (s.compareTo("allowed_with_alert_high")==0) return 3;
		if (s.compareTo("forbidden")==0) return 4;
			
		
		return 4;
	}
	
	public String getRiskLitreal(int i) {
		
		if (i<0) return "NONE";
		else if (i==1) return "TOLERABLE";
		else if (i==2) return "MODERATE";
		else if (i==3) return "HIGH";
		else return "CRITICAL";
		
	
	}
	
	// SURICATA GENERATION
	public Json toSuricataRules(Json devices,Json options) {
		String DBL_QT = "\"",
				MSG = "msg:",
				PASS = "PASS ",
				ALERT = "ALERT ",
				TCP = "TCP",
				UDP = "UDP",
				ANY = "any",
				ANY_ANY = "any any",
				SEMI_COL = ";",
				SID = "sid:",
				SPACE = " ";

		String lines = "";

		String msg_pass = JsonUtil.getStringFromJson(options, "msg_pass", "CSL pass");
		String msg_alert = JsonUtil.getStringFromJson(options, "msg_alert", "CSL Alert");
		String sid = JsonUtil.getStringFromJson(options, "sid", "9000099");
		String rev = JsonUtil.getStringFromJson(options, "rev", "0.1");
		long sidLong = Long.parseLong(sid);;


		String xtraOptions = "";

		for (Map.Entry<String, Json> e : options.asJsonMap().entrySet()) {
			boolean skip = false;
			if (e.getKey().compareToIgnoreCase("msg_pass") == 0) skip = true;
			else if (e.getKey().compareToIgnoreCase("msg_alert") == 0) skip = true;
			else if (e.getKey().compareToIgnoreCase("sid") == 0) skip = true;

			if (!skip) {
				if (!xtraOptions.isEmpty()) xtraOptions = xtraOptions + ";";
				String value = "";
				if (e.getValue().isString()) value = e.getValue().asString();
				else value = e.getValue().toString();

				xtraOptions = xtraOptions + e.getKey().toString() + ":" + value;
			}
		}

		if (xtraOptions.length() > 0) {
			xtraOptions = SPACE + xtraOptions + SEMI_COL;
		}


		String sFirst = "# Example rules for using the file handling and extraction functionality in Suricata" + "\n";

		Json j = Json.array();

		for (Json device : devices.asJsonList()) {

			String name = "#invalid descriptor";


			String ip_src = JsonUtil.getStringFromJson(device, "ip", "x.x.x.x");
			Json jjList = device.get("links");

			for (Json jj : jjList.asJsonList()) {

				Json jRule = Json.object();

				String ruleStr = "";

				String permission = JsonUtil.getStringFromJson(jj, "permission", "");
				int risk = permissionToRisk(permission);

				if (risk <= 0) {	// no risk
					ruleStr = PASS + jj.get("protocol").asString() + SPACE + ip_src + SPACE
							+ port2str(device, "port_src") + " -> " + jj.get("ip_dst").asString() + SPACE
							+ port2str(jj, "port_dst") + " (" + MSG + DBL_QT + msg_pass + DBL_QT + SEMI_COL + xtraOptions + SPACE
							+ SID + Long.toString(sidLong++) + SEMI_COL +
							")";
				} else {
					ruleStr = ALERT + jj.get("protocol").asString() + SPACE + ip_src + SPACE
							+ port2str(device, "port_src") + " -> " + jj.get("ip_dst").asString() + SPACE
							+ port2str(jj, "port_dst") + " (" + MSG + DBL_QT + msg_alert + DBL_QT + SPACE + getRiskLitreal(risk) + SEMI_COL + xtraOptions + SPACE
							+ SID + Long.toString(sidLong++) + SEMI_COL +
							")";
				}

				jRule.set("rule", ruleStr);

				j.add(ruleStr);

			}

			Json jRule = Json.object();

			String s = ALERT + TCP + SPACE + ip_src + SPACE + ANY + " -> " + ANY_ANY
					+ " (" + MSG + DBL_QT + msg_alert + DBL_QT + SEMI_COL + xtraOptions + SPACE
					+ SID + Long.toString(sidLong++) + SEMI_COL +
					")";

			jRule.set("rule", s);
			j.add(s);


			s = ALERT + UDP + SPACE + ip_src + SPACE + ANY + " -> " + ANY_ANY
					+ " (" + MSG + DBL_QT + msg_alert + DBL_QT + SEMI_COL + xtraOptions + SPACE
					+ SID + Long.toString(sidLong++) + SEMI_COL +
					")";

			jRule.set("rule", s);
			j.add(s);


			if (!lines.isEmpty()) lines = lines + "\n";

			lines = lines + s;

		}

		lines = sFirst + lines;

		lines = lines + "# " + "\n";

		// alert other traffic UDP and TCP
		String s = ALERT + UDP + SPACE + ANY_ANY + " -> " + ANY_ANY
				+ " (" + MSG + DBL_QT + msg_alert + DBL_QT + SEMI_COL + xtraOptions + SPACE
				+ SID + Long.toString(sidLong++) + SEMI_COL +
				")";

		j.add(s);

		s = ALERT + TCP + SPACE + ANY_ANY + " -> " + ANY_ANY
				+ " (" + MSG + DBL_QT + msg_alert + DBL_QT + SEMI_COL + xtraOptions + SPACE
				+ SID + Long.toString(sidLong++) + SEMI_COL +
				")";

		j.add(s);

		return j;

	}



	public static void main(String[] args) {

		
		RulesUtil z = new RulesUtil();
		
		Json devices = z.generateTestData();
		
		System.out.println(JsonUtil.prettyPrint(devices));
		
		
		Json options=Json.object();
		
		Json rules = z.toSuricataRules(devices, options);
		
		System.out.println(JsonUtil.prettyPrint(rules));
		
	}

}
