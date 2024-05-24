package main.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import com.csl.web.websockets.WebsocketClientEndpoint;

public class CSLDemo07DevicesDB {

	private static final String OBJECT_NAME = "devices_2";

	static String uuid = UUID.randomUUID().toString();
	
	static public String getServerURL() {
		return "http://localhost:8000/";
	}

	static public Json execCmd(String cmd, Json jparams) {

		Json j= Json.object();
	
		j.set("cmd", cmd);
		j.set("params",jparams);

		HttpPost post = new HttpPost(getServerURL()+"devdb");
		HttpClient  client    = HttpClientBuilder.create().build();
		StringEntity postingString = new StringEntity(j.toString(),StandardCharsets.UTF_8);
		post.setEntity(postingString);
		post.setHeader("Content-type", "application/json");
		try {
			HttpResponse response = client.execute(post);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(response
					.getEntity().getContent()));

			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();

			String  result = sb.toString();
			Json j2=Json.read(result);
			return j2;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return Json.object();
	}
	
	static public void listenDatabase() {
		 try {
	        	String s= "ws://" + "127.0.0.1" + ":" + "8000" + "/database";
	        	
	            final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(s)); 

	            // add listener
	            clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
	                public void handleMessage(String message) {
	                	Json j=Json.read(message);
	                    System.out.println("***** Database:"+j+" *****");
	                    if (j.get("database")==null) return;
	                    j=j.get("database");
	                    String m_uuid="";
	                    if (j.get("uuid")!=null) {
	                    	m_uuid=j.get("uuid").asString();
	                    	System.out.println("database modifier uuid="+m_uuid);
	                    	if (uuid.compareTo(m_uuid)==0) return ; // this update is from this process
	                    }
	                    if (j.get("name").asString().compareTo(OBJECT_NAME)==0) {
	                    	System.out.println("***** Updating "+OBJECT_NAME);
	                    }
	                }
	            });

	            Thread.sleep(100);


	        } catch (InterruptedException ex) {
	            System.err.println("InterruptedException exception: " + ex.getMessage());
	        } catch (URISyntaxException ex) {
	            System.err.println("URISyntaxException exception: " + ex.getMessage());
	        }
	}
	
	static public Json lstDevices() {
		
		
		System.out.println("TEST EXEC_CMD");
		Json jparams= Json.object();
		jparams.set("op","LST_DEVICES");
		jparams.set("user",Json.object().set("name","admin"));
		
		return  execCmd("op", jparams);
	}
	
	static public void clear() {
		Json jparams= Json.object();
		jparams.set("op","CLEAR");
		jparams.set("user",Json.object().set("name","admin"));
		
		
		
		Json result = execCmd("op", jparams);
	}
	static public void addDevice(String ip, Json params) {
		
		
		System.out.println("ADD_DEVICE");
		Json jparams= Json.object();
		jparams.set("op","ADD_DEVICE");
		jparams.set("user",Json.object().set("name","admin"));
		jparams.set("id", ip);
		jparams.set("value", params);
		
		
		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);
	}
	
	static public void delDevice(String ip) {
		
		
		System.out.println("DEL_DEVICE");
		Json jparams= Json.object();
		jparams.set("op","DEL_DEVICE");
		jparams.set("user",Json.object().set("name","admin"));
		jparams.set("id", ip);
		
		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);
	}
	
	
	static public void setDeviceProp(String ip,String path, Json value) {
		
		System.out.println("SET_DEVICE_PROP");
		
		Json jparams= Json.object();
		jparams.set("op","SET_DEVICE_PROP");
		jparams.set("user",Json.object().set("name","admin"));
		jparams.set("id", ip);
		jparams.set("path",path);
		jparams.set("value", value);
		
		
		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);
	}
	
	
	static public Json getDeviceProp(String ip,String path) {

		System.out.println("GET_DEVICE_PROP");
		
		Json jparams= Json.object();
		jparams.set("op","GET_DEVICE_PROP");
		jparams.set("user",Json.object().set("name","admin"));
		jparams.set("id", ip);
		jparams.set("path",path);
		
		
		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);
		
		return result;
	}
	
	static public void addLink(String ip, String ip2, Json params) {

		System.out.println("ADD_LINK");
		Json jparams= Json.object();
		jparams.set("op","ADD_LINK");
		jparams.set("user",Json.object().set("name","admin"));
		jparams.set("id", ip);
		jparams.set("id2", ip2);
		
		jparams.set("value", params);
		
		
		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);
	}

	static public Json getLink(String ip, String ip2) {

		System.out.println("GET_LINK");
		Json jparams= Json.object();
		jparams.set("op","GET_LINK");
		jparams.set("user",Json.object().set("name","admin"));
		jparams.set("id", ip);
		jparams.set("id2", ip2);
		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);
		return result;
	}
	
	
	static public Json getLinkPropsList(String ip, String ip2) {
		System.out.println("GET_LINK_PROPS_LIST");
		Json jparams= Json.object();
		jparams.set("op","GET_LINK_PROPS_LIST");
		jparams.set("user",Json.object().set("name","admin"));
		jparams.set("id", ip);
		jparams.set("id2", ip2);
		
		
		
		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);
		return result;
	}
	
	
	static public Json addLinkProps(String ip, String ip2, Json value) {
		
		String op="ADD_LINK_PROPS";
		
		System.out.println(op);
		Json jparams= Json.object();
		jparams.set("op",op);
		jparams.set("user",Json.object().set("name","admin"));
		jparams.set("id", ip);
		jparams.set("id2", ip2);
		jparams.set("value", value);
		
		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);
		return result;
	}
	
	static public Json setLinkProps(String ip, String ip2, String path, Json value) {
		
		String op="SET_LINK_PROPS";
		
		System.out.println(op);
		Json jparams= Json.object();
		jparams.set("op",op);
		jparams.set("user",Json.object().set("name","admin"));
		jparams.set("id", ip);
		jparams.set("id2", ip2);
		jparams.set("value", value);
		jparams.set("path", path);
		
		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);
		return result;
	}
	
	static public Json getLinkProps(String ip, String ip2, String path) {
		
		String op="GET_LINK_PROPS";
		
		System.out.println(op);
		Json jparams= Json.object();
		jparams.set("op",op);
		jparams.set("user",Json.object().set("name","admin"));
		jparams.set("id", ip);
		jparams.set("id2", ip2);
		jparams.set("path", path);
		
		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);
		return result;
	}
	
	static public Json saveViews( Json value) {
		
		String op="SET_NETWORK_VUES";
		
		System.out.println(op);
		Json jparams= Json.object();
		jparams.set("op",op);
		jparams.set("user",Json.object().set("name","admin"));
		
		jparams.set("value", value);
		
		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);
		return result;
	}
	
	
	static public Json loadViews( ) {
		
		String op="GET_NETWORK_VUES";
		
		System.out.println(op);
		Json jparams= Json.object();
		jparams.set("op",op);
		jparams.set("user",Json.object().set("name","admin"));

		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);
		return result;
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	
		listenDatabase();
		
		Json params=Json.object();
		clear();
		addDevice("2.2.2.2",params);
		Json macs=Json.array();macs.add("abcd").add("efg");
		Json p=Json.object().set("os", "linux");
		
		addDevice("2.2.2.3",params.set("macs",macs).set("props", p));
		
		delDevice("2.2.2.2");
		
		lstDevices();
		addDevice("2.2.2.4",Json.object());
		
		setDeviceProp("2.2.2.4", "macs",Json.make("mac1"));
		
		setDeviceProp("2.2.2.4", "props.vul.*",Json.make("vul1"));
		setDeviceProp("2.2.2.4", "props.os",Json.make("mac os"));
		
		System.out.println(getDeviceProp("2.2.2.4", "props"));
		System.out.println(getDeviceProp("2.2.2.4", "macs"));

		System.out.println(JsonUtil.prettyPrint(lstDevices()));
		
		Json props=Json.object().set("protocol","udp");
		addLink("2.2.2.3", "2.2.2.4", props);
		
		
		System.out.println(JsonUtil.prettyPrint(getLink("2.2.2.3", "2.2.2.4")));
		System.out.println(JsonUtil.prettyPrint(getLinkPropsList("2.2.2.3", "2.2.2.4")));
		
		props.set("protocol", "tcp");
		props.set("app_protocol", "modbus");
		
		addLinkProps("2.2.2.3", "2.2.2.4", props);
		System.out.println(JsonUtil.prettyPrint(getLinkPropsList("2.2.2.3", "2.2.2.4")));
		
		props.set("protocol", "tcp");
		props.set("app_protocol", "onvif");
	
		addLinkProps("2.2.2.3", "2.2.2.4", props);
		System.out.println(JsonUtil.prettyPrint(getLinkPropsList("2.2.2.3", "2.2.2.4")));

		props.set("protocol", "tcp");
		props.set("app_protocol", "mqtt");
	
		addLinkProps("2.2.2.3", "2.2.2.4", props);
		System.out.println(JsonUtil.prettyPrint(getLinkPropsList("2.2.2.3", "2.2.2.4")));
		props.set("protocol", "icmp");
		
		setLinkProps("2.2.2.3", "2.2.2.4","#0", props);
		System.out.println(JsonUtil.prettyPrint(getLinkPropsList("2.2.2.3", "2.2.2.4")));
		
		setLinkProps("2.2.2.3", "2.2.2.4","2.xx", Json.make("test"));
		System.out.println(JsonUtil.prettyPrint(getLinkPropsList("2.2.2.3", "2.2.2.4")));
	
		System.out.println(JsonUtil.prettyPrint(getLinkProps("2.2.2.3", "2.2.2.4","2")));
		
		Json jviews= Json.array();
		jviews.add(Json.object().set("test", "ok"));
		
		saveViews(jviews);
		System.out.print(loadViews());
		
		System.exit(0);
	}

}
