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

public class S2CEntityPositionSync implements Packet {
    public int entityId;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public boolean onGround;
    private double dx;
    private double dy;
    private double dz;

    public static S2CEntityPositionSync fromVehicle(C2SMoveVehicle moveVehicle, int vehicleId) {
        S2CEntityPositionSync positionSync = new S2CEntityPositionSync();
        positionSync.x = moveVehicle.x;
        positionSync.y = moveVehicle.y;
        positionSync.z = moveVehicle.z;

        positionSync.yaw = moveVehicle.yaw;
        positionSync.pitch = moveVehicle.pitch;
        positionSync.entityId = vehicleId;
        return positionSync;
    }

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.entityId = PacketTypes.readVarInt(byteBuf);
        if (protocolVersion < MCVersion.v1_9) {
            this.x = byteBuf.readInt() / 32D;
            this.y = byteBuf.readInt() / 32D;
            this.z = byteBuf.readInt() / 32D;
            this.yaw = (float) (byteBuf.readByte() * 360) / 256.0F;
            this.pitch = (float) (byteBuf.readByte() * 360) / 256.0F;
            this.onGround = byteBuf.readBoolean();
        } else if (protocolVersion < MCVersion.v1_21_2) {
            this.x = byteBuf.readDouble();
            this.y = byteBuf.readDouble();
            this.z = byteBuf.readDouble();
            this.yaw = (float) (byteBuf.readByte() * 360) / 256.0F;
            this.pitch = (float) (byteBuf.readByte() * 360) / 256.0F;
            this.onGround = byteBuf.readBoolean();
        } else {//>=1.21.2
            this.x = byteBuf.readDouble();
            this.y = byteBuf.readDouble();
            this.z = byteBuf.readDouble();
            this.dx = byteBuf.readDouble();
            this.dy = byteBuf.readDouble();
            this.dz = byteBuf.readDouble();
            this.yaw = byteBuf.readFloat();
            this.pitch = byteBuf.readFloat();
            this.onGround = byteBuf.readBoolean();
        }
    }


    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        PacketTypes.writeVarInt(byteBuf, this.entityId);
        if (protocolVersion < MCVersion.v1_9) {
            byteBuf.writeInt(floor_double(this.x * 32D));
            byteBuf.writeInt(floor_double(this.y * 32D));
            byteBuf.writeInt(floor_double(this.z * 32D));
            byteBuf.writeByte((byte) ((int) (this.yaw * 256.0F / 360.0F)));
            byteBuf.writeByte((byte) ((int) (this.pitch * 256.0F / 360.0F)));
            byteBuf.writeBoolean(this.onGround);
        } else if (protocolVersion < MCVersion.v1_21_2) {

            byteBuf.writeDouble(this.x);
            byteBuf.writeDouble(this.y);
            byteBuf.writeDouble(this.z);
            byteBuf.writeByte((byte) ((int) (this.yaw * 256.0F / 360.0F)));
            byteBuf.writeByte((byte) ((int) (this.pitch * 256.0F / 360.0F)));
            byteBuf.writeBoolean(this.onGround);
        } else {//>=1.21.2
            byteBuf.writeDouble(this.x);
            byteBuf.writeDouble(this.y);
            byteBuf.writeDouble(this.z);
            byteBuf.writeDouble(this.dx);
            byteBuf.writeDouble(this.dy);
            byteBuf.writeDouble(this.dz);
            byteBuf.writeFloat(this.yaw);
            byteBuf.writeFloat(this.pitch);
            byteBuf.writeBoolean(this.onGround);
        }

    }

    private static int floor_double(double value) {
        int i = (int) value;
        return value < (double) i ? i - 1 : i;
    }
}