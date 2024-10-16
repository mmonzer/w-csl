package com.csl.autocrypt.tests.module;

import com.csl.autocrypt.tests.TestConfig;
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
import static org.junit.jupiter.api.Assertions.fail;

public class TestAutoCryptModule_CA extends TestConfig {

    @BeforeEach
    public void setUp() {
        // Mock the module
        wireMockServer = new WireMockServer(PORT_MODULE);
        WireMock.configureFor(configObj.get(module).get("ip").asString(), PORT_MODULE);
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
    public void testGenerateRoot() throws Exception {
        // Define expected input/output of the mocked module
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json expectedBody = Json.object();
        expectedBody.at("common_name", commonName);
        expectedBody.at("ttl", ttl);
        expectedBody.at("type", type);
        Json returnOutput = Json.object();
        returnOutput.at("common_name", commonName);

        // Define mocked service behavior creation
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/ca/generate-root"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withRequestBody((StringValuePattern) new EqualToPattern(expectedBody.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"issuer_ref\":\"issuerRef\"}")
                );
        stubFor(x);

        // Define mocked service behavior get
        MappingBuilder y = get(urlPathMatching(ENDPOINT_MODULE + "/issuer/issuerRef"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path",(StringValuePattern) new EqualToPattern("pki"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"issuer_ref\":\""+issuerRef+"\"}")
                );
        stubFor(y);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);
        sentParams.at("type", type);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        Json result = Json.object();
        result.set("common_name", commonName);
        result.set("ttl", ttl);
        result.set("type", type);
        result.set("issuer_ref", issuerRef);
        result.set("path", "pki");
        result.set("ca_type", "root");
        recvOutput.at("result", result);

        Json response = service.generateRootCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateRoot_withoutCommonName() {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("ttl", ttl);
        sentParams.at("name", name);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_root_ca");
        sentInput.at("params", sentParams);

        String missingParam = "common_name";

        try {
            service.generateRootCA(sentParams);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(missingParam, e.getMessage());
        }

        // assert behavior
    }

    @Test
    public void testGenerateRoot_withoutTTL() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        sentParams.at("name", name);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_root_ca");
        sentInput.at("params", sentParams);

        String missingParam = "ttl";

        try {
            service.generateRootCA(sentParams);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(missingParam, e.getMessage());
        }
    }

    // Generate intermediate (POST)

    @Test
    public void testGenerateIntermediate() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", commonName);
        Json expectedBody = Json.object();
        expectedBody.at("common_name", commonName);
        expectedBody.at("ttl", ttl);
        expectedBody.at("type", type);
        Json returnOutput = Json.object();
        returnOutput.at("common_name", commonName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/ca/generate-intermediate"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path",(StringValuePattern) new EqualToPattern(commonName))
                .withRequestBody((StringValuePattern) new EqualToPattern(expectedBody.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"issuer_ref\":\""+issuerRef+"\"}")
                );
        stubFor(x);

        // Define mocked service behavior get
        MappingBuilder y = get(urlPathMatching(ENDPOINT_MODULE + "/issuer/issuerRef"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path",(StringValuePattern) new EqualToPattern(commonName))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"issuer_ref\":\"issuerRef\"}")
                );
        stubFor(y);


        // Define expected input/output of the api
        Json sentParams = Json.object();

        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);
        sentParams.at("type", type);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_inter_ca");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        Json result = Json.object();
        result.set("common_name", commonName);
        result.set("ttl", ttl);
        result.set("type", type);
        result.set("issuer_ref", issuerRef);
        result.set("path", commonName);
        result.set("ca_type", "intermediate");
        recvOutput.at("result", result);

        Json response = service.generateIntermediateCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateIntermediate_withoutCommonName() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();

        sentParams.at("path", path);
        sentParams.at("ttl", ttl);
        sentParams.at("name", name);
        sentParams.at("type", type);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_inter_ca");
        sentInput.at("params", sentParams);

        String missingParam = "common_name";

        try {
            service.generateIntermediateCA(sentParams);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(missingParam, e.getMessage());
        }
    }

    @Test
    public void testGenerateIntermediate_withoutTTL() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();

        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        sentParams.at("type", type);
        sentParams.at("name", name);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_inter_ca");
        sentInput.at("params", sentParams);

        String missingParam = "ttl";

        try {
            service.generateIntermediateCA(sentParams);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(missingParam, e.getMessage());
        }
    }

    @Test
    public void testGenerateIntermediate_withoutType() throws Exception {
        // Define expected input/output of the mocked module
        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();

        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);
        sentParams.at("name", name);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_inter_ca");
        sentInput.at("params", sentParams);

        String missingParam = "type";

        try {
            service.generateIntermediateCA(sentParams);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(missingParam, e.getMessage());
        }
    }
}
