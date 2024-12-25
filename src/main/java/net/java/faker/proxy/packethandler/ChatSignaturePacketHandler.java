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

package net.java.faker.proxy.packethandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import net.java.faker.proxy.session.DualConnection;
import net.java.faker.proxy.session.ProxyConnection;
import net.java.faker.proxy.util.chat.ChatSession1_19_3;
import net.java.faker.proxy.util.chat.MessageMetadata;
import net.java.faker.proxy.util.chat.PlayerMessageSignature;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.UnknownPacket;

import java.util.List;

public class ChatSignaturePacketHandler extends PacketHandler {
    private final int chatSessionUpdateId;
    private final int chatMessageId;

    public ChatSignaturePacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
        this.chatSessionUpdateId = MCPackets.C2S_CHAT_SESSION_UPDATE.getId(proxyConnection.getVersion());
        this.chatMessageId = MCPackets.C2S_CHAT.getId(proxyConnection.getVersion());
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) throws Exception {
        if (packet instanceof UnknownPacket unknownPacket && this.proxyConnection.getC2pConnectionState() == ConnectionState.PLAY) {
            DualConnection dualConnection = this.proxyConnection.dualConnection;
            if (unknownPacket.packetId == this.chatSessionUpdateId && (!dualConnection.isP2sEncrypted() || dualConnection.getChatSession1_19_3() != null)) {
                return false;
            } else if (unknownPacket.packetId == this.chatMessageId && dualConnection.getChatSession1_19_3() != null) {
                if (!proxyConnection.isController()) {
                    return true;
                }
                final ChatSession1_19_3 chatSession = dualConnection.getChatSession1_19_3();
                final ByteBuf oldChatMessage = Unpooled.wrappedBuffer(unknownPacket.data);
                final String message = PacketTypes.readString(oldChatMessage, 256); // message
                final long timestamp = oldChatMessage.readLong(); // timestamp
                final long salt = oldChatMessage.readLong(); // salt
                byte[] oldSignature = null;
                if (oldChatMessage.readBoolean()) {
                    oldSignature = new byte[256];
                    oldChatMessage.readBytes(oldSignature);
                }
//                int lastSeenOffset = PacketTypes.readVarInt(oldChatMessage);

                byte[] lastSeen = new byte[4];// Mth.positiveCeilDiv(20, 8) == 3, lastSeenOffset == 1
                oldChatMessage.readBytes(lastSeen);

                final MessageMetadata metadata = new MessageMetadata(null, timestamp, salt);
                //TODO: handle seen messages
                final byte[] signature = chatSession.signChatMessage(metadata, message, new PlayerMessageSignature[0]);

                final ByteBuf newChatMessage = Unpooled.buffer();
                PacketTypes.writeVarInt(newChatMessage, this.chatMessageId);
                PacketTypes.writeString(newChatMessage, message); // message
                newChatMessage.writeLong(timestamp); // timestamp
                newChatMessage.writeLong(salt); // salt

//                Types.OPTIONAL_SIGNATURE_BYTES.write(newChatMessage, signature);
                if (signature != null) {
                    newChatMessage.writeBoolean(true);
                    newChatMessage.writeBytes(signature);
                } else {
                    newChatMessage.writeBoolean(false);
                }
//                PacketTypes.writeVarInt(newChatMessage, 0); // offset
//                Types.ACKNOWLEDGED_BIT_SET.write(newChatMessage, new BitSet(20)); // acknowledged
                newChatMessage.writeBytes(new byte[4]);
                this.proxyConnection.sendToServer(newChatMessage, ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                return false;
            }
        }

        return true;
    }


}
