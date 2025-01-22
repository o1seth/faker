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

package net.java.faker.proxy.proxy2server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.java.faker.Proxy;
import net.java.faker.proxy.PacketRegistry;
import net.java.faker.proxy.session.ProxyConnection;
import net.java.faker.util.logging.Logger;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Locale;
import java.util.function.Supplier;

public class Proxy2ServerChannelInitializer extends MinecraftChannelInitializer {

    public Proxy2ServerChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        if (Proxy.getBackendProxy() != null) {
            try {
                channel.pipeline().addLast("NET_PROXY", this.getProxyHandler(Proxy.getBackendProxy()));
            } catch (Exception e) {
                Logger.error("Failed to start proxy " + Proxy.getBackendProxy(), e);
            }
        }
        super.initChannel(channel);
        ProxyConnection proxyConnection = ProxyConnection.fromChannel(channel);


        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(new PacketRegistry(true, proxyConnection.getVersion()));

    }

    protected ProxyHandler getProxyHandler(URI proxyUrl) {
        final InetSocketAddress proxyAddress = new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort());
        final String username = proxyUrl.getUserInfo() != null ? proxyUrl.getUserInfo().split(":")[0] : null;
        final String password = proxyUrl.getUserInfo() != null && proxyUrl.getUserInfo().contains(":") ? proxyUrl.getUserInfo().split(":")[1] : null;

        switch (proxyUrl.getScheme().toUpperCase(Locale.ROOT)) {
            case "HTTP", "HTTPS" -> {
                if (username != null && password != null) return new HttpProxyHandler(proxyAddress, username, password);
                else return new HttpProxyHandler(proxyAddress);
            }
            case "SOCKS4" -> {
                if (username != null) return new Socks4ProxyHandler(proxyAddress, username);
                else return new Socks4ProxyHandler(proxyAddress);
            }
            case "SOCKS5" -> {
                if (username != null && password != null)
                    return new Socks5ProxyHandler(proxyAddress, username, password);
                else return new Socks5ProxyHandler(proxyAddress);
            }
        }

        throw new IllegalArgumentException("Unknown proxy type: " + proxyUrl.getScheme());
    }

}
