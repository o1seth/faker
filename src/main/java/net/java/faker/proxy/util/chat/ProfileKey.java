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

package net.java.faker.proxy.util.chat;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.packet.PacketTypes;

//https://github.com/ViaVersion/ViaVersion/blob/master/api/src/main/java/com/viaversion/viaversion/api/minecraft/ProfileKey.java
public class ProfileKey {
    long expiresAt;
    byte[] publicKey;
    byte[] keySignature;

    public ProfileKey(long expiresAt, byte[] publicKey, byte[] keySignature) {
        this.expiresAt = expiresAt;
        this.publicKey = publicKey;
        this.keySignature = keySignature;
    }

    public long expiresAt() {
        return this.expiresAt;
    }

    public byte[] publicKey() {
        return this.publicKey;
    }

    public byte[] keySignature() {
        return this.keySignature;
    }

    public void write(final ByteBuf buffer) {
        buffer.writeLong(expiresAt());
        PacketTypes.writeVarInt(buffer, publicKey.length);
        buffer.writeBytes(publicKey);
        PacketTypes.writeVarInt(buffer, keySignature.length);
        buffer.writeBytes(keySignature);
    }
}
