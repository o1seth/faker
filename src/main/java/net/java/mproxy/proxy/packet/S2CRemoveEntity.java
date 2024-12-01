package net.java.mproxy.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;

public class S2CRemoveEntity implements Packet {
    public int entity;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.entity = PacketTypes.readVarInt(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        PacketTypes.writeVarInt(byteBuf, this.entity);
    }
}