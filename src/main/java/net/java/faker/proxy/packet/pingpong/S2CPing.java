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

public class S2CPing implements S2CAbstractPing {
    public int id;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.id = byteBuf.readInt();
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        byteBuf.writeInt(this.id);
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "S2CPing " + id;
    }
}