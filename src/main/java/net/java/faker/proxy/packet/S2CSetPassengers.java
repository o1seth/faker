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
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;

public class S2CSetPassengers implements Packet {
    public int vehicle;
    public int[] passengers;

    public S2CSetPassengers() {

    }

    public S2CSetPassengers(int vehicle) {
        this.vehicle = vehicle;
        this.passengers = new int[0];
    }

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.vehicle = PacketTypes.readVarInt(byteBuf);
        int len = PacketTypes.readVarInt(byteBuf);
        this.passengers = new int[len];
        for (int i = 0; i < len; i++) {
            this.passengers[i] = PacketTypes.readVarInt(byteBuf);
        }
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        PacketTypes.writeVarInt(byteBuf, this.vehicle);
        PacketTypes.writeVarInt(byteBuf, this.passengers.length);
        for (int i = 0; i < passengers.length; i++) {
            PacketTypes.writeVarInt(byteBuf, passengers[i]);
        }
    }
}