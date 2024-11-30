package net.java.mproxy.proxy.util;

import io.netty.channel.Channel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class TransferDataHolder {

//    private static final Map<InetAddress, InetSocketAddress> TEMP_REDIRECTS = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).<InetAddress, InetSocketAddress>build().asMap();

    private static final Map<InetAddress, InetSocketAddress> TEMP_REDIRECTS = new HashMap<>();

    public static void addTempRedirect(final Channel channel, final InetSocketAddress redirect) {
        TEMP_REDIRECTS.put(getChannelAddress(channel), redirect);
    }


    public static InetSocketAddress removeTempRedirect(final Channel channel) {
        return TEMP_REDIRECTS.remove(getChannelAddress(channel));
    }


    public static boolean hasTempRedirect(final Channel channel) {
        return TEMP_REDIRECTS.containsKey(getChannelAddress(channel));
    }


    private static InetAddress getChannelAddress(final Channel channel) {
        return ((InetSocketAddress) channel.remoteAddress()).getAddress();
    }

}
