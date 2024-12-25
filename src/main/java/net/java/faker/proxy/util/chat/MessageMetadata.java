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

import java.time.Instant;
import java.util.UUID;

//https://github.com/ViaVersion/ViaVersion/blob/master/api/src/main/java/com/viaversion/viaversion/api/minecraft/signature/model/MessageMetadata.java
public class MessageMetadata {
    private final UUID sender;
    private final Instant timestamp;
    private final long salt;

    public MessageMetadata(final UUID sender, final Instant timestamp, final long salt) {
        this.sender = sender;
        this.timestamp = timestamp;
        this.salt = salt;
    }

    public MessageMetadata(final UUID sender, final long timestamp, final long salt) {
        this(sender, Instant.ofEpochMilli(timestamp), salt);
    }

    public UUID sender() {
        return this.sender;
    }

    public Instant timestamp() {
        return this.timestamp;
    }

    public long salt() {
        return this.salt;
    }
}
