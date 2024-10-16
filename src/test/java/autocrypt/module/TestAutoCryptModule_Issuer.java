package tests.module;

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
import static org.junit.jupiter.api.Assertions.*;

public class TestAutoCryptModule_Issuer extends TestConfig {



    @BeforeEach
    public void setUp() {
        // Mock the module
        wireMockServer = new WireMockServer(PORT_MODULE);
        WireMock.configureFor(configObj.get("autocrypt").get("ip").asString(), PORT_MODULE);
        wireMockServer.start();
        // This ensures that we don't touch the DB
        service = new AutoCryptService();
        service.init(configObj.get(service.getConfigFileSectionName()), getUserDir());
        service.getAutocrypt().setSaveToDb(false);
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

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("key1", "val1");
        returnOutput.at("key2", "val2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnOutput.toString())
                );
        stubFor(x);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "get_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("key1", "val1");
        returnOutput.at("key2", "val2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnOutput.toString())
                );
        stubFor(x);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at(JCmd.CMD, "extra");
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "get_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        sentInput.at(JCmd.CMD, "get_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        // Json response = service.getIssuerInfo(sentParams);

        // assert behavior
        // assertEquals(recvOutput, response);
    }

    @Test
    public void testGetIssuer_withoutIssuerRef() throws Exception {
        // Define expected input/output of the mocked module

        // Define mock behavior
        // should not arrive to module

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "get_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "issuer_ref is missing from body");
        recvOutput.at("error", error);

        // Json response = service.getIssuerInfo(sentParams);

        // assert behavior
        // assertEquals(recvOutput, response);
    }

    // list issuers (GET)

    @Test
    public void testListIssuers_normalUseWithPath() throws Exception {
        // Define expected input/output of the mocked module

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/issuer"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "get_issuers");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");

        // Define mocked service behavior
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/issuer"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnOutput.toString())
                );
        stubFor(x);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "get_issuers");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getIssuers(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testListIssuers_normalUseWithOtherParams() {
        // Define expected input/output of the mocked module

        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");

        // Define mocked service behavior
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/issuer"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnOutput.toString())
                );
        stubFor(x);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("extra", "param");
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "get_issuers");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        String file = "This is a file.";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("file", file);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/issuer/import"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"file\":\"" + file + "\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("file", file);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "import_issuer");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.importIssuerIntermediate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testImportIssuer_file500kB() throws Exception {
        // Define expected input/output of the mocked module
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
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"file\":\"" + file + "\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("file", file.toString());
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "import_issuer");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.importIssuerIntermediate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testImportIssuer_file1MB() throws Exception {
        // Define expected input/output of the mocked module
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
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"file\":\"" + file + "\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("file", file.toString());
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "import_issuer");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        // Json response = service.importIssuerIntermediate(sentParams);

        // assert behavior
        // assertEquals(recvOutput, response);
    }

    @Test
    public void testImportIssuer_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
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
        sentInput.at(JCmd.CMD, "import_issuer");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.importIssuerIntermediate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testImportIssuer_withoutName() throws Exception {
        // Define expected input/output of the mocked module
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("name", name);
        returnOutput.at("issuer_ref", "str");

        // Define mocked service
        // should not arrive to mocked service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("issuer_ref", "str");
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "import_issuer");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        // Json response = service.importIssuerIntermediate(sentParams);

        // assert behavior
        // assertEquals(recvOutput, response);
    }

    // update issuers (PUT)

    @Test
    public void testUpdateIssuer_oneParamStr() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("allow_any_name", true);
        Json returnOutput = Json.object();
        returnOutput.at("issuer_name", name);
        returnOutput.at("allow_any_name", true);

        // Define mocked service behavior
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern(returnOutput.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("issuer_name", name);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("ttl", ttl);
        sentParams.at("allow_any_name", true);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        Json result= Json.object();
        result.set("path", path);
        result.set("issuer_ref", issuerRef);
        result.set("issuer_name", name);
        result.set("allow_any_name", true);
        result.set("type", type);
        result.set("ttl", ttl);
        recvOutput.at("result", result);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testUpdateIssuer_oneParamBool() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("enable_aia_url_templating", false);

        // Define mocked service behavior
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"enable_aia_url_templating\":false}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("enable_aia_url_templating", false);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json inputList = Json.array();
        inputList.add("element1");
        inputList.add("element2");
        Json returnOutput = Json.object();
        returnOutput.at("issuing_certificates", inputList);

        // Define mocked service behavior
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"issuing_certificates\":" + inputList + "}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnOutput.toString())
                );
        stubFor(x);

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("issuing_certificates", inputList);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern(
                        "{\"issuing_certificates\":" + inputList + ",\"ttl\":\"24h\",\"enable_aia_url_templating\":false}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnOutput.toString())
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("issuing_certificates", inputList);
        sentParams.at("ttl", "24h");
        sentParams.at("enable_aia_url_templating", false);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        sentParams.at("id", id);
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("issuer_name", name);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        String missingParam = "path";

        try {
            service.updateIssuerInfo(sentParams);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(missingParam, e.getMessage());
        }
    }

    @Test
    public void testUpdateIssuer_withoutIssuerRef() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("issuer_name", name);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        String missingParam = "issuer_ref";

        try {
            service.updateIssuerInfo(sentParams);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(missingParam, e.getMessage());
        }
    }
//
//    @Test
//    public void testUpdateIssuer_withoutDbapiName() throws Exception {
//        // Define expected input/output of the mocked module
//
//        // Define mocked service behavior
//        // should not arrive to service
//
//
//        // Define expected input/output of the api
//        Json sentParams = Json.object();
//        sentParams.at("path", path);
//        sentParams.at("issuer_ref", "str");
//        Json sentInput = Json.object();
//        sentInput.at(JCmd.CMD, "update_issuer_info");
//        sentInput.at(JCmd.PARAMETERS, sentParams);
//
//        Json recvOutput = Json.object();
//        recvOutput.at("success", false);
//        Json error = Json.object();
//        error.at("reason", "name is missing from body");
//        recvOutput.at("error", error);
//
//        Json response = service.updateIssuerInfo(sentParams);
//
//        // assert behavior
//        assertEquals(recvOutput, response);
//    }

    // delete issuers (DELETE)

    @Test
    public void testDeleteIssuer() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();

        // Define mocked service behavior
        MappingBuilder x = delete(urlPathMatching(ENDPOINT_MODULE + "/issuer/" + issuerRef))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("id", 1);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "delete_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        sentParams.at("id", id);
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "delete_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        String missingParam = "path";

        try {
            service.deleteIssuer(sentParams);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(missingParam, e.getMessage());
        }
    }

    @Test
    public void testDeleteIssuer_withoutIssuerRef() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("name", name);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "delete_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        String missingParam = "issuer_ref";

        try {
            service.deleteIssuer(sentParams);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(missingParam, e.getMessage());
        }
    }
}
