package main.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.csl.util.JCmd;
import com.csl.web.HTTPConstants;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.ucsl.json.Json;

public class CSLDemo10XApiCommand {

	public static final String RESULT = "Result =";
	String token ="";
	
	public String getServerURL() {
		return "http://localhost:8000/";
	}


	
	public Json doLogin(String user,String pass) {

		
		Json j= Json.object();
		
		j.set("username", user);
		j.set("password",pass);

		HttpPost post = new HttpPost(getServerURL()+"auth/login");
		
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
	
	
	



	public Json execCmd(String api,String cmd, Json jparams) {


		Json j= Json.object();
	
		j.set(JCmd.CMD, cmd);
		j.set(JCmd.PARAMETERS,jparams);

		HttpPost post = new HttpPost(getServerURL()+api);
		
		post.addHeader("Authorization", "Bearer "+token);
		
		
		int timeout = 5; // seconds
		
		RequestConfig config = RequestConfig.custom()
				  .setConnectTimeout(timeout * 1000)
				  .setConnectionRequestTimeout(timeout * 1000)
				  .setSocketTimeout(timeout * 1000).build();
				//CloseableHttpClient client = 
				//  HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		
		HttpClient  client    = HttpClientBuilder.create().setDefaultRequestConfig(config).build(); //HttpClientBuilder.create().build();
		
			
		
		
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
			System.out.println("Result:"+result);
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
		jparams.set("op","LST_DEVICES");
		jparams.set("info", "test");
		Json jUser= Json.object();
		jUser.set("name", "operator#1");
		jparams.set("user", jUser);
				
		
		Json result = execCmd("devdb", "op", jparams);
		
		System.out.println(RESULT +result);
		
	//	System.out.println("\n\n\n");
	}
	
	
	
	public void testCmd2() {
		
		
		System.out.println("TEST EXEC_CMD");
		Json jparams= Json.object();
		jparams.set("op","test");
		jparams.set("info", "test");
		Json jUser= Json.object();
		jUser.set("name", "operator#1");
		jparams.set("user", jUser);
				
		
		Json result = execCmd("ids", "op_alert", jparams);
		
		System.out.println(RESULT +result);
		
	//	System.out.println("\n\n\n");
	}
	
	public void testLogin() {
		
		
		System.out.println("TEST LOGIN");
		
		
		Json result = doLogin("user123", "123456");
		
		if (result.has("token")) {
			this.token=result.get("token").asString();
		}
		else {
			token="";
		}
		System.out.println(RESULT +result);
		
		
		
	//	System.out.println("\n\n\n");
	}
	

	public static void main(String[] args) {
		//testCmd();
	
		CSLDemo10XApiCommand runner = new CSLDemo10XApiCommand();
		
		runner.testLogin();
		
		runner.testCmd();
		runner.testCmd2();
	}

}
