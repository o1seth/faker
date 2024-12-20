package net.java.faker.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.packet.Packet;

public class C2SSetCarriedItem implements Packet {
    public int slot;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.slot = byteBuf.readShort();
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        byteBuf.writeShort(slot);
    }
}
