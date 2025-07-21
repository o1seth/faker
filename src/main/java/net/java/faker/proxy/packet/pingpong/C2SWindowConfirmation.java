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

package net.java.faker.proxy.packet.pingpong;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public class C2SWindowConfirmation implements C2SAbstractPong {
    public int windowId;
    public short uid;
    public boolean accepted;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.windowId = byteBuf.readByte();
        this.uid = byteBuf.readShort();
        this.accepted = byteBuf.readByte() != 0;
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        byteBuf.writeByte(this.windowId);
        byteBuf.writeShort(this.uid);
        byteBuf.writeByte(this.accepted ? 1 : 0);
    }

    @Override
    public long getId() {
        return uid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        C2SWindowConfirmation that = (C2SWindowConfirmation) o;
        return windowId == that.windowId && uid == that.uid && accepted == that.accepted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowId, uid, accepted);
    }

    @Override
    public String toString() {
        return "C2SWindowConfirmation " + windowId + ": " + uid + ", " + accepted;
    }
}