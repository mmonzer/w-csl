package autocrypt.integration;

import com.csl.util.JCmd;
import com.ucsl.json.Json;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.jupiter.api.Test;

import static autocrypt.OutilsForTesting.sendPostTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestIssuer {

    // API module
    protected static final int PORT_MODULE = 8082; // Change this to your actual base URL
    protected static final String IP_MODULE = "localhost"; // Change this to your actual base URL
    protected static final String BASE_URL_MODULE = "http://" + IP_MODULE + ":" + PORT_MODULE; // Change this to your actual base URL
    protected static final String ENDPOINT_MODULE = "/api";
    // API client
    protected static final int PORT_CLIENT = 9900; // Change this to your actual base URL
    protected static final String BASE_URL_CLIENT = "http://localhost:" + PORT_CLIENT; // Change this to your actual base URL
    protected static final String ENDPOINT_CLIENT = "/autocrypt";

    //@Test
    public void testImporte() throws Exception {
        Json params = Json.object();
        params.at("path","pki");
        params.at("file","-----BEGIN CERTIFICATE-----\n" +
                "MIIDNTCCAh2gAwIBAgIUdEfPt45tjdxnZLQDXwGgYjyE9qQwDQYJKoZIhvcNAQEL\n" +
                "BQAwFjEUMBIGA1UEAxMLZXhhbXBsZS5jb20wHhcNMjQwNjEzMTI1MDMyWhcNMjQw\n" +
                "NzE1MTI1MTAyWjAWMRQwEgYDVQQDEwtleGFtcGxlLmNvbTCCASIwDQYJKoZIhvcN\n" +
                "AQEBBQADggEPADCCAQoCggEBAKyhK4eiGPas+ToVtaCZGyD6jPJg5ZKhi0fcWCoZ\n" +
                "8SzsqLE7qixvqcjlB4q0HZlWf3hhuVgzLrh6pd32Mlu02SPHgXyVepabe/zjX3gm\n" +
                "32hEHGAwt3luurG6gHFBaPVDpxhH3xViIjKPKBWLpOw9BR7W1aoYF4TiM8cYTgOM\n" +
                "F9TWLeEmpb2uw+RzHUhO/7/i37LRD+B1VLvzNTRaRGKf6ozL1uYHFvTdXsMuZMZR\n" +
                "ENGb4vw2aWGWMW8p/CoTU0PZOd2HeV+gE+POfApmUsecYRXd670Tih3hyWjT8KOG\n" +
                "uSTYRDzPEwEdkfJ2jQnTs44s5xeSW8ANNf8deM9+VUySVBUCAwEAAaN7MHkwDgYD\n" +
                "VR0PAQH/BAQDAgEGMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFEtW3pyyH4nx\n" +
                "/XvRCPzKGe9tSFiBMB8GA1UdIwQYMBaAFEtW3pyyH4nx/XvRCPzKGe9tSFiBMBYG\n" +
                "A1UdEQQPMA2CC2V4YW1wbGUuY29tMA0GCSqGSIb3DQEBCwUAA4IBAQCREXIlK+bW\n" +
                "PrpMBiKGkyMhSVVkjiHdFxN4CizIJUcnOl4M9z8IigHrhMlrmVIV87R9WB76H01F\n" +
                "tymCzC44ZQeddQBEOa3VcaAARgVSEWTOuxBJA1AhE/zO7ubdzTE+I5/jDyVjQjvr\n" +
                "6UmuEtCd1M2YLpxQFQnksZ+zvu+/z8BhWCjHMvCBfmDJ00/2RNZG8DUc3T0nlWS5\n" +
                "sUdqzTm1apTdxwFB4watN2uAMVBV6IF1jZEntKszqW/+wT+Ot0tMTJr1OIeEGGAn\n" +
                "uOLP4m2HjrqpNECGaFFS+i49ze8HTtaMT/7ANaAPox4bIjxwh6UUoxP2sd1SrnkM\n" +
                "VwS1KkfPDiLr\n" +
                "-----END CERTIFICATE-----");
        params.at("name","abc");
        Json inputJson = Json.object();
        inputJson.at(JCmd.CMD, "import_issuer");
        inputJson.at(JCmd.PARAMETERS, params);

        // Define request to th mocked service
        ContentResponse response = sendPostTo(BASE_URL_CLIENT+ENDPOINT_CLIENT, inputJson);

        // assert behavior
        assertEquals(200, response.getStatus());
        assertEquals("", response.getContentAsString());
    }
}
