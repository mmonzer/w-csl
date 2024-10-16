package com.csl.autocrypt.tests.module;

import com.csl.autocrypt.tests.TestConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

import static com.csl.intercom.jsoncmd.JServiceLoader.getUserDir;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.ucsl.json.Json;
import main.services.AutoCryptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAutoCryptModule_Roles extends TestConfig {


    @BeforeEach
    public void setUp() {
        // Mock the module
        wireMockServer = new WireMockServer(PORT_MODULE);
        WireMock.configureFor(configObj.get("auto_crypt").get("ip").asString(), PORT_MODULE);
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

    // Get roles  (GET)

    @Test
    public void testGetRole_normalUse() throws Exception {
        // Define expected input/output of the mocked module
        Json returnOutput = mockGetRole(path, name);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("name", name);
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGetRole_normalUse_extraParams() throws Exception {
        // Define expected input/output of the mocked module
        String name = "dummy";
        String path = "/dev/null";

        Json returnOutput = mockGetRole(path, name);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("name", name);
        sentParams.at("cmd", "extra");
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGetRole_woPath() throws Exception {
        // Define expected input/output of the mocked module
        String name = "dummy";
        String path = "/dev/null";

        mockGetRole(path, name);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.getRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGetRole_woName() throws Exception {
        // Define expected input/output of the mocked module
        String name = "dummy";
        String path = "/dev/null";

        mockGetRole(path, name);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        Json response = service.getRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    /**
     * Mocks the getRole endpoint
     */
    private static Json mockGetRole(String path, String name) {
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("key1", "key1");
        returnOutput.at("key2", "key2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/role/" + name))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                //.withRequestBody(equalToJson(expectedInput.toString(), true, true))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);
        return returnOutput;
    }

    // list roles (GET)

    @Test
    public void testListRoles_normalUseWithPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/role"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_roles");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getRoles(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testListRoles_normalUseWithoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        Json expectedInput = Json.object();
        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/role"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_roles");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getRoles(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testListRoles_normalUseWithOtherParams() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        Json expectedInput = Json.object();
        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/role"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("extra", "param");
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_roles");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getRoles(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // create roles (POST)

    @Test
    public void testCreateRole() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String name = "dummy";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", issuerRef);
        // Define mocked service
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/role"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "create_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.createRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testCreateRole_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String name = "dummy";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", issuerRef);

        // Define mocked service
        // not needed, should not arrive to module

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "create_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.createRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testCreateRole_withoutName() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String name = "dummy";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", issuerRef);

        // Define mocked service
        // not needed, should not arrive to module

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "create_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        Json response = service.createRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // update roles (PUT)

    @Test
    public void testUpdateRole() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", issuerRef);
        returnOutput.at("ttl", ttl);
        // Define mocked service
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/role/"+name))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.updateRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testUpdateRole_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", issuerRef);
        returnOutput.at("ttl", ttl);

        // Define mocked service
        // not needed, should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.updateRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testUpdateRole_withoutName() throws Exception {
        // Define expected input/output of the mocked module
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", issuerRef);
        returnOutput.at("ttl", ttl);
        // Define mocked service
        // not needed, should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        Json response = service.updateRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testUpdateRole_withoutDbapiId() throws Exception {
        // Define expected input/output of the mocked module
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", issuerRef);
        returnOutput.at("ttl", ttl);
        // Define mocked service
        // not needed, should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "id is missing from body");
        recvOutput.at("error", error);

        Json response = service.updateRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // delete roles (DELETE)

    @Test
    public void testDeleteRole() throws Exception {
        // Define expected input/output of the mocked module
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", issuerRef);
        returnOutput.at("ttl", ttl);
        // Define mocked service
        MappingBuilder x = delete(urlPathMatching(ENDPOINT_MODULE + "/role/"+name))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "delete_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);

        Json response = service.deleteRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testDeleteRole_withoutPath() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", issuerRef);
        returnOutput.at("ttl", ttl);
        // Define mocked service
        // not needed, should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "delete_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.deleteRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testDeleteRole_withoutName() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", issuerRef);
        returnOutput.at("ttl", ttl);
        // Define mocked service
        // not needed, should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "delete_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        Json response = service.deleteRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testDeleteRole_withoutDbapiId() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", issuerRef);
        returnOutput.at("ttl", ttl);
        // Define mocked service
        // not needed, should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "delete_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "id is missing from body");
        recvOutput.at("error", error);

        Json response = service.deleteRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }
}
