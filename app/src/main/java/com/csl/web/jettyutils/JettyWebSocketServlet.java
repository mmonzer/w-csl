package com.csl.web.jettyutils;

import com.ucsl.json.Json;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JettyWebSocketServlet extends WebSocketServlet {
    private final Class<?> handler;
    private static final Logger logger = LoggerFactory.getLogger(JettyServerErrorHandler.class);

    public JettyWebSocketServlet(Class<?> handler) {
        super();
        this.handler = handler;
    }
    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(10000);
        factory.register(handler);
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().write("GET method is supported for /cmd");
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
        System.out.println("Before cmd");
        System.out.println(body);

        resp.setContentType("application/json");
        resp.getWriter().write("{\"status\":\"POST method is supported for /cmd\"}");
    }
}
