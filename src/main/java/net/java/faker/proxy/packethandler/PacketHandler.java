package net.java.faker.proxy.packethandler;

import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.packet.Packet;
import net.java.faker.proxy.session.ProxyConnection;

import java.util.List;

public abstract class PacketHandler {

    protected final ProxyConnection proxyConnection;

    public PacketHandler(final ProxyConnection proxyConnection) {
        this.proxyConnection = proxyConnection;
    }


    public boolean handleC2P(final Packet packet, final List<ChannelFutureListener> listeners) throws Exception {
        return true;
    }


    public void handleP2S(final Packet packet, final List<ChannelFutureListener> listeners) throws Exception {

    }

}
