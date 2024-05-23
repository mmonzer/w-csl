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






public class CSLDemo20OpModelIDSModifs {

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

	//get_devices

	public Json exec(String op, Json params) {

		params.set("op", op);

		Json result = execCmd("op_model_ids", params);

		return result;
	}





	public void testDevices() {

		CSLDemo20OpModelIDSModifs opManager = this;

		Json r=opManager.exec("test", Json.object());
		System.out.println(r);


	String ip="10.0.208.215";
		
		String name1="computer #112";
		r=opManager.exec("add_device", Json.object().set("ip", ip).set("name", name1).set("risk",0));
		System.out.println(r);

		
		r=opManager.exec("set_device_ip", Json.object().set("ip", ip).set("new_ip","1.1.1.1"));
		
		System.out.println(r);
		r=opManager.exec("get_devices", Json.object());
		System.out.println(r);
		
		r=opManager.exec("get_device", Json.object().set("ip", ip));
		System.out.println(r);
		
		r=opManager.exec("get_device", Json.object().set("name", name1));
		System.out.println(r);
		
		r=opManager.exec("get_device", Json.object().set("ip", "1.1.1.1"));
		System.out.println(r);
		
		r=opManager.exec("del_device", Json.object().set("ip", "1.1.1.1"));
		System.out.println(r);
		
		
		r=opManager.exec("add_device", Json.object().set("ip", "10.0.208.100").set("name", "essai").set("risk", 3));
		r=opManager.exec("get_devices", Json.object());
		System.out.println(r);
			
		
		String ip2="10.0.208.100";
		r=opManager.exec("add_device", Json.object().set("ip", "10.0.208.213"));
		System.out.println(r);
		
		
		r=opManager.exec("add_device", Json.object().set("ip", "10.0.208.100").set("name", "essai").set("risk", 3));
		
		
		
		r=opManager.exec("get_device", Json.object().set("ip", "10.0.208.213"));
		System.out.println(r);
		
		r=opManager.exec("update_device", Json.object().set("ip", "10.0.208.213").set("name", "xx").set("mac","ere").set("key","ip").set("risk",3));
		
		
				
		//OP to get the flux
				
		
		
		r=opManager.exec("get_device", Json.object().set("ip", "10.0.208.213"));
		System.out.println(r);
		
			
		r=opManager.exec("set_address_variable", Json.object().set("name", "$TEST").set("value","192.1.1.3"));
		System.out.println(r);
		r=opManager.exec("get_address_variables", Json.object());
		System.out.println(r);
		r=opManager.exec("get_address_variable", Json.object().set("name", "$TEST"));
		System.out.println(r);
			
		
		
		Json dev=Json.object();
		dev.set("name", "testxxx");
		dev.set("ip", "1.2.3.4");
		Json jlinks=Json.array();
		Json jl=Json.object();
		jl.set("ip_dst", "1.1.11.1");
		jl.set("port_dst",80);
		jlinks.add(jl);
		
		jl=Json.object();
		jl.set("ip_dst", "1.1.11.121");
		jl.set("port_dst",773);
		jl.set("port_src",102);
		jl.set("risk", 4);
		
		jlinks.add(jl);
		dev.set("links", jlinks);
		
		Json devs=Json.array();
		devs.add(dev);
		
		
		r=opManager.exec("add_authorized_links", Json.object().set("authorized_links", devs));
		
		
		
		r=opManager.exec("get_devices_and_links", Json.object());
		System.out.println(JsonUtil.prettyPrint(r));
		
		
		
		r=opManager.exec("generate_rules", Json.object().set("options",Json.object().set("msg", "CSL rule")));
		
		System.out.println(r);
		


	}


	public void test() {
		testDevices();

	}


	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CSLDemo20OpModelIDSModifs runner= new CSLDemo20OpModelIDSModifs();
		runner.test();

	}

}
