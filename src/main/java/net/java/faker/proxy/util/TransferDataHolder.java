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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TransferDataHolder {

    private static final long REDIRECT_TTL_MILLIS = TimeUnit.MINUTES.toMillis(1);
    private static final Map<InetAddress, Entry> TEMP_REDIRECTS = new ConcurrentHashMap<>();

    private static final class Entry {
        final InetSocketAddress address;
        final long expiresAt;

        private Entry(InetSocketAddress address, long expiresAt) {
            this.address = address;
            this.expiresAt = expiresAt;
        }
    }

    public static void addTempRedirect(final Channel channel, final InetSocketAddress redirect) {
        cleanupExpired();
        TEMP_REDIRECTS.put(getChannelAddress(channel), new Entry(redirect, System.currentTimeMillis() + REDIRECT_TTL_MILLIS));
    }


    public static InetSocketAddress removeTempRedirect(final Channel channel) {
        final Entry removed = TEMP_REDIRECTS.remove(getChannelAddress(channel));
        if (removed == null) {
            return null;
        }
        if (removed.expiresAt < System.currentTimeMillis()) {
            return null;
        }
        return removed.address;
    }


    public static boolean hasTempRedirect(final Channel channel) {
        cleanupExpired();
        final Entry entry = TEMP_REDIRECTS.get(getChannelAddress(channel));
        if (entry == null) {
            return false;
        }
        if (entry.expiresAt < System.currentTimeMillis()) {
            TEMP_REDIRECTS.remove(getChannelAddress(channel), entry);
            return false;
        }
        return true;
    }


    private static InetAddress getChannelAddress(final Channel channel) {
        return ((InetSocketAddress) channel.remoteAddress()).getAddress();
    }

    private static void cleanupExpired() {
        final long now = System.currentTimeMillis();
        TEMP_REDIRECTS.entrySet().removeIf(e -> e.getValue().expiresAt < now);
    }

}
