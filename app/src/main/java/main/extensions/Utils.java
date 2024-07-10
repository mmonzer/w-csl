package main.extensions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.csl.intercom.jsoncmd.JServiceLoader;
import com.ucsl.json.Json;

public class Utils {
	static public String getServerURL() {
		return "http://localhost:8000/";
	}
	private static Json execCmd(String api,String cmd, Json jparams) {


		Json j= Json.object();

		j.set("cmd", cmd);
		j.set("params",jparams);
		return JServiceLoader.cslInterModuleCommunicationManager.executeCommand(api, j);
	}


	/**
	 * Ecrit le contenu de contents dans le fichier filename. 
	 * uuid est un identifiant qui permet d'identifier qui a fait l'écriture.
	 * @param filename Le chemin vers le fichier à écrire
	 * @param contents le contenu à écrire
	 * @param uuid l'UUID a utiliser pour l'écriture
	 * @return un Json vide
	 */
	static public Json writeObjectToDatabase(String filename, Json contents, String uuid) {
		Json p= Json.object();
		p.set("name", filename);
		p.set("contents", contents);

		String api="dbjson";
		Json j= execCmd(api,"save_jsonfile", p);
		if (j.has("contents")) j=j.get("contents");
		return j;
	}
	
	/**
	 * Lis le contenu d'un fichier et le renvoie sous forme de Json
	 * @param name Le chemin vers le fichier à lire
	 * @return le contenu du fichier
	 */
	static public Json readObjectFromDatabase(String name) {
		Json p= Json.object();
		p.set("name", name);
	
		String api="dbjson";
		Json j= execCmd(api,"load_jsonfile", p);
		if (j.has("contents")) j=j.get("contents");
		return j;
	}
	
	static public Json execCmd(String cmd, Json jparams) {


		Json j= Json.object();
		System.out.println("params : "+jparams);
		j.set("cmd", cmd);
		j.set("params",jparams);

		HttpPost post = new HttpPost(getServerURL()+"devdb");
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
			Json j2=Json.read(result);
			return j2;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return Json.object();
	}
	static public void setDeviceProp(String ip,String path, Json value) {
		
		System.out.println("SET_DEVICE_PROP");
		
		Json jparams= Json.object();
		jparams.set("op","SET_DEVICE_PROP");
		jparams.set("user",Json.object().set("name","admin"));
		jparams.set("id", ip);
		jparams.set("path",path);
		jparams.set("value", value);
		
		
		Json result = execCmd("op", jparams);
		
		System.out.println("Result ="+result);

	}
	static public void addDevice(String ip, Json params) {
		
		System.out.println("ADD_DEVICE");
		Json jparams= Json.object();
		jparams.set("op","ADD_DEVICE");
		jparams.set("user",Json.object().set("name","admin"));
		jparams.set("id", ip);
		jparams.set("value", params);
		Json result = execCmd("op", jparams);
		System.out.println("Result ="+result);
		
	}
	static public Json listDevices() {
		
		
		System.out.println("TEST EXEC_CMD");
		Json jparams= Json.object();
		jparams.set("op","LST_DEVICES");
		jparams.set("user",Json.object().set("name","admin"));
		
		return  execCmd("op", jparams);
	}
	/**
	 * Génère un identifiant unique lors de l'appel et le renvoie sous forme de String.
	 * @return Une string contenant l'UUID
	 */
	static public String generateUUID() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}
	
	/**
	 * Met a jour le contenu de graph.json en fonction des informations contenues dans devices.json
	 */
	static public void updateGraphFromDevices() {
		Json graph = Utils.readObjectFromDatabase("graph");
		Json devices = Utils.readObjectFromDatabase("devices");
		ArrayList<Json> nodes = (ArrayList<Json>) graph.get("nodes").asJsonList();
		ArrayList<Json> newNodes = (ArrayList<Json>) nodes.clone();

		for(Json currentDevice : devices.get("devices").asJsonList()) {
			for(Json currentNode : nodes) {
				if(currentDevice.get("id").asString().contentEquals(currentNode.get("id").asString())) {
					newNodes.remove(currentNode);
					currentNode.at("cslinfo",currentDevice);
					newNodes.add(currentNode);
				}
			}
		}
		graph.at("nodes",newNodes);
		Utils.writeObjectToDatabase("graph",graph,"graph");
	}
	
	/**
	 * Met à jour le contenu de devices.json en fonction des informations contenues dans graph.json
	 */
	static public void updateDevicesFromGraph() {
		Json graph = Utils.readObjectFromDatabase("graph");
		ArrayList<Json> nodes = (ArrayList<Json>) graph.get("nodes").asJsonList();
		Json result = Json.object();
		ArrayList<Json> newDevices = new ArrayList<Json>();

		for(Json currentNode : nodes) {
			newDevices.add(currentNode.get("cslinfo"));
		}
		
		result.at("devices",newDevices);
		Utils.writeObjectToDatabase("devices",result,"devices");
	}
}
