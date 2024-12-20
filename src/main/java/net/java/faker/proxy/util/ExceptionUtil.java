package net.java.faker.proxy.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.java.faker.util.logging.Logger;
import net.java.faker.proxy.session.ProxyConnection;

import java.nio.channels.ClosedChannelException;

public class ExceptionUtil {

    public static void handleNettyException(ChannelHandlerContext ctx, Throwable cause, ProxyConnection proxyConnection, boolean client2Proxy) {
        if (!ctx.channel().isOpen()) return;
        if (cause instanceof ClosedChannelException) return;
        if (cause instanceof CloseAndReturn) {
            ctx.channel().close();
            return;
        }

        Logger.error("Caught unhandled netty exception", cause);
        try {
            if (proxyConnection != null) {
                proxyConnection.kickClient("An unhandled error occurred in your connection and it has been closed.\n§aError details for report:" + ExceptionUtil.prettyPrint(cause));
            }
        } catch (Throwable ignored) {
        }
        ctx.channel().close();
    }

    public static String prettyPrint(Throwable t) {
        final StringBuilder msg = new StringBuilder();
        if (t instanceof EncoderException && t.getCause() != null) t = t.getCause();
        if (t instanceof DecoderException && t.getCause() != null) t = t.getCause();
        while (t != null) {
            msg.append("\n");
            msg.append("§c").append(t.getClass().getSimpleName()).append("§7: §f").append(t.getMessage());
            t = t.getCause();
            if (t != null) {
                msg.append(" §9Caused by");
            }
        }
        return msg.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwException(Throwable exception, Object dummy) throws T {
        throw (T) exception;
    }

    public static void throwException(Throwable exception) {
        ExceptionUtil.<RuntimeException>throwException(exception, null);
    }
}
