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

import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;






public class CSLDemo23OpModelIDSAddAuthorizedFlows {

	
	static String uuid = UUID.randomUUID().toString();


	Json testObject=null;
	
	static public String getServerURL() {
		return "http://localhost:8000/";
	}


	
	

	
	
	



	public Json execCmd(String cmd, Json jparams) {


		Json j= Json.object();
	
		j.set("cmd", cmd);
		j.set("params",jparams);

		HttpPost post = new HttpPost(getServerURL()+"ids");
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
			System.out.println("RESULT="+result);
			Json j2=Json.read(result);
			return j2;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return Json.object();
	}

	public void testCmd() {
		
		
		System.out.println("TEST EXEC_CMD");
		Json jparams= Json.object();
		jparams.set("x",1);
		jparams.set("info", "test");

		
		Json result = execCmd("op_model_ids", jparams);
		
		System.out.println("Result ="+result);
		
	//	System.out.println("\n\n\n");
	}
	
	//get_devices
	
	public Json exec(String op, Json params) {
		
		params.set("op", op);
		
		Json result = execCmd("op_model_ids", params);
		
		return result;
	}

	
	
	public void testAdd() {
		
		
		Json r=exec("reset_learned_model_to_last_learned", Json.object());
		r=exec("clear_all_alerts", Json.object());
		
		
		r=exec("get_devices_and_links", Json.object());
		//System.out.println(JsonUtil.prettyPrint(r));
		
		UtilDemo.printModel(r);
		
		
		
		Json dev=Json.object();
		dev.set("name", "testxxx");
		dev.set("ip", "1.2.3.7");
		Json jlinks=Json.array();
		Json jl=Json.object();
		jl.set("ip_dst", "1.1.11.1");
		jl.set("port_dst",80);
		jlinks.add(jl);
		
		jl=Json.object();
		jl.set("ip_dst", "1.1.11.121");
		jl.set("port_dst",773);
		jl.set("port_src",102);
		
		jlinks.add(jl);
		dev.set("links", jlinks);
		
		Json devs=Json.array();
		devs.add(dev);
		
		System.out.println(JsonUtil.prettyPrint(devs));
		
		
		r=exec("add_authorized_links", Json.object().set("authorized_links", devs));
		
		r=exec("get_devices_and_links", Json.object());
		UtilDemo.printModel(r);
		
		
	
		
	}


	
	
	public void test() {
		
		
		testAdd();
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		CSLDemo23OpModelIDSAddAuthorizedFlows runner= new CSLDemo23OpModelIDSAddAuthorizedFlows();
		
		runner.test();
	
		
		
	}

}
