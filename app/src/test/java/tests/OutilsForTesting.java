package com.csl.autocrypt.tests;

import com.ucsl.json.Json;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;

public class OutilsForTesting {

    /**
     * Sends a POST request with the given content (body) to the given uri and returns the response
     */
    public static ContentResponse sendPostTo(String uri, String content) throws Exception {
        // Define request to the mocked service
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        Request request = httpClient.newRequest(uri);
        request.header("Content-Type", "application/json");
        request.method(HttpMethod.POST);
        request.content(new StringContentProvider(content), "application/json");
        return request.send();
    }
    public static ContentResponse sendPostTo(String uri, Json content) throws Exception {
        return sendPostTo(uri, content.toString());
    }


}
