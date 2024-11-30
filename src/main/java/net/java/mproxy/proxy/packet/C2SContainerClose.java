package net.java.mproxy.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.constants.MCVersion;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;

public class C2SContainerClose implements Packet {
    public int containerId;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        if (protocolVersion >= MCVersion.v1_21_2) {
            this.containerId = PacketTypes.readVarInt(byteBuf);
        } else {
            this.containerId = byteBuf.readByte();
        }

    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        if (protocolVersion >= MCVersion.v1_21_2) {
            PacketTypes.writeVarInt(byteBuf, this.containerId);
        } else {
            byteBuf.writeByte(this.containerId);
        }
    }
}