package main.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;






public class CSLDemo22OpModelIDSTestAlerts {

	static public String getServerURL() {
		return "http://localhost:8000/";
	}

	public Json execCmd(String api,String cmd, Json jparams) {

		Json j= Json.object();
	
		j.set("cmd", cmd);
		j.set("params",jparams);

		HttpPost post = new HttpPost(getServerURL()+api);
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
	
	//get_devices
	
	public Json exec(String op, Json params) {
		
		params.set("op", op);
		
		Json result = execCmd("ids","op_model_ids", params);
		
		return result;
	}

	
	
	public void testPacket() {
		

		Json jl=Json.object();
		jl.set("time",System.currentTimeMillis());
		
		jl.set("tap_id", "tap01");
		
		
		jl.set("ip_src","10.0.208.15");
		jl.set("mac_src","00:0c:29:2f:a0:1a");
		jl.set("port_src",22);
		
		jl.set("ip_dst","10.0.208.18");
		jl.set("mac_dst","00:0c:29:86:e2:e6");
		jl.set("port_dst",8000);
		
		
		System.out.println("Process packet:"+jl);
		Json r=exec("test_packet", jl);
		
	}

	public void testAlerts() {
		
		
		Json r=exec("reset_learned_model_to_last_learned", Json.object());
		r=exec("clear_all_alerts", Json.object());
		
		
		r=exec("get_devices_and_links", Json.object());
		//System.out.println(JsonUtil.prettyPrint(r));
		
		UtilDemo.printModel(r);
		
		execCmd("alerts","clear_list_of_all_alerts",Json.object());
		
		System.out.println("==== INITIAL TEST ====");		
		testPacket();
		System.out.println("====");
		
		
		Json list=execCmd("alerts","get_alerts_list",Json.object());
		
		
		for (Json a:list.asJsonList()) {
			
			System.out.println(a);
			
			Json params=Json.object();
			
			params.set("alert", a);
			
			 r=exec("add_alert_to_model", params);
		}
		
		
		 r=exec("get_devices_and_links", Json.object());
		
		UtilDemo.printModel(r);
		
		r=exec("clear_all_alerts", Json.object());
		execCmd("alerts","clear_list_of_all_alerts",Json.object());

		System.out.println("");
		System.out.println("TEST PACKET WITH NEWMODEL WITH ALERTS");
		
		testPacket();
		Json list2=execCmd("alerts","get_alerts_list",Json.object());
		for (Json a:list2.asJsonList()) {
			System.out.println(a);
			}
		
		System.out.println();
		

		System.out.println("REMOVE ALERTS");
		for (Json a:list.asJsonList()) {
			
			Json params=Json.object();
			
			params.set("alert", a);
			
			 r=exec("remove_alert_from_model", params);
		}
		
		
		r=exec("get_devices_and_links", Json.object());
		UtilDemo.printModel(r);
		
		execCmd("alerts","clear_list_of_all_alerts",Json.object());
		
		System.out.println("TEST PACKET WITH MODEL AFTER REMOVE");
		
		testPacket();
		 list2=execCmd("alerts","get_alerts_list",Json.object());
		for (Json a:list2.asJsonList()) {
			System.out.println(a);
			}
		System.out.println();
	}

	
	public void test() {
		testAlerts();
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CSLDemo22OpModelIDSTestAlerts runner= new CSLDemo22OpModelIDSTestAlerts();
		runner.test();
		
	}

}
