package com.csl.intercom.cslscan;

import com.csl.autocrypt.IJsonApeResponseToJsonApiResponse;
import com.csl.core.Config;
import com.ucsl.interfaces.IVoidToJsonApiResponse;
import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;
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
import java.util.concurrent.TimeUnit;

/**
 * Class to handle communication for API client.
 */
public class ApiHandler implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ApiHandler.class);
    private final String moduleName;
    @Getter @Setter
    protected HashMap<HttpHeader, String> headers = new HashMap<>();
    protected HttpClient httpClient;
    private IJsonApeResponseToJsonApiResponse outputReformer = (e) -> e;
    private boolean useSSL = false;
    private int port = 80;
    private String ip = "localhost";
    private String uriCommonPath = "";

//    /**
//     * General constructor
//     *
//     * @param nameModule nameof the module
//     * @param url        url of the service api
//     */
//    public ApiHandler(String nameModule, String url, boolean useSSL) {
//        this.moduleName = nameModule;
//        this.url = url;
//        headers.put(HttpHeader.CONTENT_TYPE, "application/json");
//        httpClient = initClient();
//        this.useSSL = useSSL;
//
//        try {
//            logger.info("Connecting with {} ...", moduleName);
//            httpClient.start();
//            logger.info("Connection successful with {}", moduleName);
//        } catch (Exception e) {
//            logger.error("Could not start the http client for {} API.", nameModule, e);
//        }
//    }

    /**
     * General constructor
     *
     * @param nameModule nameof the module
     * @param url        url of the service api
     */
    public ApiHandler(String nameModule, String url) {
        this(nameModule, url, false);
    }

    /**
     * General constructor
     */
    public ApiHandler(String nameModule, String ip, int port, boolean useSSL) {
//        this(nameModule, createUrl(ip, port, useSSL));
        this.moduleName = nameModule;
        this.ip = ip;
        this.port = port;
        this.useSSL = useSSL;
        headers.put(HttpHeader.CONTENT_TYPE, "application/json");
        httpClient = initClient();

        try {
            logger.debug("Connecting with {} ...", moduleName);
            httpClient.start();
            logger.info("Connection successful with {}", moduleName);
        } catch (Exception e) {
            logger.error("Could not start the http client for {} API.", nameModule, e);
        }
    }

    /**
     * General constructor
     */
    public void testConnexion(IVoidToJsonApiResponse testMethod) {
        if (testMethod.apply().isSuccess()) {
            logger.info("Connection successfully established with {} : server is running", moduleName);
        } else {
            logger.warn("Connection could not be established with {} : server is probably not started", moduleName);
        }
    }

    /**
     * General constructor
     */
    public ApiHandler(String nameModule, String ip, boolean useSSL) {
        this(nameModule, ip, useSSL?443:80, useSSL);
    }

    // region initialize client

    /**
     * Initialize the httpClient
     * @return the new client
     */
    protected HttpClient initClient() {
        if (useSSL) {
            return initSSLApiHandler();
        }
        logger.info("Initialized HTTP client for {}", moduleName);
        return new HttpClient();
    }

    /**
     * Ensures the connexion with SSL with the Dbapi.
     * Return the new client well configured
     */
    private HttpClient initSSLApiHandler(){
        // Retrieve system properties
        String trustStorePath = System.getProperty("javax.net.ssl.trustStore");
        String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");

        // Ensure the properties are set
        if (trustStorePath == null || trustStorePassword == null) {
            logger.info("Initialized CA-signed HTTPS client for {}", moduleName);
            return new HttpClient();
//            throw new IllegalStateException("Trust store properties are not set.");
        }

        // Configure SslContextFactory with the retrieved properties
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustStorePath(trustStorePath);
        sslContextFactory.setTrustStorePassword(trustStorePassword);
        sslContextFactory.setTrustAll(true);

        logger.info("Initialized self-signed HTTPS client for {}", moduleName);
        return new HttpClient(sslContextFactory);
    }

    // endregion initialize client

    @Override
    public void close() throws Exception {
        try {
            logger.debug("Disconnecting from {} ...", moduleName);
            this.httpClient.stop();
            logger.info("Disconnected successfully from {} ...", moduleName);
        } catch (Exception e) {
            logger.error("Could not stop the {} HTTP client.", moduleName, e);
        }
    }

    // region create uri

    /**
     * Creates a http/https client's url from the ip, port and whether we use ssl or not
     * @param ip ip of the http server
     * @param port port of the http server
     * @param useSSL whether the connexion uses SSL
     * @return the url
     */
    private static String createBaseUrl(String ip, int port, boolean useSSL) {
        if (useSSL && port==443) {
            return "https://"+ ip;
        }
        if (!useSSL && port==80) {
            return "http://"+ ip;
        }
        return (useSSL ? "https://" : "http://") + ip + ":" + port;
    }
    /**
     * Get the url
     * @return the url
     */
    public String getUrl() {
        return createBaseUrl(ip, port,useSSL)+ uriCommonPath;
    }

    /**
     * Create the custom uri for the request
     * @param endpoint endpoint for the request
     * @return the full uri of the request
     */
    public String createUriFrom(String endpoint) {
        return getUrl() + endpoint.replace(" ", "%20").replace(":", "%3A");
    }

    public void setCustomHeaders(String contentType) {
        /**
         * Add the api key to the headers
         * @param contentType content type of the request
         */
//        String apiKey = JsonUtil.getStringFromJson(CSLContext.instance.getConfig().get("global"), "api_key", "");
        String apiKey = Config.instance.Client.getApiKey();
        HashMap<HttpHeader, String> customHeaders = new HashMap<>();
        customHeaders.put(HttpHeader.AUTHORIZATION, "Api-Key " + apiKey);
        customHeaders.put(HttpHeader.CONTENT_TYPE, contentType);
        setHeaders(customHeaders);
    }
    // endregion create uri

    /**
     * Adds a common path to a api url : for dbapi /api
     *
     * @param uriCommonPath common path
     */
    public void addUriCommonPath(String uriCommonPath) {
        this.uriCommonPath = uriCommonPath;
    }

    // region customize headers of the request

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

    // endregion customize headers of the request

    // region create request

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
        return createRequest(method.toString(), endpoint, params, body);
    }

    /**
     * Creates the request with the custom parameters
     *
     * @param method   http method to use : GET POST PUT DELETE
     * @param uri      uri to send the request
     * @param params   parameters of the request
     * @param body     body of the request
     * @return the request created
     */
    public Request createRequest(String method, String uri, Json params, Json body) {
        logger.trace("Creating request {} to {} : params : {} body : {}", method, uri, params, body);
        Request request = initRequest(method, uri, httpClient);
        fillRequest(request, params, body);
        logger.trace("Sending request {} to {}", method, uri);
        return request;
    }

    /**
     * Initialize a request with the headers
     *
     * @param method method of the request
     * @param endpoint endpoint of the request
     */
    protected Request initRequestWithHeaders(String method, String endpoint) {
        Request request = initRequest(method, createUriFrom(endpoint), httpClient);
        addHeadersToRequest(headers, request);
        return request;
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

    // endregion create request

    // region send request

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
        return sendRequestToApi(method.toString(), endpoint, params, null, true);
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
            ContentResponse response = createRequest(method, createUriFrom(endpoint), params, body).send();
            res = parseResponse(response, moduleName);
        } catch (UnsupportedOperationException e) {
            logger.error("Malformed json", e);
            res = JsonApiResponse.error("Malformed json : " + e.getMessage());
        } catch (Exception e) {
            if (!quiet) {
                logger.error("Error while sending request to {}  at {} with params : {}", moduleName, endpoint, params);
            }
            res = JsonApiResponse.error("exception when sending request to "+moduleName+" : " + e.getMessage());
            if (e.getCause() instanceof ConnectException) {
                res = JsonApiResponse.error("Connection error with " + moduleName);
            }
        }

        return outputReformer.apply(res);
    }

    // endregion send request


    // TODO : doPost instead sendPost

    // region handlers for  GET, POST, PUT, DELETE, PATCH

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
     * Send a HTTP GET request to the scanner.
     *
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    public JsonApiResponse sendGet(String endpoint, Json params, boolean quiet) {
        return sendRequestToApi(HttpMethod.GET, endpoint, params, null, quiet);
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

    // endregion handlers for  GET, POST, PUT, DELETE, PATCH

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
            logger.error("Error while sending request to {} : status_code : {} content: {}", moduleName, response.getStatus(), response.getContentAsString());
            return JsonApiResponse.error("Error while sending request to " + moduleName, Json.object("status_code", response.getStatus(), "content", response.getContentAsString()));
        }
        if (response.getContent().length > 0) {
            if (response.getContent()[0] == '{' || response.getContent()[0] == '[') {
                logger.trace("Successful request : parsing json response : {}", response.getContent());
                parsedResponse = JsonApiResponse.result(
                        Json.read(response.getContentAsString()),
                        Json.object("status_code", response.getStatus())
                );
            } else {
                logger.trace("Successful request : parsing plain text response : {}", response.getContent());
                parsedResponse = JsonApiResponse.result(Json.object("value", response.getContentAsString()),
                        Json.object("status_code", response.getStatus())
                );
            }
        } else {
            logger.trace("Successful request : parsing empty response : {}", response.getStatus());
            parsedResponse = JsonApiResponse.result(null,
                    Json.object("status_code", response.getStatus())
            );
        }
        logger.trace("Successful request : successfully parsed response : {}", parsedResponse.getResult());
        return parsedResponse;
    }

    // endregion -- static methods


    // region post, get file, octect-stream/multipart, http stream, ...

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

    // endregion post, get file, octect-stream/multipart, http stream, ...

////////////////////////////////////////////////////////////////////////////////


    /**
     * Add callback for cleaning output
     *
     * @param callback callbacks that cleans output.
     */
    public void addOutputReformer(IJsonApeResponseToJsonApiResponse callback) {
        this.outputReformer = callback;
    }
}