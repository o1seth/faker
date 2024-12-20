package net.java.faker.proxy.util.chat;

import java.util.UUID;

public class PlayerMessageSignature {
    private final UUID uuid;
    private final byte[] signatureBytes;

    public PlayerMessageSignature(UUID uuid, byte[] signatureBytes) {
        this.uuid = uuid;
        this.signatureBytes = signatureBytes;
    }

    public UUID uuid() {
        return uuid;
    }

    public byte[] signatureBytes() {
        return signatureBytes;
    }
}
