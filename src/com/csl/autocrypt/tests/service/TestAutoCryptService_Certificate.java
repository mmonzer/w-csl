package com.csl.autocrypt.tests.service;

import com.csl.core.CSLContext;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
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

public class TestAutoCryptService_Certificate {

    // API module
    private static final int PORT_MODULE = 8082; // Change this to your actual base URL
    private static final String BASE_URL_MODULE = "http://localhost:" + PORT_MODULE; // Change this to your actual base URL
    private static final String ENDPOINT_MODULE = "/api";
    // API db
    private static final int PORT_DBAPI = 8787; // Change this to your actual base URL
    private static final String BASE_URL_DBAPI = "http://localhost:" + PORT_DBAPI; // Change this to your actual base URL
    private static final String ENDPOINT_DBAPI = "/api/autocrypt";
    // API client
    private static final int PORT_CLIENT = 9900; // Change this to your actual base URL
    private static final String BASE_URL_CLIENT = "http://localhost:" + PORT_CLIENT; // Change this to your actual base URL
    private static final String ENDPOINT_CLIENT = "/autocrypt";

    private WireMockServer wireMockServerModule;
    private WireMockServer wireMockServerBd;

    private AutoCryptService service;
    private static final Json configObj = CSLContext.instance.getConfig();

    String path = "/dev/null";
    String serialNumber = "serialNumber";
    int id = 1;
    String name = "dummy";


    @BeforeEach
    public void setUp() {
        // Mock the module
        wireMockServerModule = new WireMockServer(PORT_MODULE);
        wireMockServerModule.start();
        wireMockServerBd = new WireMockServer(8787);
        wireMockServerBd.start();
        // This ensures that we touch the DB
        Json globalConfig = configObj.get("global");
        globalConfig.delAt("ip_server_remote");
        globalConfig.at("ip_server_remote", "localhost:"+PORT_DBAPI);
        globalConfig.delAt("api_key");
        globalConfig.at("api_key", "");
        globalConfig.delAt("use_ssl");
        globalConfig.at("use_ssl", false);

        Json config = Json.object();
        config.at("ip", "localhost");
        config.at("port", 8082);
        config.at("global", globalConfig);

        service = new AutoCryptService();
        service.init(config, getUserDir());
        service. getManager().getMethods().setSaveToDb(true);
    }

    @AfterEach
    public void tearDown() {
        // Stop the WireMock server
        wireMockServerModule.stop();
        wireMockServerBd.stop();
    }

    // Import certificate (POST)

    @Test
    public void testGenerateCertificate_oneParam() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String roleName = "roleName";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();
        returnModule.at("role_name", roleName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/certificate/issue"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"role_name\":\""+roleName+"\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnModule.toString())
                );
        wireMockServerModule.stubFor(x);

        Json returnBd = Json.object();
        returnBd.at("role_name", roleName);
        returnBd.at("id", id);
        // Define mocked service behavior
        MappingBuilder y = post(urlPathMatching(ENDPOINT_DBAPI + "/certificates"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withRequestBody((StringValuePattern) new EqualToPattern(returnModule.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("role_name", roleName);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_certificate");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnBd);

        Json response = service.generateCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateCertificate_multipleParam() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String roleName = "roleName";
        String commonName = "commonName";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();
        returnModule.at("role_name", roleName);
        returnModule.at("common_name", commonName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/certificate/issue"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"role_name\":\""+roleName+"\",\"common_name\":\""+commonName+"\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnModule.toString())
                );
        wireMockServerModule.stubFor(x);

        Json returnBd = Json.object();
        returnBd.at("role_name", roleName);
        returnBd.at("common_name", commonName);
        returnBd.at("id", id);
        // Define mocked service behavior
        MappingBuilder y = post(urlPathMatching(ENDPOINT_DBAPI + "/certificates"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withRequestBody((StringValuePattern) new EqualToPattern(returnModule.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("role_name", roleName);
        sentParams.at("common_name", commonName);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_certificate");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnBd);

        Json response = service.generateCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // revoke certificate (DELETE)

    @Test
    public void testDeleteCertificate() throws Exception {
        // Define expected input/output of the mocked module
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();

        // Define mocked service behavior
        MappingBuilder x = delete(urlPathMatching(ENDPOINT_MODULE + "/certificate/revoke/"+serialNumber))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        wireMockServerModule.stubFor(x);

        Json returnBd = Json.object();
        returnBd.at("id", id);
        // Define mocked service behavior
        MappingBuilder y = delete(urlPathMatching(ENDPOINT_DBAPI + "/certificates/"+id))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at("cmd", "revoke_certificate");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnBd);

        Json response = service.revokeCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testDeleteCertificate_withoutPath() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("name", name);
        sentParams.at("certificate_ref", "certificateRef");
        Json sentInput = Json.object();
        sentInput.at("cmd", "revoke_certificate");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.revokeCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testDeleteCertificate_withoutSerialNumber() throws Exception {
        // Define expected input/output of the mocked module
        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at("cmd", "revoke_certificate");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "serial_number is missing from body");
        recvOutput.at("error", error);

        Json response = service.revokeCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testDeleteCertificate_withoutDbapiId() throws Exception {
        // Define expected input/output of the mocked module
        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at("cmd", "revoke_certificate");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "id is missing from body");
        recvOutput.at("error", error);

        Json response = service.revokeCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }
}
