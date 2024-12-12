package net.java.mproxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.GlobalEventExecutor;
import net.java.mproxy.save.SaveManager;
import net.java.mproxy.ui.Window;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.NetServer;
import net.java.mproxy.proxy.client2proxy.Client2ProxyChannelInitializer;
import net.java.mproxy.proxy.client2proxy.Client2ProxyHandler;
import net.java.mproxy.proxy.session.DualConnection;
import net.java.mproxy.proxy.session.ProxyConnection;
import net.java.mproxy.auth.Account;
import net.java.mproxy.auth.MicrosoftAccount;
import net.java.mproxy.util.logging.Logger;
import net.raphimc.netminecraft.util.ChannelType;
import net.raphimc.netminecraft.util.MinecraftServerAddress;

import java.io.File;
import java.io.FileReader;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

public class Proxy {
    public static InetSocketAddress proxyAddress = new InetSocketAddress("0.0.0.0", 445);
    private static InetSocketAddress targetHandshakeAddress = setTargetHandshakeAddress((String) null);
    private static InetSocketAddress targetAddress;
    public static final int compressionThreshold = 256;
    public static final int connectTimeout = 8000;


    public static Account account;
    public static final boolean SIGN_CHAT = true;
    public static final boolean ONLINE_MODE = true; // also encrypt client -> proxy connection
    private static NetServer currentProxyServer;
    private static ChannelGroup CLIENT_CHANNELS;

    public static ChannelGroup getConnectedClients() {
        return CLIENT_CHANNELS;
    }

    public static ArrayList<InetSocketAddress> connectedAddresses = new ArrayList<>();

    private static MicrosoftAccount auth() {
        HttpClient httpClient = MinecraftAuth.createHttpClient();
        try {
            try {
                File account = new File("account.json");
                JsonObject serializedSession = (JsonObject) JsonParser.parseReader(new FileReader(account));
                StepFullJavaSession.FullJavaSession loadedSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(serializedSession);
                StepFullJavaSession.FullJavaSession readyToUseSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(httpClient, loadedSession);
                return new MicrosoftAccount(readyToUseSession);
            } catch (Throwable e) {
                StepFullJavaSession.FullJavaSession javaSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                    // Method to generate a verification URL and a code for the user to enter on that page
//                    System.out.println("Go to " + msaDeviceCode.getVerificationUri());
//                    System.out.println("Enter code " + msaDeviceCode.getUserCode());

                    // There is also a method to generate a direct URL without needing the user to enter a code
                    System.out.println("Go to " + msaDeviceCode.getDirectVerificationUri());
                }));
                JsonObject serializedSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(javaSession);
                Files.write(new File("account.json").toPath(), serializedSession.toString().getBytes(StandardCharsets.UTF_8));
                return new MicrosoftAccount(javaSession);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public static long forward_redirect;
    public static long redirect;

    private static int getTargetPort() {
        if (targetAddress == null) {
            return 25565;
        }
        return targetAddress.getPort();
    }

    public static void main(String[] args) throws Throwable {

        Logger.setup();
        account = auth();
        loadNetty();
        Window window = new Window();
        startProxy();
    }


    public static void startProxy() {
        if (currentProxyServer != null) {
            throw new IllegalStateException("Proxy is already running");
        }
        try {
            Logger.LOGGER.info("Starting proxy server");
            currentProxyServer = new NetServer(Client2ProxyHandler::new, Client2ProxyChannelInitializer::new) {
                @Override
                public void bind(SocketAddress address, boolean blocking) {
                    if (this.channelFuture == null) {
                        this.initialize(ChannelType.get(address), new ServerBootstrap());
                    }
                    System.out.println("1");
                    this.getChannel().bind(address).syncUninterruptibly();
                    System.out.println("2 "+getChannel());
                    if (blocking) {
                        this.getChannel().closeFuture().syncUninterruptibly();
                    }
                    System.out.println("3");
                }
            };
            Logger.LOGGER.info("Binding proxy server to " + proxyAddress);
            try {
                currentProxyServer.bind(proxyAddress);
            } catch (Exception ex) {
                if (ex instanceof BindException) {
                    System.out.println("TRY");
                    currentProxyServer.bind(new InetSocketAddress(proxyAddress.getAddress(), 0));
                    System.out.println("TRY");
                    proxyAddress = (InetSocketAddress) currentProxyServer.getChannel().localAddress();
                }
            }

            System.out.println("HERE? " + proxyAddress);
            forward_redirect = WinRedirect.redirectStart(getTargetPort(), proxyAddress.getPort(), null, null, WinRedirect.Layer.NETWORK_FORWARD);
            if (forward_redirect == 0) {
                System.out.println(WinRedirect.getError());
                return;
            }
            redirect = WinRedirect.redirectStart(getTargetPort(), proxyAddress.getPort(), null, null, WinRedirect.Layer.NETWORK);
            if (redirect == 0) {
                System.out.println(WinRedirect.getError());
                return;
            }

        } catch (Throwable e) {
            currentProxyServer = null;
            throw e;
        }
    }

    public static void stopProxy() {
        if (currentProxyServer != null) {
            Logger.LOGGER.info("Stopping proxy server");


            currentProxyServer.getChannel().close();
            currentProxyServer = null;

            for (Channel channel : CLIENT_CHANNELS) {
                try {
                    ProxyConnection.fromChannel(channel).kickClient("Proxy stopped");
                } catch (Throwable ignored) {
                }
            }
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

    public static DualConnection dualConnection;

    public static InetSocketAddress getTargetHandshakeAddress() {
        return targetHandshakeAddress;
    }

    public static InetSocketAddress getTargetAddress() {
        return targetAddress;
    }

    public static InetSocketAddress setTargetHandshakeAddress(String host) {
        return setTargetHandshakeAddress(host, 25565);
    }

    public static InetSocketAddress setTargetHandshakeAddress(String host, int port) {
        if (host == null) {
            return setTargetHandshakeAddress((InetSocketAddress) null);
        }
        return setTargetHandshakeAddress(new InetSocketAddress(host, port));
    }

    public static InetSocketAddress setTargetHandshakeAddress(InetSocketAddress targetHandshakeAddress) {
        Proxy.targetHandshakeAddress = targetHandshakeAddress;
        if (targetHandshakeAddress == null) {
            targetAddress = null;
        } else {
            targetAddress = MinecraftServerAddress.ofResolved(targetHandshakeAddress.getHostName(), targetHandshakeAddress.getPort());
        }
        return targetHandshakeAddress;
    }

    private static SaveManager saveManager = new SaveManager();

    public static SaveManager getSaveManager() {
        return saveManager;
    }

    public static Account getAccount() {
        return account;
    }

    public static void setAccount(Account account) {
        Proxy.account = account;
    }
}
