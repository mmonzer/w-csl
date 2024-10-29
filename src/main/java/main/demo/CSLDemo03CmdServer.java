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






public class CSLDemo03CmdServer {
	
	public static String getServerURL() {
		return "http://localhost:8000/";
	}

	public static Json execCmd(String cmd, Json jparams) {
		Json j= Json.object();
	
		j.set(JCmd.CMD, cmd);
		j.set(JCmd.PARAMETERS,jparams);

		HttpPost post = new HttpPost(getServerURL()+"exec_jsoncmd");
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
			
			// e.printStackTrace();
		}

		return Json.object();
	}

	
	public static void testCmd() {
		System.out.println("TEST EXEC_CMD");
		Json jparams= Json.object();
		jparams.set("x",1);
		jparams.set("info", "test");

		Json result = execCmd("test_cmd", jparams);
		
		System.out.println("Result ="+result);
	}

	public static void main(String[] args) {
		testCmd();
	}

}
