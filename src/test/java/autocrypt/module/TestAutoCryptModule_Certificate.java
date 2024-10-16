package com.csl.autocrypt.tests.module;

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
import static com.csl.web.HTTPConstants.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAutoCryptModule_Certificate extends TestConfig {


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

    // Get certificate  (GET)

    @Test
    public void testGetCertificate_normalUse() throws Exception {
        // Define expected input/output of the mocked module

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("key1", "val1");
        returnOutput.at("key2", "val2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/certificate/"+serialNumber))
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
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "get_certificate_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("key1", "val1");
        returnOutput.at("key2", "val2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/certificate/"+serialNumber))
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
        sentParams.at("serial_number", serialNumber);
        sentParams.at(JCmd.CMD, "extra");
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "get_certificate_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "get_certificate_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

        // Define mock behavior
        // should not arrive to module

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "get_certificate_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/certificate"))
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
        sentInput.at(JCmd.CMD, "get_certificates");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");

        // Define mocked service behavior
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/certificate"))
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
        sentInput.at(JCmd.CMD, "get_certificates");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

        Json returnOutput = Json.array();
        returnOutput.add("string1");
        returnOutput.add("string2");

        // Define mocked service behavior
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/certificate"))
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
        sentInput.at(JCmd.CMD, "get_certificates");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.getCertificates(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // download certificate (GET)

    @Test
    public void testDownloadCertificate() throws Exception {
        // Define expected input/output of the mocked module

        // Define mock behavior
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        String returnOutput = "this is the safest certificate";
        // Define mocked service
        MappingBuilder x = get(urlPathMatching(ENDPOINT_MODULE + "/certificate/download/"+serialNumber))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_DISPOSITION)
                        .withBody(returnOutput)
                );
        stubFor(x);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "download_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        Json expectedOutput = Json.object();
        expectedOutput.at(CONTENT_TYPE, CONTENT_DISPOSITION);
        expectedOutput.at("Content", returnOutput);

        recvOutput.at("result", expectedOutput);

        Json response = service.downloadCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testDownloadCertificate_withoutPath() throws Exception {
        // Define expected input/output of the mocked module

        // Define mock behavior

        // Define mocked service
        // not needed here, should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "download_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.downloadCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testDownloadCertificate_withoutSerialNumber() throws Exception {
        // Define expected input/output of the mocked module

        // Define mock behavior

        // Define mocked service
        // not needed here, should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "download_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "serial_number is missing from body");
        recvOutput.at("error", error);

        Json response = service.downloadCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // Import certificate (POST)

    @Test
    public void testValidateTemplate() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("message", "msg");
        returnOutput.at("valid", true);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/certificate/validate-template"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"name\":\""+name+"\",\"issuer_ref\":\""+issuerRef+"\"}"))
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
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "validate_template");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnOutput);

        Json response = service.validateTemplate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testValidateTemplate_withoutPath() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // not needed here, should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "validate_template");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.validateTemplate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testValidateTemplate_withoutIssuerRef() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // not needed here, should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "validate_template");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "issuer_ref is missing from body");
        recvOutput.at("error", error);

        Json response = service.validateTemplate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testValidateTemplate_withoutName() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // not needed here, should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "validate_template");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        Json response = service.validateTemplate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // Import certificate (POST)

    @Test
    public void testGenerateCertificate_oneParam() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("role_name", roleName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/certificate/issue"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"role_name\":\""+roleName+"\"}"))
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
        sentParams.at("role_name", roleName);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnOutput = Json.object();
        returnOutput.at("role_name", roleName);
        returnOutput.at("common_name", commonName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/certificate/issue"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"role_name\":\""+roleName+"\",\"common_name\":\""+commonName+"\"}"))
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
        sentParams.at("role_name", roleName);
        sentParams.at("common_name", commonName);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

        // Define mocked service
        // should not arrive to mocked service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("role_name", name);
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

        // Define mocked service
        // should not arrive to mocked service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "role_name is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateCertificate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    @Test
    public void testGenerateCertificate_withoutName() throws Exception {
        // Define expected input/output of the mocked module
        // Define mocked service
        // should not arrive to mocked service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

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
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "revoke_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);

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
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "revoke_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
        sentParams.at("name", name);
        sentParams.at("path", path);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "revoke_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
    public void testDeleteCertificate_withoutDbapiName() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "revoke_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
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
        sentParams.at("serial_number", serialNumber);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "revoke_certificate");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
