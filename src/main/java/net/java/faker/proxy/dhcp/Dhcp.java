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

package net.java.faker.proxy.dhcp;

import net.java.faker.proxy.util.ExceptionUtil;
import net.java.faker.util.logging.Logger;
import net.java.faker.util.network.NetworkUtil;
import org.apache.directory.server.dhcp.options.vendor.DomainNameServers;
import org.apache.directory.server.dhcp.options.vendor.Routers;
import org.apache.directory.server.dhcp.service.manager.AbstractDynamicLeaseManager;

import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;

public class Dhcp {
    private static net.java.faker.util.network.NetworkInterface dhcpInterface;
    private static DhcpServer server;

    private static Routers createRouter(String address) {
        Routers router = new Routers();
        try {
            router.setAddresses((Inet4Address) InetAddress.getByName(address));
        } catch (Exception e) {
            Logger.error("", e);
        }
        return router;
    }

    public static boolean isStarted() {
        return server != null;
    }

    private static DomainNameServers createDns(String[] addresses) {
        DomainNameServers dns = new DomainNameServers();
        try {
            ArrayList<Inet4Address> inet4Addresses = new ArrayList<>(addresses.length);
            for (String address : addresses) {
                if (address == null || address.isEmpty()) {
                    continue;
                }
                try {
                    inet4Addresses.add((Inet4Address) InetAddress.getByName(address));
                } catch (Exception ignored) {

                }
            }
            dns.setAddresses(inet4Addresses.toArray(new Inet4Address[0]));
        } catch (Exception e) {
            Logger.error("", e);
        }
        return dns;
    }

    public static void stop() {
        if (server != null) {
            try {
                server.stop();
            } catch (Throwable ignored) {
            }
            server = null;
        }
        if (dhcpInterface != null) {
            try {
                NetworkUtil.restoreAddress(dhcpInterface);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void start(net.java.faker.util.network.NetworkInterface networkInterface, String address, String mask, String start, String end, String[] dns) {
        if (address == null || address.isEmpty()) {
            throw new RuntimeException("Empty address");
        }
        if (mask == null) {
            mask = "255.255.255.0";
        }
        try {
            Inet4Address ip = (Inet4Address) InetAddress.getByName(address);
            int intIp = NetworkUtil.getIntAddress(ip);

            if (start == null) {
                start = NetworkUtil.fromIntAddress(intIp + 1).getHostAddress();
            }
            if (end == null) {
                end = NetworkUtil.getEnd(ip, NetworkUtil.getPrefix(mask)).getHostAddress();
            }
            int intStart = NetworkUtil.getIntAddress((Inet4Address) InetAddress.getByName(start));
            int intEnd = NetworkUtil.getIntAddress((Inet4Address) InetAddress.getByName(end));
            if (intStart > intEnd) {
                String tmp = start;
                start = end;
                end = tmp;
            }
            if (dns == null) {
                dns = new String[]{address};
            }
            Logger.info("Set interface address: " + ip.getHostAddress() + " " + mask + "...");
            NetworkUtil.setAddress(networkInterface, address, mask, null);
            dhcpInterface = networkInterface;


            AbstractDynamicLeaseManager manager = new DynamicLeaseManager(start, end, createRouter(address), createDns(dns));
            server = new DhcpServer(manager);

            NetworkInterface updatedInterface = null;
            for (int i = 0; i < 10; i++) {
                updatedInterface = networkInterface.getUpdatedJavaInterface();
                if (NetworkUtil.hasInterfaceAddress(updatedInterface, address)) {
                    break;
                } else {
                    updatedInterface = null;
                }
                Logger.info("Wait for address set...");
                Thread.sleep(1000);
            }
            if (updatedInterface == null) {
                throw new RuntimeException("Could not find updated network interface");
            }
            Logger.info("Done");
            Logger.info("Starting DHCP server " + ip.getHostAddress() + ", ip range: " + start + " - " + end + ", dns: " + Arrays.toString(dns));
            server.addInterface(updatedInterface);
            Exception exception = null;
            for (int i = 0; i < 8; i++) {
                try {
                    server.start();
                    break;
                } catch (Exception e) {
                    exception = e;
                    if (e instanceof BindException) {
                        Thread.sleep(1000);
                    } else {
                        break;
                    }
                }
            }
            if (server.getChannel() == null) {
                if (exception != null) {
                    throw exception;
                }
                throw new RuntimeException("Failed to start dhcp server");
            }
            Logger.info("DHCP server started");
        } catch (Throwable e) {
            if (dhcpInterface != null) {
                try {
                    NetworkUtil.restoreAddress(dhcpInterface);
                } catch (Throwable ignored) {

                }
                dhcpInterface = null;
            }
            ExceptionUtil.throwException(e);
        }
    }

    //netsh interface ip set address "AP" static 192.168.159.1 255.255.255.0 196.168.159.1
//    public static void main(String[] args) throws Throwable {
//
//        for (net.java.faker.util.network.NetworkInterface ni : NetworkUtil.getNetworkInterfaces()) {
//            if (ni.getName().equals("wlan0")) {
//                start(ni, "192.168.100.1", "255.255.255.0", "192.168.100.60", "192.168.100.5", new String[]{"1.1.1.1", "8.8.8.8"});
//                break;
//            }
//        }
//
//        Thread.sleep(2000000);
//    }
}
