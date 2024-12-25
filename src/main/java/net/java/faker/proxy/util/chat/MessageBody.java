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


import java.nio.charset.StandardCharsets;
import java.time.Instant;

//https://github.com/ViaVersion/ViaVersion/blob/master/api/src/main/java/com/viaversion/viaversion/api/minecraft/signature/model/chain/v1_19_3/MessageBody.java
public class MessageBody {
    private final String content;
    private final Instant timestamp;
    private final long salt;
    private final PlayerMessageSignature[] lastSeenMessages;

    public MessageBody(final String content, final Instant timestamp, final long salt, final PlayerMessageSignature[] lastSeenMessages) {
        this.content = content;
        this.timestamp = timestamp;
        this.salt = salt;
        this.lastSeenMessages = lastSeenMessages;
    }

    public void update(final DataConsumer dataConsumer) {
        dataConsumer.accept(Longs.toByteArray(this.salt));
        dataConsumer.accept(Longs.toByteArray(this.timestamp.getEpochSecond()));
        final byte[] contentData = this.content.getBytes(StandardCharsets.UTF_8);
        dataConsumer.accept(Ints.toByteArray(contentData.length));
        dataConsumer.accept(contentData);

        dataConsumer.accept(Ints.toByteArray(this.lastSeenMessages.length));
        for (PlayerMessageSignature messageSignatureData : this.lastSeenMessages) {
            dataConsumer.accept(messageSignatureData.signatureBytes());
        }
    }
}
