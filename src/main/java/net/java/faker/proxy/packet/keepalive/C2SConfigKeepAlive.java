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

package net.java.faker.proxy.packet.keepalive;

import net.raphimc.netminecraft.packet.impl.common.S2CKeepAlivePacket;
import net.raphimc.netminecraft.packet.impl.configuration.C2SConfigKeepAlivePacket;

public class C2SConfigKeepAlive extends C2SConfigKeepAlivePacket implements C2SAbstractKeepAlive {
    public C2SConfigKeepAlive() {
    }

    public C2SConfigKeepAlive(final long id) {
        super(id);
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        S2CKeepAlivePacket that = (S2CKeepAlivePacket) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "C2SConfigKeepAlive " + id;
    }
}
