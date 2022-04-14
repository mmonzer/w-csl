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

import com.csl.alert.AlertDescriptor;
import com.csl.core.CSLContext;
import com.xcsl.alert.AlertInfoFromIDS;
import com.xcsl.interfaces.IAlertDescriptor;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

import main.util.CSLRunningArgs;
import main.xcom.WebsocketClientEndpoint;






public class CSLDemo09Alerts {

	
	private static final String OBJECT_NAME = "devices_2";


	static String uuid = UUID.randomUUID().toString();


	static Json testObject=null;
	
	static public String getServerURL() {
		return "http://localhost:8000/";
	}


	
	
	
	static public Json execCmd(String cmd, Json jparams) {


		Json j= Json.object();
	
		j.set("cmd", cmd);
		j.set("params",jparams);

		HttpPost post = new HttpPost(getServerURL()+"exec_jsoncmd");
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
	                    	//testObject=readObjectFromDatabase(OBJECT_NAME);
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
		
		return  execCmd("devicesDBOperation", jparams);
		
		//System.out.println("Result ="+result);
		
	//	System.out.println("\n\n\n");
	}
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	
		
		CSLContext.instance.init(new CSLRunningArgs().parseArgs(args));
		
		//CSLContext.context.setDataDir(CSLRunningArgs.instance.getDataDir());
		
		Json j =CSLContext.instance.getConfig();


		j.at("xx");
				
		CSLContext.instance.setDebug(true);
		CSLContext.instance.setTestMode(CSLContext.instance.getIdsParams().isTestMode());
		
		//CSLContext.instance.init();
		
		//IDSRunner idsRunner= CSLC
		
		//IDSRunner.instance.getIdsParams().initFromJson(j, CSLRunningArgs.instance.getworkingDir(),CSLRunningArgs.instance.getDataDir());
		
		CSLContext.instance.getCslUDPServer().start();
		CSLContext.instance.getCslUDPServer().start();

		//===================
		
		IAlertDescriptor a=CSLContext.instance.getIDSMainProcessor().getAlertFactory()
				.createAlertDescriptor(5, "ALERT", System.currentTimeMillis());
		a.setProp("p1", "34");
		CSLContext.instance.getCSLAlertManager().sendAlert(a);
		
		
		AlertInfoFromIDS ai= AlertInfoFromIDS.UNRECOGNIZED_IP; //(999, new SeverityLevel(3),"New machine detected");
		ai.setIP("1.1.1.1");
	//	ai.setIP2("2.2.2.2");
		ai.setMac("mac1");
		
		IAlertDescriptor a2= ai.getAlertDescriptor(CSLContext.instance.getIDSMainProcessor().getAlertFactory());
		CSLContext.instance.getCSLAlertManager().sendAlert(a2);
				
		AlertInfoFromIDS ai3= AlertInfoFromIDS.INVALID_COMMUNICATION; //(999, new SeverityLevel(3),"Incorrect communication");
		ai3.setIP("3.1.1.1");
		ai3.setIP2("3.2.2.2");
		ai3.setMac("mac3");
		ai3.setAddr(34);
		ai3.setValue(12);
		
		IAlertDescriptor a3= ai3.getAlertDescriptor(CSLContext.instance.getIDSMainProcessor().getAlertFactory());
		
		CSLContext.instance.getCSLAlertManager().sendAlert(a3);
	
		Json j1=a.toJson();
		
		System.out.println(JsonUtil.prettyPrint(j1));
		System.out.println(JsonUtil.prettyPrint(new AlertDescriptor(j1).toJson()));
		
		
		System.out.println(
				JsonUtil.prettyPrint(a2.toJson()));
		System.out.println(JsonUtil.prettyPrint(new AlertDescriptor(a2.toJson()).toJson()));
		
		System.out.println(
				JsonUtil.prettyPrint(a3.toJson()));
		System.out.println(JsonUtil.prettyPrint(new AlertDescriptor(a3.toJson()).toJson()));
		
		
		CSLContext.instance.getCSLAlertManager().saveListOfCurrentAlerts( );
		
		//System.out.println(CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir());
		CSLContext.instance.getCSLAlertManager().resetListOfCurrentAlerts();
		
		System.exit(0);
	}

}
