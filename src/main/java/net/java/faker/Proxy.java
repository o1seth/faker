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

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.GlobalEventExecutor;
import net.java.faker.auth.Account;
import net.java.faker.proxy.client2proxy.Client2ProxyChannelInitializer;
import net.java.faker.proxy.client2proxy.Client2ProxyHandler;
import net.java.faker.proxy.event.ConnectEvent;
import net.java.faker.proxy.event.Event;
import net.java.faker.proxy.event.ProxyStateEvent;
import net.java.faker.proxy.event.RedirectStateChangeEvent;
import net.java.faker.proxy.session.DualConnection;
import net.java.faker.proxy.session.ProxyConnection;
import net.java.faker.proxy.util.chat.Ints;
import net.java.faker.save.AccountManager;
import net.java.faker.save.Config;
import net.java.faker.ui.Window;
import net.java.faker.ui.tab.AdvancedTab;
import net.java.faker.util.HttpHostSpoofer;
import net.java.faker.util.Sys;
import net.java.faker.util.logging.Logger;
import net.java.faker.util.network.NetworkInterface;
import net.java.faker.util.network.NetworkUtil;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.NetServer;

import java.io.File;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Proxy {
    public static final String VERSION = "1.1";
    public static InetSocketAddress proxyAddress = new InetSocketAddress("127.0.0.1", 25565);
    private static URI backendProxy;
    public static final int compressionThreshold = 256;
    public static final int connectTimeout = 8000;
    public static DualConnection dualConnection;

    private static Config config;
    private static AccountManager accountManager;
    private static Account account;

    private static NetServer currentProxyServer;
    private static ChannelGroup CLIENT_CHANNELS;
    private static NetworkInterface targetAdapter;

    public static ChannelGroup getConnectedClients() {
        return CLIENT_CHANNELS;
    }

    public static ArrayList<InetSocketAddress> connectedAddresses = new ArrayList<>();
    private static final List<Consumer<Event>> events = new ArrayList<>();
    public static long forward_redirect;
    public static long redirect;
    private static long mdns;
    private static long routerBlockTraffic;
    private static long routerPortForward;
    private static HttpHostSpoofer httpHostSpoofer;
    private static long blockTraffic;

    private static void addPortsToFilter(StringBuilder filter, int[] skipTcpPorts, int[] skipUdpPorts) {
        if (skipTcpPorts != null && skipTcpPorts.length > 0 || skipUdpPorts != null && skipUdpPorts.length > 0) {
            filter.append(" and (");
            if (skipTcpPorts != null && skipTcpPorts.length > 0) {
                for (int i = 0; i < skipTcpPorts.length; i++) {
                    filter.append("tcp.DstPort != ").append(skipTcpPorts[i]);
                    if (i + 1 < skipTcpPorts.length) {
                        filter.append(" and ");
                    }
                }
            }

            if (skipUdpPorts != null && skipUdpPorts.length > 0) {
                if (skipTcpPorts != null && skipTcpPorts.length > 0) {
                    filter.append(" or ");
                }
                for (int i = 0; i < skipUdpPorts.length; i++) {
                    filter.append("udp.DstPort != ").append(skipUdpPorts[i]);
                    if (i + 1 < skipUdpPorts.length) {
                        filter.append(" and ");
                    }
                }
            }

            filter.append(")");
        }
    }

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        if (args != null) {
            for (String arg : args) {
                if ("--showdebug".equalsIgnoreCase(arg)) {
                    AdvancedTab.showDebug = true;
                    break;
                }
            }
        }
        Logger.setup();
        loadNetty();
        config = new Config(new File(getFakerDirectory(), "faker_config.json"));
        accountManager = new AccountManager(new File(getFakerDirectory(), "faker_accounts.json"));
        Window.getInstance();
        registerEvents();
    }

    private static void registerEvents() {

        registerEvent(e -> {
            //uses if block traffic or router spoof was failed for some reason
            if (e instanceof ConnectEvent event) {
                if (event.getConnection().isRedirected()) {
                    InetAddress realSrc = event.getConnection().getRealSrcAddress().getAddress();
                    if (realSrc instanceof Inet4Address ipv4) {
                        if (getConfig().blockTraffic.get() && blockTraffic == 0) {
                            if (!NetworkUtil.isFromNetworkWithInternetAccess(ipv4)) {
                                startBlockTraffic(ipv4);
                            }
                        }

                        if (getConfig().routerSpoof.get() && routerPortForward == 0 && routerBlockTraffic == 0) {
                            if (!NetworkUtil.isFromNetworkWithInternetAccess(ipv4)) {
                                startRouterBlockTraffic(ipv4);
                            }
                        }
                    }
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (isStarted()) {
                Logger.info("Shutdown...");
                kickAllClients(null);
//                stopProxy();
            }
        }));
    }

    public static boolean isStarted() {
        return currentProxyServer != null;
    }

    public static void startProxy() {
        if (currentProxyServer != null) {
            throw new IllegalStateException("Proxy is already running");
        }
        try {

            Logger.info("Starting proxy server");
            event(new ProxyStateEvent(ProxyStateEvent.State.STARTING));
            currentProxyServer = new NetServer(Client2ProxyHandler::new, Client2ProxyChannelInitializer::new);

            try {
                currentProxyServer.bind(proxyAddress, false);
            } catch (Exception ex) {
                Logger.info("Failed bind proxy server to " + proxyAddress);
                //noinspection ConstantConditions
                if (ex instanceof BindException) {
                    currentProxyServer.bind(new InetSocketAddress(proxyAddress.getAddress(), 0), false);
                    proxyAddress = (InetSocketAddress) currentProxyServer.getChannel().localAddress();
                }
            }
            Logger.info("Bind proxy server to " + proxyAddress);

            if (Proxy.getConfig().routerSpoof.get()) {
                startPortForward();
            }
            if (Proxy.getConfig().blockTraffic.get()) {
                startBlockTraffic();
            }

            startRedirect();

            if (Proxy.getConfig().tracerouteFix.get()) {
                enableTtlFix();
            }
            if (Proxy.getConfig().mdnsDisable.get()) {
                mdnsDisable();
            }

            event(new ProxyStateEvent(ProxyStateEvent.State.STARTED));
//            currentProxyServer.getChannel().closeFuture().syncUninterruptibly();
        } catch (Throwable e) {
            event(new ProxyStateEvent(ProxyStateEvent.State.STOPPED));
            currentProxyServer = null;
            throw e;
        }
    }

    public static void kickAllClients(String reason) {
        for (Channel channel : CLIENT_CHANNELS) {
            try {
                ProxyConnection.fromChannel(channel).kickClient(reason);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void stopProxy() {
        if (currentProxyServer != null) {
            Logger.info("Stopping proxy server");
            event(new ProxyStateEvent(ProxyStateEvent.State.STOPPING));
            stopRedirect();
            if (Proxy.getConfig().tracerouteFix.get()) {
                disableTtlFix();
            }
            if (Proxy.getConfig().mdnsDisable.get()) {
                mdnsRestore();
            }
            if (Proxy.getConfig().routerSpoof.get()) {
                stopPortForward();
            }
            if (Proxy.getConfig().blockTraffic.get()) {
                stopBlockTraffic();
            }
            currentProxyServer.getChannel().close();
            currentProxyServer = null;

            kickAllClients("Proxy stopped");
            event(new ProxyStateEvent(ProxyStateEvent.State.STOPPED));
        }
    }

    private static void loadNetty() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        if (System.getProperty("io.netty.allocator.maxOrder") == null) {
            System.setProperty("io.netty.allocator.maxOrder", "9");
        }
        MCPipeline.useOptimizedPipeline();
        CLIENT_CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    }


    public static InetSocketAddress getTargetHandshakeAddress() {
        return config.getTargetHandshakeAddress();
    }

    public static InetSocketAddress getTargetAddress() {
        return config.getTargetAddress();
    }

    private static int getTargetPort() {
        InetSocketAddress targetAddress = getTargetAddress();
        if (targetAddress == null) {
            return 25565;
        }
        return targetAddress.getPort();
    }

    public static void setBackendProxy(URI backendProxy) {
        Proxy.backendProxy = backendProxy;
    }

    public static URI getBackendProxy() {
        return backendProxy;
    }

    public static AccountManager getAccountManager() {
        return accountManager;
    }

    public static Account getAccount() {
        return account;
    }

    public static void setAccount(Account account) {
        Proxy.account = account;
    }

    public static Config getConfig() {
        return config;
    }

    public static NetworkInterface getTargetAdapter() {
        return targetAdapter;
    }

    public static void setTargetAdapter(NetworkInterface targetAdapter) {
        Proxy.targetAdapter = targetAdapter;
    }

    public static void suspendRedirect() {
        if (!WinRedirect.isSupported()) {
            return;
        }
        if (forward_redirect != 0) {
            WinRedirect.redirectPause(forward_redirect);
        }
        if (redirect != 0) {
            WinRedirect.redirectPause(redirect);
        }
        event(new RedirectStateChangeEvent(RedirectStateChangeEvent.State.PAUSED));
    }

    public static void resumeRedirect() {
        if (!WinRedirect.isSupported()) {
            return;
        }
        if (forward_redirect != 0) {
            WinRedirect.redirectResume(forward_redirect);
        }
        if (redirect != 0) {
            WinRedirect.redirectResume(redirect);
        }
        event(new RedirectStateChangeEvent(RedirectStateChangeEvent.State.RESUMED));
    }

    private static void mdnsDisable() {
        if (!WinRedirect.isSupported()) {
            return;
        }
        mdns = WinRedirect.mdnsLlmnrDisable(null);
    }

    private static void mdnsRestore() {
        if (!WinRedirect.isSupported()) {
            return;
        }
        if (mdns != 0) {
            WinRedirect.mdnsLlmnrRestore(mdns);
            mdns = 0;
        }
    }

    private static void enableTtlFix() {
        if (!WinRedirect.isSupported()) {
            return;
        }
        WinRedirect.enableTtlFix();
    }

    private static void disableTtlFix() {
        if (!WinRedirect.isSupported()) {
            return;
        }
        WinRedirect.disableTtlFix();
    }

    private static void stopRedirect() {
        if (!WinRedirect.isSupported()) {
            return;
        }
        if (forward_redirect != 0) {
            WinRedirect.redirectStop(forward_redirect);
            forward_redirect = 0;
        }
        if (redirect != 0) {
            WinRedirect.redirectStop(redirect);
            redirect = 0;
        }
    }

    private static void startRouterBlockTraffic(Inet4Address address) {

        if (!WinRedirect.isSupported()) {
            return;
        }
        Inet4Address listen = NetworkUtil.getLocalAddressInSameNetworkFor(address);
        if (listen == null) {
            Logger.error("(router block traffic) Not found local address for " + address.getHostAddress());
            return;
        }

        String srcIp = address.getHostAddress();
        String dstIp = listen.getHostAddress();
        int[] skipTcpPorts = null;
        int[] skipUdpPorts = {53};
        try {
            if (httpHostSpoofer != null) {
                httpHostSpoofer.interrupt();
            }
            NetworkInterface internetInterface = NetworkUtil.getInternetInterface();
            if (internetInterface != null) {
                Inet4Address gateway = internetInterface.get4Gateway();
                if (gateway != null) {
                    httpHostSpoofer = new HttpHostSpoofer(gateway, dstIp, gateway.getHostAddress());
                    httpHostSpoofer.start();
                    skipTcpPorts = new int[]{80};
                }
            }

        } catch (Throwable ignored) {
            httpHostSpoofer = null;
        }
        StringBuilder filter = new StringBuilder();
        filter.append(String.format("(ip.SrcAddr == %s and ip.DstAddr == %s)", srcIp, dstIp));
        addPortsToFilter(filter, skipTcpPorts, skipUdpPorts);
        routerBlockTraffic = WinRedirect.blockIpStart(filter.toString(), WinRedirect.Layer.NETWORK);

    }

    private static void startPortForward() {
        if (!WinRedirect.isSupported()) {
            return;
        }
        if (targetAdapter == NetworkInterface.NULL || targetAdapter == null) {
            Logger.error("(port forward) Target adapter is null");
            return;
        }
        if (targetAdapter.hasInternetAccess()) {
            Logger.error("(port forward) Target adapter has Internet access, skip");
            return;
        }
        InterfaceAddress listenInterfaceAddress = targetAdapter.getFirstIpv4InterfaceAddress();
        if (listenInterfaceAddress == null) {
            Logger.error("(port forward) Not found ipv4 address in target adapter");
            return;
        }
        Inet4Address listen = (Inet4Address) listenInterfaceAddress.getAddress();
        Inet4Address start = NetworkUtil.getStart(listen, listenInterfaceAddress.getNetworkPrefixLength());
        if (listen.hashCode() == start.hashCode()) {
            try {
                start = (Inet4Address) InetAddress.getByAddress(Ints.toByteArray(start.hashCode() + 1));
            } catch (Exception ignored) {

            }
        }
        Inet4Address end = NetworkUtil.getEnd(listen, listenInterfaceAddress.getNetworkPrefixLength());
        NetworkInterface internetInterface = NetworkUtil.getInternetInterface();
        if (internetInterface == null) {
            Logger.error("(port forward) Internet adapter not found");
            return;
        }
        Inet4Address to = internetInterface.get4Gateway();
        if (to == null) {
            Logger.error("(port forward) Internet gateway not found");
            return;
        }
        Inet4Address thisPC = internetInterface.getFirstIpv4Address();
        if (thisPC == null) {
            Logger.error("(port forward) This pc ipv4 not found");
            return;
        }
        String listenIp = listen.getHostAddress();
        String startIp = start.getHostAddress();
        String endIp = end.getHostAddress();
        String toIp = to.getHostAddress();
        String thisIp = thisPC.getHostAddress();
        Logger.raw("port forward");
        Logger.raw(listenIp + " " + startIp + " " + endIp + " " + toIp + " " + thisIp);
        int[] skipPorts = null;
        try {
            if (httpHostSpoofer == null) {
                httpHostSpoofer = new HttpHostSpoofer(to, listenIp, toIp);
            }
            httpHostSpoofer.start();
            skipPorts = new int[]{80};
        } catch (Throwable ignored) {
            httpHostSpoofer = null;
        }
        routerPortForward = WinRedirect.portForwardStart(listenIp, startIp, endIp, toIp, thisIp, skipPorts);
    }

    private static void stopPortForward() {
        if (!WinRedirect.isSupported()) {
            return;
        }
        if (routerPortForward != 0) {
            WinRedirect.portForwardStop(routerPortForward);
            routerPortForward = 0;
        }

        if (httpHostSpoofer != null) {
            httpHostSpoofer.interrupt();
            httpHostSpoofer = null;
        }
        if (routerBlockTraffic != 0) {
            WinRedirect.blockIpStop(routerBlockTraffic);
            routerBlockTraffic = 0;
        }
    }

    private static void startBlockTraffic() {
        if (!WinRedirect.isSupported()) {
            return;
        }

        startBlockTraffic(targetAdapter);
    }

    private static void startBlockTraffic(Inet4Address targetAddress) {
        NetworkInterface internetInterface = NetworkUtil.getInternetInterface();
        if (internetInterface == null) {
            Logger.error("(block traffic) Internet adapter not found");
            return;
        }
        InterfaceAddress internetInterfaceAddress = internetInterface.getFirstIpv4InterfaceAddress();
        if (internetInterfaceAddress == null) {
            Logger.error("(block traffic) Internet ipv4 not found");
            return;
        }
        Inet4Address internet = (Inet4Address) internetInterfaceAddress.getAddress();
        Inet4Address fromStart = NetworkUtil.getStart(internet, internetInterfaceAddress.getNetworkPrefixLength());
        Inet4Address fromEnd = NetworkUtil.getEnd(internet, internetInterfaceAddress.getNetworkPrefixLength());
        String filter = String.format("(ip.SrcAddr >= %s and ip.SrcAddr <= %s and ip.DstAddr == %s)", fromStart.getHostAddress(), fromEnd.getHostAddress(), targetAddress.getHostAddress());
        blockTraffic = WinRedirect.blockIpStart(filter, WinRedirect.Layer.NETWORK_FORWARD);
    }

    private static void startBlockTraffic(NetworkInterface targetAdapter) {
        if (targetAdapter == NetworkInterface.NULL || targetAdapter == null) {
            Logger.error("(block traffic) Target adapter is null");
            return;
        }
        if (targetAdapter.hasInternetAccess()) {
            Logger.error("(block traffic) Target adapter has Internet access, skip");
            return;
        }
        InterfaceAddress listenInterfaceAddress = targetAdapter.getFirstIpv4InterfaceAddress();
        if (listenInterfaceAddress == null) {
            Logger.error("(block traffic) Not found ipv4 address in target adapter");
            return;
        }
        NetworkInterface internetInterface = NetworkUtil.getInternetInterface();
        if (internetInterface == null) {
            Logger.error("(block traffic) Internet adapter not found");
            return;
        }
        InterfaceAddress internetInterfaceAddress = internetInterface.getFirstIpv4InterfaceAddress();
        if (internetInterfaceAddress == null) {
            Logger.error("(block traffic) Internet ipv4 not found");
            return;
        }

        Inet4Address listen = (Inet4Address) listenInterfaceAddress.getAddress();
        Inet4Address toStart = NetworkUtil.getStart(listen, listenInterfaceAddress.getNetworkPrefixLength());
        Inet4Address toEnd = NetworkUtil.getEnd(listen, listenInterfaceAddress.getNetworkPrefixLength());

        Inet4Address internet = (Inet4Address) internetInterfaceAddress.getAddress();
        Inet4Address fromStart = NetworkUtil.getStart(internet, internetInterfaceAddress.getNetworkPrefixLength());
        Inet4Address fromEnd = NetworkUtil.getEnd(internet, internetInterfaceAddress.getNetworkPrefixLength());

        if (toStart.equals(fromStart) || toEnd.equals(fromEnd)) {
            Logger.error("(block traffic) Target and Internet adapters same network");
            return;
        }
        String filter = String.format("(ip.SrcAddr >= %s and ip.SrcAddr <= %s and ip.DstAddr >= %s and ip.DstAddr <= %s)", fromStart.getHostAddress(), fromEnd.getHostAddress(), toStart.getHostAddress(), toEnd.getHostAddress());
        blockTraffic = WinRedirect.blockIpStart(filter, WinRedirect.Layer.NETWORK_FORWARD);
    }

    private static void stopBlockTraffic() {
        if (!WinRedirect.isSupported()) {
            return;
        }
        if (blockTraffic != 0) {
            WinRedirect.blockIpStop(blockTraffic);
            blockTraffic = 0;
        }
    }

    private static void startRedirect() {
        if (!WinRedirect.isSupported()) {
            return;
        }
        forward_redirect = WinRedirect.redirectStart(getTargetPort(), proxyAddress.getPort(), null, null, WinRedirect.Layer.NETWORK_FORWARD);
        if (forward_redirect == 0) {
            currentProxyServer.getChannel().close();
            throw new RuntimeException(WinRedirect.getError());
        }
        redirect = WinRedirect.redirectStart(getTargetPort(), proxyAddress.getPort(), null, null, WinRedirect.Layer.NETWORK);
        if (redirect == 0) {
            currentProxyServer.getChannel().close();
            throw new RuntimeException(WinRedirect.getError());
        }
    }

    public static void registerEvent(Consumer<Event> consumer) {
        events.add(consumer);
    }

    public static void unregisterEvent(Consumer<Event> consumer) {
        events.remove(consumer);
    }

    public static void event(Event event) {
        for (Consumer<Event> c : events) {
            c.accept(event);
        }
    }

    private static String fakerDirectory;

    public static String getFakerDirectory() {
        if (fakerDirectory == null) {
            File dir;
            if (Sys.isWindows()) {
                String appdata = System.getenv("APPDATA");
                if (appdata != null) {
                    dir = new File(appdata, ".faker");
                } else {
                    dir = new File(System.getProperty("user.home"), ".faker");
                }
            } else if (Sys.isMac()) {
                dir = new File(System.getProperty("user.home") + "/Library/Application Support", "faker");
            } else {
                dir = new File(System.getProperty("user.home"), ".faker");
            }
            if (!dir.exists() && !dir.mkdirs()) {
                dir = null;
            } else if (dir.isFile()) {
                dir = null;
            }
            if (dir == null) {
                dir = new File("").getParentFile();
            }
            fakerDirectory = dir.getAbsolutePath();
        }
        return fakerDirectory;
    }
}
