package net.java.mproxy.proxy.packethandler;

import io.netty.channel.ChannelFutureListener;
import net.java.mproxy.proxy.packet.*;
import net.java.mproxy.proxy.session.DualConnection;
import net.java.mproxy.proxy.session.ProxyConnection;
import net.raphimc.netminecraft.constants.MCVersion;
import net.raphimc.netminecraft.packet.Packet;

import java.util.List;

public class MovePlayerPacketHandler extends PacketHandler {
    public static final boolean YAW_FIX = true;//fix for AimModulo360 detection when swapController();

    public MovePlayerPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    boolean lastMoveVehicle;

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) {

        if (packet instanceof C2SMoveVehicle moveVehicle) {
            lastMoveVehicle = true;

            if (!proxyConnection.isController()) {
                return true;
            }
            DualConnection dualConnection = proxyConnection.dualConnection;
            if (dualConnection == null) {
                return true;
            }
            if (dualConnection.vehicleId < 0) {
                return true;
            }
            S2CEntityPositionSync positionSync = new S2CEntityPositionSync();
            positionSync.x = moveVehicle.x;
            positionSync.y = moveVehicle.y;
            positionSync.z = moveVehicle.z;

            positionSync.yaw = moveVehicle.yaw;
            positionSync.pitch = moveVehicle.pitch;
            positionSync.entityId = dualConnection.vehicleId;
            ProxyConnection follower = dualConnection.getFollower();
            if (follower != null && !follower.isClosed()) {
                S2CSetPassengers removePass = new S2CSetPassengers();
                removePass.vehicle = dualConnection.vehicleId;
                removePass.passengers = new int[0];
                follower.sendToClient(removePass);

                follower.sendToClient(positionSync);

                S2CSetPassengers addPass = new S2CSetPassengers();
                addPass.vehicle = dualConnection.vehicleId;
                addPass.passengers = new int[]{dualConnection.entityId};
                follower.sendToClient(addPass);
//                S2CPlayerPosition pos = new S2CPlayerPosition.v1_21_2();
//                pos.x = moveVehicle.x;
//                pos.y = moveVehicle.y;
//                pos.z = moveVehicle.z;
//                pos.yaw = moveVehicle.yaw;
//                pos.pitch = moveVehicle.pitch;
//                follower.sendToClient(positionSync);
//                System.out.println("SYNC VEHICLE " + dualConnection.vehicleId + " " + moveVehicle.x + " " + moveVehicle.y + " " + moveVehicle.z);

            }
            return true;
        }

        if (packet instanceof C2SMovePlayer move) {
            if (!proxyConnection.isController()) {
                return true;
            }
            DualConnection dualConnection = proxyConnection.dualConnection;
            if (dualConnection == null) {
                return true;
            }

            double packetX = move.getX(dualConnection.playerX);
            double packetY = move.getY(dualConnection.playerY);
            double packetZ = move.getZ(dualConnection.playerZ);
            float packetYaw = move.getYaw(dualConnection.playerYaw);
            float packetPitch = move.getPitch(dualConnection.playerPitch);
            boolean packetOnGround = move.isOnGround();

            dualConnection.prevX = dualConnection.playerX;
            dualConnection.prevY = dualConnection.playerY;
            dualConnection.prevZ = dualConnection.playerZ;
            dualConnection.prevYaw = dualConnection.playerYaw;
            dualConnection.prevPitch = dualConnection.playerPitch;
            dualConnection.prevOnGround = dualConnection.onGround;

            dualConnection.playerX = packetX;
            dualConnection.playerY = packetY;
            dualConnection.playerZ = packetZ;

            float newYaw = packetYaw;
            //no yaw % 360 on client side on <= 1.14.4
            if (YAW_FIX) {
                newYaw = wrapDegrees(newYaw);

                float correctYaw = wrapDegrees(dualConnection.playerYaw);
                float dYaw = correctYaw - newYaw;
                if (dYaw < -180) {
                    dYaw += 360;
                }
                if (dYaw > 180) {
                    dYaw -= 360;
                }
                newYaw = dualConnection.playerYaw - dYaw;
//                if (p.getYaw(dualConnection.playerYaw) != newYaw) {
//                    Logger.raw("patch yaw  " + p.getYaw(dualConnection.playerYaw) + " -> " + newYaw);
//                }
                move.setYaw(newYaw);
            }

            dualConnection.playerYaw = newYaw;
            dualConnection.clientPlayerYaw = dualConnection.playerYaw % 360;
            dualConnection.playerPitch = packetPitch;
            dualConnection.onGround = packetOnGround;
            ProxyConnection follower = dualConnection.getFollower();
            if (follower != null && !follower.isClosed()) {

//                if (lastMoveVehicle) {
//                    lastMoveVehicle = false;
                if (packet instanceof C2SMovePlayer.Rot && dualConnection.isPassenger()) {
                    if (proxyConnection.getVersion() >= MCVersion.v1_21_2) {
                        S2CPlayerRotation rotation = new S2CPlayerRotation();
                        rotation.yaw = newYaw;
                        rotation.pitch = packetPitch;
                        follower.sendToClient(rotation);
                    }

                    return true;
                }
//                }


                S2CPlayerPosition newPos = S2CPlayerPosition.createFrom(dualConnection, move, proxyConnection.getVersion());
                follower.sendToClient(newPos);
                follower.syncPosState = ProxyConnection.SYNC_POS_SENT;
//                if (dualConnection.entityId != 0) {
//                    S2CSetEntityMotion motion = new S2CSetEntityMotion();
//                    motion.entityId = dualConnection.entityId;
////                    motion.motionX = dualConnection.playerX - prevX;
////                    motion.motionY = dualConnection.playerY - prevY;
////                    motion.motionZ = dualConnection.playerZ - prevZ;
//                    motion.motionY = -0.0784;
//                    notController.sendToClient(motion);
//                }
            }
        }
        return true;
    }

    public static float wrapDegrees(float value) {
        value = value % 360.0F;

        if (value >= 180.0F) {
            value -= 360.0F;
        }

        if (value < -180.0F) {
            value += 360.0F;
        }

        return value;
    }

    @Override
    public void handleP2S(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof S2CSetPassengers pass) {
            DualConnection dualConnection = proxyConnection.dualConnection;
            if (dualConnection == null) {
                return;
            }
//            System.out.println("PASS me: " + dualConnection.entityId + " :: " + pass.vehicle + " " + Arrays.toString(pass.passengers));
            if (dualConnection.vehicleId == pass.vehicle) {
                dualConnection.vehicleId = -1;
            }
            for (int id : pass.passengers) {
                if (id == dualConnection.entityId) {
                    dualConnection.vehicleId = pass.vehicle;
                    System.out.println("NEW VEHICLE " + dualConnection.vehicleId);
                    break;
                }
            }

        }
        if (packet instanceof S2CPlayerPosition || packet instanceof S2CPlayerRotation) {
//            if (!proxyConnection.isController()) {
//                return;
//            }
            DualConnection dualConnection = proxyConnection.dualConnection;
            if (dualConnection == null) {
                return;
            }
            if (packet instanceof S2CPlayerPosition p) {
                dualConnection.playerYaw = p.yaw % 360;
            } else {
                S2CPlayerRotation p = (S2CPlayerRotation) packet;
                dualConnection.playerYaw = p.yaw % 360;
            }
            dualConnection.clientPlayerYaw = dualConnection.playerYaw;
        }
    }
}
