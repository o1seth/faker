/*
 * This file is part of faker - https://github.com/o1seth/faker
 * Copyright (C) 2024 o1seth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.java.faker.proxy.util.chat;


import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

//https://github.com/ViaVersion/ViaVersion/blob/master/api/src/main/java/com/viaversion/viaversion/api/minecraft/signature/storage/ChatSession.java
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
