package net.java.mproxy.proxy.packethandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.impl.common.S2CCustomPayloadPacket;
import net.java.mproxy.proxy.session.ProxyConnection;
import net.java.mproxy.util.logging.Logger;

import java.net.InetSocketAddress;
import java.util.List;

public class SimpleVoiceChatPacketHandler extends PacketHandler {

    private static final String SECRET_CHANNEL = "voicechat:secret";

    public SimpleVoiceChatPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    public static String namespaced(final String identifier) {
        final int index = identifier.indexOf(':');
        if (index == -1) {
            return "minecraft:" + identifier;
        } else if (index == 0) {
            return "minecraft" + identifier;
        }
        return identifier;
    }

    @Override
    public void handleP2S(Packet packet, List<ChannelFutureListener> listeners) throws Exception {
        if (packet instanceof S2CCustomPayloadPacket customPayloadPacket) {
            if (namespaced(customPayloadPacket.channel).equals(SECRET_CHANNEL)) {
                final ByteBuf data = Unpooled.wrappedBuffer(customPayloadPacket.data);
                try {
                    final ByteBuf newData = Unpooled.buffer();
                    PacketTypes.writeUuid(newData, PacketTypes.readUuid(data)); // secret
                    final int port = data.readInt(); // port
                    newData.writeInt(port);
                    PacketTypes.writeUuid(newData, PacketTypes.readUuid(data)); // player uuid
                    newData.writeByte(data.readByte()); // codec
                    newData.writeInt(data.readInt()); // mtu size
                    newData.writeDouble(data.readDouble()); // voice chat distance
                    newData.writeInt(data.readInt()); // keep alive
                    newData.writeBoolean(data.readBoolean()); // groups enabled
                    final String voiceHost = PacketTypes.readString(data, Short.MAX_VALUE); // voice host
                    if (voiceHost.isEmpty()) {
                        if (this.proxyConnection.getServerAddress() instanceof InetSocketAddress serverAddress) {
                            PacketTypes.writeString(newData, new InetSocketAddress(serverAddress.getAddress(), port).toString());
                        } else {
                            throw new IllegalArgumentException("Server address must be an InetSocketAddress");
                        }
                    } else {
                        PacketTypes.writeString(newData, voiceHost);
                    }
                    newData.writeBytes(data);
                    customPayloadPacket.data = ByteBufUtil.getBytes(newData);
                } catch (Throwable e) {
                    Logger.LOGGER.error("Failed to handle simple voice chat packet", e);
                }
            }
        }

    }

}
