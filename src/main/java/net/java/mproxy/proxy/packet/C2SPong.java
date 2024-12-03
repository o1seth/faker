package net.java.mproxy.proxy.packet;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.packet.Packet;

public class C2SPong implements Packet {
    public int id;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
        this.id = byteBuf.readInt();
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
        byteBuf.writeInt(this.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        C2SPong c2SPong = (C2SPong) o;
        return id == c2SPong.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "C2SPong " + id;
    }
}