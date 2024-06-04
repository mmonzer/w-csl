package com.csl.autocrypt.tests;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

import static com.csl.autocrypt.tests.OutilsForTesting.sendPostTo;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.ucsl.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.jetty.client.api.ContentResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAutoCryptService_Roles {

    // API module
    private static final int PORT_MODULE = 8989; // Change this to your actual base URL
    private static final String BASE_URL_MODULE = "http://localhost:" + PORT_MODULE; // Change this to your actual base URL
    private static final String ENDPOINT_MODULE = "/api";
    // API client
    private static final int PORT_CLIENT = 9900; // Change this to your actual base URL
    private static final String BASE_URL_CLIENT = "http://localhost:" + PORT_CLIENT; // Change this to your actual base URL
    private static final String ENDPOINT_CLIENT = "/autocrypt";

    private WireMockServer wireMockServer;

    @BeforeEach
    public void setUp() {
        wireMockServer = new WireMockServer(PORT_MODULE);
        WireMock.configureFor("localhost", PORT_MODULE);
        wireMockServer.start();
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
        String name = "dummy";
        String path = "/dev/null";

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

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
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

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
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

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
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

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
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

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
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

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
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

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
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
        returnOutput.at("issuer_ref", "str");
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
        sentParams.at("issuer_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "create_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
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
        returnOutput.at("issuer_ref", "str");

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
        sentParams.at("name", name);
        sentParams.at("issuer_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "create_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
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
        returnOutput.at("issuer_ref", "str");

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
        sentParams.at("issuer_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "create_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
    }

    // update roles (PUT)

    @Test
    public void testUpdateRole() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String name = "dummy";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", "default");
        returnOutput.at("ttl", "24h");
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
        sentParams.at("path", path);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
    }

    @Test
    public void testUpdateRole_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String name = "dummy";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", "default");
        returnOutput.at("ttl", "24h");
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
        sentParams.at("name", name);
        sentParams.at("issuer_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
    }

    @Test
    public void testUpdateRole_withoutName() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String name = "dummy";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", "default");
        returnOutput.at("ttl", "24h");
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
        sentParams.at("path", path);
        sentParams.at("issuer_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
    }

    // delete roles (DELETE)

    @Test
    public void testDeleteRole() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String name = "dummy";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", "default");
        returnOutput.at("ttl", "24h");
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
        sentParams.at("path", path);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "delete_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
    }

    @Test
    public void testDeleteRole_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String name = "dummy";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", "default");
        returnOutput.at("ttl", "24h");
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
        sentParams.at("name", name);
        sentParams.at("issuer_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "delete_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
    }

    @Test
    public void testDeleteRole_withoutName() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String name = "dummy";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", "default");
        returnOutput.at("ttl", "24h");
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
        sentParams.at("path", path);
        sentParams.at("issuer_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "delete_role");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        ContentResponse response = sendPostTo(BASE_URL_CLIENT + ENDPOINT_CLIENT, sentInput.toString());

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals(recvOutput.toString(), response.getContentAsString());
    }
}
