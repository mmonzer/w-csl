package main.util;


import static com.ucsl.util.FileUtils.readFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.ucsl.json.Json;


public class DBDjango {
	static String CONFIGURATION_FILE = "configuration.json";
	static String API_KEY = "";
	static String BASE_URL = "http://localhost:8005/";
	static String DEVICE_ENDPOINT = "api/devices";
	static int PORT = 4445;

	// A utility method to convert the byte array
	// data into a string representation.
	public static StringBuilder data(byte[] a) {
		if (a == null)
			return null;
		StringBuilder ret = new StringBuilder();
		int i = 0;
		while (a[i] != 0) {
			ret.append((char) a[i]);
			i++;
		}
		return ret;
	}


	public static void createDevice(Json jDevice) {

		System.out.println("Create Device:" + jDevice);
		DBDjango al = new DBDjango();
		
		al.doPost(DEVICE_ENDPOINT, jDevice, API_KEY);
		
	}


	public String getServerURL() {
		return BASE_URL;
	}

	public Json doPost(String api, Json jbody, String token) {


		Json j = jbody;


		HttpPost post = new HttpPost(getServerURL() + api);

		post.addHeader("Authorization", "Api-Key " + token);


		int timeout = 5; // seconds

		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000)
				.setSocketTimeout(timeout * 1000).build();

		HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build(); //HttpClientBuilder.create().build();


		StringEntity postingString = new StringEntity(j.toString(), StandardCharsets.UTF_8);
		post.setEntity(postingString);
		post.setHeader("Content-type", "application/json");
		try {
			HttpResponse response = client.execute(post);

			BufferedReader in = new BufferedReader(new InputStreamReader(response
					.getEntity().getContent()));

			StringBuffer sb = new StringBuffer();
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();

			String result = sb.toString();
			System.out.println("Result:" + result);
			Json j2 = Json.read(result);
			return j2;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return Json.object();
	}

	
	public Json doPut(String api, Json jbody, String token) {


		Json j = jbody;


		HttpPut post = new HttpPut(getServerURL() + api);

		post.addHeader("Authorization", "Api-Key " + token);


		int timeout = 5; // seconds

		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000)
				.setSocketTimeout(timeout * 1000).build();

		HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build(); //HttpClientBuilder.create().build();


		StringEntity postingString = new StringEntity(j.toString(), StandardCharsets.UTF_8);
		post.setEntity(postingString);
		post.setHeader("Content-type", "application/json");
		try {
			HttpResponse response = client.execute(post);

			BufferedReader in = new BufferedReader(new InputStreamReader(response
					.getEntity().getContent()));

			StringBuffer sb = new StringBuffer();
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();

			String result = sb.toString();
			System.out.println("Result:" + result);
			Json j2 = Json.read(result);
			return j2;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return Json.object();
	}

	public static void test1() {


		DBDjango al = new DBDjango();
		


		Json body = Json.object();
		body.set("cmd", "test_alert0");
		body.set("params", Json.object());


		System.out.println(al.doPost(DEVICE_ENDPOINT, body, API_KEY));
	}


	private static Json readJsonFile(String f) {
		String content="{}";
		try {
			content = readFile(f);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Cannot read config file :" + f);
		}
		Json jConfig=Json.read(content);
		return jConfig;
	}

	private static boolean readConfiguration() {
		Json configObj = readJsonFile(CONFIGURATION_FILE);
		if (configObj != null) {
			API_KEY = (String) configObj.get("apikey").getValue();
			BASE_URL = (String) configObj.get("baseurl").getValue();
			if (API_KEY.length() > 0 && BASE_URL.length() > 0) {
				System.out.println("apikey: " + API_KEY);
				System.out.println("baseurl: " + BASE_URL);
				return true;
			}
		}
		return false;
	}

}
