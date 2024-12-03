package net.java.mproxy.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.packet.Packet;

public class S2CPing implements Packet {
    public int id;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.id = byteBuf.readInt();
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        byteBuf.writeInt(this.id);
    }

}