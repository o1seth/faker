package net.java.mproxy.proxy.packethandler;

import io.netty.channel.ChannelFutureListener;
import net.java.mproxy.proxy.packet.C2SSetCarriedItem;
import net.java.mproxy.proxy.packet.S2CSetCarriedItem;
import net.java.mproxy.proxy.session.DualConnection;
import net.java.mproxy.proxy.session.ProxyConnection;
import net.raphimc.netminecraft.packet.Packet;

import java.util.List;

public class CarriedItemHandler extends PacketHandler {

    public CarriedItemHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) throws Exception {
        if (packet instanceof C2SSetCarriedItem p) {
            if (!proxyConnection.isController()) {
                return true;
            }
            DualConnection dualConnection = proxyConnection.dualConnection;
            if (dualConnection == null) {
                return true;
            }

            ProxyConnection follower = dualConnection.getFollower();
            if (follower != null && !follower.isClosed()) {
                S2CSetCarriedItem setCarriedItem = new S2CSetCarriedItem();

                setCarriedItem.slot = p.slot;
                follower.sendToClient(setCarriedItem);
            }
        }
        return true;
    }


}
