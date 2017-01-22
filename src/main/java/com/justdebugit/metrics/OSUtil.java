package com.justdebugit.metrics;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * USER: fangjiahao
 * DATE: 2016/11/17
 * TIME: 15:43
 */
public class OSUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(OSUtil.class);

    private static volatile String hostName = null;

    public static String getHostName() {
        if (hostName == null) {
            synchronized (OSUtil.class) {
                if (hostName == null) {
                    hostName = retriveHostName();
                }
            }
        }
        return hostName;
    }

    public static List<String> getAllIpv4() {
        List<InetAddress> addresses = getAllAddresses();
        List<String> ips = new ArrayList<>(addresses.size());
        for (InetAddress address : addresses) {
            if (!(address instanceof Inet4Address)) {
                continue;
            }
            ips.add(address.getHostAddress());
        }
        return ips;
    }

    /**
     * Get host IP address
     *
     * @return IP Address
     */
    private static List<InetAddress> getAllAddresses() {
        List<InetAddress> allAddresses = new ArrayList<>();
        try {
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
                NetworkInterface netIf = interfaces.nextElement();
                if (netIf.isLoopback() || netIf.isVirtual() || !netIf.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = netIf.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    allAddresses.add(addresses.nextElement());
                }
            }
        } catch (SocketException e) {
            LOGGER.debug("Error when getting host ip address: <{}>.", e.getMessage());
        }
        return allAddresses;
    }

    private static InetAddress getLocalHost() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOGGER.debug("Error when getting local address: <{}>.", e.getMessage());
        }
        return null;
    }

    private static String retriveHostName() {
        String hostname = getHostNameFromEnv();
        if (StringUtils.isNotEmpty(hostname)) {
            return hostname;
        }
        hostname = getHostNameFromCommand();
        if (StringUtils.isNotEmpty(hostname)) {
            return hostname;
        }
        InetAddress address = getLocalHost();
        if (address != null) {
            hostname = address.getHostName();
        }
        return hostname;
    }

    private static String getHostNameFromEnv() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return System.getenv("COMPUTERNAME");
        } else if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_UNIX) {
            return System.getenv("HOSTNAME");
        }
        return "";
    }

    private static String getHostNameFromCommand() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return readFromCommand("hostname.exe");
        } else if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_UNIX) {
            return readFromCommand("hostname");
        }
        return "";
    }

    private static String readFromCommand(String command) {
        InputStream in = null;
        try {
            Process proc = Runtime.getRuntime().exec(command);
            in = proc.getInputStream();
            StringWriter writer = new StringWriter(100);
            IOUtils.copy(in, writer, Charset.defaultCharset());
            // 去除尾部换行符
            return writer.toString().trim();
        } catch (IOException e) {
            return "";
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
