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


import java.util.UUID;

//https://github.com/ViaVersion/ViaVersion/blob/master/api/src/main/java/com/viaversion/viaversion/api/minecraft/signature/model/chain/v1_19_3/MessageLink.java
public class MessageLink {

    private final int index;
    private final UUID sender;
    private final UUID sessionId;

    public MessageLink(final UUID sender, final UUID sessionId) {
        this(0, sender, sessionId);
    }

    public MessageLink(final int index, final UUID sender, final UUID sessionId) {
        this.index = index;
        this.sender = sender;
        this.sessionId = sessionId;
    }

    public void update(final DataConsumer dataConsumer) {
        dataConsumer.accept(this.sender);
        dataConsumer.accept(this.sessionId);
        dataConsumer.accept(Ints.toByteArray(this.index));
    }

    public MessageLink next() {
        return this.index == Integer.MAX_VALUE ? null : new MessageLink(this.index + 1, this.sender, this.sessionId);
    }

}
