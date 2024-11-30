package net.java.mproxy.proxy.packethandler;

import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.packet.Packet;
import net.java.mproxy.proxy.session.ProxyConnection;

import java.util.List;

public class UnexpectedPacketHandler extends PacketHandler {

    public UnexpectedPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) {
        final ConnectionState connectionState = this.proxyConnection.getC2pConnectionState();
        if (connectionState.equals(ConnectionState.HANDSHAKING)) {
            throw new IllegalStateException("Unexpected packet in " + connectionState + " state");
        }

        return true;
    }

}
