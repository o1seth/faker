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

package net.java.faker.proxy.packethandler;

import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.packet.Packet;
import net.java.faker.proxy.session.ProxyConnection;

import java.util.List;

public abstract class PacketHandler {

    protected final ProxyConnection proxyConnection;

    public PacketHandler(final ProxyConnection proxyConnection) {
        this.proxyConnection = proxyConnection;
    }


    public boolean handleC2P(final Packet packet, final List<ChannelFutureListener> listeners) throws Exception {
        return true;
    }


    public void handleP2S(final Packet packet, final List<ChannelFutureListener> listeners) throws Exception {

    }

}
