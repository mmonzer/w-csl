package com.csl.intercom.cslscan;

import com.csl.autocrypt.ICleaner;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.MultiPartContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Class to handle communication for API client.
 */
public class ApiHandler implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ApiHandler.class);
    private final String moduleName;
    protected final String url;
    protected final HashMap<HttpHeader, String> headers = new HashMap<>();
    protected HttpClient httpClient;
    private ICleaner outputCleaner = (e) -> e;

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
        ensureSSLDbApiHandlerInitialization();
        this.url = url;
        this.moduleName = nameModule;
        headers.put(HttpHeader.CONTENT_TYPE, "application/json");

        try {
            httpClient.start();
        } catch (Exception e) {
            logger.error("Could not start the http client for " + nameModule + " API.", e);
        }
    }
    private void ensureSSLDbApiHandlerInitialization(){
        // Retrieve system properties
        String trustStorePath = System.getProperty("javax.net.ssl.trustStore");
        String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");

        // Ensure the properties are set
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustStorePath(trustStorePath);
        sslContextFactory.setTrustStorePassword(trustStorePassword);
        httpClient = new HttpClient(sslContextFactory);
    }

    @Override
    public void close() throws Exception {
        try {
        this.httpClient.stop();}
        catch (Exception e) {
            logger.error("Could not stop the {} HTTP client.", moduleName, e);
        }
    }

    /**
     * Adds a header to the api request
     *
     * @param header   header name
     * @param newValue header new value
     */
    public void addHeader(HttpHeader header, String newValue) {
        headers.put(header, newValue);
    }

    /**
     * Adds a header to the api request
     *
     * @param apiKey apiKey for the connection
     */
    public void setApiKey(String apiKey) {
        addHeader(HttpHeader.AUTHORIZATION, "Api-Key " + apiKey);
    }

    /**
     * Send an HTTP request to the scanner.
     *
     * @param method   The HTTP method to use (GET, POST, PUT, ...)
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @param body     The body to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    private JsonApiResponse sendRequestToApi(HttpMethod method, String endpoint, Json params, Json body, boolean quiet) {
        return sendRequestToApi(method.toString(), endpoint, params, body, quiet);
    }

    /**
     * Send an HTTP request to the scanner.
     *
     * @param method   The HTTP method to use (GET, POST, PUT, ...)
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @param body     The body to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    private JsonApiResponse sendRequestToApi(String method, String endpoint, Json params, Json body, boolean quiet) {
        JsonApiResponse res = JsonApiResponse.error(null);

        try {
            ContentResponse response = sendRequest(method, endpoint, params, body);
            res = parseResponse(response, moduleName);
        } catch (UnsupportedOperationException e) {
            logger.error("Malformed json", e);
            res = JsonApiResponse.error("Malformed json : " + e.getMessage());
        } catch (Exception e) {
            if (!quiet) {
                logger.error("Error while sending request to " + moduleName);
            }
            if (e.getCause() instanceof ConnectException) {
                res = JsonApiResponse.error("Connection error with " + moduleName);
            }
            res = JsonApiResponse.error("exception : " + e.getMessage());
        }

        return outputCleaner.clean(res);
    }

    protected ContentResponse sendRequest(String method, String endpoint, Json params, Json body) throws InterruptedException, TimeoutException, ExecutionException {
        Request request = createRequest(method, endpoint, params, body);
        ContentResponse response = request.send();
        return response;
    }

    /**
     * Creates the request with the custom parameters
     *
     * @param method   http method to use : GET POST PUT DELETE
     * @param endpoint endpoint to send the request
     * @param params   parameters of the request
     * @param body     body of the request
     * @return the request created
     */
    private Request createRequest(HttpMethod method, String endpoint, Json params, Json body) {
        endpoint = endpoint.replace(" ", "%20").replace(":", "%3A");
        Request request = initRequest(method, url + endpoint, httpClient);
        return fillRequest(request, params, body);
    }

    /**
     * Creates the request with the custom parameters
     *
     * @param method   http method to use : GET POST PUT DELETE
     * @param endpoint endpoint to send the request
     * @param params   parameters of the request
     * @param body     body of the request
     * @return the request created
     */
    public Request createRequest(String method, String endpoint, Json params, Json body) {
        endpoint = endpoint.replace(" ", "%20").replace(":", "%3A");
        Request request = initRequest(method, url + endpoint, httpClient);
        return fillRequest(request, params, body);
    }

    /**
     * Fills the request with the custom information
     *
     * @param request   request to fill
     * @param params   parameters of the request
     * @param body     body of the request
     * @return the request created
     */
    private Request fillRequest(Request request, Json params, Json body) {
        addHeadersToRequest(headers, request);
        addParamsToRequest(params, request);
        if (headers.get(HttpHeader.CONTENT_TYPE).contains("json")) {
            addBodyToRequestJson(body, request);
        } else if (headers.get(HttpHeader.CONTENT_TYPE).contains("multipart")) {
            addBodyToRequestMultipart(body, request);
        }
        return request;
    }

    /**
     * Initialize a request with the headers
     *
     * @param method method of the request
     * @param endpoint endpoint of the request
     */
    protected Request initRequestWithHeaders(String method, String endpoint) {
        Request request = initRequest(method, url+endpoint, httpClient);
        addHeadersToRequest(headers, request);
        return request;
    }

    // region -- static methods

    /**
     * Creates the request
     *
     * @param method method of the new request
     * @param uri    uri of the new request
     * @param client client for the request
     * @return new request
     */
    protected static Request initRequest(String method, String uri, HttpClient client) {
        List<String> allowedMethods = List.of("GET","POST","PUT", "DELETE","PATCH");
        if (!allowedMethods.contains(method)) {
            throw new UnsupportedOperationException("Wrong http method : "+method);
        }
        Request request = client.newRequest(uri);
        request.method(method);
        return request;
    }

    /**
     * Creates the request
     *
     * @param method method of the new request
     * @param uri    uri of the new request
     * @param client client for the request
     * @return new request
     */
    private static Request initRequest(HttpMethod method, String uri, HttpClient client) {
        Request request = client.newRequest(uri);
        request.method(method);
        return request;
    }

    /**
     * Adds the parameters to the request
     *
     * @param params  parameters to add
     * @param request request to add parameters
     */
    private static void addParamsToRequest(Json params, Request request) {
        if (params == null) {
            return;
        }

        for (Map.Entry<String, Json> param : params.asJsonMap().entrySet()) {
            if (param.getValue().isString()) {
                request.param(param.getKey(), param.getValue().asString());
            } else {
                request.param(param.getKey(), param.getValue().toString());
            }
        }
    }

    /**
     * Adds the headers to the request
     *
     * @param headers headers to add
     * @param request request to add parameters
     */
    protected static void addHeadersToRequest(HashMap<HttpHeader, String> headers, Request request) {
        if (headers == null) {
            return;
        }

        for (Map.Entry<HttpHeader, String> param : headers.entrySet()) {
            request.header(param.getKey().toString(), param.getValue());
        }
    }

    /**
     * Adds the body to the request json
     *
     * @param body    body of the request
     * @param request request to add parameters
     */
    private static void addBodyToRequestJson(Json body, Request request) {
        if (body != null) {
            if (request.getMethod().equals(HttpMethod.POST.toString()) ||
                    request.getMethod().equals(HttpMethod.PUT.toString()) ||
                    request.getMethod().equals(HttpMethod.DELETE.toString())) {
                request.content(new StringContentProvider(body.toString()), "application/json");
            } else if (!request.getMethod().equals(HttpMethod.GET.toString())) {
                throw new UnsupportedOperationException("Unsupported HTTP method: " + request.getMethod());
            }
        }
    }

    /**
     * Adds the body to the request multipart
     *
     * @param body    body of the request
     * @param request request to add parameters
     */
    private static void addBodyToRequestMultipart(Json body, Request request) {
        if (body != null) {
            MultiPartContentProvider multiPart = new MultiPartContentProvider();
            for (Map.Entry<String, Json> e : body.asJsonMap().entrySet()) {
                multiPart.addFieldPart(e.getKey(), new StringContentProvider(e.getValue().toString()), null);
            }
            multiPart.close();
            request.content(multiPart);
        }
    }

    /**
     * Parses the response of the request
     *
     * @param response response of the request
     * @return parsed response.
     */
    private static JsonApiResponse parseResponse(ContentResponse response, String moduleName) {
        JsonApiResponse parsedResponse;
        if (response.getStatus() >= 400) {
            return JsonApiResponse.error("Error while sending request to " + moduleName, Json.object("status_code", response.getStatus(), "content", response.getContentAsString()));
        }
        if (response.getContent().length > 0) {
            if (response.getContent()[0] == '{' || response.getContent()[0] == '[') {
                parsedResponse = JsonApiResponse.result(
                        Json.read(response.getContentAsString()),
                        Json.object("status_code", response.getStatus())
                );
            } else {
                parsedResponse = JsonApiResponse.result(Json.object("value", response.getContentAsString()),
                        Json.object("status_code", response.getStatus())
                );
            }
        } else {
            parsedResponse = JsonApiResponse.result(null,
                    Json.object("status_code", response.getStatus())
            );
        }
        return parsedResponse;
    }

    // endregion -- static methods

    /**
     * Downloads a file from the given endpoint (from POST method)
     *
     * @param endpoint endpoint to fetch the file
     * @param body     body needed for the fetch
     * @return a Json Object with the fields : {"Content-Type":"...", "Content-disposition":"...", "Content":"..."}
     * @throws Exception if it couldn't fetch the file from the module
     */
    public JsonApiResponse downloadFilePost(String endpoint, Json body) throws Exception {
        return downloadFile(HttpMethod.POST, endpoint, null, body);
    }

    /**
     * Downloads a file from the given endpoint. (from GET method)
     *
     * @param endpoint endpoint to fetch the file
     * @param params   parameters needed for the fetch
     * @return a Json Object with the fields : {"Content-Type":"...", "Content-disposition":"...", "Content":"..."}
     * @throws Exception if it couldn't fetch the file from the module
     */
    public JsonApiResponse downloadFileGet(String endpoint, Json params) throws Exception {
        return downloadFile(HttpMethod.GET, endpoint, params, null);
    }

    /**
     * Downloads a file from the given endpoint.
     *
     * @param endpoint endpoint to fetch the file
     * @param body     parameters needed for the fetch
     * @return a Json Object with the fields : {"Content-Type":"...", "Content-disposition":"...", "Content":"..."}
     */
    public JsonApiResponse downloadFile(HttpMethod method, String endpoint, Json params, Json body) {
        endpoint = endpoint.replace(":", "%3A").replace(" ", "%20");
        Request request = createRequest(method, endpoint, params, body);
        request.header("Accept", "application/octet-stream");

        // Send the request and get the response async
        InputStreamResponseListener listener = new InputStreamResponseListener();
        request.send(listener);

        JsonApiResponse responseApi = JsonApiResponse.error(null);
        try {
            Response response = listener.get(5, TimeUnit.SECONDS); // Wait for the response
            responseApi = parseStreamResponse(response, listener);
        } catch (IOException e) {
            logger.error("Error while reading the response from {}", moduleName);
            responseApi = JsonApiResponse.error("File reading error from connection to " + moduleName);
        } catch (Exception e) {
            logger.error("Error while sending request to " + moduleName);
            if (e.getCause() instanceof ConnectException) {
                responseApi = JsonApiResponse.error("Connection error with " + moduleName);
            }
        }
        return responseApi;
    }

    /**
     * Parse stream response from http
     *
     * @param response response
     * @param listener listener of the response
     * @return the string read from the http stream
     * @throws IOException if response could not be read
     */
    private JsonApiResponse parseStreamResponse(Response response, InputStreamResponseListener listener) throws IOException {
        Json responseJson = Json.object();
        if (response.getHeaders().containsKey("Content-Type")) {
            responseJson.at("Content-Type", response.getHeaders().getField("Content-Type").getValue());
        }
        if (response.getHeaders().containsKey("Content-disposition")) {
            responseJson.at("Content-disposition", response.getHeaders().getField("Content-disposition").getValue());
        }
        // Check if the response status is OK (200)
        JsonApiResponse responseApi;
        if (response.getStatus() == 200) {
            try (InputStream inputStream = listener.getInputStream()) {
                responseJson.at("Content", readStream(inputStream));
                responseApi = JsonApiResponse.result(responseJson);
                logger.info("Successfully downloaded file.");
            }
        } else {
            responseApi = JsonApiResponse.error("Failed to download the file");
            logger.error("Failed to download file: {}", response.getStatus());
        }
        return responseApi;
    }

    /**
     * Read stream for http stream
     *
     * @param inputStream the stream to read
     * @return the read string
     * @throws IOException if first byte cannot be read
     */
    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder response = new StringBuilder();
        byte[] buffer = new byte[8192];
        while (inputStream.read(buffer) != -1) {
            response.append(new String(buffer));
        }
        return response.toString().trim();
    }

