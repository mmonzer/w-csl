package com.csl.autocrypt.tests;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.ucsl.json.Json;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.csl.autocrypt.tests.OutilsForTesting.sendPostTo;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAutoCryptService {

    // API module
    protected static final int PORT_MODULE = 8082; // Change this to your actual base URL
    protected static final String IP_MODULE = "localhost"; // Change this to your actual base URL
    protected static final String BASE_URL_MODULE = "http://"+IP_MODULE+":" + PORT_MODULE; // Change this to your actual base URL
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
    public void testPostMethod() throws Exception {
        String expectedOutput = "{\"success\":true,\"result\":{\"status\":\"OK\"}}";
        String returnedOutput = "{\"status\":\"OK\"}";
        String inputJson = "{ \"cmd\" : \"command_to_change\", \"params\" : { \"param1\" : \"val1\" } }";

        // Define mocked service
        MappingBuilder x = post(urlEqualTo("/config"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
//                .withRequestBody(equalToJson(inputJson, true, true))   // TODO : match the body
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnedOutput));
        stubFor(x);

        // Define request to CSL-Client
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        Request request = httpClient.newRequest(BASE_URL_CLIENT + ENDPOINT_CLIENT);
        request.method(HttpMethod.POST);
        request.content(new StringContentProvider(inputJson), "application/json");
        ContentResponse response = request.send();

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(expectedOutput, response.getContentAsString());
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

    // TODO: test changeIp and changePort
}
