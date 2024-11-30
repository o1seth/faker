package net.java.mproxy.proxy.util;

import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.handshaking.C2SHandshakingClientIntentionPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket;

public class PacketUtils {
    public static String toString(Packet packet) {
        if (packet instanceof C2SHandshakingClientIntentionPacket p) {
            return "C2SIntention " + p.address + " " + p.port + " " + p.protocolVersion + " " + p.intendedState;
        }
        if (packet instanceof C2SLoginHelloPacket p) {
            if (p.uuid != null) {//protocolVersion >= MCVersion.v1_20_2
                return "C2SLoginHello " + p.name + " " + p.expiresAt + " " + p.key + " " + p.uuid;
            }
        }
        if (packet instanceof C2SLoginKeyPacket p) {

        }
        return packet.toString();
    }
}
