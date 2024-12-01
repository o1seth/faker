package net.java.mproxy.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;

public class S2CEntityAttach implements Packet {
    public int leash;
    public int entity;
    public int vehicle;

    public S2CEntityAttach() {

    }


    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.entity = byteBuf.readInt();
        this.vehicle = byteBuf.readInt();
        this.leash = byteBuf.readByte();
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        byteBuf.writeInt(this.entity);
        byteBuf.writeInt(this.vehicle);
        byteBuf.writeByte(this.leash);
    }
}