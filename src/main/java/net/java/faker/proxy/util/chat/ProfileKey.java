package net.java.faker.proxy.util.chat;

import io.netty.buffer.ByteBuf;
import net.raphimc.netminecraft.packet.PacketTypes;

public class ProfileKey {
    long expiresAt;
    byte[] publicKey;
    byte[] keySignature;

    public ProfileKey(long expiresAt, byte[] publicKey, byte[] keySignature) {
        this.expiresAt = expiresAt;
        this.publicKey = publicKey;
        this.keySignature = keySignature;
    }

    public long expiresAt() {
        return this.expiresAt;
    }

    public byte[] publicKey() {
        return this.publicKey;
    }

    public byte[] keySignature() {
        return this.keySignature;
    }

    public void write(final ByteBuf buffer) {
        buffer.writeLong(expiresAt());
        PacketTypes.writeVarInt(buffer, publicKey.length);
        buffer.writeBytes(publicKey);
        PacketTypes.writeVarInt(buffer, keySignature.length);
        buffer.writeBytes(keySignature);
    }
}
