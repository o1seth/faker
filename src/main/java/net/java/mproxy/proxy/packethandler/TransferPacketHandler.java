package net.java.mproxy.proxy.packethandler;

import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.common.S2CTransferPacket;
import net.java.mproxy.Proxy;
import net.java.mproxy.proxy.session.ProxyConnection;
import net.java.mproxy.proxy.util.TransferDataHolder;
import net.java.mproxy.util.logging.Logger;

import java.net.InetSocketAddress;
import java.util.List;

public class TransferPacketHandler extends PacketHandler {

    public TransferPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public void handleP2S(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof S2CTransferPacket transferPacket) {
            final InetSocketAddress newAddress = new InetSocketAddress(transferPacket.host, transferPacket.port);
            TransferDataHolder.addTempRedirect(this.proxyConnection.getC2P(), newAddress);
            InetSocketAddress clientHandshakeAddress = this.proxyConnection.getClientHandshakeAddress();
            if (clientHandshakeAddress != null) {
                //redirect client to proxy address
                transferPacket.host = clientHandshakeAddress.getHostName();
                transferPacket.port = clientHandshakeAddress.getPort();
            } else {
                Logger.u_warn("transfer", this.proxyConnection, "Client handshake address is invalid, using ViaProxy bind address instead");

                if (!(this.proxyConnection.getC2P().localAddress() instanceof InetSocketAddress clientAddress)) {
                    throw new IllegalArgumentException("Client address must be an InetSocketAddress");
                }
                transferPacket.host = clientAddress.getHostString();
                transferPacket.port = Proxy.proxyAddress.getPort();
            }
        }

    }

}
