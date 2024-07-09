package com.xcsl.miniserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.IApiGetHelp;
import com.ucsl.json.Json;
import org.eclipse.jetty.server.Request;

import javax.servlet.MultipartConfigElement;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class that manages the generic api.
 */
public class ApiHttpServer {
    boolean debug = false;
    HttpServer server = null;
    List apiNames = new ArrayList<>();
    List apiDescriptions = new ArrayList<>();
    private IApiGetHelp apiGetHelp;

    public ApiHttpServer createServer(InetSocketAddress inetAddress, List apis, IApiGetHelp apiGetHelp) {
        this.apiGetHelp = apiGetHelp;

        try {
            this.server = HttpServer.create(inetAddress, 0);
        } catch (IOException var6) {
            var6.printStackTrace();
            return null;
        }

        this.server.createContext("/test", new CustomHandler0());
        this.server.createContext("/apihelp", new CustomHandlerHelp());
        this.server.createContext("/", (httpExchange) -> {
            byte[] response = "API not found" .getBytes("UTF-8");
            httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            httpExchange.sendResponseHeaders(200, (long) response.length);
            OutputStream out = httpExchange.getResponseBody();
            out.write(response);
            out.close();
        });
        Iterator apiIterator = apis.iterator();

        while (apiIterator.hasNext()) {
            IApiCommands api = (IApiCommands) apiIterator.next();
            this.server.createContext("/" + api.getName(), new CustomHandlerApi(api));
            this.apiNames.add(api.getName());
            this.apiDescriptions.add(api.getDescription());
        }

        this.startServer();
        return this;
    }

    /**
     * Starts the server
     */
    public void startServer() {
        if (this.server != null) {
            this.server.start();
        }

        System.out.println("Miniserver started");
    }

    /**
     * Handler for the api test
     */
    class CustomHandler0 extends CustomHttpHandler {
        /**
         * Handler of the connexion.
         *
         * @param exchange {@link HttpExchange} contains the information of the connexion, in particular the request and the response
         * @throws IOException if cant get the content of the exchange.
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            System.out.println("req");
            URI requestURI = exchange.getRequestURI();
            this.printRequestInfo(exchange);
            System.out.println("Body:\n" + this.getContent(exchange));
            String query = exchange.getRequestURI().getQuery();
            String response = "This is the response at " + requestURI;
            exchange.sendResponseHeaders(200, (long) response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    /**
     * Handler for the api
     */
    class CustomHandlerApi extends CustomHttpHandler {
        private IApiCommands api;

        private final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement("./tmp");

        public CustomHandlerApi(IApiCommands api) {
            super();
            this.api = api;
        }

        /**
         * Generic method that POST the given body at the given api endpoint
         *
         * @param api  endpoint of the api
         * @param body body to post
         * @return the result of the execution of the given function
         */
        private Json execPostCommand(IApiCommands api, String body) {
            Json data = Json.read(body);
            Json cmd = data.get("cmd");
            Json params = data.get("params");
            if (cmd == null) {
                System.out.println("Invalid jcmd:" + cmd);
                return Json.object();
            }

            if (params == null) {
                params = Json.object();
            }

            return api.exec(cmd.asString(), params);
        }

        /**
         * Handler of the connexion.
         *
         * @param exchange {@link HttpExchange} contains the information of the connexion, in particular the request and the response
         * @throws IOException if cant get the content of the exchange.
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            if (requestMethod.equals("POST")) {
                handlePOST(exchange);
            }
        }

        /**
         * Handler of the POST connexions
         *
         * @param exchange {@link HttpExchange} contains the information of the connexion, in particular the request and the response
         * @throws IOException if cant get the content of the exchange.
         */
        private void handlePOST(HttpExchange exchange) throws IOException {
            if (exchange.getRequestHeaders().get("Content-type").get(0).contains("application/json")) {
                handlePOST_json(exchange);
            } else if (exchange.getRequestHeaders().get("Content-type").get(0).contains("multipart/form-data")) {
                handlePOST_multipart(exchange);
                // TODO : upload file
            }
        }

        /**
         * Handler of the connexion for POST methods and multipart data (upload files)
         *
         * @param exchange {@link HttpExchange} contains the information of the connexion, in particular the request and the response
         * @throws IOException if cant get the content of the exchange.
         */
        private void handlePOST_multipart(HttpExchange exchange) throws IOException {
            if (ApiHttpServer.this.debug) {
                this.printRequestInfo(exchange);
            }

            exchange.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);

            String rawBody = this.getContent(exchange);
            if (ApiHttpServer.this.debug) {
                System.out.println("Body:\n" + rawBody);
            }

            String body="";
            if (exchange.getRequestHeaders().containsKey("Content-Type")) {
                // String boundary = exchange.getRequestHeaders().get("Content-Type").get(0).split("boundary=")[1];
                String boundary = rawBody.split("\n")[0].substring(2);
                body = parseMultipart(rawBody, boundary).toString();
            }

            Json responseJson = this.execPostCommand(this.api, body);

