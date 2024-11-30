package net.java.mproxy.proxy.util.chat;


import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatSession {
    private final UUID uuid;
    private final PrivateKey privateKey;
    private final ProfileKey profileKey;
    private final Signature signer;

    public ChatSession(UUID uuid, PrivateKey privateKey, ProfileKey profileKey) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(privateKey, "privateKey");
        Objects.requireNonNull(profileKey, "profileKey");
        this.uuid = uuid;
        this.privateKey = privateKey;
        this.profileKey = profileKey;

        try {
            this.signer = Signature.getInstance("SHA256withRSA");
            this.signer.initSign(this.privateKey);
        } catch (Throwable var5) {
            Throwable e = var5;
            throw new RuntimeException("Failed to initialize signature", e);
        }
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public ProfileKey getProfileKey() {
        return this.profileKey;
    }

    public byte[] sign(Consumer<DataConsumer> dataConsumer) throws SignatureException {
        dataConsumer.accept((bytes) -> {
            try {
                this.signer.update(bytes);
            } catch (SignatureException var3) {
                SignatureException e = var3;
                throw new RuntimeException(e);
            }
        });
        return this.signer.sign();
    }
}
