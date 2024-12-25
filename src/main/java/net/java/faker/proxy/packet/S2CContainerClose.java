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

package net.java.faker.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.constants.MCVersion;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;

public class S2CContainerClose implements Packet {
    public int containerId;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        if (protocolVersion >= MCVersion.v1_21_2) {
            this.containerId = PacketTypes.readVarInt(byteBuf);
        } else {
            this.containerId = byteBuf.readUnsignedByte();
        }

    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        if (protocolVersion >= MCVersion.v1_21_2) {
            PacketTypes.writeVarInt(byteBuf, this.containerId);
        } else {
            byteBuf.writeByte(this.containerId);
        }
    }
}