package com.csl.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

public class NetUtil {

    public static String findIPAddress() {

        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

            for (NetworkInterface netint : Collections.list(nets)) {
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    if (inetAddress instanceof Inet4Address) {
                        String ip = inetAddress.getHostAddress();
                        if (ip.compareTo("127.0.0.1") != 0) return ip;
                    }
                }
            }
        } catch (SocketException e) {
        }

        return "";
    }
}
