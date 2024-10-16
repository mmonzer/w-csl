package com.csl.web.jettyutils;

import com.csl.core.Config;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.csl.web.HTTPConstants.JSON_FORMAT;

public class JettyWebSocketServlet extends WebSocketServlet {
    private final Class<?> handler;

    public JettyWebSocketServlet(Class<?> handler) {
        super();
        this.handler = handler;
    }
    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(Config.instance.Server.getWebsocketTimeout());
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
