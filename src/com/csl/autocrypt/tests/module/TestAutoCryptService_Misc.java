package com.csl.autocrypt.tests.module;

import com.csl.core.CSLContext;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.ucsl.json.Json;
import main.services.AutoCryptService;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.csl.autocrypt.tests.OutilsForTesting.sendPostTo;
import static com.csl.intercom.jsoncmd.JServiceLoader.getUserDir;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAutoCryptService_Misc {

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

    // Get certificate  (GET)

    @Test
    public void testGetHealthCheck() throws Exception {
        // Define expected input/output of the mocked module
        // not used

        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/general/health-check"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                );
        stubFor(x);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        Json sentInput = Json.object();
        sentInput.at("cmd", "is_alive");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        Json result = Json.object();
        result.at("is_http_api_reachable", true);
        recvOutput.at("result", result);

        Json response = service.getStatus(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // Activate OCSP (POST)

    @Test
    public void testActivateOCSP() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String ocspServers = "ocspServers";

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/general/activate-ocsp"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withQueryParam("ocsp_servers", (StringValuePattern) new EqualToPattern(ocspServers))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("ocsp_servers", ocspServers);
        Json sentInput = Json.object();
        sentInput.at("cmd", "activate_ocsp");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);

        Json response = service.activateOCSP(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testActivateOCSP_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String ocspServers = "ocspServers";

        // Define mocked service behavior
        // should not arrive to module

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("ocsp_servers", ocspServers);
        Json sentInput = Json.object();
        sentInput.at("cmd", "activate_ocsp");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.activateOCSP(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testActivateOCSP_withoutOcspServer() throws Exception {
        // Define expected input/output of the mocked module
        String path = "path";

        // Define mocked service behavior
        // should not arrive to module

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at("cmd", "activate_ocsp");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "ocsp_servers is missing from body");
        recvOutput.at("error", error);

        Json response = service.activateOCSP(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }
}
