package net.java.faker.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;

public class S2CSetEntityMotion implements Packet {
    public int entityId;
    public double motionX;
    public double motionY;
    public double motionZ;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.entityId = PacketTypes.readVarInt(byteBuf);
        this.motionX = byteBuf.readShort() / 8000D;
        this.motionY = byteBuf.readShort() / 8000D;
        this.motionZ = byteBuf.readShort() / 8000D;
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        PacketTypes.writeVarInt(byteBuf, this.entityId);
        byteBuf.writeShort((int) (motionX * 8000D));
        byteBuf.writeShort((int) (motionY * 8000D));
        byteBuf.writeShort((int) (motionZ * 8000D));

    }
}
