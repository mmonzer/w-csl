package com.csl.autocrypt.tests;

import com.csl.core.CSLContext;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.ucsl.json.Json;
import main.services.AutoCryptService;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.csl.intercom.jsoncmd.JServiceLoader.getUserDir;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAutoCryptService {

    // API module
    protected static final int PORT_MODULE = 8082; // Change this to your actual base URL
    protected static final String IP_MODULE = "localhost"; // Change this to your actual base URL
    protected static final String BASE_URL_MODULE = "http://" + IP_MODULE + ":" + PORT_MODULE; // Change this to your actual base URL
    protected static final String ENDPOINT_MODULE = "/api";
    // API client
    protected static final int PORT_CLIENT = 9900; // Change this to your actual base URL
    protected static final String BASE_URL_CLIENT = "http://localhost:" + PORT_CLIENT; // Change this to your actual base URL
    protected static final String ENDPOINT_CLIENT = "/autocrypt";

    protected WireMockServer wireMockServer;

    @BeforeEach
    public void setUp() {
        wireMockServer = new WireMockServer(PORT_MODULE);
        WireMock.configureFor(IP_MODULE, PORT_MODULE);
        wireMockServer.start();
    }

    @AfterEach
    public void tearDown() {
        // Stop the WireMock server
        wireMockServer.stop();
    }

    @Test
    public void testSelfMethod() throws Exception {
        String expectedOutput = "{\"success\":true,\"result\":{\"status\":\"OK\"}}";
        String returnedOutput = "{\"status\":\"OK\"}";
        String inputJson = "{ \"cmd\" : \"command_to_change\", \"params\" : { \"param1\" : \"val1\" } }";

        // Define mocked service
        MappingBuilder x = post(urlEqualTo(ENDPOINT_MODULE))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withRequestBody(equalToJson(inputJson, true, true))   // TODO : match the body
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnedOutput)
                );
        stubFor(x);

        // Define request to th mocked service
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        Request request = httpClient.newRequest(BASE_URL_MODULE + ENDPOINT_MODULE);
        request.method(HttpMethod.POST);
        request.content(new StringContentProvider(inputJson), "application/json");
        ContentResponse response = request.send();

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(returnedOutput, response.getContentAsString());
    }

    @Test
    public void testBDConnection() throws Exception {
        Json configObj = CSLContext.instance.getConfig();

        Json globalConfig = configObj.get("global");
        globalConfig.delAt("ip_server_remote");
        globalConfig.at("ip_server_remote", "localhost:8765");
        globalConfig.delAt("api_key");
        globalConfig.at("api_key", "");
        globalConfig.delAt("use_ssl");
        globalConfig.at("use_ssl", false);

        Json config = Json.object();
        config.at("ip", "localhost");
        config.at("port", 8083);
        config.at("global", globalConfig);

        AutoCryptService service = new AutoCryptService();
        service.init(config, getUserDir());
        service.getManager().getMethods().setSaveToDb(true);

        WireMockServer wireMockServer1 = new WireMockServer(8083);
        WireMock.configureFor("localhost", 8083);
        wireMockServer1.start();

        WireMockServer wireMockServer2 = new WireMockServer(8765);
        WireMock.configureFor("localhost", 8765);
        wireMockServer2.start();


        String path = "/dev/null";
        String roleName = "root-role";
        String name = "name";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("role_name", roleName);

        // Define mocked AutoCrypt service
        MappingBuilder x = post(urlPathMatching("/api/certificate/issue"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"role_name\":\"" + roleName + "\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        wireMockServer1.stubFor(x);

        Json returnOutput2 = Json.object();
        returnOutput.at("role_name", roleName);
        returnOutput2.at("idDB", "id");
        // Define mocked bd service
        MappingBuilder y = post(urlPathMatching("/api/autocrypt/certificates"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                //.withRequestBody((StringValuePattern) new EqualToPattern(returnOutput.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput2.toString())
                );
        wireMockServer2.stubFor(y);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("role_name", roleName);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_certificate");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput2);

        Json response = service.generateCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testValidate() throws Exception {
        String expectedOutput = "{\"success\":true,\"result\":{\"status\":\"OK\"}}";
        String returnedOutput = "{\"status\":\"OK\"}";
        String inputJson = "{ \"cmd\" : \"validate_template\", \"params\" : { \"path\":\"pki\"," +
                "        \"issuer_ref\":\"bbae9fe3-e1f6-6732-5d99-9b1b45856847\"," +
                "        \"name\": \"abc\" } }";

        // Define request to th mocked service
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        Request request = httpClient.newRequest("http://localhost:9900/autocrypt");
        request.method(HttpMethod.POST);
        request.content(new StringContentProvider(inputJson), "application/json");
        ContentResponse response = request.send();

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(returnedOutput, response.getContentAsString());
    }

    // TODO: test changeIp and changePort
}
