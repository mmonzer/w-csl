package main.help;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;






public class HelpGenerator {

	String API_DBJSON="dbjson";

	String URL_BACKEND="http://localhost:8000/";

	String token ="";
	String uuid = UUID.randomUUID().toString();

	String urlServer=URL_BACKEND;

	Json testObject=null;

	
	String apiName="";
	Json examplesForApi=null;
	
	boolean replaceExisting=true;
	
	
	
	
	
	public HelpGenerator(String apiName, String ip, int port) {
		
		this.apiName=apiName;
		this.URL_BACKEND="http:://"+ip+":"+port+"/";
		
	}
	
	
	
	public boolean isReplaceExisting() {
		return replaceExisting;
	}

	public void setReplaceExisting(boolean replaceExisting) {
		this.replaceExisting = replaceExisting;
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

		j.set("cmd", cmd);
		j.set("params",jparams);

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
			//System.out.println("Result:"+result);
			Json j2=Json.read(result);
			return j2;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return Json.object();
	}








//	public void listenDatabase() {
//		try {
//			String s= "ws://" + "127.0.0.1" + ":" + "7999" + "/database";
//
//			final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(s)); 
//
//			// add listener
//			clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
//				public void handleMessage(String message) {
//					Json j=Json.read(message);
//					System.out.println("Database:"+j);
//					if (j.get("database")==null) return;
//					j=j.get("database");
//					String m_uuid="";
//					if (j.get("uuid")!=null) {
//						m_uuid=j.get("uuid").asString();
//						System.out.println("modifier uuid="+m_uuid);
//						if (uuid.compareTo(m_uuid)==0) return ; // this update is from this process
//					}
////					if (j.get("name").asString().compareTo(OBJECT_NAME)==0) {
////						testObject=readObjectFromDatabase(OBJECT_NAME);
////						System.out.println("***** Updated to "+testObject);
////					}
//				}
//			});
//
//			Thread.sleep(100);
//
//
//		} catch (InterruptedException ex) {
//			System.err.println("InterruptedException exception: " + ex.getMessage());
//		} catch (URISyntaxException ex) {
//			System.err.println("URISyntaxException exception: " + ex.getMessage());
//		}
//	}


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



//	public void updateData() {
//
//		System.out.println("TEST UPDATE TO DB");
//
//		if (testObject==null) testObject=Json.object();
//
//		if (testObject.get("ctr")==null) {
//			testObject.set("ctr",0);
//		}
//		int ctr=testObject.get("ctr").asInteger();
//		testObject.set("ctr",ctr+1);
//
//
//		writeObjectToDatabase(OBJECT_NAME,testObject);
//
//		System.out.println("New value send to database:"+testObject);
//		//System.out.println("\n\n\n");
//	}

	public void testdirJsonFile() {

		System.out.println("TEST DIR");

		System.out.println(getListOfObjects());
		//System.out.println("\n\n\n");
	}


	private void saveHelp(String apiname, Json examples) {

		
		writeObjectToDatabase("helpex_"+apiname,examples);
		
	}

	
	private Json addExampleToList(Json examples, String cmd, Json params, Json result ) {
		
		if (!isReplaceExisting()) examples=removeExampleFromList(examples, cmd);
		
		Json jrow= Json.object();
		
		jrow.set("cmd", cmd);
		jrow.set("params", params);
		//Json result= execCmd(apiname, cmd, jparams);
		jrow.set("result",result);
		
		examples.add(jrow);
		
		
		return examples;
	}
	
	private Json findExampleInList(Json examples, String cmd) {
		
		for (Json j:examples) {
			if (j.has("cmd")) {
				String c=JsonUtil.getStringFromJson(j,"cmd","");
				if (cmd.compareTo(c)==0) return j;
			}
		}
		
		return null;
	}
	
	
	private Json removeExampleFromList(Json examples, String cmd) {
		
		Json new_list= Json.array();
		for (Json j:examples) {
			if (j.has("cmd")) {
				String c=JsonUtil.getStringFromJson(j,"cmd","");
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
