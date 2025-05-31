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

package net.java.faker;

import net.java.faker.util.Sys;
import net.java.faker.util.Util;
import net.java.faker.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Arrays;

public class WinRedirect {
    private static final boolean isSupported;

    static {
        isSupported = initNative();
    }

    private static boolean isEquals(File file, byte[] bytes) {
        try {
            return Arrays.equals(Files.readAllBytes(file.toPath()), bytes);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean initNative() {
        if (Sys.isWindows() && Sys.isX64()) {
            byte[] dllBytes = Util.getResourceBytes("/assets/faker/lib/win_redirect.dll");
            if (dllBytes == null) {
                return false;
            }
            File dllFile = new File(Proxy.getFakerDirectory(), "win_redirect.dll");
            if (!dllFile.exists() || dllFile.length() != dllBytes.length || !isEquals(dllFile, dllBytes)) {
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

    public static native boolean setRedirectLatency(long redirect, String ip, int port, int latencyIn, int latencyOut);

    public static native boolean getRedirectLatency(long redirect, String ip, int port, int[] out);

    public static native int getLatency(String fromIp, int fromPort, String toIp, int toPort);

    public static native boolean redirectSetDefaultLatency(long redirect, int latencyIn, int latencyOut);

    public static native int[] redirectGetDefaultLatency(long redirect);

    public static long redirectStart(int targetPort, int localPort, Inet4Address[] srcAddresses, Inet4Address[] dstAddresses, Layer layer, int latencyIn, int latencyOut) {
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
        return redirectStart(localPort, filter.toString(), layer.ordinal(), latencyIn, latencyOut);
    }

    protected static native long redirectStart(int redirect_port, String filter, int layer, int latencyIn, int latencyOut);

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
