package net.java.faker.proxy.packet;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public class C2SWindowConfirmation extends C2SAbstractPong {
    public int windowId;
    public short uid;
    public boolean accepted;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.windowId = byteBuf.readByte();
        this.uid = byteBuf.readShort();
        this.accepted = byteBuf.readByte() != 0;
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        byteBuf.writeByte(this.windowId);
        byteBuf.writeShort(this.uid);
        byteBuf.writeByte(this.accepted ? 1 : 0);
    }

    @Override
    public int getId() {
        return uid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        C2SWindowConfirmation that = (C2SWindowConfirmation) o;
        return windowId == that.windowId && uid == that.uid && accepted == that.accepted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowId, uid, accepted);
    }

    @Override
    public String toString() {
        return "C2SWindowConfirmation " + windowId + ": " + uid + ", " + accepted;
    }
}