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

public class CSLDemo08TapsManager {

	public static final String TAP_28 = "tap28";
	public static final String TAP_15 = "tap15";
	public static final String TAP_27 = "tap27";

	public static String getServerURL() {
		return "http://localhost:8000/";
	}

	public static Json execCmd(String operation,String idname, Json jparams) {


		Json j= Json.object();
	
		j.set(JCmd.CMD, "opTaps");
		
		jparams.set("operation",operation);
		jparams.set("idname",idname);
			
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
			Json j2=Json.read(result);
			return j2;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return Json.object();
	}

	
	public static void main(String[] args) {
		Json params=Json.object();
		execCmd("add", TAP_28, Json.object());
		System.out.println(execCmd("list", TAP_28, Json.object()));
		System.out.println(execCmd("load", TAP_28, Json.object().set("type",  5)));
		System.out.println(execCmd("save", TAP_28, Json.object().set("type",  5).set("text","Essai 5")));
		
		
		System.out.println(execCmd("load", TAP_28, Json.object().set("type",  5)));
		
		
		System.out.println(execCmd("del", TAP_15, Json.object().set("type",  5)));
		
		System.out.println(execCmd("add", TAP_27, Json.object().set("type",  5)));
		for (int  i=3;i<7; i++)
			System.out.println(execCmd("save", TAP_27, Json.object().set("type",  i).set("text","Essai "+i)));
			
		System.out.println(execCmd("rename", TAP_27, Json.object().set("new_idname", TAP_15)));
		System.out.println(execCmd("update", TAP_15, Json.object().set("ip",  "192.168.0.200")));
		System.out.println(execCmd("update", TAP_15, Json.object().set("username",  "user").set("password",  "pass")));
	
		System.out.println(execCmd("list", TAP_28, Json.object()));
		
		System.out.println(execCmd("del", TAP_15, Json.object().set("type",  5)));
		
		
		System.exit(0);
	}

}
