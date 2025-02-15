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
