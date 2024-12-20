package net.java.faker.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;

public class S2CDestroyEntities implements Packet {
    public int[] entities;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {

        int len = PacketTypes.readVarInt(byteBuf);
        this.entities = new int[len];
        for (int i = 0; i < len; i++) {
            this.entities[i] = PacketTypes.readVarInt(byteBuf);
        }
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        PacketTypes.writeVarInt(byteBuf, this.entities.length);
        for (int i = 0; i < entities.length; i++) {
            PacketTypes.writeVarInt(byteBuf, entities[i]);
        }
    }
}