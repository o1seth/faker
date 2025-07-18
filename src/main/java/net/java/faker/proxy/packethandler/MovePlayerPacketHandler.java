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

import io.netty.channel.ChannelFutureListener;
import net.java.faker.proxy.packet.*;
import net.java.faker.proxy.session.DualConnection;
import net.java.faker.proxy.session.ProxyConnection;
import net.raphimc.netminecraft.constants.MCVersion;
import net.raphimc.netminecraft.packet.Packet;

import java.util.List;

public class MovePlayerPacketHandler extends PacketHandler {
    public static final boolean YAW_FIX = true;//fix for AimModulo360 detection when swapController();

    public MovePlayerPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }


    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) {

        DualConnection dualConnection = proxyConnection.dualConnection;
        if (dualConnection == null) {
            return true;
        }
        if (!dualConnection.isBothPlayState()) {
            return true;
        }
        if (packet instanceof C2SMoveVehicle moveVehicle) {
            if (!proxyConnection.isController()) {
                return true;
            }

            if (dualConnection.vehicleId < 0) {
                return true;
            }

            ProxyConnection follower = dualConnection.getFollower();
            if (follower != null && !follower.isClosed()) {
                if (follower.isPassenger) {
                    //remove passengers
                    follower.sendToClient(new S2CSetPassengers(dualConnection.vehicleId));
                    follower.isPassenger = false;
                }
                S2CPlayerPosition newPos = S2CPlayerPosition.create(proxyConnection.getVersion());
                newPos.x = moveVehicle.x;
                newPos.y = moveVehicle.y;
                newPos.z = moveVehicle.z;
                newPos.yaw = dualConnection.playerYaw;
                newPos.pitch = dualConnection.playerPitch;
                follower.sendToClient(newPos);
                S2CEntityPositionSync positionSync = S2CEntityPositionSync.fromVehicle(moveVehicle, dualConnection.vehicleId);
                follower.sendToClient(positionSync);

//                S2CSetPassengers addPass = new S2CSetPassengers();
//                addPass.vehicle = dualConnection.vehicleId;
//                addPass.passengers = new int[]{dualConnection.entityId};
//                follower.sendToClient(addPass);
//                S2CPlayerPosition pos = new S2CPlayerPosition.v1_21_2();
//                pos.x = moveVehicle.x;
//                pos.y = moveVehicle.y;
//                pos.z = moveVehicle.z;
//                pos.yaw = moveVehicle.yaw;
//                pos.pitch = moveVehicle.pitch;
//                follower.sendToClient(positionSync);
//                Logger.raw("SYNC VEHICLE " + dualConnection.vehicleId + " " + moveVehicle.x + " " + moveVehicle.y + " " + moveVehicle.z);

            }
            return true;
        }

        if (packet instanceof C2SMovePlayer move) {
            if (!proxyConnection.isController()) {
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
//                if (packet instanceof C2SMovePlayer.Rot && dualConnection.isPassenger()) {
//                    if (proxyConnection.getVersion() >= MCVersion.v1_21_2) {
//                        S2CPlayerRotation rotation = new S2CPlayerRotation();
//                        rotation.yaw = newYaw;
//                        rotation.pitch = packetPitch;
//                        follower.sendToClient(rotation);
//                    }
//
//                    return true;
//                }
                if (proxyConnection.getVersion() < MCVersion.v1_9 && dualConnection.isPassenger()) {
                    return true;
                }
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
        DualConnection dualConnection = proxyConnection.dualConnection;

        if (dualConnection == null) {
            return;
        }
        //do only once
        if (dualConnection.getMainConnection() != this.proxyConnection) {
            return;
        }
        if (packet instanceof S2CEntityAttach attach) {
            if (dualConnection.entityId == attach.entity && attach.leash == 0) {
                if (attach.vehicle == -1) {
                    dualConnection.clearVehicle();
                } else {
                    dualConnection.vehicleId = attach.vehicle;
                    dualConnection.getMainConnection().isPassenger = true;
                    dualConnection.getSideConnection().isPassenger = true;
                }
            }
        } else if (packet instanceof S2CDestroyEntities destroy) {

            if (dualConnection.isPassenger()) {
                for (int id : destroy.entities) {
                    if (id == dualConnection.vehicleId) {
                        dualConnection.clearVehicle();
                        break;
                    }
                }
            }
        } else if (packet instanceof S2CRemoveEntity remove) {

            if (dualConnection.isPassenger()) {
                if (remove.entity == dualConnection.vehicleId) {
                    dualConnection.clearVehicle();
                }
            }
        } else if (packet instanceof S2CSetPassengers pass) {


            if (dualConnection.vehicleId == pass.vehicle) {
                dualConnection.clearVehicle();
            }
            for (int id : pass.passengers) {
                if (id == dualConnection.entityId) {
                    dualConnection.vehicleId = pass.vehicle;
                    dualConnection.setPassengersPacket = pass;
                    dualConnection.getMainConnection().isPassenger = true;
                    dualConnection.getSideConnection().isPassenger = true;
                    break;
                }
            }
        } else if (packet instanceof S2CPlayerPosition || packet instanceof S2CPlayerRotation) {
//            if (!proxyConnection.isController()) {
//                return;
//            }
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