////////////////////////////////////////////////////////////////////////////////

    //* TODO : doPost instead sendPost

    /**
     * Send a DELETE HTTP request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendDelete(String endpoint, Json params) {
        return sendDelete( endpoint, params, null);
    }

    /**
     * Send a DELETE HTTP request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendDelete(String endpoint, Json params, Json body) {
        return sendRequestToApi(HttpMethod.DELETE, endpoint, params, body, false);
    }

    /**
     * Send a HTTP GET request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendGet(String endpoint, Json params) {
        return sendRequestToApi(HttpMethod.GET, endpoint, params, null, false);
    }

    /**
     * Send a HTTP POST request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param body     The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendPostFile(String endpoint, Json params, Json body) {
        addHeader(HttpHeader.CONTENT_TYPE, "multipart/form-data");
        JsonApiResponse response = sendRequestToApi(HttpMethod.POST, endpoint, params, body, false);
        addHeader(HttpHeader.CONTENT_TYPE, "application/json");
        return response;
    }

    /**
     * Send a HTTP POST request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param body     The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendPost(String endpoint, Json params, Json body) {
        return sendRequestToApi(HttpMethod.POST, endpoint, params, body, false);
    }

    /**
     * Send a HTTP POST request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param body     The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendPost(String endpoint, Json body) {
        return sendPost( endpoint, null, body);
    }

    /**
     * Send a HTTP PUT request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param body     The body to send, if any (if not, should be an empty {@link Json} object, not null).
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
     * @param body     The body to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendPut(String endpoint, Json body) {
        return sendPut(endpoint, null, body);
    }

    /**
     * Send an HTTP PATCH request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param body     The body to send, if any (if not, should be an empty {@link Json} object, not null).
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendPatch(String endpoint, Json params, Json body) {
        return sendRequestToApi("PATCH", endpoint, params, body, false);
    }

    /**
     * Send an HTTP PATCH request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param body     The body to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendPatch(String endpoint, Json body) {
        return sendPatch( endpoint, null, body);
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
        // TODO : change
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
        // TODO : remove
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
     * Add callback for cleaning output
     *
     * @param cleaner callbacks that cleans output.
     */
    public void addCleaner(ICleaner cleaner) {
        this.outputCleaner = cleaner;
    }
}