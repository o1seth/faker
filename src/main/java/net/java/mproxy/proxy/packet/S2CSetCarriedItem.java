package net.java.mproxy.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.constants.MCVersion;
import net.raphimc.netminecraft.packet.Packet;

public class S2CSetCarriedItem implements Packet {
    public int slot;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.slot = byteBuf.readByte();
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        byteBuf.writeByte(this.slot);
    }
}
