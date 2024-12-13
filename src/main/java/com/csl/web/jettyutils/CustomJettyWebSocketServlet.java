package com.csl.web.jettyutils;

import com.csl.core.Config;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
import  org.eclipse.jetty.websocket.server.JettyWebSocketServlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;

import static com.csl.web.HTTPConstants.JSON_FORMAT;


public class CustomJettyWebSocketServlet extends JettyWebSocketServlet {
    private final Class<?> handler;

    public CustomJettyWebSocketServlet(Class<?> handler) {
//        super();
        this.handler = handler;
    }
    @Override
    public void configure(JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofSeconds(Config.getInstance().server.getWebsocketTimeout()));
        factory.register(handler);
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().write("GET method is supported for " + req.getRequestURI());
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//        String body = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);

        resp.setContentType(JSON_FORMAT);
        resp.getWriter().write("POST method is supported for "   + req.getRequestURI());
    }
}