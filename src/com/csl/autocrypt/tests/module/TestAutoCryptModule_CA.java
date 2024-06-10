package com.csl.autocrypt.tests.module;

import com.csl.core.CSLContext;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.ucsl.json.Json;
import main.services.AutoCryptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.csl.intercom.jsoncmd.JServiceLoader.getUserDir;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAutoCryptModule_CA {

    // API module
    private static final int PORT_MODULE = 8082; // Change this to your actual base URL
    private static final String BASE_URL_MODULE = "http://localhost:" + PORT_MODULE; // Change this to your actual base URL
    private static final String ENDPOINT_MODULE = "/api";
    // API client
    private static final int PORT_CLIENT = 9900; // Change this to your actual base URL
    private static final String BASE_URL_CLIENT = "http://localhost:" + PORT_CLIENT; // Change this to your actual base URL
    private static final String ENDPOINT_CLIENT = "/autocrypt";

    private WireMockServer wireMockServer;

    private AutoCryptService service;
    private static final Json configObj = CSLContext.instance.getConfig();

    @BeforeEach
    public void setUp() {
        // Mock the module
        wireMockServer = new WireMockServer(PORT_MODULE);
        WireMock.configureFor("localhost", PORT_MODULE);
        wireMockServer.start();
        // This ensures that we don't touch the DB
        service = new AutoCryptService();
        service.init(configObj.get(service.getConfigFileSectionName()), getUserDir());
        service. getManager().getMethods().setSaveToDb(false);
    }

    @AfterEach
    public void tearDown() {
        // Stop the WireMock server
        wireMockServer.stop();
    }

    // Generate root (POST)

    @Test
    public void testGenerateRoot_withPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String commonName = "commonName";
        String ttl = "24h";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json expectedBody = Json.object();
        expectedBody.at("common_name", commonName);
        expectedBody.at("ttl", ttl);
        Json returnOutput = Json.object();
        returnOutput.at("common_name", commonName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/ca/generate-root"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("common_name",(StringValuePattern) new EqualToPattern(commonName))
                .withQueryParam("ttl",(StringValuePattern) new EqualToPattern(ttl))
                .withQueryParam("path",(StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);

        Json response = service.generateRootCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateRoot_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String commonName = "commonName";
        String ttl = "24h";

        Json expectedBody = Json.object();
        expectedBody.at("common_name", commonName);
        expectedBody.at("ttl", ttl);
        Json returnOutput = Json.object();
        returnOutput.at("common_name", commonName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/ca/generate-root"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("common_name",(StringValuePattern) new EqualToPattern(commonName))
                .withQueryParam("ttl",(StringValuePattern) new EqualToPattern(ttl))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_root_ca");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);

        Json response = service.generateRootCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateRoot_withoutCommonName() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String ttl = "24h";

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("ttl", ttl);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_root_ca");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "common_name is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateRootCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateRoot_withoutTTL() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String commonName = "commonName";

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_root_ca");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "ttl is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateRootCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // Generate intermediate (POST)

    @Test
    public void testGenerateIntermediate_withPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String commonName = "commonName";
        String ttl = "24h";
        String type = "type";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json expectedBody = Json.object();
        expectedBody.at("common_name", commonName);
        expectedBody.at("ttl", ttl);
        expectedBody.at("type", type);
        Json returnOutput = Json.object();
        returnOutput.at("common_name", commonName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/ca/generate-intermediate"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path",(StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern(expectedBody.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);
        sentParams.at("type", type);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_inter_ca");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);

        Json response = service.generateIntermediateCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateIntermediate_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String commonName = "commonName";
        String ttl = "24h";
        String type = "type";

        Json expectedBody = Json.object();
        expectedBody.at("common_name", commonName);
        expectedBody.at("ttl", ttl);
        expectedBody.at("type", type);
        Json returnOutput = Json.object();
        returnOutput.at("common_name", commonName);

        // Define mocked service behavior
        // should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);
        sentParams.at("type", type);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_inter_ca");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateIntermediateCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateIntermediate_withoutCommonName() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String ttl = "24h";
        String type = "type";

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("ttl", ttl);
        sentParams.at("type", type);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_inter_ca");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "common_name is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateIntermediateCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateIntermediate_withoutTTL() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String type = "type";
        String commonName = "commonName";

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        sentParams.at("type", type);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_inter_ca");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "ttl is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateIntermediateCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateIntermediate_withoutType() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String ttl = "24h";
        String commonName = "commonName";

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_inter_ca");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "type is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateIntermediateCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }
}
