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

import net.java.faker.proxy.packet.C2SPong;
import net.java.faker.proxy.packet.S2CPing;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.handshaking.C2SHandshakingClientIntentionPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket;

public class PacketUtils {
    public static String toString(Packet packet) {
        if (packet instanceof C2SHandshakingClientIntentionPacket p) {
            return "C2SIntention " + p.address + " " + p.port + " " + p.protocolVersion + " " + p.intendedState;
        }
        if (packet instanceof C2SLoginHelloPacket p) {
            if (p.uuid != null) {//protocolVersion >= MCVersion.v1_20_2
                return "C2SLoginHello " + p.name + " " + p.expiresAt + " " + p.key + " " + p.uuid;
            }
        }
        if (packet instanceof C2SPong p) {
            return "C2SPong " + p.id;
        }
        if (packet instanceof S2CPing p) {
            return "S2CPing " + p.id;
        }
        return packet.toString();
    }
}
