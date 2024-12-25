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

public abstract class C2SMovePlayer implements Packet {
    private static final int CHANGE_POSITION_FLAG = 1;
    private static final int CHANGE_LOOK_FLAG = 2;
    protected double x;
    protected double y;
    protected double z;
    protected float yaw;
    protected float pitch;
    protected boolean onGround;
    protected boolean horizontalCollision;
    protected boolean changePosition;
    protected boolean changeLook;

    protected C2SMovePlayer() {

    }

    protected C2SMovePlayer(double x, double y, double z, float yaw, float pitch, boolean onGround, boolean horizontalCollision, boolean changePosition, boolean changeLook) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
        this.changePosition = changePosition;
        this.changeLook = changeLook;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getName());
        sb.append(' ');
        if (changePosition) {
            sb.append(' ');
            sb.append(this.x);
            sb.append(' ');
            sb.append(this.y);
            sb.append(' ');
            sb.append(this.z);
        }
        if (changeLook) {
            sb.append(' ');
            sb.append(this.yaw);
            sb.append(' ');
            sb.append(this.pitch);
        }
        sb.append(' ');
        sb.append(onGround);
        return sb.toString();
    }

    public double getX(double currentX) {
        return this.changePosition ? this.x : currentX;
    }

    public double getY(double currentY) {
        return this.changePosition ? this.y : currentY;
    }

    public double getZ(double currentZ) {
        return this.changePosition ? this.z : currentZ;
    }

    public float getYaw(float currentYaw) {
        return this.changeLook ? this.yaw : currentYaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch(float currentPitch) {
        return this.changeLook ? this.pitch : currentPitch;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean horizontalCollision() {
        return this.horizontalCollision;
    }

    static int toFlag(boolean changePosition, boolean changeLook) {
        int i = 0;
        if (changePosition) {
            i |= 1;
        }

        if (changeLook) {
            i |= 2;
        }

        return i;
    }

    static boolean changePosition(int flag) {
        return (flag & 1) != 0;
    }

    static boolean changeLook(int flag) {
        return (flag & 2) != 0;
    }

    public static class Status extends C2SMovePlayer {
        public Status() {

        }

        @Override
        public void read(ByteBuf byteBuf, int protocolVersion) {
            if (protocolVersion >= MCVersion.v1_21_2) {
                short s = byteBuf.readUnsignedByte();
                this.onGround = changePosition(s);
                this.horizontalCollision = changeLook(s);
            } else {
                this.onGround = byteBuf.readBoolean();
            }
        }

        @Override
        public void write(ByteBuf byteBuf, int protocolVersion) {
            if (protocolVersion >= MCVersion.v1_21_2) {
                byteBuf.writeByte(toFlag(this.onGround, this.horizontalCollision));
            } else {
                byteBuf.writeBoolean(this.onGround);
            }
        }
    }

    public static class Rot extends C2SMovePlayer {
        public Rot() {
        }

        @Override
        public void read(ByteBuf byteBuf, int protocolVersion) {
            this.changeLook = true;
            this.yaw = byteBuf.readFloat();
            this.pitch = byteBuf.readFloat();
            if (protocolVersion >= MCVersion.v1_21_2) {
                short s = byteBuf.readUnsignedByte();
                this.onGround = changePosition(s);
                this.horizontalCollision = changeLook(s);
            } else {
                this.onGround = byteBuf.readBoolean();
            }
        }

        @Override
        public void write(ByteBuf byteBuf, int protocolVersion) {
            byteBuf.writeFloat(this.yaw);
            byteBuf.writeFloat(this.pitch);
            if (protocolVersion >= MCVersion.v1_21_2) {
                byteBuf.writeByte(toFlag(this.onGround, this.horizontalCollision));
            } else {
                byteBuf.writeBoolean(this.onGround);
            }
        }
    }

    public static class Pos extends C2SMovePlayer {

        public Pos() {

        }

        @Override
        public void read(ByteBuf byteBuf, int protocolVersion) {
            this.changePosition = true;
            this.x = byteBuf.readDouble();
            this.y = byteBuf.readDouble();
            this.z = byteBuf.readDouble();
            if (protocolVersion >= MCVersion.v1_21_2) {
                short s = byteBuf.readUnsignedByte();
                this.onGround = changePosition(s);
                this.horizontalCollision = changeLook(s);
            } else {
                this.onGround = byteBuf.readBoolean();
            }
        }

        @Override
        public void write(ByteBuf byteBuf, int protocolVersion) {
            byteBuf.writeDouble(this.x);
            byteBuf.writeDouble(this.y);
            byteBuf.writeDouble(this.z);
            if (protocolVersion >= MCVersion.v1_21_2) {
                byteBuf.writeByte(toFlag(this.onGround, this.horizontalCollision));
            } else {
                byteBuf.writeBoolean(this.onGround);
            }
        }
    }

    public static class PosRot extends C2SMovePlayer {

        public PosRot() {
        }

        @Override
        public void read(ByteBuf byteBuf, int protocolVersion) {
            this.changePosition = true;
            this.changeLook = true;
            this.x = byteBuf.readDouble();
            this.y = byteBuf.readDouble();
            this.z = byteBuf.readDouble();
            this.yaw = byteBuf.readFloat();
            this.pitch = byteBuf.readFloat();
            if (protocolVersion >= MCVersion.v1_21_2) {
                short s = byteBuf.readUnsignedByte();
                this.onGround = changePosition(s);
                this.horizontalCollision = changeLook(s);
            } else {
                this.onGround = byteBuf.readBoolean();
            }
        }

        @Override
        public void write(ByteBuf byteBuf, int protocolVersion) {
            byteBuf.writeDouble(this.x);
            byteBuf.writeDouble(this.y);
            byteBuf.writeDouble(this.z);
            byteBuf.writeFloat(this.yaw);
            byteBuf.writeFloat(this.pitch);

            if (protocolVersion >= MCVersion.v1_21_2) {
                byteBuf.writeByte(toFlag(this.onGround, this.horizontalCollision));
            } else {
                byteBuf.writeBoolean(this.onGround);
            }
        }
    }
}
