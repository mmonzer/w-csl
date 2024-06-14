package com.csl.autocrypt.tests.integration;

import com.ucsl.json.Json;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.jupiter.api.Test;

import static com.csl.autocrypt.tests.OutilsForTesting.sendPostTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCertificates {

    // API module
    protected static final int PORT_MODULE = 8082; // Change this to your actual base URL
    protected static final String IP_MODULE = "localhost"; // Change this to your actual base URL
    protected static final String BASE_URL_MODULE = "http://" + IP_MODULE + ":" + PORT_MODULE; // Change this to your actual base URL
    protected static final String ENDPOINT_MODULE = "/api";
    // API client
    protected static final int PORT_CLIENT = 9900; // Change this to your actual base URL
    protected static final String BASE_URL_CLIENT = "http://localhost:" + PORT_CLIENT; // Change this to your actual base URL
    protected static final String ENDPOINT_CLIENT = "/autocrypt";

    @Test
    public void testValidate() throws Exception {
        Json params = Json.object();
        params.at("path","pki");
        params.at("issuer_ref","bbae9fe3-e1f6-6732-5d99-9b1b45856847");
        params.at("name","abc");
        Json inputJson = Json.object();
        inputJson.at("cmd", "validate_template");
        inputJson.at("params", params);

        // Define request to th mocked service
        ContentResponse response = sendPostTo(BASE_URL_CLIENT+ENDPOINT_CLIENT, inputJson);

        // assert behavior
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testGenerate() throws Exception {
        Json params = Json.object();
        params.at("path","pki");
        params.at("role_name","root-role");
        params.at("name","abg");
        params.at("ttl","24h");
        Json inputJson = Json.object();
        inputJson.at("cmd", "generate_certificate");
        inputJson.at("params", params);

        // Define request to th mocked service
        ContentResponse response = sendPostTo(BASE_URL_CLIENT+ENDPOINT_CLIENT, inputJson);

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals("200", response.getContentAsString());
    }

    @Test
    public void testRevoke() throws Exception {

        Json params = Json.object();
        params.at("path","pki");
        params.at("serial_number","189479376794655192154469549103470209110193126165");
        params.at("id",6);
        params.at("name","dummyCert2");
        Json inputJson = Json.object();
        inputJson.at("cmd", "revoke_certificate");
        inputJson.at("params", params);

        // Define request to th mocked service
        ContentResponse response = sendPostTo(BASE_URL_CLIENT+ENDPOINT_CLIENT, inputJson);

        // assert behavior
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDownload() throws Exception {

        Json params = Json.object();
        params.at("path","pki");
        params.at("serial_number","74:47:cf:b7:8e:6d:8d:dc:67:64:b4:03:5f:01:a0:62:3c:84:f6:a4");
        Json inputJson = Json.object();
        inputJson.at("cmd", "download_certificate");
        inputJson.at("params", params);

        // Define request to th mocked service
        ContentResponse response = sendPostTo(BASE_URL_CLIENT+ENDPOINT_CLIENT, inputJson);

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals("", response.getContentAsString());
    }

    // TODO: test changeIp and changePort
}
