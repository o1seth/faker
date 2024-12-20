package net.java.faker.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;

public class S2CSetPassengers implements Packet {
    public int vehicle;
    public int[] passengers;

    public S2CSetPassengers() {

    }

    public S2CSetPassengers(int vehicle) {
        this.vehicle = vehicle;
        this.passengers = new int[0];
    }

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.vehicle = PacketTypes.readVarInt(byteBuf);
        int len = PacketTypes.readVarInt(byteBuf);
        this.passengers = new int[len];
        for (int i = 0; i < len; i++) {
            this.passengers[i] = PacketTypes.readVarInt(byteBuf);
        }
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        PacketTypes.writeVarInt(byteBuf, this.vehicle);
        PacketTypes.writeVarInt(byteBuf, this.passengers.length);
        for (int i = 0; i < passengers.length; i++) {
            PacketTypes.writeVarInt(byteBuf, passengers[i]);
        }
    }
}