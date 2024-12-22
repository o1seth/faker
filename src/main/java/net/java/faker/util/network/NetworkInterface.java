package net.java.faker.util.network;

import net.java.faker.proxy.util.ExceptionUtil;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

public class NetworkInterface {

    public static final NetworkInterface NULL = new NetworkInterface(null);
    final java.net.NetworkInterface javaInterface;
    List<InetAddress> dns;
    List<InetAddress> gateways;
    InetAddress dhcp;
    boolean hasInternetAccess;

    public NetworkInterface(java.net.NetworkInterface javaInterface) {
        this.javaInterface = javaInterface;
    }


    public byte[] getHardwareAddress() {
        if (this.javaInterface == null) {
            return null;
        }
        try {
            return this.javaInterface.getHardwareAddress();
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
            return null;
        }
    }

    public String getName() {
        return this.javaInterface.getName();
    }

    public String getDisplayName() {
        return this.javaInterface.getDisplayName();
    }

    public List<InterfaceAddress> getInterfaceAddresses() {
        return this.javaInterface.getInterfaceAddresses();
    }

    public boolean isUp() {
        try {
            return this.javaInterface.isUp();
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
            return false;
        }
    }

    public boolean isLoopback() {
        try {
            return this.javaInterface.isLoopback();
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
            return false;
        }
    }

    public boolean hasAddress(InetAddress address) {
        for (InterfaceAddress interfaceAddress : this.getInterfaceAddresses()) {
            if (interfaceAddress.getAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    public int getIndex() {
        return this.javaInterface.getIndex();
    }

    public boolean isVirtual() {
        return this.javaInterface.isVirtual();
    }

    public InetAddress getDhcp() {
        return dhcp;
    }

    public List<InetAddress> getDns() {
        return dns;
    }

    public List<InetAddress> getGateways() {
        return gateways;
    }

    public InetAddress getGateway() {
        if (gateways != null && !gateways.isEmpty()) {
            return gateways.get(0);
        }
        return null;
    }

    public Inet4Address get4Gateway() {
        if (gateways != null && !gateways.isEmpty()) {
            for (InetAddress address : gateways) {
                if (address instanceof Inet4Address v4) {
                    return v4;
                }
            }
        }
        return null;
    }

    public java.net.NetworkInterface getJavaInterface() {
        return javaInterface;
    }

    public java.net.NetworkInterface getUpdatedJavaInterface() {
        int index = javaInterface.getIndex();
        try {
            Enumeration<java.net.NetworkInterface> enumeration = java.net.NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                java.net.NetworkInterface ni = enumeration.nextElement();
                if (ni.getIndex() == index) {
                    return ni;
                }
            }
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
            return null;
        }

        return javaInterface;
    }

    public Inet4Address getFirstIpv4Address() {
        Enumeration<InetAddress> enumeration = javaInterface.getInetAddresses();
        while (enumeration.hasMoreElements()) {
            InetAddress address = enumeration.nextElement();
            if (address instanceof Inet4Address ipv4) {
                return ipv4;
            }
        }
        return null;
    }

    public InterfaceAddress getFirstIpv4InterfaceAddress() {
        for (InterfaceAddress interfaceAddress : this.javaInterface.getInterfaceAddresses()) {
            if (interfaceAddress.getAddress() instanceof Inet4Address ipv4) {
                return interfaceAddress;
            }
        }
        return null;
    }

    public boolean equalsNameAndMac(NetworkInterface other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (this.javaInterface == null || other.javaInterface == null) {
            return false;
        }
        return getDisplayName().equals(other.getDisplayName()) && Arrays.equals(getHardwareAddress(), other.getHardwareAddress());
    }

    @Override
    public String toString() {
        if (this.javaInterface == null) {
            return "";
        }
        if (javaInterface.getInterfaceAddresses().isEmpty()) {
            return javaInterface.getDisplayName();
        }
        InetAddress address = getFirstIpv4Address();
        if (address == null) {
            List<InterfaceAddress> addresses = javaInterface.getInterfaceAddresses();
            if (addresses != null && !addresses.isEmpty()) {
                if (addresses.get(0) != null) {
                    address = addresses.get(0).getAddress();
                }
            }
        }
        if (address == null) {
            return javaInterface.getDisplayName();
        }
        return javaInterface.getDisplayName() + ' ' + address.getHostAddress();
    }

    public boolean hasInternetAccess() {
        return hasInternetAccess;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkInterface that = (NetworkInterface) o;
        return hasInternetAccess == that.hasInternetAccess && Objects.equals(javaInterface, that.javaInterface) && Objects.equals(dns, that.dns) && Objects.equals(gateways, that.gateways) && Objects.equals(dhcp, that.dhcp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaInterface, dns, gateways, dhcp, hasInternetAccess);
    }
}
