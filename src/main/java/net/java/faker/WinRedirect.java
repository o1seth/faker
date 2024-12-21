package net.java.faker;

import net.java.faker.util.Sys;
import net.java.faker.util.Util;
import net.java.faker.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.file.Files;

public class WinRedirect {
    private static final boolean isSupported;

    static {
        isSupported = initNative();
    }

    private static boolean initNative() {
        if (Sys.isWindows() && Sys.isX64()) {
            byte[] dllBytes = Util.getResourceBytes("/assets/faker/lib/win_redirect.dll");
            if (dllBytes == null) {
                return false;
            }
            File dllFile = new File(Proxy.getFakerDirectory(), "win_redirect.dll");
            if (!dllFile.exists() || dllFile.length() != dllBytes.length) {
                try {
                    Files.write(dllFile.toPath(), dllBytes);
                } catch (IOException e) {
                    return false;
                }
            }

            byte[] driverBytes = Util.getResourceBytes("/assets/faker/lib/WinDivert64.sys");
            if (driverBytes == null) {
                return false;
            }
            File driverFile = new File(Proxy.getFakerDirectory(), "WinDivert64.sys");
            if (!driverFile.exists() || driverFile.length() != driverBytes.length) {
                try {
                    Files.write(driverFile.toPath(), driverBytes);
                } catch (IOException e) {
                    return false;
                }
            }
            System.load(dllFile.getAbsolutePath());
            return true;
        }
        return false;
    }

    public static boolean isSupported() {
        return isSupported;
    }

    public enum Layer {
        NETWORK, NETWORK_FORWARD
    }

    public static long redirectStart(int targetPort, int localPort, Inet4Address[] srcAddresses, Inet4Address[] dstAddresses, Layer layer) {
        StringBuilder filter = new StringBuilder();
        filter.append("tcp");
        filter.append(" and (tcp.DstPort == ");
        filter.append(targetPort);
        filter.append(")");
        filter.append(" and (ip.DstAddr < 127.0.0.1 or ip.DstAddr > 127.255.255.254)");//we don't want to redirect localhost connections
        if (srcAddresses != null) {
            if (srcAddresses.length > 0) {
                filter.append(" and (");
            }
            for (int i = 0; i < srcAddresses.length; i++) {
                filter.append("ip.SrcAddr == ").append(srcAddresses[i].getHostAddress());
                if (i < srcAddresses.length - 1) {
                    filter.append(" or ");
                }
            }
            if (srcAddresses.length > 0) {
                filter.append(")");
            }
        }

        if (dstAddresses != null) {
            if (dstAddresses.length > 0) {
                filter.append(" and (");
            }
            for (int i = 0; i < dstAddresses.length; i++) {
                filter.append("ip.DstAddr == ").append(dstAddresses[i].getHostAddress());
                if (i < dstAddresses.length - 1) {
                    filter.append(" or ");
                }
            }
            if (dstAddresses.length > 0) {
                filter.append(")");
            }
        }
        Logger.raw("Redirect filter:\n" + filter);
        return redirectStart(localPort, filter.toString(), layer.ordinal());
    }

    protected static native long redirectStart(int redirect_port, String filter, int layer);

    public static native void redirectStop(long redirect);

    public static native void redirectPause(long redirect);

    public static native void redirectResume(long redirect);

    public static native boolean redirectAddSkipPort(long redirect, int port);

    public static native boolean redirectRemoveSkipPort(long redirect, int port);

    public static native int redirectGetActiveConnectionsCount(long redirect);

    public static native boolean redirectGetRealAddresses(long redirect, String ip, int port, InetSocketAddress[] out);

    public static native String getError();

    public static native long mdnsLlmnrDisable(String ip);

    public static native boolean mdnsLlmnrRestore(long mdns);

    public static native boolean enableTtlFix();

    public static native boolean disableTtlFix();

    //example: portForwardStart("192.168.137.1", "192.168.137.2", "192.168.137.254", "192.168.0.1", "192.168.0.2", 80);
    //192.168.0.2 - this pc ip address from which there is access to "toIp"
    public static native long portForwardStart(String listenIp, String listenStartIp, String listenEndIp, String toIp, String thisIp, int... skipPorts);

    public static native boolean portForwardStop(long portForward);

    //(ip.SrcAddr >= %s and ip.SrcAddr <= %s and ip.DstAddr >= %s and ip.DstAddr <= %s)
    public static long blockIpStart(String filter, Layer layer) {
        return firewallStart(filter, layer.ordinal());
    }

    public static boolean blockIpStop(long blockIp) {
        return firewallStop(blockIp);
    }

    private static native long firewallStart(String filter, int layer);

    private static native boolean firewallStop(long blockIp);

    public static native void setLogLevel(int level);
}
