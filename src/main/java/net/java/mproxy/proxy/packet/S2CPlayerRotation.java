package net.java.mproxy.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.packet.Packet;

public class S2CPlayerRotation implements Packet {
    public float yaw;
    public float pitch;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.yaw = byteBuf.readFloat();
        this.pitch = byteBuf.readFloat();
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        byteBuf.writeFloat(this.yaw);
        byteBuf.writeFloat(this.pitch);
    }
}