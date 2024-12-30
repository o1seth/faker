/*
 * This file is part of faker - https://github.com/o1seth/faker
 * Copyright (C) 2024 o1seth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.java.faker.util.network;

import net.java.faker.proxy.util.ExceptionUtil;
import net.java.faker.proxy.util.chat.Ints;
import net.java.faker.util.Sys;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

    public static boolean localIpExists(String ip) {
        return localIpExists(ip, null);
    }

    public static boolean localIpExists(String ip, java.net.NetworkInterface except) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            Enumeration<java.net.NetworkInterface> enumeration = java.net.NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                java.net.NetworkInterface ni = enumeration.nextElement();
                if (ni == except) {
                    continue;
                }
                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                    if (interfaceAddress.getAddress().equals(address)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {

        }
        return false;
    }

    public static boolean isIpv4(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address instanceof Inet4Address ipv4) {
                return ipv4.getHostAddress().equals(ip);
            }
            return false;
        } catch (Throwable ignored) {

        }
        return false;

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

    public static boolean hasInterfaceAddress(java.net.NetworkInterface networkInterface, String address) {
        try {
            InetAddress ip = InetAddress.getByName(address);
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                if (ip.equals(interfaceAddress.getAddress())) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
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

    public static Inet4Address fromIntAddress(int address) {
        byte[] addr = new byte[4];
        addr[0] = (byte) (address >>> 24);
        addr[1] = (byte) (address >>> 16);
        addr[2] = (byte) (address >>> 8);
        addr[3] = (byte) (address);
        try {
            return (Inet4Address) InetAddress.getByAddress(addr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getIntAddress(Inet4Address address) {
        return address.hashCode();
    }

    public static int getStartInt(Inet4Address address, int prefix) {
        int shift = 0xffffffff << (32 - prefix);
        int start = shift & getIntAddress(address);
        start++;
        return start;
    }

    public static Inet4Address getStart(Inet4Address address, int prefix) {
        try {
            return (Inet4Address) InetAddress.getByAddress(Ints.toByteArray(getStartInt(address, prefix)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static int getEndInt(Inet4Address address, int prefix) {
        int shift = 0xffffffff << (32 - prefix);
        int end = shift & getIntAddress(address);
        end += (int) Math.pow(2, 32 - prefix);
        end -= 2;
        return end;
    }

    public static Inet4Address getEnd(Inet4Address address, int prefix) {
        try {
            return (Inet4Address) InetAddress.getByAddress(Ints.toByteArray(getEndInt(address, prefix)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isIpInSameNetwork(int prefix, String... ip) {
        try {
            if (ip == null || ip.length == 0) {
                return false;
            }
            if (ip.length == 1) {
                return true;
            }
            Inet4Address address = (Inet4Address) InetAddress.getByName(ip[0]);
            int start = getStartInt(address, prefix);
            int end = getEndInt(address, prefix);
            for (int i = 1; i < ip.length; i++) {
                address = (Inet4Address) InetAddress.getByName(ip[i]);
                int intAddress = getIntAddress(address);
                if (intAddress < start || intAddress > end) {
                    return false;
                }
            }
            return true;
        } catch (Exception ignored) {

        }
        return false;

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

    public static java.net.NetworkInterface getInterfaceByIp(String ip) {
        try {
            Enumeration<java.net.NetworkInterface> enumeration = java.net.NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                java.net.NetworkInterface i = enumeration.nextElement();
                if (!i.isUp()) {
                    continue;
                }
                for (InterfaceAddress ia : i.getInterfaceAddresses()) {
                    if (ia.getAddress() instanceof Inet4Address ipv4) {
                        if (ipv4.getHostAddress().equals(ip)) {
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

    private static List<String> createNetsh(String mode, NetworkInterface networkInterface, String address, String mask, String gateway) {
        List<String> command = new ArrayList<>();
        command.add("netsh");
        command.add("interface");
        command.add("ip");
        command.add(mode);
        command.add("address");
        command.add(networkInterface.getIndex() + "");
        command.add("static");
        if (address != null) {
            command.add("address=" + address);
        }
        if (mask != null) {
            command.add("mask=" + mask);
        }
        if (gateway != null) {
            command.add("gateway=" + gateway);
        }
        return command;
    }

    public static void restoreAddress(NetworkInterface networkInterface) {
        List<InetAddress> gateways = networkInterface.getGateways();
        List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
        String mode = "set";
        for (int i = 0; i < interfaceAddresses.size(); i++) {
            InterfaceAddress interfaceAddress = interfaceAddresses.get(i);
            if (!(interfaceAddress.getAddress() instanceof Inet4Address)) {
                continue;
            }
            InetAddress gatewayAddress = null;
            if (i < gateways.size()) {
                if (gateways.get(i) instanceof Inet4Address) {
                    gatewayAddress = gateways.get(i);
                }
            }
            String address = interfaceAddress.getAddress().getHostAddress();
            String mask = getMask(interfaceAddress.getNetworkPrefixLength());
            String gateway = gatewayAddress == null ? null : gatewayAddress.getHostAddress();

            int code;
            InputStream is;
            try {
                List<String> command = createNetsh(mode, networkInterface, address, mask, gateway);
                ProcessBuilder pb = new ProcessBuilder(command);
                Process process = pb.start();
                is = process.getInputStream();
                code = process.waitFor();
            } catch (Exception e) {
                throw new RuntimeException("Failed to set ip: " + e.getMessage());
            }
            if (code != 0) {
                String error = null;
                try {
                    int c;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((c = is.read()) != -1) {
                        baos.write(c);
                    }
                    ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "chcp");
                    Process process = pb.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line = reader.readLine();
                    String encoding = line.substring(line.indexOf(':') + 2);
                    error = new String(baos.toString(Charset.forName(encoding)).getBytes(StandardCharsets.UTF_8));
                    error = error.replace("\r\n", "");
                } catch (Throwable ignored) {

                }
                if (error != null) {
                    throw new RuntimeException("Failed to set ip: " + error);
                }
                throw new RuntimeException("Failed to set ip");
            }

            mode = "add";
        }
    }


    public static void setAddress(NetworkInterface networkInterface, String address, String mask, String gateway) {
        int code;
        InputStream is;
        try {
            List<String> command = createNetsh("set", networkInterface, address, mask, gateway);
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            is = process.getInputStream();
            code = process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ip: " + e.getMessage());
        }
        if (code != 0) {
            String error = null;
            try {
                int i;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while ((i = is.read()) != -1) {
                    baos.write(i);
                }
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "chcp");
                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                String encoding = line.substring(line.indexOf(':') + 2);
                error = new String(baos.toString(Charset.forName(encoding)).getBytes(StandardCharsets.UTF_8));
                error = error.replace("\r\n", "");
            } catch (Throwable ignored) {

            }
            if (error != null) {
                throw new RuntimeException("Failed to set ip: " + error);
            }
            throw new RuntimeException("Failed to set ip");
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

    public static NetworkInterface findPotentialWifiHotspotInterface(List<NetworkInterface> interfaces) {
        for (NetworkInterface ni : interfaces) {
            if (ni.getDisplayName().equals("Microsoft Wi-Fi Direct Virtual Adapter #2") || ni.getDisplayName().equals("Microsoft Wi-Fi Direct Virtual Adapter #4")) {
                Inet4Address ipv4 = ni.getFirstIpv4Address();
                if (ipv4.getHostAddress().equals("192.168.137.1")) {
                    return ni;
                }
            }
        }

        //windows 10
        for (NetworkInterface ni : interfaces) {
            if (ni.getDisplayName().equals("Microsoft Wi-Fi Direct Virtual Adapter #2")) {
                return ni;
            }
        }
        //windows 11
        for (NetworkInterface ni : interfaces) {
            if (ni.getDisplayName().equals("Microsoft Wi-Fi Direct Virtual Adapter #4")) {
                return ni;
            }
        }

        for (NetworkInterface ni : interfaces) {
            if (ni.getDisplayName().equals("Microsoft Wi-Fi Direct Virtual Adapter")) {
                Inet4Address ipv4 = ni.getFirstIpv4Address();
                if (ipv4.getHostAddress().equals("192.168.137.1")) {
                    return ni;
                }
            }
        }
        return null;
    }

}
