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

public class TestAutoCryptModule_Certificate {

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
    public void testGetCertificate_normalUse() throws Exception {
        // Define expected input/output of the mocked module
        String serialNumber = "serialNumber";
        String path = "/dev/null";

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("key1", "val1");
        returnOutput.at("key2", "val2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/certificate/"+serialNumber))
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
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_certificate_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getCertificateInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGetCertificate_normalUse_extraParams() throws Exception {
        // Define expected input/output of the mocked module
        String serialNumber = "serialNumber";
        String path = "/dev/null";

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("key1", "val1");
        returnOutput.at("key2", "val2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/certificate/"+serialNumber))
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
        sentParams.at("serial_number", serialNumber);
        sentParams.at("cmd", "extra");
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_certificate_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getCertificateInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGetCertificate_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String serialNumber = "serialNumber";

        // Define mock behavior
        // should not arrive to module

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("certificate_ref", serialNumber);
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_certificate_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.getCertificateInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGetCertificate_withoutSerialNumber() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        // Define mock behavior
        // should not arrive to module

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_certificate_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "serial_number is missing from body");
        recvOutput.at("error", error);

        Json response = service.getCertificateInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // list certificates (GET)

    @Test
    public void testListCertificates_normalUseWithPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/certificate"))
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
        sentInput.at("cmd", "get_certificates");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getCertificates(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testListCertificates_normalUseWithoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");

        // Define mocked service behavior
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/certificate"))
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
        sentInput.at("cmd", "get_certificates");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getCertificates(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testListCertificates_normalUseWithOtherParams() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");

        // Define mocked service behavior
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/certificate"))
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
        sentInput.at("cmd", "get_certificates");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getCertificates(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // Import certificate (POST)

    @Test
    public void testGenerateCertificate_oneParam() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String roleName = "roleName";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("role_name", roleName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/certificate/issue"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"role_name\":\""+roleName+"\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("role_name", roleName);
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_certificate");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

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
        Json returnOutput = Json.object();
        returnOutput.at("role_name", roleName);
        returnOutput.at("commonName", commonName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/certificate/issue"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"role_name\":\""+roleName+"\",\"common_name\":\""+commonName+"\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


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
        recvOutput.at("result", returnOutput);

        Json response = service.generateCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateCertificate_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String name = "dummy";

        // Define mocked service
        // should not arrive to mocked service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("role_name", name);
        sentParams.at("certificate_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_certificate");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateCertificate_withoutRoleName() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        // Define mocked service
        // should not arrive to mocked service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("certificate_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "generate_certificate");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "role_name is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // revoke certificate (DELETE)

    @Test
    public void testDeleteCertificate() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String serialNumber = "serialNumber";

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
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at("cmd", "revoke_certificate");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);

        Json response = service.revokeCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testDeleteCertificate_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String name = "dummy";

        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
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
        String path = "/dev/null";

        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
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
}
