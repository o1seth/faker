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
import net.raphimc.netminecraft.packet.impl.common.S2CTransferPacket;
import net.java.faker.Proxy;
import net.java.faker.proxy.session.ProxyConnection;
import net.java.faker.proxy.util.TransferDataHolder;
import net.java.faker.util.logging.Logger;

import java.net.InetSocketAddress;
import java.util.List;

public class TransferPacketHandler extends PacketHandler {

    public TransferPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public void handleP2S(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof S2CTransferPacket transferPacket) {
            final InetSocketAddress newAddress = new InetSocketAddress(transferPacket.host, transferPacket.port);
            TransferDataHolder.addTempRedirect(this.proxyConnection.getC2P(), newAddress);
            InetSocketAddress clientHandshakeAddress = this.proxyConnection.getClientHandshakeAddress();
            if (clientHandshakeAddress != null) {
                //redirect client to proxy address
                transferPacket.host = clientHandshakeAddress.getHostName();
                transferPacket.port = clientHandshakeAddress.getPort();
            } else {
                Logger.u_warn("transfer", this.proxyConnection, "Client handshake address is invalid, using Faker bind address instead");

                if (!(this.proxyConnection.getC2P().localAddress() instanceof InetSocketAddress clientAddress)) {
                    throw new IllegalArgumentException("Client address must be an InetSocketAddress");
                }
                transferPacket.host = clientAddress.getHostString();
                transferPacket.port = Proxy.proxyAddress.getPort();
            }
        }

    }

}
