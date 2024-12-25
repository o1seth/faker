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
