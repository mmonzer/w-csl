package autocrypt.module;

import com.csl.autocrypt.tests.TestConfig;
import com.csl.util.JCmd;
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
import static com.csl.web.HTTPConstants.CONTENT_TYPE;
import static com.csl.web.HTTPConstants.JSON_FORMAT;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAutoCryptModule_Misc extends TestConfig {

    @BeforeEach
    public void setUp() {
        // Mock the module
        wireMockServer = new WireMockServer(PORT_MODULE);
        WireMock.configureFor(configObj.get("auto_crypt").get("ip").asString(), PORT_MODULE);
        wireMockServer.start();
        // This ensures that we don't touch the DB
        service = new AutoCryptService();
        service.init();
//        service. getManager().getMethods().setSaveToDb(false);
    }

    @AfterEach
    public void tearDown() {
        // Stop the WireMock server
        wireMockServer.stop();
    }

    // Get certificate  (GET)

    //@Test
    public void testGetHealthCheck() throws Exception {
        // Define expected input/output of the mocked module
        // not used

        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/general/health-check"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                );
        stubFor(x);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "is_alive");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

    //@Test
    public void testActivateOCSP() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String ocspServers = "ocspServers";

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/general/activate-ocsp"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withQueryParam("ocsp_servers", (StringValuePattern) new EqualToPattern(ocspServers))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("ocsp_servers", ocspServers);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "activate_ocsp");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);

        Json response = service.activateOCSP(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testActivateOCSP_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String ocspServers = "ocspServers";

        // Define mocked service behavior
        // should not arrive to module

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("ocsp_servers", ocspServers);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "activate_ocsp");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.activateOCSP(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testActivateOCSP_withoutOcspServer() throws Exception {
        // Define expected input/output of the mocked module
        String path = "path";

        // Define mocked service behavior
        // should not arrive to module

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "activate_ocsp");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
