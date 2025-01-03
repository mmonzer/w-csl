package autocrypt;

import com.ucsl.json.Json;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;

import static com.csl.web.HTTPConstants.CONTENT_TYPE;
import static com.csl.web.HTTPConstants.JSON_FORMAT;
import static com.csl.web.apiclient.ApiHandler.addBodyTo;
import static com.csl.web.apiclient.ApiHandler.addHeaderTo;

public class OutilsForTesting {

    /**
     * Sends a POST request with the given content (body) to the given uri and returns the response
     */
    public static ContentResponse sendPostTo(String uri, String content) throws Exception {
        // Define request to the mocked service
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        Request request = httpClient.newRequest(uri);
        addHeaderTo(request, CONTENT_TYPE, JSON_FORMAT);
        request.method(HttpMethod.POST);
        addBodyTo(request, content, JSON_FORMAT);
        return request.send();
    }
    public static ContentResponse sendPostTo(String uri, Json content) throws Exception {
        return sendPostTo(uri, content.toString());
    }


}
