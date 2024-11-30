package net.java.mproxy.proxy.packethandler;

import io.netty.channel.ChannelFutureListener;
import net.java.mproxy.proxy.packet.C2SContainerClose;
import net.java.mproxy.proxy.packet.S2CContainerClose;
import net.java.mproxy.proxy.session.DualConnection;
import net.java.mproxy.proxy.session.ProxyConnection;
import net.raphimc.netminecraft.packet.Packet;

import java.util.List;

public class CloseContainerHandler extends PacketHandler {

    public CloseContainerHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) throws Exception {
        if (packet instanceof C2SContainerClose p) {
            if (!proxyConnection.isController()) {
                return true;
            }
            DualConnection dualConnection = proxyConnection.dualConnection;
            if (dualConnection == null) {
                return true;
            }

            ProxyConnection follower = dualConnection.getFollower();
            if (follower != null && !follower.isClosed()) {
                S2CContainerClose containerClose = new S2CContainerClose();
                containerClose.containerId = p.containerId;
                follower.sendToClient(containerClose);
            }
        }
        return true;
    }


}
