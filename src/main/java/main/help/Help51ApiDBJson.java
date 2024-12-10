package main.help;

import com.csl.util.JCmd;
import com.csl.web.HTTPConstants;
import com.ucsl.json.Json;
import com.csl.web.WebsocketClientEndpoint;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Help51ApiDBJson {

    String apiName = "dbjson";

    String api = "dbjson";
    private static final String OBJECT_NAME = "obj.drx";

    String token = "";
    String uuid = UUID.randomUUID().toString();

    Json testObject = null;

    public Help51ApiDBJson(String apiname) {
        this.apiName = apiname;
    }

    public static String getServerURL() {
        return "http://localhost:8000/";
    }

    public Json execCmd(String api, String cmd, Json jparams) {

        Json j = Json.object();

        j.set(JCmd.CMD, cmd);
        j.set(JCmd.PARAMETERS, jparams);

        HttpPost post = new HttpPost(getServerURL() + api);

        post.addHeader("Authorization", "Bearer " + token);

        int timeout = 5; // seconds

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();

        HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

        StringEntity postingString = new StringEntity(j.toString(), StandardCharsets.UTF_8);
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

            String result = sb.toString();
            System.out.println("Result:" + result);
            Json j2 = Json.read(result);
            return j2;
        } catch (IOException e) {
            
            // e.printStackTrace();
        }

        return Json.object();
    }

    public void listenDatabase() {
        try {
            String s = "ws://" + "127.0.0.1" + ":" + "7999" + "/database";

            final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(s));

            // add listener
            clientEndPoint.setMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
                public void handleMessage(String message) {
                    Json j = Json.read(message);
                    System.out.println("Database:" + j);
                    if (j.get("database") == null) return;
                    j = j.get("database");
                    String m_uuid = "";
                    if (j.get("uuid") != null) {
                        m_uuid = j.get("uuid").asString();
                        System.out.println("modifier uuid=" + m_uuid);
                        if (uuid.compareTo(m_uuid) == 0) return; // this update is from this process
                    }
                    if (j.get("name").asString().compareTo(OBJECT_NAME) == 0) {
                        testObject = readObjectFromDatabase(OBJECT_NAME);
                        System.out.println("***** Updated to " + testObject);
                    }
                }
            });

            Thread.sleep(100);
        } catch (InterruptedException ex) {
            System.err.println("InterruptedException exception: " + ex.getMessage());
        } catch (URISyntaxException ex) {
            System.err.println("URISyntaxException exception: " + ex.getMessage());
        }
    }

    public Json readObjectFromDatabase(String name) {
        Json p = Json.object();
        p.set("name", name);

        return execCmd(api, "load_jsonfile", p);
    }

    public Json writeObjectToDatabase(String filename, Json contents) {


        Json p = Json.object();
        p.set("name", filename);
        p.set("contents", contents);

        return execCmd(api, "save_jsonfile", p);
    }

    public Json getListOfObjects() {

        return execCmd(apiName, "dir_jsonfile", Json.object());
    }

    public void updateData() {

        System.out.println("TEST UPDATE TO DB");

        if (testObject == null) testObject = Json.object();

        if (testObject.get("ctr") == null) {
            testObject.set("ctr", 0);
        }
        int ctr = testObject.get("ctr").asInteger();
        testObject.set("ctr", ctr + 1);


        writeObjectToDatabase(OBJECT_NAME, testObject);

        System.out.println("New value send to database:" + testObject);
    }

    public void testdirJsonFile() {

        System.out.println("TEST DIR");

        System.out.println(getListOfObjects());
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Help51ApiDBJson z = new Help51ApiDBJson("xx");
    }
}
