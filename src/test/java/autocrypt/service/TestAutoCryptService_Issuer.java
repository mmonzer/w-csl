package autocrypt.service;

import com.csl.autocrypt.tests.TestConfig;
import com.csl.util.JCmd;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.ucsl.json.Json;
import main.services.AutoCryptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

import static com.csl.web.HTTPConstants.CONTENT_TYPE;
import static com.csl.web.HTTPConstants.JSON_FORMAT;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAutoCryptService_Issuer extends TestConfig {
    
    
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
        config.at("ip", configObj.get("auto_crypt").get("ip").asString());
        config.at("port", PORT_MODULE);
        config.at("global", globalConfig);

        service = new AutoCryptService();
        service.init();
//        service. getManager().getMethods().setSaveToDb(true);
    }

    @AfterEach
    public void tearDown() {
        // Stop the WireMock server
        wireMockServerModule.stop();
        wireMockServerBd.stop();
    }

    // Import issuer (POST)

    //@Test
    public void testImportIssuer() throws Exception {
        // Define expected input/output of the mocked module
        String file = "This is a file.";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();
        returnModule.at("file", file);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/issuer/import"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"file\":\""+file+"\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnModule.toString())
                );
        wireMockServerModule.stubFor(x);

        Json expectedDbapi = Json.read(returnModule.toString());
        expectedDbapi.at("name", name);
        Json returnBd = Json.object();
        for (Map.Entry<String, Object> e : returnModule.asMap().entrySet()) {returnBd.at(e.getKey(), e.getValue());}
        returnBd.at("id", id);
        // Define mocked service behavior
        MappingBuilder y = post(urlPathMatching(ENDPOINT_DBAPI + "/issuer"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withRequestBody((StringValuePattern) new EqualToPattern(expectedDbapi.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


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
        recvOutput.at("result", returnBd);

        Json response = service.importIssuerIntermediate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testImportIssuer_file500kB() throws Exception {
        // Define expected input/output of the mocked module
        StringBuilder file = new StringBuilder("This is a file.");
        for (int i=0; i<15; i++) {
            file.append(file);
        }

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();
        returnModule.at("file", file.toString());

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/issuer/import"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"file\":\""+file+"\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnModule.toString())
                );
        wireMockServerModule.stubFor(x);

        Json expectedDbapi = Json.read(returnModule.toString());
        expectedDbapi.at("name", name);
        Json returnBd = Json.object();
        for (Map.Entry<String, Object> e : returnModule.asMap().entrySet()) {returnBd.at(e.getKey(), e.getValue());}
        returnBd.at("id", id);
        // Define mocked service behavior
        MappingBuilder y = post(urlPathMatching(ENDPOINT_DBAPI + "/issuer"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withRequestBody((StringValuePattern) new EqualToPattern(expectedDbapi.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


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
        recvOutput.at("result", returnBd);

        Json response = service.importIssuerIntermediate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testImportIssuer_file1MB() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        StringBuilder file = new StringBuilder("This is a file.");
        for (int i=0; i<16; i++) {
            file.append(file);
        }

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();
        returnModule.at("file", file.toString());

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/issuer/import"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"file\":\""+file+"\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnModule.toString())
                );
        wireMockServerModule.stubFor(x);

        Json expectedDbapi = Json.read(returnModule.toString());
        expectedDbapi.at("name", name);
        Json returnBd = Json.object();
        for (Map.Entry<String, Object> e : returnModule.asMap().entrySet()) {returnBd.at(e.getKey(), e.getValue());}
        returnBd.at("id", id);
        // Define mocked service behavior
        MappingBuilder y = post(urlPathMatching(ENDPOINT_DBAPI + "/issuer"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withRequestBody((StringValuePattern) new EqualToPattern(expectedDbapi.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


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
        recvOutput.at("result", returnBd);

        Json response = service.importIssuerIntermediate(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testImportIssuer_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        String path = "/dev/null";
        String name = "dummy";

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();
        returnModule.at("name", name);
        returnModule.at("issuer_ref", "str");

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

    // update issuers (PUT)

    //@Test
    public void testUpdateIssuer_oneParamStr() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();
        returnModule.at("ttl", "24h");

        // Define mocked service behavior
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/issuer/"+issuerRef))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"ttl\":\"24h\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnModule.toString())
                );
        wireMockServerModule.stubFor(x);

        Json expectedDbapi = Json.read(returnModule.toString());
        expectedDbapi.at("name", name);
        Json returnBd = Json.object();
        for (Map.Entry<String, Object> e : returnModule.asMap().entrySet()) {returnBd.at(e.getKey(), e.getValue());}
        returnBd.at("id", id);
        // Define mocked service behavior
        MappingBuilder y = put(urlPathMatching(ENDPOINT_DBAPI + "/issuer/"+id))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withRequestBody((StringValuePattern) new EqualToPattern(expectedDbapi.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("ttl", "24h");
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnBd);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testUpdateIssuer_oneParamBool() throws Exception {
        // Define expected input/output of the mocked module
        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();
        returnModule.at("enable_aia_url_templating", false);

        // Define mocked service behavior
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/issuer/"+issuerRef))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"enable_aia_url_templating\":false}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnModule.toString())
                );
        wireMockServerModule.stubFor(x);

        Json expectedDbapi = Json.read(returnModule.toString());
        expectedDbapi.at("name", name);
        Json returnBd = Json.object();
        for (Map.Entry<String, Object> e : returnModule.asMap().entrySet()) {returnBd.at(e.getKey(), e.getValue());}
        returnBd.at("id", id);
        // Define mocked service behavior
        MappingBuilder y = put(urlPathMatching(ENDPOINT_DBAPI + "/issuer/"+id))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withRequestBody((StringValuePattern) new EqualToPattern(expectedDbapi.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


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
        recvOutput.at("result", returnBd);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testUpdateIssuer_oneParamList() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json inputList = Json.array();
        inputList.add("element1");
        inputList.add("element2");
        Json returnModule = Json.object();
        returnModule.at("issuing_certificates", inputList);

        // Define mocked service behavior
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/issuer/"+issuerRef))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern("{\"issuing_certificates\":"+inputList+"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnModule.toString())
                );
        wireMockServerModule.stubFor(x);

        Json expectedDbapi = Json.read(returnModule.toString());
        expectedDbapi.at("name", name);
        Json returnBd = Json.object();
        for (Map.Entry<String, Object> e : returnModule.asMap().entrySet()) {returnBd.at(e.getKey(), e.getValue());}
        returnBd.at("id", id);
        // Define mocked service behavior
        MappingBuilder y = put(urlPathMatching(ENDPOINT_DBAPI + "/issuer/"+id))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withRequestBody((StringValuePattern) new EqualToPattern(expectedDbapi.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);

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
        recvOutput.at("result", returnBd);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testUpdateIssuer_multipleParams() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json inputList = Json.array();
        inputList.add("element1");
        inputList.add("element2");
        Json returnModule = Json.object();
        returnModule.at("issuing_certificates", inputList);
        returnModule.at("ttl", "24h");
        returnModule.at("enable_aia_url_templating", false);

        // Define mocked service behavior
        MappingBuilder x = put(urlPathMatching(ENDPOINT_MODULE + "/issuer/"+issuerRef))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path", (StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern(
                        "{\"issuing_certificates\":"+inputList+",\"ttl\":\"24h\",\"enable_aia_url_templating\":false}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnModule.toString())
                );
        wireMockServerModule.stubFor(x);


        Json expectedDbapi = Json.read(returnModule.toString());
        expectedDbapi.at("name", name);
        Json returnBd = Json.object();
        for (Map.Entry<String, Object> e : returnModule.asMap().entrySet()) {returnBd.at(e.getKey(), e.getValue());}
        returnBd.at("id", id);
        // Define mocked service behavior
        MappingBuilder y = put(urlPathMatching(ENDPOINT_DBAPI + "/issuer/"+id))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withRequestBody((StringValuePattern) new EqualToPattern(expectedDbapi.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


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
        recvOutput.at("result", returnBd);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testUpdateIssuer_withoutPath() throws Exception {
        // Define expected input/output of the mocked module
        // not used

        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("issuer_ref", issuerRef);
        sentParams.at("name", name);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testUpdateIssuer_withoutIssuerRef() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("name", name);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "issuer_ref is missing from body");
        recvOutput.at("error", error);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testUpdateIssuer_withoutDbapiName() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        Json response = service.updateIssuerInfo(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testUpdateIssuer_withoutDbapiId() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "update_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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

    //@Test
    public void testDeleteIssuer() throws Exception {
        // Define expected input/output of the mocked module
        

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json returnModule = Json.object();

        // Define mocked service behavior
        MappingBuilder x = delete(urlPathMatching(ENDPOINT_MODULE + "/issuer/"+issuerRef))
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
        MappingBuilder y = delete(urlPathMatching(ENDPOINT_DBAPI + "/issuer/"+id))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "delete_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnBd);

        Json response = service.deleteIssuer(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testDeleteIssuer_withoutPath() throws Exception {
        // Define expected input/output of the mocked module

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

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.deleteIssuer(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
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

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "issuer_ref is missing from body");
        recvOutput.at("error", error);

        Json response = service.deleteIssuer(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testDeleteIssuer_withoutDbapiName() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("id", id);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "delete_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        Json response = service.deleteIssuer(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testDeleteIssuer_withoutDbapiId() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to service

        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("issuer_ref", issuerRef);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "delete_issuer_info");
        sentInput.at(JCmd.PARAMETERS, sentParams);

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