            String response = responseJson.toString();
            if (responseJson.has("Content-Type")) {
                exchange.setAttribute("Content-Type", responseJson.get("Content-Type").asString());
            }
            if (responseJson.has("Content-disposition")) {
                exchange.setAttribute("Content-disposition", responseJson.get("Content-disposition").asString());
            }
            if (responseJson.has("Content")) {
                response = responseJson.get("Content").asString();
            }
            exchange.sendResponseHeaders(200, (long) response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        /**
         * Handler of the connexion for POST methods and json input
         *
         * @param exchange {@link HttpExchange} contains the information of the connexion, in particular the request and the response
         * @throws IOException if cant get the content of the exchange.
         */
        private void handlePOST_json(HttpExchange exchange) throws IOException {
            if (ApiHttpServer.this.debug) {
                this.printRequestInfo(exchange);
            }

            String body = this.getContent(exchange);
            if (ApiHttpServer.this.debug) {
                System.out.println("Body:\n" + body);
            }

            Json responseJson = this.execPostCommand(this.api, body);
            String response = responseJson.toString();
            if (responseJson.has("Content-Type")) {
                exchange.setAttribute("Content-Type", responseJson.get("Content-Type").asString());
            }
            if (responseJson.has("Content-disposition")) {
                exchange.setAttribute("Content-disposition", responseJson.get("Content-disposition").asString());
            }
            if (responseJson.has("Content")) {
                response = responseJson.get("Content").asString();
            }
            exchange.sendResponseHeaders(200, (long) response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        /**
         * Get the content of the request
         *
         * @param body     the raw body of the request
         * @param boundary the boundary of the multipart request
         * @return the content of the request
         */
        protected Json parseMultipart(String body, String boundary) {
            String[] parts = body.split("--" + boundary);
            Json query = Json.object();
            String content;
            String headers;
            String name;
            String name1;
            for (String part : parts) {
                part = part.trim();
                name = "";
                name1 = "";
                content = "";
                headers = "";
                if (!part.isEmpty() && !part.equals("--")) {
                    content = part.substring(part.indexOf("\n\n") + 2);
                    headers = part.substring(0, part.indexOf("\n\n"));
                    name1 = headers.substring(headers.indexOf("name=\"") + 6);
                    if (headers.contains("application/octet-stream")) {
                        name = name1.substring(0, name1.indexOf("\";"));
                    } else {
                        name = name1.substring(0, name1.indexOf("\""));
                    }
                    query.at(name, content);
                }
            }
            // Reformating
            Json params = query.get("params");
            if (params != null) {
                query.delAt("params");
                query.at("params", Json.read(params.asString()));
            }
            Json file = query.get("file");
            if (file != null) {
                Json par = query.get("params");
                if (par==null) { par=Json.object();}
                par.at("file", file.asString());
                query.at("params", par);
                query.delAt("file");
            }
            return query;
        }
    }

    /**
     * Handler for the api help
     */
    class CustomHandlerHelp extends CustomHttpHandler {
        /**
         * Handler of the connexion.
         *
         * @param exchange {@link HttpExchange} contains the information of the connexion, in particular the request and the response
         * @throws IOException if cant get the content of the exchange.
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            if (ApiHttpServer.this.debug) {
                this.printRequestInfo(exchange);
            }

            String query = exchange.getRequestURI().getQuery();
            Json jj = Json.object();
            if (query != null) {
                String[] tokens = query.split(";");

                for (int i = 0; i < tokens.length; ++i) {
                    String token = tokens[i];
                    String name = "";
                    String value = "";
                    if (token.indexOf(61) >= 0) {
                        String[] z = token.split("=");
                        jj.set(z[0], (Object) z[1]);
                    } else {
                        name = token;
                    }

                    jj.set(name, (Object) value);
                }
            }

            String response = ApiHttpServer.this.apiGetHelp.getHelp(ApiHttpServer.this.apiNames, ApiHttpServer.this.apiDescriptions, jj);
            exchange.sendResponseHeaders(200, (long) response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    /**
     * Handler for the api
     */
    abstract class CustomHttpHandler implements HttpHandler {
        /**
         * Prints information of the exchange : RequestHeaders, Method, query, authentication, ...
         *
         * @param exchange {@link HttpExchange} of the current connexion to sho information
         */
        protected void printRequestInfo(HttpExchange exchange) {
            System.out.println("-- headers --");
            Headers requestHeaders = exchange.getRequestHeaders();
            Set headers = requestHeaders.entrySet();
            PrintStream var10001 = System.out;
            headers.forEach(var10001::println);
            System.out.println("-- principle --");
            HttpPrincipal principal = exchange.getPrincipal();
            System.out.println(principal);
            System.out.println("-- HTTP method --");
            String requestMethod = exchange.getRequestMethod();
            System.out.println(requestMethod);
            System.out.println("-- query --");
            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getQuery();
            System.out.println(query);
        }

        /**
         * Get the content of the request
         *
         * @param exchange {@link HttpExchange} of the current connexion
         * @return the content of the request
         * @throws IOException if the content cannot be read.
         */
        protected String getContent(HttpExchange exchange) throws IOException {
            BufferedReader httpInput = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), 32000);
            StringBuilder in = new StringBuilder();

            String input;
            while ((input = httpInput.readLine()) != null) {
                in.append(input).append("\n");
            }

            httpInput.close();
            return in.toString().trim();
        }

        /**
         * Handler of the connexion.
         *
         * @param exchange {@link HttpExchange} contains the information of the connexion, in particular the request and the response
         * @throws IOException if cant get the content of the exchange.
         */
        @Override
        public abstract void handle(HttpExchange exchange) throws IOException;
    }
}
