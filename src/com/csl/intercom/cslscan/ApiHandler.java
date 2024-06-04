package com.csl.intercom.cslscan;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Class to handle communication for API client.
 */
public class ApiHandler implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ApiHandler.class);
    private final String moduleName;
    private final String url;
    private HttpClient httpClient = new HttpClient();

    /**
     * Constructor with no module name
     *
     * @param url url of the service api
     */
    public ApiHandler(String url) {
        this("", url);
    }

    /**
     * General constructor
     *
     * @param nameModule nameof the module
     * @param url        url of the service api
     */
    public ApiHandler(String nameModule, String url) {
        this.url = url;
        this.moduleName = nameModule;

        try {
            httpClient.start();
        } catch (Exception e) {
            logger.error("Could not start the http client for " + nameModule + " API.", e);
        }
    }

    @Override
    public void close() throws Exception {
        this.httpClient.stop();
    }

    /**
     * Send a DELETE HTTP request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendDelete(String endpoint, Json params) {
        return sendRequestToApi(HttpMethod.DELETE, endpoint, params, null,false);
    }

    /**
     * Send a DELETE HTTP request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendDelete(String endpoint, Json params, Json body) {
        return sendRequestToApi(HttpMethod.DELETE, endpoint, params, body,false);
    }

    /**
     * Send a HTTP GET request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendGet(String endpoint, Json params) {
        return sendRequestToApi(HttpMethod.GET, endpoint, params,null, false);
    }

    /**
     * Send a HTTP POST request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param body   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendPost(String endpoint, Json params, Json body) {
        return sendRequestToApi(HttpMethod.POST, endpoint, params, body, false);
    }

    /**
     * Send a HTTP POST request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param body   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendPost(String endpoint, Json body) {
        return sendRequestToApi(HttpMethod.POST, endpoint, null, body, false);
    }

    /**
     * Send a HTTP PUT request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param body   The body to send, if any (if not, should be an empty {@link Json} object, not null).
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendPut(String endpoint, Json params, Json body) {
        return sendRequestToApi(HttpMethod.PUT, endpoint, params, body, false);
    }

    /**
     * Send a HTTP PUT request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param body   The body to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendPut(String endpoint, Json body) {
        return sendRequestToApi(HttpMethod.PUT, endpoint, null, body, false);
    }

    /**
     * Send an HTTP request to the scanner.
     *
     * @param method   The HTTP method to use (GET, POST, PUT, ...)
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendRequestToApiQuiet(HttpMethod method, String endpoint, Json params) {
        return sendRequestToApi(method, endpoint, params, true);
    }

    /**
     * Send an HTTP request to the scanner.
     *
     * @param method   The HTTP method to use (GET, POST, PUT, ...)
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendRequestToApi(HttpMethod method, String endpoint, Json params, boolean quiet) {
        JsonApiResponse res = JsonApiResponse.error(null);
        Request request;
        String URI = url + endpoint.replace(" ", "%20");

        request = httpClient.newRequest(URI);
        request.method(method);
        try {
            switch (method) {
                case POST:
                case PUT:
                    request.content(new StringContentProvider(params.toString()), "application/json");
                    break;
                case GET:
                case DELETE:
                    addParamsToRequest(params, request);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported HTTP method: " + method.asString());
            }
            ContentResponse response = request.send();
            if (response.getStatus() >= 400) {
                return JsonApiResponse.error("Error while sending request to " + moduleName, Json.object("status_code", response.getStatus(), "content", response.getContentAsString()));
            }
            if (response.getContent().length > 0) {
                if (response.getContent()[0] == '{' || response.getContent()[0] == '[') {
                    res = JsonApiResponse.result(
                            Json.read(response.getContentAsString()),
                            Json.object("status_code", response.getStatus())
                    );
                } else {
                    res = JsonApiResponse.result(Json.object("value", response.getContentAsString()),
                            Json.object("status_code", response.getStatus())
                    );
                }
            } else {
                res = JsonApiResponse.result(null,
                        Json.object("status_code", response.getStatus())
                );
            }
        } catch (UnsupportedOperationException e) {
            logger.error("Malformed json", e);
            res = JsonApiResponse.error(e.getMessage());
        } catch (Exception e) {
            if (!quiet) {
                logger.error("Error while sending request to " + moduleName);
            }
            if (e.getCause() instanceof ConnectException) {
                res = JsonApiResponse.error("Connection error with " + moduleName);
            }
        }
        return res;
    }

    /**
     * Send an HTTP request to the scanner.
     *
     * @param method   The HTTP method to use (GET, POST, PUT, ...)
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @param body   The body to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendRequestToApi(HttpMethod method, String endpoint, Json params, Json body, boolean quiet) {
        JsonApiResponse res = JsonApiResponse.error(null);
        Request request;
        String URI = url + endpoint.replace(" ", "%20");

        request = httpClient.newRequest(URI);
        request.method(method);
        addParamsToRequest(params, request);


        request.header("Content-Type", "application/json");
        try {
            switch (method) {
                case POST:
                case PUT:
                case DELETE:
                    if (body!=null) {request.content(new StringContentProvider(body.toString()), "application/json");}
                    break;
                case GET:
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported HTTP method: " + method.asString());
            }
            ContentResponse response = request.send();
            if (response.getStatus() >= 400) {
                return JsonApiResponse.error("Error while sending request to " + moduleName, Json.object("status_code", response.getStatus(), "content", response.getContentAsString()));
            }
            if (response.getContent().length > 0) {
                if (response.getContent()[0] == '{' || response.getContent()[0] == '[') {
                    res = JsonApiResponse.result(
                            Json.read(response.getContentAsString()),
                            Json.object("status_code", response.getStatus())
                    );
                } else {
                    res = JsonApiResponse.result(Json.object("value", response.getContentAsString()),
                            Json.object("status_code", response.getStatus())
                    );
                }
            } else {
                res = JsonApiResponse.result(null,
                        Json.object("status_code", response.getStatus())
                );
            }
        } catch (UnsupportedOperationException e) {
            logger.error("Malformed json", e);
            res = JsonApiResponse.error(e.getMessage());
        } catch (Exception e) {
            if (!quiet) {
                logger.error("Error while sending request to " + moduleName);
            }
            if (e.getCause() instanceof ConnectException) {
                res = JsonApiResponse.error("Connection error with " + moduleName);
            }
        }
        return res;
    }

    /**
     * Adds the parameters to the request
     * @param params parameters to add
     * @param request request to add parameters
     */
    private static void addParamsToRequest(Json params, Request request) {
        if (params==null) { return ;}

        for (Map.Entry<String, Json> param : params.asJsonMap().entrySet()) {
            if (param.getValue().isString()) {
                request.param(param.getKey(), param.getValue().asString());
            } else {
                request.param(param.getKey(), param.getValue().toString());
            }
        }
    }

    /**
     * Downloads a file from the given endpoint.
     *
     * @param endpoint endpoint to fetch the file
     * @param params   parameters needed for the fetch
     * @return a Json Object with the fields : {"Content-Type":"...", "Content-disposition":"...", "Content":"..."}
     * @throws Exception if it couldn't fetch the file from the module
     */
    public Json downloadFile(String endpoint, Json params) throws Exception {
        return downloadFile(HttpMethod.POST, endpoint, params.toString());
    }

    /**
     * Downloads a file from the given endpoint.
     *
     * @param endpoint endpoint to fetch the file
     * @param body     parameters needed for the fetch
     * @return a Json Object with the fields : {"Content-Type":"...", "Content-disposition":"...", "Content":"..."}
     * @throws Exception if it couldn't fetch the file from the module
     */
    public Json downloadFile(HttpMethod method, String endpoint, String body) throws Exception {
        String URI = url + endpoint.replace(" ", "%20");

        Json responseJson = Json.object();
        // Create a POST request to the server
        Request request = httpClient.newRequest(URI)
                .method(method)
                .content(new StringContentProvider(body));

        // Send the request and get the response
        InputStreamResponseListener listener = new InputStreamResponseListener();
        request.send(listener);

        Response response = listener.get(5, TimeUnit.SECONDS); // Wait for the response
        if (response.getHeaders().containsKey("Content-Type")) {
            responseJson.at("Content-Type", response.getHeaders().getField("Content-Type").getValue());
        }
        if (response.getHeaders().containsKey("Content-disposition")) {
            responseJson.at("Content-disposition", response.getHeaders().getField("Content-disposition").getValue());
        }
        String strResponse = "";
        // Check if the response status is OK (200)
        if (response.getStatus() == 200) {
            try (InputStream inputStream = listener.getInputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    strResponse += new String(buffer);
                }
                responseJson.at("Content", strResponse);
                logger.info("Successfully downloaded file.");
            }
        } else {
            responseJson.at("success", "false");
            responseJson.at("error", "Failed to download the file");
            logger.error("Failed to download file: {}", response.getStatus());
        }
        return responseJson;
    }
}