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
import net.java.faker.proxy.session.DualConnection;
import net.java.faker.proxy.session.ProxyConnection;
import net.java.faker.util.logging.Logger;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.configuration.C2SConfigFinishConfigurationPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginAcknowledgedPacket;
import net.raphimc.netminecraft.packet.impl.play.C2SPlayConfigurationAcknowledgedPacket;

import java.util.List;

public class ConfigurationPacketHandler extends PacketHandler {

    public ConfigurationPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) throws Exception {

        DualConnection dualConnection = this.proxyConnection.dualConnection;
        boolean isController = this.proxyConnection.isController();
        if (packet instanceof C2SLoginAcknowledgedPacket) {
            this.proxyConnection.setC2pConnectionState(ConnectionState.CONFIGURATION);
            if (isController) {
                listeners.add(f -> {
                    if (f.isSuccess()) {
                        Logger.u_info("session", this.proxyConnection, " (login) Switching to CONFIGURATION state (controller)");
                        this.proxyConnection.setP2sConnectionState(ConnectionState.CONFIGURATION);
                        dualConnection.restoreAutoRead();
                    }
                });
            } else {
                Logger.u_info("session", this.proxyConnection, " (login) Switching to CONFIGURATION state (follower)");
                this.proxyConnection.setP2sConnectionState(ConnectionState.CONFIGURATION);
                dualConnection.restoreAutoRead();
            }
        } else if (packet instanceof C2SConfigFinishConfigurationPacket) {
            this.proxyConnection.setC2pConnectionState(ConnectionState.PLAY);
            if (isController) {
                listeners.add(f -> {
                    if (f.isSuccess()) {
                        Logger.u_info("session", this.proxyConnection, "Configuration finished! Switching to PLAY state (controller)");
                        this.proxyConnection.setP2sConnectionState(ConnectionState.PLAY);
                        dualConnection.restoreAutoRead();
                    }
                });
            } else {
                Logger.u_info("session", this.proxyConnection, "Configuration finished! Switching to PLAY state (follower)");
                this.proxyConnection.setP2sConnectionState(ConnectionState.PLAY);
                dualConnection.restoreAutoRead();
            }
        } else if (packet instanceof C2SPlayConfigurationAcknowledgedPacket) {
            this.proxyConnection.setC2pConnectionState(ConnectionState.CONFIGURATION);
            if (isController) {
                listeners.add(f -> {
                    if (f.isSuccess()) {
                        Logger.u_info("session", this.proxyConnection, "Switching to CONFIGURATION state (controller)");
                        this.proxyConnection.setP2sConnectionState(ConnectionState.CONFIGURATION);
                        dualConnection.restoreAutoRead();
                    }
                });
            } else {
                Logger.u_info("session", this.proxyConnection, "Switching to CONFIGURATION state (follower)");
                this.proxyConnection.setP2sConnectionState(ConnectionState.CONFIGURATION);
                dualConnection.restoreAutoRead();
            }
        }
        return true;
    }

//    @Override
//    public void handleC2P(DualConnection dualConnection, Packet packet, List<ChannelFutureListener> listeners) {
//        ProxyConnection mainConnection = dualConnection.getMainConnection();
//        ProxyConnection sideConnection = dualConnection.getSideConnection();
//        if (packet instanceof C2SLoginAcknowledgedPacket) {
//            mainConnection.setC2pConnectionState(ConnectionState.CONFIGURATION);
//            sideConnection.setC2pConnectionState(ConnectionState.CONFIGURATION);
//            listeners.add(f -> {
//                if (f.isSuccess()) {
//                    this.proxyConnection.setP2sConnectionState(ConnectionState.CONFIGURATION);
//                    dualConnection.restoreAutoRead();
//                }
//            });
//        } else if (packet instanceof C2SConfigFinishConfigurationPacket) {
//            mainConnection.setC2pConnectionState(ConnectionState.PLAY);
//            sideConnection.setC2pConnectionState(ConnectionState.PLAY);
//            listeners.add(f -> {
//                if (f.isSuccess()) {
//                    Logger.u_info("session", this.proxyConnection, "Configuration finished! Switching to PLAY state");
//                    this.proxyConnection.setP2sConnectionState(ConnectionState.PLAY);
//                    dualConnection.restoreAutoRead();
//                    this.proxyConnection.onPlay();
//                }
//            });
//        } else if (packet instanceof C2SPlayConfigurationAcknowledgedPacket) {
//            mainConnection.setC2pConnectionState(ConnectionState.CONFIGURATION);
//            sideConnection.setC2pConnectionState(ConnectionState.CONFIGURATION);
//            listeners.add(f -> {
//                if (f.isSuccess()) {
//                    Logger.u_info("session", this.proxyConnection, "Switching to CONFIGURATION state");
//                    this.proxyConnection.setP2sConnectionState(ConnectionState.CONFIGURATION);
//                    dualConnection.restoreAutoRead();
//                }
//            });
//        }
//    }

//    @Override
//    public void handleP2S(DualConnection dualConnection, Packet packet, List<ChannelFutureListener> listeners) {
//        if (packet instanceof S2CConfigFinishConfigurationPacket || packet instanceof S2CPlayStartConfigurationPacket) {
//            dualConnection.disableAutoRead();
//        }
//    }

}
