package net.java.mproxy.util.network;

import net.java.mproxy.proxy.util.ExceptionUtil;
import net.java.mproxy.proxy.util.chat.Ints;
import net.java.mproxy.util.Sys;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;

public class NetworkUtil {
    private static final String[] masks = new String[33];
    private static final Map<String, Integer> maskMap = new HashMap<>();

    static {
        for (int i = 0; i <= 32; i++) {
            String mask = createMask(i);
            masks[i] = mask;
            maskMap.put(mask, i);
        }
    }

    private static String createMask(int prefix) {
        if (prefix == 0) {
            return "0.0.0.0";
        }
        int shift = 0xffffffff << (32 - prefix);
        int oct1 = ((byte) ((shift & 0xff000000) >> 24)) & 0xff;
        int oct2 = ((byte) ((shift & 0x00ff0000) >> 16)) & 0xff;
        int oct3 = ((byte) ((shift & 0x0000ff00) >> 8)) & 0xff;
        int oct4 = ((byte) (shift & 0x000000ff)) & 0xff;
        return oct1 + "." + oct2 + "." + oct3 + "." + oct4;
    }

    private static boolean isIp(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        try {
            InetAddress.getByName(host);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    private static void addUntilEnd(List<String> config, int from, List<String> to) {
        for (int j = from; j < config.size(); j++) {
            String nLine = config.get(j);
            int del = nLine.indexOf(" : ");
            if (del >= 0) {
                break;
            }
            String nValue = nLine.trim();
            to.add(nValue);
        }
    }

    private static InetAddress findDhcp(List<String> config) {
        for (int i = config.size() - 1; i >= 0; i--) {
            String line = config.get(i);
            int del = line.indexOf(" : ");
            if (del < 0) {
                continue;
            }
            if (line.contains("DHCP-")) {
                String value = line.substring(del + 3);
                try {
                    return InetAddress.getByName(value);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static List<InetAddress> findDns(List<String> config) {
        List<String> dnsString = new ArrayList<>(2);
        for (int i = config.size() - 1; i >= 0; i--) {
            String line = config.get(i);
            int del = line.indexOf(" : ");
            if (del < 0) {
                continue;
            }
            if (line.contains("DNS-")) {
                String value = line.substring(del + 3);
                if (isIp(value)) {
                    dnsString.add(value);
                }
                addUntilEnd(config, i + 1, dnsString);
            }
        }
        List<InetAddress> dns = new ArrayList<>(dnsString.size());
        for (String s : dnsString) {
            try {
                dns.add(InetAddress.getByName(s));
            } catch (Exception ignored) {

            }
        }
        return dns;
    }

    private static List<InetAddress> findGateways(List<String> config) {
        List<String> gatewaysString = new ArrayList<>(1);
        for (int i = config.size() - 1; i >= 0; i--) {
            String line = config.get(i);
            int del = line.indexOf(" : ");
            if (del < 0) {
                continue;
            }
            String value = line.substring(del + 3);

            //last subnet mask
            if (getPrefix(value) >= 0) {
                if (i + 1 >= config.size()) {
                    continue;
                }
                String n1Line = config.get(i + 1);
                del = n1Line.indexOf(" : ");
                if (del < 0) {
                    continue;
                }
                String n1Value = n1Line.substring(del + 3);
                if (isIp(n1Value)) {
                    gatewaysString.add(n1Value);
                    addUntilEnd(config, i + 2, gatewaysString);
                } else {
                    //two line for DHCP rent time
                    if (i + 3 >= config.size()) {
                        continue;
                    }
                    String n3Line = config.get(i + 3);
                    del = n3Line.indexOf(" : ");
                    if (del < 0) {
                        continue;
                    }
                    String n3Value = n3Line.substring(del + 3);
                    if (isIp(n3Value)) {
                        gatewaysString.add(n3Value);
                        addUntilEnd(config, i + 4, gatewaysString);
                    }

                }
                break;
            }
        }
        List<InetAddress> gateways = new ArrayList<>(gatewaysString.size());
        for (String s : gatewaysString) {
            try {
                gateways.add(InetAddress.getByName(s));
            } catch (Exception ignored) {

            }
        }
        return gateways;
    }

    private static List<String> findByMac(List<List<String>> configs, String mac) {
        mac = ": " + mac;
        for (List<String> config : configs) {
            for (String line : config) {
                if (line.contains(mac)) {
                    return config;
                }
            }
        }
        return null;
    }

    private static List<List<String>> ipconfig() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("ipconfig", "/all");
        Process p = pb.start();
        BufferedInputStream is = new BufferedInputStream(p.getInputStream());
        List<List<String>> configs = new ArrayList<>();
        List<String> current = null;
        StringBuilder line = new StringBuilder(64);
        int i;
        while (true) {
            i = is.read();
            if (i == '\n' || i < 0) {
                if (!line.isEmpty()) {
                    if (!Character.isWhitespace(line.charAt(0))) {
                        current = new ArrayList<>();
                        configs.add(current);
                    }
                    if (current != null) {
                        current.add(line.toString());
                    }
                }

                line.delete(0, line.length());
                if (i < 0) {
                    break;
                }
            } else if (i != '\r') {
                line.append((char) i);
            }
        }
        return configs;
    }

    public static String getMask(int prefix) {
        if (prefix < 0 || prefix > 32) {
            return null;
        }
        return masks[prefix];
    }

    public static int getPrefix(String mask) {
        Integer prefix = maskMap.get(mask);
        if (prefix == null) {
            return -1;
        }
        return prefix;
    }


    public static String toWindowsMac(byte[] mac) {
        if (mac == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            String hex = Integer.toHexString(mac[i] & 0xFF);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
            if (i < mac.length - 1) {
                sb.append('-');
            }
        }
        return sb.toString().toUpperCase();
    }

    @Nonnull
    public static List<NetworkInterface> getNetworkInterfaces() {
        InetAddress localInternet = getLocalInternetAddress();
        List<NetworkInterface> interfaces = new ArrayList<>();
        try {
            Enumeration<java.net.NetworkInterface> javaInterfaces = java.net.NetworkInterface.getNetworkInterfaces();

            while (javaInterfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = javaInterfaces.nextElement();
                if (ni.isUp() && ni.getHardwareAddress() != null) {
                    interfaces.add(new NetworkInterface(ni));
                }
            }
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
            return null;
        }
        if (!Sys.isWindows()) {
            return interfaces;
        }
        try {
            List<List<String>> ipconfig = ipconfig();
            for (NetworkInterface ni : interfaces) {
                String mac = toWindowsMac(ni.getHardwareAddress());
                List<String> config = findByMac(ipconfig, mac);
                if (config != null) {
                    ni.gateways = findGateways(config);
                    ni.dns = findDns(config);
                    ni.dhcp = findDhcp(config);
                    if (ni.hasAddress(localInternet)) {
                        ni.hasInternetAccess = true;
                    }
                }
            }
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
            return null;
        }
        return interfaces;
    }

    public static int getIntAddress(Inet4Address address) {
        return address.hashCode();
    }

    public static Inet4Address getStart(Inet4Address address, int prefix) {
        int shift = 0xffffffff << (32 - prefix);
        int start = shift & getIntAddress(address);
        start++;
        try {
            return (Inet4Address) InetAddress.getByAddress(Ints.toByteArray(start));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Inet4Address getEnd(Inet4Address address, int prefix) {
        int shift = 0xffffffff << (32 - prefix);
        int start = shift & getIntAddress(address);
        start += (int) Math.pow(2, 32 - prefix);
        start -= 2;
        try {
            return (Inet4Address) InetAddress.getByAddress(Ints.toByteArray(start));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static long lastInternetInterface;
    private static NetworkInterface internetInterface;

    public static NetworkInterface getInternetInterface() {
        if (System.currentTimeMillis() - lastInternetInterface > 10000 || internetInterface == null) {
            lastInternetInterface = System.currentTimeMillis();
        } else {
            return internetInterface;
        }
        List<NetworkInterface> interfaces = getNetworkInterfaces();
        for (NetworkInterface ni : interfaces) {
            if (ni.hasInternetAccess()) {
                internetInterface = ni;
                return ni;
            }
        }
        return null;
    }

    private static boolean hasAddress(java.net.NetworkInterface ni, InetAddress address) {
        for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
            if (interfaceAddress.getAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFromNetworkWithInternetAccess(Inet4Address address) {
        Inet4Address internetAddress = getLocalInternet4Address();
        if (internetAddress == null) {
            return false;
        }
        if (address.equals(internetAddress)) {
            return true;
        }

        try {
            Enumeration<java.net.NetworkInterface> enumeration = java.net.NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                java.net.NetworkInterface ni = enumeration.nextElement();
                if (!ni.isUp()) {
                    continue;
                }
                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                    if (interfaceAddress.getAddress().equals(internetAddress)) {
                        Inet4Address startAddress = getStart(internetAddress, interfaceAddress.getNetworkPrefixLength());
                        Inet4Address endAddress = getEnd(internetAddress, interfaceAddress.getNetworkPrefixLength());
                        int start = getIntAddress(startAddress);
                        int end = getIntAddress(endAddress);
                        int intAddress = getIntAddress(address);
                        if (intAddress >= start && intAddress <= end) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
            return false;
        }
    }

    public static java.net.NetworkInterface getJavaInternetInterface() {
        InetAddress internetAddress = getLocalInternetAddress();
        try {
            Enumeration<java.net.NetworkInterface> enumeration = java.net.NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                java.net.NetworkInterface ni = enumeration.nextElement();
                if (ni.isUp() && hasAddress(ni, internetAddress)) {
                    return ni;
                }
            }
            return null;
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
            return null;
        }
    }

    public static Inet4Address getLocalAddressInSameNetworkFor(Inet4Address address) {
        try {
            Enumeration<java.net.NetworkInterface> enumeration = java.net.NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                java.net.NetworkInterface i = enumeration.nextElement();
                if (!i.isUp()) {
                    continue;
                }
                for (InterfaceAddress ia : i.getInterfaceAddresses()) {
                    if (ia.getAddress() instanceof Inet4Address ipv4) {
                        Inet4Address startAddress = getStart(ipv4, ia.getNetworkPrefixLength());
                        Inet4Address endAddress = getEnd(ipv4, ia.getNetworkPrefixLength());
                        int a = getIntAddress(address);
                        int start = getIntAddress(startAddress);
                        int end = getIntAddress(endAddress);
                        if (a >= start && a <= end) {
                            return ipv4;
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
            return null;
        }
    }

    public static java.net.NetworkInterface getJavaNetworkInterfaceFor(Inet4Address address) {
        try {
            Enumeration<java.net.NetworkInterface> enumeration = java.net.NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                java.net.NetworkInterface i = enumeration.nextElement();
                if (!i.isUp()) {
                    continue;
                }
                for (InterfaceAddress ia : i.getInterfaceAddresses()) {
                    if (ia.getAddress() instanceof Inet4Address ipv4) {
                        Inet4Address startAddress = getStart(ipv4, ia.getNetworkPrefixLength());
                        Inet4Address endAddress = getEnd(ipv4, ia.getNetworkPrefixLength());
                        int a = getIntAddress(address);
                        int start = getIntAddress(startAddress);
                        int end = getIntAddress(endAddress);
                        if (a >= start && a <= end) {
                            return i;
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
            return null;
        }

    }

    private static long lastInternetCheck;
    private static InetAddress localAddressInternet;

    public static Inet4Address getLocalInternet4Address() {
        InetAddress address = getLocalInternetAddress();
        if (address instanceof Inet4Address ipv4) {
            return ipv4;
        }
        return null;
    }

    public static InetAddress getLocalInternetAddress() {
        if (System.currentTimeMillis() - lastInternetCheck > 10000) {
            lastInternetCheck = System.currentTimeMillis();
        } else {
            return localAddressInternet;
        }


        final String[] addresses = {"github.com", "www.baidu.com", "google.com", "x.com", "yandex.ru"};
        for (String address : addresses) {
            try {
                Socket s = new Socket(address, 443);
                InetAddress local = s.getLocalAddress();
                s.close();
                localAddressInternet = local;
                return local;
            } catch (Throwable ignored) {

            }
        }
        return null;
    }

}
