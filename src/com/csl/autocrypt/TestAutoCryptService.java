package com.csl.autocrypt;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.ucsl.json.Json;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
//import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAutoCryptService {
    String expectedOutput = "{\"success\":true,\"result\":{\"status\":\"OK\"}}";
    String returnedOutput = "{\"status\":\"OK\"}";
    String inputJson = "{ \"cmd\" : \"command_to_change\", \"params\" : { \"param1\" : \"val1\" } }";

    // API module
    private static final String BASE_URL_MODULE = "http://localhost:8989"; // Change this to your actual base URL
    private static final String ENDPOINT_MODULE = "/config";
    // API client
    private static final String BASE_URL_CLIENT = "http://localhost:9900"; // Change this to your actual base URL
    private static final String ENDPOINT_CLIENT = "/autocrypt";

    private WireMockServer wireMockServer;

    @BeforeEach
    public void setUp() {
        wireMockServer = new WireMockServer(8989);
        WireMock.configureFor("localhost", 8989);
        wireMockServer.start();
    }

    @AfterEach
    public void tearDown() {
        // Stop the WireMock server
        wireMockServer.stop();
    }

    @Test
    public void testPostMethod() throws Exception {
        // Define mocked service
        MappingBuilder x = post(urlEqualTo(ENDPOINT_MODULE))
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
        Request request = httpClient.newRequest(BASE_URL_CLIENT+ENDPOINT_CLIENT);
        request.method(HttpMethod.POST);
        request.content(new StringContentProvider(inputJson), "application/json");
        ContentResponse response = request.send();

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(expectedOutput, response.getContentAsString());
    }

    @Test
    public void testSelfMethod() throws Exception {
        // Define mocked service
        MappingBuilder x = post(urlEqualTo(ENDPOINT_MODULE))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
//                .withRequestBody(equalToJson(inputJson, true, true))   // TODO : match the body
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnedOutput));
        stubFor(x);

        // Define request to th mocked service
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        Request request = httpClient.newRequest(BASE_URL_MODULE+ENDPOINT_MODULE);
        request.method(HttpMethod.POST);
        request.content(new StringContentProvider(inputJson), "application/json");
        ContentResponse response = request.send();

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(returnedOutput, response.getContentAsString());
    }
}
