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
import java.util.Arrays;

public class Dhcp {
    private static net.java.faker.util.network.NetworkInterface dhcpInterface;
    private static DhcpServer server;

    private static Routers createRouter(String address) {
        Routers router = new Routers();
        try {
            router.setAddresses((Inet4Address) InetAddress.getByName(address));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return router;
    }

    private static DomainNameServers createDns(String[] addresses) {
        DomainNameServers dns = new DomainNameServers();
        try {
            Inet4Address[] inet4Addresses = new Inet4Address[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                inet4Addresses[i] = (Inet4Address) InetAddress.getByName(addresses[i]);
            }
            dns.setAddresses(inet4Addresses);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dns;
    }

    public static void stop() {
        if (server != null) {
            try {
                server.stop();
            } catch (Throwable ignored) {
            }
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
    public static void main(String[] args) throws Throwable {

        for (net.java.faker.util.network.NetworkInterface ni : NetworkUtil.getNetworkInterfaces()) {
            if (ni.getName().equals("wlan0")) {
                start(ni, "192.168.100.1", "255.255.255.0", "192.168.100.60", "192.168.100.5", new String[]{"1.1.1.1", "8.8.8.8"});
                break;
            }
        }

        Thread.sleep(2000000);
    }
}
