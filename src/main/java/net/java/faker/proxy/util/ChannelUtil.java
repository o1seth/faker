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
