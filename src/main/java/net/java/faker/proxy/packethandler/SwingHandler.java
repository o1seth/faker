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
import net.java.faker.proxy.packet.C2SSetCarriedItem;
import net.java.faker.proxy.packet.C2SSwing;
import net.java.faker.proxy.packet.S2CAnimate;
import net.java.faker.proxy.packet.S2CSetCarriedItem;
import net.java.faker.proxy.session.DualConnection;
import net.java.faker.proxy.session.ProxyConnection;
import net.raphimc.netminecraft.packet.Packet;

import java.util.List;

public class SwingHandler extends PacketHandler {

    public SwingHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) throws Exception {
        if (packet instanceof C2SSwing p) {
            if (!proxyConnection.isController()) {
                return true;
            }
            DualConnection dualConnection = proxyConnection.dualConnection;
            if (dualConnection == null) {
                return true;
            }

            ProxyConnection follower = dualConnection.getFollower();
            if (follower != null && !follower.isClosed()) {
                S2CAnimate animate = new S2CAnimate();

                animate.entityId = dualConnection.entityId;
                animate.action = 0;
                if (p.hand == 1) {
                    animate.action = 3;
                }
                follower.sendToClient(animate);
            }
        }
        return true;
    }


}
