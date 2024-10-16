package main.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.csl.util.JCmd;
import com.csl.web.HTTPConstants;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;






public class CSLDemo21OpModelIDSTestOnePacket {

	public static String getServerURL() {
		return "http://localhost:8000/";
	}

	public Json execCmd(String cmd, Json jparams) {


		Json j= Json.object();
	
		j.set(JCmd.CMD, cmd);
		j.set(JCmd.PARAMETERS,jparams);

		HttpPost post = new HttpPost(getServerURL()+"ids");
		HttpClient  client    = HttpClientBuilder.create().build();
		StringEntity postingString = new StringEntity(j.toString(),StandardCharsets.UTF_8);
		post.setEntity(postingString);
		post.setHeader(HTTPConstants.CONTENT_TYPE, HTTPConstants.JSON_FORMAT);
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
		
		Json r=exec("test_packet", jl);
		System.out.println(JsonUtil.prettyPrint(r));
		
	}

	
	
	public void test() {
		testPacket();
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		CSLDemo21OpModelIDSTestOnePacket runner= new CSLDemo21OpModelIDSTestOnePacket();
		runner.test();
	}

}
