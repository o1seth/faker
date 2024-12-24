package net.java.faker.proxy.util;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.java.faker.proxy.session.ProxyConnection;
import net.java.faker.util.logging.Logger;

import java.util.Stack;

public class ChannelUtil {

    private static final AttributeKey<Stack<Boolean>> LAST_AUTO_READ = AttributeKey.valueOf("last-auto-read");

    public static void disableAutoRead(final Channel channel) {
        if (channel == null) {
            return;
        }
//        Logger.info("DISABLE AUTOREAD " + channel);
        if (channel.attr(LAST_AUTO_READ).get() == null) {
            channel.attr(LAST_AUTO_READ).set(new Stack<>());
        }

        channel.attr(LAST_AUTO_READ).get().push(channel.config().isAutoRead());
        channel.config().setAutoRead(false);
    }

    public static void restoreAutoRead(final Channel channel) {
        if (channel == null) {
            return;
        }
//         Logger.LOGGER.info("RESTORE AUTOREAD " + channel);
        if (channel.attr(LAST_AUTO_READ).get() == null) {
            Logger.error("Tried to restore auto read, but it was never disabled");
            return;
        }
        if (channel.config().isAutoRead()) {
            ProxyConnection proxyConnection = ProxyConnection.fromChannel(channel);
            if (proxyConnection == null) {
                Logger.error("Race condition detected: Auto read has been enabled somewhere else, channel " + channel);
            } else {
                Logger.error("Race condition detected: Auto read has been enabled somewhere else, controller? " + proxyConnection.isController() + ", " + channel);
            }
            return;
        }
        channel.config().setAutoRead(channel.attr(LAST_AUTO_READ).get().pop());
    }

}