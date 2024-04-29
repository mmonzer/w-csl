package com.xcsl.miniserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.IApiGetHelp;
import com.ucsl.json.Json;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ApiHttpServer {
   boolean debug = false;
   HttpServer server = null;
   List apiNames = new ArrayList();
   List apiDescriptions = new ArrayList();
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
         byte[] response = "API not found".getBytes("UTF-8");
         httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
         httpExchange.sendResponseHeaders(200, (long)response.length);
         OutputStream out = httpExchange.getResponseBody();
         out.write(response);
         out.close();
      });
      Iterator var5 = apis.iterator();

      while(var5.hasNext()) {
         IApiCommands api = (IApiCommands)var5.next();
         this.server.createContext("/" + api.getName(), new CustomHandlerApi(api));
         this.apiNames.add(api.getName());
         this.apiDescriptions.add(api.getDescription());
      }

      this.startServer();
      return this;
   }

   public void startServer() {
      if (this.server != null) {
         this.server.start();
      }

      System.out.println("Miniserver started");
   }

   class CustomHandler0 implements HttpHandler {
      private IApiCommands api;

      private void printRequestInfo(HttpExchange exchange) {
         System.out.println("-- headers --");
         Headers requestHeaders = exchange.getRequestHeaders();
         Set var10000 = requestHeaders.entrySet();
         PrintStream var10001 = System.out;
         var10000.forEach(var10001::println);
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

      protected String getContent(HttpExchange exchange) throws IOException {
         BufferedReader httpInput = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
         StringBuilder in = new StringBuilder();

         String input;
         while((input = httpInput.readLine()) != null) {
            in.append(input).append(" ");
         }

         httpInput.close();
         return in.toString().trim();
      }

      public void handle(HttpExchange exchange) throws IOException {
         String requestMethod = exchange.getRequestMethod();
         System.out.println("req");
         URI requestURI = exchange.getRequestURI();
         this.printRequestInfo(exchange);
         System.out.println("Body:\n" + this.getContent(exchange));
         String query = exchange.getRequestURI().getQuery();
         String response = "This is the response at " + requestURI;
         exchange.sendResponseHeaders(200, (long)response.getBytes().length);
         OutputStream os = exchange.getResponseBody();
         os.write(response.getBytes());
         os.close();
      }
   }

   class CustomHandlerApi implements HttpHandler {
      private IApiCommands api;

      public CustomHandlerApi(IApiCommands api) {
         this.api = api;
      }

      private void printRequestInfo(HttpExchange exchange) {
         System.out.println("-- headers --");
         Headers requestHeaders = exchange.getRequestHeaders();
         Set var10000 = requestHeaders.entrySet();
         PrintStream var10001 = System.out;
         var10000.forEach(var10001::println);
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

      protected String getContent(HttpExchange exchange) throws IOException {
         BufferedReader httpInput = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
         StringBuilder in = new StringBuilder();

         String input;
         while((input = httpInput.readLine()) != null) {
            in.append(input).append(" ");
         }

         httpInput.close();
         return in.toString().trim();
      }

      private String execPostCommand(IApiCommands api, String body) {
         Json data = Json.read(body);
         Json cmd = data.get("cmd");
         Json params = data.get("params");
         if (cmd == null) {
            System.out.println("Invalid jcmd:" + cmd);
         }

         if (params == null) {
            params = Json.object();
         }

         String cresult = api.exec(cmd.asString(), params).toString();
         return cresult;
      }

      public void handle(HttpExchange exchange) throws IOException {
         String requestMethod = exchange.getRequestMethod();
         if (ApiHttpServer.this.debug) {
            this.printRequestInfo(exchange);
         }

         String body = this.getContent(exchange);
         if (ApiHttpServer.this.debug) {
            System.out.println("Body:\n" + body);
         }

         String response = this.execPostCommand(this.api, body);
         exchange.sendResponseHeaders(200, (long)response.getBytes().length);
         OutputStream os = exchange.getResponseBody();
         os.write(response.getBytes());
         os.close();
      }
   }

   class CustomHandlerHelp implements HttpHandler {
      private IApiCommands api;

      private void printRequestInfo(HttpExchange exchange) {
         System.out.println("-- headers --");
         Headers requestHeaders = exchange.getRequestHeaders();
         Set var10000 = requestHeaders.entrySet();
         PrintStream var10001 = System.out;
         var10000.forEach(var10001::println);
         System.out.println("-- principle --");
         HttpPrincipal principal = exchange.getPrincipal();
         System.out.println(principal);
         System.out.println("-- HTTP method --");
         String requestMethod = exchange.getRequestMethod();
         System.out.println(requestMethod);
         System.out.println("-- query --");
         URI requestURI = exchange.getRequestURI();
         String query = requestURI.getQuery();
      }

      protected String getContent(HttpExchange exchange) throws IOException {
         BufferedReader httpInput = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
         StringBuilder in = new StringBuilder();

         String input;
         while((input = httpInput.readLine()) != null) {
            in.append(input).append(" ");
         }

         httpInput.close();
         return in.toString().trim();
      }

      public void handle(HttpExchange exchange) throws IOException {
         String requestMethod = exchange.getRequestMethod();
         if (ApiHttpServer.this.debug) {
            this.printRequestInfo(exchange);
         }

         String query = exchange.getRequestURI().getQuery();
         Json jj = Json.object();
         if (query != null) {
            String[] tokens = query.split(";");

            for(int i = 0; i < tokens.length; ++i) {
               String token = tokens[i];
               String name = "";
               String value = "";
               if (token.indexOf(61) >= 0) {
                  String[] z = token.split("=");
                  jj.set(z[0], (Object)z[1]);
               } else {
                  name = token;
               }

               jj.set(name, (Object)value);
            }
         }

         String response = ApiHttpServer.this.apiGetHelp.getHelp(ApiHttpServer.this.apiNames, ApiHttpServer.this.apiDescriptions, jj);
         exchange.sendResponseHeaders(200, (long)response.getBytes().length);
         OutputStream os = exchange.getResponseBody();
         os.write(response.getBytes());
         os.close();
      }
   }
}
