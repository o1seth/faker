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

import java.nio.charset.StandardCharsets;

public class C2SCookieResponse implements Packet {
    public String key;
    public byte[] payload;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.key = PacketTypes.readString(byteBuf, 32767);
        boolean notNull = byteBuf.readBoolean();
        if (notNull) {
            this.payload = PacketTypes.readByteArray(byteBuf, 5120);
        }

    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        PacketTypes.writeString(byteBuf, this.key);
        if (payload == null) {
            byteBuf.writeBoolean(false);
        } else {
            byteBuf.writeBoolean(true);
            PacketTypes.writeByteArray(byteBuf, this.payload);
        }


    }

    @Override
    public String toString() {
        return "C2SCookieResponse " + key + " : " + (payload == null ? "null" : new String(payload, StandardCharsets.UTF_8));
    }

    public static class Login extends C2SCookieResponse {

    }

    public static class Config extends C2SCookieResponse {

    }
}