package com.csl.web.websockets;

import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

@SuppressWarnings("serial")
public class WebSocketServlet extends JettyWebSocketServlet {
    Class clazz;

    @Override
    public void configure(JettyWebSocketServletFactory factory) {
        factory.register(clazz);
    }

    public WebSocketServlet(Class clazz) {
        super();
        this.clazz = clazz;
    }
}
