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
import net.java.faker.proxy.packet.C2SPlayerCommand;
import net.java.faker.proxy.session.DualConnection;
import net.java.faker.proxy.session.ProxyConnection;
import net.raphimc.netminecraft.packet.Packet;

import java.util.List;

public class PlayerCommandHandler extends PacketHandler {

    public PlayerCommandHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) throws Exception {
        if (packet instanceof C2SPlayerCommand p) {
            if (!proxyConnection.isController()) {
                return true;
            }
            DualConnection dualConnection = proxyConnection.dualConnection;
            if (dualConnection == null) {
                return true;
            }

            if (p.action == C2SPlayerCommand.Action.PRESS_SHIFT_KEY || p.action == C2SPlayerCommand.Action.RELEASE_SHIFT_KEY) {
                dualConnection.shiftState = p.action;
            } else if (p.action == C2SPlayerCommand.Action.START_SPRINTING || p.action == C2SPlayerCommand.Action.STOP_SPRINTING) {
                dualConnection.sprintState = p.action;
            }
        }
        return true;
    }


}
