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
import net.java.faker.proxy.session.DualConnection;
import net.raphimc.netminecraft.constants.MCVersion;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;

import java.util.Random;

public abstract class S2CPlayerPosition implements Packet {
    public static int MAGIC_TELEPORT_ID = new Random().nextInt();
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public abstract int getTeleportId();

    public abstract void setTeleportId(int id);

    private static S2CPlayerPosition create0(int protocolVersion) {
        if (protocolVersion >= MCVersion.v1_21_2) {
            return new S2CPlayerPosition.v1_21_2();
        } else if (protocolVersion >= MCVersion.v1_19_4) {
            return new S2CPlayerPosition.v1_19_4();
        } else if (protocolVersion >= MCVersion.v1_17) {
            return new S2CPlayerPosition.v1_17();
        } else if (protocolVersion >= MCVersion.v1_9) {
            return new S2CPlayerPosition.v1_9();
        } else if (protocolVersion >= MCVersion.v1_7_2) {
            return new S2CPlayerPosition.v1_7_2();
        }
        return null;
    }

    public static S2CPlayerPosition create(int protocolVersion) {
        S2CPlayerPosition p = create0(protocolVersion);
        if (protocolVersion >= MCVersion.v1_9 && p != null) {
            p.setTeleportId(MAGIC_TELEPORT_ID);
        }
        return p;
    }

    public static S2CPlayerPosition createFrom(DualConnection connection, C2SMovePlayer move, int protocolVersion) {
        S2CPlayerPosition p = create0(protocolVersion);
        if (p == null) {
            return null;
        }
        if (protocolVersion >= MCVersion.v1_9) {
            p.setTeleportId(MAGIC_TELEPORT_ID);
        }
        p.x = move.getX(connection.playerX);
        p.y = move.getY(connection.playerY);
        p.z = move.getZ(connection.playerZ);
        p.yaw = move.getYaw(connection.playerYaw);
        p.pitch = move.getPitch(connection.playerPitch);

        return p;
    }

    public S2CPlayerPosition() {

    }

    public static class v1_7_2 extends S2CPlayerPosition {
        //1.7-1.8.9
        int flags;

        @Override
        public void read(ByteBuf byteBuf, int protocolVersion) {
            this.x = byteBuf.readDouble();
            this.y = byteBuf.readDouble();
            this.z = byteBuf.readDouble();
            this.yaw = byteBuf.readFloat();
            this.pitch = byteBuf.readFloat();
            this.flags = byteBuf.readUnsignedByte();
        }

        @Override
        public void write(ByteBuf byteBuf, int protocolVersion) {
            byteBuf.writeDouble(this.x);
            byteBuf.writeDouble(this.y);
            byteBuf.writeDouble(this.z);
            byteBuf.writeFloat(this.yaw);
            byteBuf.writeFloat(this.pitch);
            byteBuf.writeByte(flags);
        }

        @Override
        public int getTeleportId() {
            throw new RuntimeException("teleportId doesn't exists on v1_7_2");
        }

        @Override
        public void setTeleportId(int id) {
            throw new RuntimeException("teleportId doesn't exists on v1_7_2");
        }
    }

    public static class v1_9 extends S2CPlayerPosition {
        //1.9-1.16.5
        public int flags;
        public int teleportId;

        @Override
        public void read(ByteBuf byteBuf, int protocolVersion) {
            this.x = byteBuf.readDouble();
            this.y = byteBuf.readDouble();
            this.z = byteBuf.readDouble();
            this.yaw = byteBuf.readFloat();
            this.pitch = byteBuf.readFloat();
            this.flags = byteBuf.readUnsignedByte();
            this.teleportId = PacketTypes.readVarInt(byteBuf);
        }

        @Override
        public void write(ByteBuf byteBuf, int protocolVersion) {
            byteBuf.writeDouble(this.x);
            byteBuf.writeDouble(this.y);
            byteBuf.writeDouble(this.z);
            byteBuf.writeFloat(this.yaw);
            byteBuf.writeFloat(this.pitch);
            byteBuf.writeByte(this.flags);
            PacketTypes.writeVarInt(byteBuf, this.teleportId);
        }

        @Override
        public String toString() {
            return "S2CPlayerPosition.v1_9 " + teleportId;
        }

        @Override
        public int getTeleportId() {
            return this.teleportId;
        }

        @Override
        public void setTeleportId(int teleportId) {
            this.teleportId = teleportId;
        }
    }

    public static class v1_17 extends S2CPlayerPosition {
        //1.17-1.19.3
        public int flags;
        public int teleportId;
        public boolean dismountVehicle;

        @Override
        public void read(ByteBuf byteBuf, int protocolVersion) {
            this.x = byteBuf.readDouble();
            this.y = byteBuf.readDouble();
            this.z = byteBuf.readDouble();
            this.yaw = byteBuf.readFloat();
            this.pitch = byteBuf.readFloat();
            this.flags = byteBuf.readUnsignedByte();
            this.teleportId = PacketTypes.readVarInt(byteBuf);
            this.dismountVehicle = byteBuf.readBoolean();
        }

        @Override
        public void write(ByteBuf byteBuf, int protocolVersion) {
            byteBuf.writeDouble(this.x);
            byteBuf.writeDouble(this.y);
            byteBuf.writeDouble(this.z);
            byteBuf.writeFloat(this.yaw);
            byteBuf.writeFloat(this.pitch);
            byteBuf.writeByte(this.flags);
            PacketTypes.writeVarInt(byteBuf, this.teleportId);
            byteBuf.writeBoolean(this.dismountVehicle);
        }

        @Override
        public String toString() {
            return "S2CPlayerPosition.v1_17 " + teleportId;
        }

        @Override
        public int getTeleportId() {
            return this.teleportId;
        }

        @Override
        public void setTeleportId(int teleportId) {
            this.teleportId = teleportId;
        }
    }

    public static class v1_19_4 extends v1_9 {
        //1.19.4 - 1.21.1
    }

    public static class v1_21_2 extends S2CPlayerPosition {
        public int flags;
        public int teleportId;
        public double dx;
        public double dy;
        public double dz;

        public void read(ByteBuf byteBuf, int protocolVersion) {
            this.teleportId = PacketTypes.readVarInt(byteBuf);

            this.x = byteBuf.readDouble();
            this.y = byteBuf.readDouble();
            this.z = byteBuf.readDouble();

            this.dx = byteBuf.readDouble();
            this.dy = byteBuf.readDouble();
            this.dz = byteBuf.readDouble();

            this.yaw = byteBuf.readFloat();
            this.pitch = byteBuf.readFloat();
            this.flags = byteBuf.readInt();
        }

        @Override
        public void write(ByteBuf byteBuf, int protocolVersion) {
            PacketTypes.writeVarInt(byteBuf, this.teleportId);
            byteBuf.writeDouble(this.x);
            byteBuf.writeDouble(this.y);
            byteBuf.writeDouble(this.z);

            byteBuf.writeDouble(this.dx);
            byteBuf.writeDouble(this.dy);
            byteBuf.writeDouble(this.dz);

            byteBuf.writeFloat(this.yaw);
            byteBuf.writeFloat(this.pitch);
            byteBuf.writeInt(this.flags);
        }

        @Override
        public String toString() {
            return "S2CPlayerPosition.v1_21_2 " + teleportId;
        }

        @Override
        public int getTeleportId() {
            return this.teleportId;
        }

        @Override
        public void setTeleportId(int teleportId) {
            this.teleportId = teleportId;
        }
    }
}
