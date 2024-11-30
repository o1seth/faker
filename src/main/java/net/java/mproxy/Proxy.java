package net.java.mproxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.GlobalEventExecutor;
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

import java.io.File;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Proxy {
    public static final InetSocketAddress proxyAddress = new InetSocketAddress("0.0.0.0", 25565);
    public static final InetSocketAddress targetAddress = new InetSocketAddress("127.0.0.1", 25500);
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

    public static void main(String[] args) throws Throwable {

        Logger.setup();
        account = auth();
        loadNetty();
        startProxy();
    }


    public static void startProxy() {
        if (currentProxyServer != null) {
            throw new IllegalStateException("Proxy is already running");
        }
        try {
            Logger.LOGGER.info("Starting proxy server");
            currentProxyServer = new NetServer(Client2ProxyHandler::new, Client2ProxyChannelInitializer::new);
            Logger.LOGGER.info("Binding proxy server to " + proxyAddress);
            currentProxyServer.bind(proxyAddress);
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
}
