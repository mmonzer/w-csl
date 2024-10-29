package autocrypt.integration;

import com.csl.util.JCmd;
import com.ucsl.json.Json;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.jupiter.api.Test;

import static autocrypt.OutilsForTesting.sendPostTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCA {

    // API module
    protected static final int PORT_MODULE = 8082; // Change this to your actual base URL
    protected static final String IP_MODULE = "localhost"; // Change this to your actual base URL
    protected static final String BASE_URL_MODULE = "http://" + IP_MODULE + ":" + PORT_MODULE; // Change this to your actual base URL
    protected static final String ENDPOINT_MODULE = "/api";
    // API client
    protected static final int PORT_CLIENT = 9900; // Change this to your actual base URL
    protected static final String BASE_URL_CLIENT = "http://localhost:" + PORT_CLIENT; // Change this to your actual base URL
    protected static final String ENDPOINT_CLIENT = "/autocrypt";

    //@Test
    public void testGenerateRoot() throws Exception {
        String expectedOutput = "{\"success\":true,\"result\":{\"status\":\"OK\"}}";
        String returnedOutput = "{\"status\":\"OK\"}";

        Json params = Json.object();
        params.at("path","pki");
        params.at("common_name","blabla");
        params.at("ttl","24h");
        params.at("name","abc");
        Json inputJson = Json.object();
        inputJson.at(JCmd.CMD, "generate_root_ca");
        inputJson.at(JCmd.PARAMETERS, params);

        // Define request to th mocked service
        ContentResponse response = sendPostTo(BASE_URL_CLIENT+ENDPOINT_CLIENT, inputJson);

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(returnedOutput, response.getContentAsString());
    }

    //@Test
    public void testGenerateIntermediate() throws Exception {
        String expectedOutput = "{\"success\":true,\"result\":{\"status\":\"OK\"}}";
        String returnedOutput = "{\"status\":\"OK\"}";

        Json params = Json.object();
        params.at("path","pki");
        params.at("common_name","blabla");
        params.at("ttl","24h");
        params.at("name","abc2");
        params.at("type","external");
        Json inputJson = Json.object();
        inputJson.at(JCmd.CMD, "generate_root_ca");
        inputJson.at(JCmd.PARAMETERS, params);

        // Define request to th mocked service
        ContentResponse response = sendPostTo(BASE_URL_CLIENT+ENDPOINT_CLIENT, inputJson);

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(returnedOutput, response.getContentAsString());
    }
}
