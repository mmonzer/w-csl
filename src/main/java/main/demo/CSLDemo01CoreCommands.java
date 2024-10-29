package main.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.csl.web.HTTPConstants;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.ucsl.json.Json;

import static com.csl.util.FileUtils.FILENAME;

public class CSLDemo01CoreCommands {


	public static String getServerURL() {
		return "http://localhost:8000/";
	}


	public static String doGet(String cmd) {



		HttpGet get = new HttpGet(getServerURL()+cmd);
		HttpClient  client    = HttpClientBuilder.create().build();

		try {
			HttpResponse response = client.execute(get);
			
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
			return result;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}

		

	}


	public static String doSave(String filename, String fileContents) {


		Json j= Json.object();
	
		j.set("contents",fileContents );
		j.set(FILENAME,filename );
		
		j.set("extra_info","more_info");

		HttpPost post = new HttpPost(getServerURL()+"setfile");
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
		return result;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}

		
	}
	
	public static void testCore1() {
		
		System.out.println("TEST GET TIME");
		
		System.out.println(doGet("/gettime"));
		System.out.println("\n\n\n");
	}

	
	public static void main(String[] args) {
			testCore1();
	}

}
