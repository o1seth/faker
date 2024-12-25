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
import java.security.SignatureException;
import java.util.UUID;

//https://github.com/ViaVersion/ViaVersion/blob/master/api/src/main/java/com/viaversion/viaversion/api/minecraft/signature/storage/ChatSession1_19_3.java
public class ChatSession1_19_3 extends ChatSession {
    private final UUID sessionId = UUID.randomUUID();
    private MessageLink link;

    public ChatSession1_19_3(UUID uuid, PrivateKey privateKey, ProfileKey profileKey) {
        super(uuid, privateKey, profileKey);
        this.link = new MessageLink(uuid, this.sessionId);
    }

    public byte[] signChatMessage(MessageMetadata metadata, String content, PlayerMessageSignature[] lastSeenMessages) throws SignatureException {
        return this.sign((signer) -> {
            MessageLink messageLink = this.nextLink();
            MessageBody messageBody = new MessageBody(content, metadata.timestamp(), metadata.salt(), lastSeenMessages);
            signer.accept(Ints.toByteArray(1));
            messageLink.update(signer);
            messageBody.update(signer);
        });
    }

    private MessageLink nextLink() {
        MessageLink messageLink = this.link;
        if (messageLink != null) {
            this.link = messageLink.next();
        }

        return messageLink;
    }

    public UUID getSessionId() {
        return this.sessionId;
    }
}
