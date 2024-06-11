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

public class TestAutoCryptModule_Issuer {

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
        service.getManager().getMethods().setSaveToDb(false);
    }

    @AfterEach
    public void tearDown() {
        // Stop the WireMock server
        wireMockServer.stop();
    }

    // Get issuer  (GET)

    @Test
    public void testGetIssuer_normalUse() throws Exception {
        // Define expected input/output of the mocked module
        String issuerRef = "dummyRef";
        String path = "/dev/null";

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("key1", "val1");
        returnOutput.at("key2", "val2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
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
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGetIssuer_normalUse_extraParams() throws Exception {
        // Define expected input/output of the mocked module
        String issuerRef = "dummyRef";
        String path = "/dev/null";

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("key1", "val1");
        returnOutput.at("key2", "val2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
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
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("cmd", "extra");
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGetIssuer_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String issuerRef = "dummyRef";

        // Define mock behavior
        // should not arrive to module

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.getIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGetIssuer_withoutIssuerRef() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        // Define mock behavior
        // should not arrive to module

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at("cmd", "get_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "issuer_ref is missing from body");
        recvOutput.at("error", error);

        Json response = service.getIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // list issuers (GET)

    @Test
    public void testListIssuers_normalUseWithPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/issuer"))
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
        sentInput.at("cmd", "get_issuers");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getIssuers(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testListIssuers_normalUseWithoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");

        // Define mocked service behavior
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/issuer"))
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
        sentInput.at("cmd", "get_issuers");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getIssuers(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testListIssuers_normalUseWithOtherParams() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");

        // Define mocked service behavior
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/issuer"))
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
        sentInput.at("cmd", "get_issuers");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getIssuers(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // Import issuer (POST)

    @Test
    public void testImportIssuer() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String file = "This is a file.";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("file", file);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/issuer/import"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"file\":\"" + file + "\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("file", file);
        Json sentInput = Json.object();
        sentInput.at("cmd", "import_issuer");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.importCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testImportIssuer_file500kB() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        StringBuilder file = new StringBuilder("This is a file.");
        for (int i = 0; i < 15; i++) {
            file.append(file);
        }

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("file", file.toString());

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/issuer/import"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"file\":\"" + file + "\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("file", file.toString());
        Json sentInput = Json.object();
        sentInput.at("cmd", "import_issuer");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.importCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testImportIssuer_file1MB() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        StringBuilder file = new StringBuilder("This is a file.");
        for (int i = 0; i < 16; i++) {
            file.append(file);
        }

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("file", file.toString());

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/issuer/import"))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"file\":\"" + file + "\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("file", file.toString());
        Json sentInput = Json.object();
        sentInput.at("cmd", "import_issuer");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.importCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testImportIssuer_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String name = "dummy";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", "str");

        // Define mocked service
        // should not arrive to mocked service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("issuer_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "import_issuer");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.importCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // update issuers (PUT)

    @Test
    public void testUpdateIssuer_oneParamStr() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String issuerRef = "issuerRef";
        int id = 1;

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("ttl", "24h");

        // Define mocked service behavior
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"ttl\":\"24h\"}"))
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
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("ttl", "24h");
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testUpdateIssuer_oneParamBool() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String issuerRef = "issuerRef";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("enable_aia_url_templating", false);

        // Define mocked service behavior
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"enable_aia_url_templating\":false}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", 1);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("enable_aia_url_templating", false);
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testUpdateIssuer_oneParamList() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String issuerRef = "issuerRef";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json inputList = Json.array();
        inputList.add("element1");
        inputList.add("element2");
        Json returnOutput = Json.object();
        returnOutput.at("issuing_certificates", inputList);

        // Define mocked service behavior
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"issuing_certificates\":" + inputList + "}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", 1);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("issuing_certificates", inputList);
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testUpdateIssuer_multipleParams() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String issuerRef = "issuerRef";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json inputList = Json.array();
        inputList.add("element1");
        inputList.add("element2");
        Json returnOutput = Json.object();
        returnOutput.at("issuing_certificates", inputList);
        returnOutput.at("ttl", "24h");
        returnOutput.at("enable_aia_url_templating", false);

        // Define mocked service behavior
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
                .withHeader("Content-Type", (StringValuePattern) new EqualToPattern("application/json"))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern(
                        "{\"issuing_certificates\":" + inputList + ",\"ttl\":\"24h\",\"enable_aia_url_templating\":false}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", 1);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("issuing_certificates", inputList);
        sentParams.at("ttl", "24h");
        sentParams.at("enable_aia_url_templating", false);
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testUpdateIssuer_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        // not used

        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("issuer_ref", "str");
        sentParams.at("id", 1);
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testUpdateIssuer_withoutIssuerRef() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        // Define mocked service behavior
        // should not arrive to service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("id", 1);
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "issuer_ref is missing from body");
        recvOutput.at("error", error);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testUpdateIssuer_withoutDbapiId() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";

        // Define mocked service behavior
        // should not arrive to service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("issuer_ref", "str");
        Json sentInput = Json.object();
        sentInput.at("cmd", "update_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "id is missing from body");
        recvOutput.at("error", error);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // delete issuers (DELETE)

    @Test
    public void testDeleteIssuer() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String issuerRef = "issuerRef";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();

        // Define mocked service behavior
        MappingBuilder x = delete(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
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
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("id", 1);
        Json sentInput = Json.object();
        sentInput.at("cmd", "delete_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);

        Json response = service.deleteIssuer(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testDeleteIssuer_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String name = "dummy";

        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("issuer_ref", "issuerRef");
        sentParams.at("id", 1);
        Json sentInput = Json.object();
        sentInput.at("cmd", "delete_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.deleteIssuer(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testDeleteIssuer_withoutIssuerRef() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String issuerRef = "issuerRef";

        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("id", 1);
        Json sentInput = Json.object();
        sentInput.at("cmd", "delete_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "issuer_ref is missing from body");
        recvOutput.at("error", error);

        Json response = service.deleteIssuer(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testDeleteIssuer_withoutDbapiId() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String issuerRef = "issuerRef";

        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at("cmd", "delete_issuer_info");
        sentInput.at("params", sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "id is missing from body");
        recvOutput.at("error", error);

        Json response = service.deleteIssuer(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }
}
