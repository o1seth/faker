package net.java.faker.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;

public class C2SPlayerCommand implements Packet {
    public int id;
    public Action action;
    public int data;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.id = PacketTypes.readVarInt(byteBuf);
        this.action = Action.values()[PacketTypes.readVarInt(byteBuf)];
        this.data = PacketTypes.readVarInt(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        PacketTypes.writeVarInt(byteBuf, this.id);
        PacketTypes.writeVarInt(byteBuf, this.action.ordinal());
        PacketTypes.writeVarInt(byteBuf, this.data);
    }

    public static enum Action {
        PRESS_SHIFT_KEY,
        RELEASE_SHIFT_KEY,
        STOP_SLEEPING,
        START_SPRINTING,
        STOP_SPRINTING,
        START_RIDING_JUMP,
        STOP_RIDING_JUMP,
        OPEN_INVENTORY,
        START_FALL_FLYING;
    }
}