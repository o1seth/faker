package net.java.faker.proxy.packethandler;

import io.netty.channel.ChannelFutureListener;
import net.java.faker.proxy.packet.C2SPlayerCommand;
import net.java.faker.proxy.session.DualConnection;
import net.java.faker.proxy.session.ProxyConnection;
import net.raphimc.netminecraft.packet.Packet;

import java.util.List;

public class PlayerCommandHandler extends PacketHandler {

    public PlayerCommandHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) throws Exception {
        if (packet instanceof C2SPlayerCommand p) {
            if (!proxyConnection.isController()) {
                return true;
            }
            DualConnection dualConnection = proxyConnection.dualConnection;
            if (dualConnection == null) {
                return true;
            }

            if (p.action == C2SPlayerCommand.Action.PRESS_SHIFT_KEY || p.action == C2SPlayerCommand.Action.RELEASE_SHIFT_KEY) {
                dualConnection.shiftState = p.action;
            } else if (p.action == C2SPlayerCommand.Action.START_SPRINTING || p.action == C2SPlayerCommand.Action.STOP_SPRINTING) {
                dualConnection.sprintState = p.action;
            }
        }
        return true;
    }


}
