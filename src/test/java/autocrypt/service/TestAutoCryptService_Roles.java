package com.csl.autocrypt.tests.service;

import com.csl.autocrypt.tests.TestConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.ucsl.json.Json;
import main.services.AutoCryptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.csl.intercom.jsoncmd.JServiceLoader.getUserDir;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAutoCryptService_Roles extends TestConfig {

    @BeforeEach
    public void setUp() {
        // Mock the module
        wireMockServerModule = new WireMockServer(PORT_MODULE);
        wireMockServerModule.start();
        wireMockServerBd = new WireMockServer(PORT_DBAPI);
        wireMockServerBd.start();
        // This ensures that we touch the DB
        Json globalConfig = configObj.get("global");
        globalConfig.delAt("ip_server_remote");
        globalConfig.at("ip_server_remote", "localhost:" + PORT_DBAPI);
        globalConfig.delAt("api_key");
        globalConfig.at("api_key", "");
        globalConfig.delAt("use_ssl");
        globalConfig.at("use_ssl", false);

        Json config = Json.object();
        config.at("ip", configObj.get("auto_crypt").get("ip").asString());
        config.at("port", PORT_MODULE);
        config.at("global", globalConfig);

        service = new AutoCryptService();
        service.init(config, getUserDir());
        service.getManager().getMethods().setSaveToDb(true);
    }

    @AfterEach
    public void tearDown() {
        // Stop the WireMock server
        wireMockServerModule.stop();
        wireMockServerBd.stop();
    }

    // create roles (POST)

    @Test
    public void testCreateRole() throws Exception {
        // Define expected input/output of the mocked module
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();
        returnModule.at("name", name);
        returnModule.at("issuer_ref", issuerRef);
        // Define mocked service
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/role"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnModule.toString())
                );
        wireMockServerModule.stubFor(x);

        Json returnBd = Json.object();
        returnBd.at("id", id);
        for (Map.Entry<String, Object> e : returnModule.asMap().entrySet()) {
            returnBd.at(e.getKey(), e.getValue());
        }
        // Define mocked service behavior
        MappingBuilder y = post(urlPathMatching(ENDPOINT_DBAPI + "/vault_roles"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withRequestBody((StringValuePattern) new EqualToPattern(returnModule.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "create_role");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnBd);

        Json response = service.createRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testCreateRole_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();
        returnModule.at("name", name);
        returnModule.at("issuer_ref", issuerRef);

        // Define mocked service
        // should not be used

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "create_role");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();
        returnModule.at("name", name);
        returnModule.at("issuer_ref", issuerRef);

        // Define mocked service
        // should not be used

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "create_role");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        Json returnModule = Json.object();
        returnModule.at("name", name);
        returnModule.at("issuer_ref", "default");
        returnModule.at("ttl", "24h");
        // Define mocked service
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/role/" + name))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnModule.toString())
                );
        wireMockServerModule.stubFor(x);

        Json returnBd = Json.object();
        returnBd.at("id", id);
        for (Map.Entry<String, Object> e : returnModule.asMap().entrySet()) {
            returnBd.at(e.getKey(), e.getValue());
        }
        // Define mocked service behavior
        MappingBuilder y = put(urlPathMatching(ENDPOINT_DBAPI + "/vault_roles/"+id))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withRequestBody((StringValuePattern) new EqualToPattern(returnModule.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_role");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnBd);

        Json response = service.updateRole(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testUpdateRole_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();
        returnModule.at("name", name);
        returnModule.at("issuer_ref", "default");
        returnModule.at("ttl", "24h");
        // Define mocked service
        // should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_role");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        Json returnModule = Json.object();
        returnModule.at("name", name);
        returnModule.at("issuer_ref", "default");
        returnModule.at("ttl", "24h");
        // Define mocked service
        // should not arrive


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_role");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        Json returnModule = Json.object();
        returnModule.at("name", name);
        returnModule.at("issuer_ref", "default");
        returnModule.at("ttl", "24h");
        // Define mocked service
        // should not arrive


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_role");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        Json returnModule = Json.object();
        returnModule.at("name", name);
        returnModule.at("issuer_ref", issuerRef);
        returnModule.at("ttl", ttl);
        // Define mocked service
        MappingBuilder x = delete(urlPathMatching(ENDPOINT_MODULE + "/role/" + name))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                );
        wireMockServerModule.stubFor(x);

        Json returnBd = Json.object();
        returnBd.at("id", id);
        // Define mocked service behavior
        MappingBuilder y = delete(urlPathMatching(ENDPOINT_DBAPI + "/vault_roles/"+id))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                );
        wireMockServerBd.stubFor(y);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "delete_role");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        Json returnModule = Json.object();
        returnModule.at("name", name);
        returnModule.at("issuer_ref", "default");
        returnModule.at("ttl", "24h");
        // Define mocked service
        // should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "delete_role");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        Json returnModule = Json.object();
        returnModule.at("name", name);
        returnModule.at("issuer_ref", "default");
        returnModule.at("ttl", "24h");
        // Define mocked service
        // should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "delete_role");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        Json returnModule = Json.object();
        returnModule.at("name", name);
        returnModule.at("issuer_ref", "default");
        returnModule.at("ttl", "24h");
        // Define mocked service
        // should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "delete_role");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
