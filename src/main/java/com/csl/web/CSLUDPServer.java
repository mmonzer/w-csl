package com.csl.web;

import com.csl.core.Config;
import com.csl.logger.CSLNetworkLogger;
import com.csl.udp.CSLFlowManager;
import com.csl.util.NetUtil;
import com.ucsl.interfaces.ICSLFlowListener;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;

/**
 * This class uses the ICRoute interface to create void routes.
 * The response for an ICRoute is rendered in an after-filter.
 */
public class CSLUDPServer {
    /**
     * Logger of the UDP server
     */
    private static final Logger logger = LoggerFactory.getLogger(CSLUDPServer.class);
    /**
     * Flow manager of the UDP server
     */
    private CSLFlowManager flowManager;
    /**
     * Max size for the packets in UDP server
     */
    private int maxsize;
    /**
     * Max number of flows in UDP server
     */
    private int maxflows;
    /**
     * Listing ip of the UDP server
     */
    private String ip = "127.0.0.1";
    /**
     * Listing port of the UDP server
     */
    private int port = -1;
    /**
     * Whether the server is initialized
     */
    private boolean initialized = false;
    /**
     * Whether the server has started
     */
    @Setter
    @Getter
    private boolean started = false;

    /**
     * Datagram socket
     */
    DatagramSocket dsocket = null;

    /**
     * Initialization of the UDP server
     *
     * @param config configuration for the UDP server
     */
    public void initUDPServer(Config.UdpServerConf config) {
        if (config == null) return;

        boolean on = config.getOn();
        if (!on) return;


        if (initialized) {
            logger.error("already initialized");
            System.exit(0);
        }

        if (!config.getOn()) return;

        initialized = true;

        maxflows = config.getMaxInputQueues();
        maxsize = config.getMaxSizeOfInputQueues();

        port = config.getPort();
        ip = config.getIp();
        if (ip.isEmpty()) {
            ip = NetUtil.findIPAddress();
        }
        if (ip.isEmpty()) {
            ip = "127.0.0.1";
        }

        int port = getCurrentPortForUDP();
        setCurrentPortForUDP(port);

        CSLNetworkLogger.info(logger, "TAP", "UDP","UDP server initialized at "+ip+":"+ port);
    }

    /**
     * Getter of the listening port of the UDP server
     */
    public int getCurrentPortForUDP() {
        return port;
    }

    /**
     * Getter of the listening IP of the UDP server
     */
    public String getCurrentIPForUDP() {
        return ip;
    }

    /**
     * Setter of the listening port of UDP server
     *
     * @param currentPortForUDP new port for listening
     */
    private void setCurrentPortForUDP(int currentPortForUDP) {
        port = currentPortForUDP;
    }

    /**
     * Start the UDP server
     */
    public void start() {

        if (!initialized) {
            CSLNetworkLogger.info(logger, "TAP", "UDP","CSL UDP server not initialized at "+ip+":"+ port);
            System.exit(0);
        }

        getFlowManager().init();
        getFlowManager().startListener();

        CSLNetworkLogger.info(logger, "TAP", "UDP","UDP server listening at "+ip+":"+ port);
    }

    /**
     * Stop the UDP server
     */
    public void stop() {

        if (dsocket != null) dsocket.close();
        getFlowManager().stopListener();

        CSLNetworkLogger.info(logger, "TAP", "UDP","UDP server stopped listening at "+ip+":"+ port);
    }

    /**
     * Getter of the flow manager of the UDP server
     *
     * @return the flow manager of the server
     */
    private CSLFlowManager getFlowManager() {

        if (!initialized) {
            logger.error("CSL UDP server not initialized, cannot start");
            System.exit(0);
        }

        if (flowManager == null) {

            flowManager = new CSLFlowManager(maxflows, maxsize);
        }
        return flowManager;
    }

    /**
     * Add listener to UDP server
     *
     * @param queueNumber  number of packets to queue
     * @param flowListener flow listener of the UDP server
     */
    public void addListener(int queueNumber, ICSLFlowListener flowListener) {

        getFlowManager().addListener(queueNumber, flowListener);
    }
}
