package main.help;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.csl.util.JCmd;
import com.csl.web.HTTPConstants;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;






public class HelpGenerator {

	String API_DBJSON="dbjson";

	String URL_BACKEND="http://localhost:8000/";

	String token ="";
	String uuid = UUID.randomUUID().toString();

	String urlServer=URL_BACKEND;

	Json testObject=null;

	
	String apiName="";
	Json examplesForApi=null;
	
	@Setter
    @Getter
    boolean replaceExisting=true;

	public HelpGenerator(String apiName, String ip, int port) {
		this.apiName=apiName;
		this.URL_BACKEND="http:://"+ip+":"+port+"/";
		
	}
    public String getServerURL() {
		return urlServer;
	}

	public HelpGenerator setServerURL(String url) {
		this.urlServer=url;
		return this;
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

		HttpClient  client    = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

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

	public Json readObjectFromDatabase(String name) {
		Json p= Json.object();
		p.set("name", name);
	
		return execCmd(API_DBJSON,"load_jsonfile", p);
	
	}

	public Json writeObjectToDatabase(String filename, Json contents) {


		Json p= Json.object();
		p.set("name", filename);
		p.set("contents", contents);
		
		return execCmd(API_DBJSON,"save_jsonfile", p);

		
	}

	public Json getListOfObjects() {

		return execCmd(API_DBJSON,"dir_jsonfile", Json.object());
	}

	public void testdirJsonFile() {

		System.out.println("TEST DIR");

		System.out.println(getListOfObjects());
	}


	private void saveHelp(String apiname, Json examples) {

		
		writeObjectToDatabase("helpex_"+apiname,examples);
		
	}

	
	private Json addExampleToList(Json examples, String cmd, Json params, Json result ) {
		
		if (!isReplaceExisting()) examples=removeExampleFromList(examples, cmd);
		
		Json jrow= Json.object();
		
		jrow.set(JCmd.CMD, cmd);
		jrow.set(JCmd.PARAMETERS, params);
		jrow.set("result",result);
		
		examples.add(jrow);
		
		
		return examples;
	}
	
	private Json findExampleInList(Json examples, String cmd) {
		
		for (Json j:examples) {
			if (j.has(JCmd.CMD)) {
				String c=JsonUtil.getStringFromJson(j,JCmd.CMD,"");
				if (cmd.compareTo(c)==0) return j;
			}
		}
		
		return null;
	}
	
	
	private Json removeExampleFromList(Json examples, String cmd) {
		
		Json new_list= Json.array();
		for (Json j:examples) {
			if (j.has(JCmd.CMD)) {
				String c=JsonUtil.getStringFromJson(j,JCmd.CMD,"");
				if (cmd.compareTo(c)!=0) new_list.add(j);
			}
		}
		
		return new_list;
	}
	
	
	
	private Json loadHelp(String apiname) {

		Json jData=readObjectFromDatabase("helpex_"+apiname);
		
			
		if (jData.has("contents")&&!jData.has("error")) {
			return jData.get("contents");
		}
		return Json.array();   // not found
	}
	
	
	public Json addExample(String cmd, Json params ) {
		
		if (examplesForApi==null) {
			examplesForApi=loadHelp(apiName);
		}
		
		
		Json result= execCmd(apiName, cmd, params);
		
		examplesForApi=addExampleToList(examplesForApi, cmd, params, result);
		return result;
	}
	
	
	public HelpGenerator loadHelp() {
		
		loadHelp(apiName);
		return this;
	}
	
	public HelpGenerator saveHelp() {
		
		saveHelp(apiName, examplesForApi);
		return this;
	}

}
