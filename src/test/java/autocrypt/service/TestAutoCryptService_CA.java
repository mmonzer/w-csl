package autocrypt.service;

import com.csl.util.JCmd;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.ucsl.json.Json;

import java.util.Map;

import static com.csl.web.HTTPConstants.CONTENT_TYPE;
import static com.csl.web.HTTPConstants.JSON_FORMAT;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAutoCryptService_CA extends TestAutoCryptServiceTemplate {
    // Generate root (POST)

    //@Test
    public void testGenerateRoot_withPath() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json expectedBody = Json.object();
        expectedBody.at("common_name", commonName);
        expectedBody.at("ttl", ttl);
        Json returnModule = Json.object();
        returnModule.at("common_name", commonName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/ca/generate-root"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("common_name",(StringValuePattern) new EqualToPattern(commonName))
                .withQueryParam("ttl",(StringValuePattern) new EqualToPattern(ttl))
                .withQueryParam("path",(StringValuePattern) new EqualToPattern(path))
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
        MappingBuilder y = post(urlPathMatching(ENDPOINT_DBAPI + "/certificate_authorities"))
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
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnBd);

        Json response = service.generateRootCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testGenerateRoot_withoutPath() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedBody = Json.object();
        expectedBody.at("common_name", commonName);
        expectedBody.at("ttl", ttl);
        Json returnModule = Json.object();
        returnModule.at("common_name", commonName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/ca/generate-root"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("common_name",(StringValuePattern) new EqualToPattern(commonName))
                .withQueryParam("ttl",(StringValuePattern) new EqualToPattern(ttl))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                );
        wireMockServerModule.stubFor(x);

        Json returnBd = Json.object();
        returnBd.at("id", id);
        // Define mocked service behavior
        MappingBuilder y = post(urlPathMatching(ENDPOINT_DBAPI + "/certificate_authorities"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnBd.toString())
                );
        wireMockServerBd.stubFor(y);


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_root_ca");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnBd);

        Json response = service.generateRootCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testGenerateRoot_withoutCommonName() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("ttl", ttl);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_root_ca");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "common_name is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateRootCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testGenerateRoot_withoutTTL() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_root_ca");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "ttl is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateRootCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testGenerateRoot_withoutDbapiName() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("ttl", ttl);
        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_root_ca");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateRootCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    // Generate intermediate (POST)

    //@Test
    public void testGenerateIntermediate_withPath() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedInput = Json.object();
        expectedInput.at("path", path);
        Json expectedBody = Json.object();
        expectedBody.at("common_name", commonName);
        expectedBody.at("ttl", ttl);
        expectedBody.at("type", type);
        Json returnModule = Json.object();
        returnModule.at("common_name", commonName);

        // Define mocked service behavior
        MappingBuilder x = post(urlPathMatching(ENDPOINT_MODULE + "/ca/generate-intermediate"))
                .withHeader(CONTENT_TYPE, (StringValuePattern) new EqualToPattern(JSON_FORMAT))
                .withQueryParam("path",(StringValuePattern) new EqualToPattern(path))
                .withRequestBody((StringValuePattern) new EqualToPattern(expectedBody.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, JSON_FORMAT)
                        .withBody(returnModule.toString())
                );
        wireMockServerModule.stubFor(x);

        Json expectedDbapi = Json.read(returnModule.toString());
        expectedDbapi.at("name", name);
        Json returnBd = Json.object();
        returnBd.at("id", id);
        for (Map.Entry<String, Object> e: returnModule.asMap().entrySet()) { returnBd.at(e.getKey(), e.getValue());}

        // Define mocked service behavior
        MappingBuilder y = post(urlPathMatching(ENDPOINT_DBAPI + "/certificate_authorities"))
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
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);
        sentParams.at("type", type);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_inter_ca");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", true);
        recvOutput.at("result", returnBd);

        Json response = service.generateIntermediateCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testGenerateIntermediate_withoutPath() throws Exception {
        // Define expected input/output of the mocked module

        Json expectedBody = Json.object();
        expectedBody.at("common_name", commonName);
        expectedBody.at("ttl", ttl);
        expectedBody.at("type", type);
        Json returnOutput = Json.object();
        returnOutput.at("common_name", commonName);

        // Define mocked service behavior
        // should not arrive to module


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);
        sentParams.at("type", type);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_inter_ca");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "path is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateIntermediateCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testGenerateIntermediate_withoutCommonName() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("ttl", ttl);
        sentParams.at("type", type);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_inter_ca");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "common_name is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateIntermediateCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testGenerateIntermediate_withoutTTL() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        sentParams.at("type", type);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_inter_ca");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "ttl is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateIntermediateCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testGenerateIntermediate_withoutType() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("name", name);
        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_inter_ca");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "type is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateIntermediateCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }

    //@Test
    public void testGenerateIntermediate_withoutDbapiName() throws Exception {
        // Define expected input/output of the mocked module

        // Define mocked service behavior
        // should not arrive to mocker service


        // Define expected input/output of the api
        Json sentParams = Json.object();
        sentParams.at("path", path);
        sentParams.at("common_name", commonName);
        sentParams.at("ttl", ttl);
        sentParams.at("type", type);
        Json sentInput = Json.object();
        sentInput.at(JCmd.CMD, "generate_inter_ca");
        sentInput.at(JCmd.PARAMETERS, sentParams);

        Json recvOutput = Json.object();
        recvOutput.at("success", false);
        Json error = Json.object();
        error.at("reason", "name is missing from body");
        recvOutput.at("error", error);

        Json response = service.generateIntermediateCA(sentParams);

        // assert behavior
        assertEquals(recvOutput, response);
    }
}
