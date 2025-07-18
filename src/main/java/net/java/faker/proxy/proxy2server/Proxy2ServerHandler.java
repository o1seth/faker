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

package net.java.faker.proxy.proxy2server;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.java.faker.Proxy;
import net.java.faker.proxy.PacketRegistry;
import net.java.faker.proxy.auth.ExternalInterface;
import net.java.faker.proxy.packethandler.PacketHandler;
import net.java.faker.proxy.session.DualConnection;
import net.java.faker.proxy.session.ProxyConnection;
import net.java.faker.proxy.util.ExceptionUtil;
import net.java.faker.proxy.util.PacketUtils;
import net.java.faker.proxy.util.TransferDataHolder;
import net.java.faker.proxy.util.chat.ChatSession1_19_3;
import net.java.faker.util.logging.Logger;
import net.raphimc.netminecraft.constants.*;
import net.raphimc.netminecraft.netty.crypto.AESEncryption;
import net.raphimc.netminecraft.netty.crypto.CryptUtil;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.netminecraft.packet.impl.common.S2CDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.common.S2CTransferPacket;
import net.raphimc.netminecraft.packet.impl.configuration.S2CConfigFinishConfigurationPacket;
import net.raphimc.netminecraft.packet.impl.login.*;
import net.raphimc.netminecraft.packet.impl.play.S2CPlaySetCompressionPacket;
import net.raphimc.netminecraft.packet.impl.play.S2CPlayStartConfigurationPacket;
import net.raphimc.netminecraft.util.MinecraftServerAddress;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class Proxy2ServerHandler extends SimpleChannelInboundHandler<Packet> {

    private ProxyConnection proxyConnection;
    private Channel channel;
    private int joinGamePacketId;
    private int chatSessionUpdatePacketId;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
        this.proxyConnection = ProxyConnection.fromChannel(this.channel);
        this.joinGamePacketId = MCPackets.S2C_LOGIN.getId(proxyConnection.getVersion());
        this.chatSessionUpdatePacketId = MCPackets.C2S_CHAT_SESSION_UPDATE.getId(proxyConnection.getVersion());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (ctx.channel().remoteAddress() instanceof InetSocketAddress isa) {
            Proxy.connectedAddresses.remove(isa);
        }

        Logger.u_info("disconnect", this.proxyConnection, "Connection closed (proxy->server)");
        if (proxyConnection.dualConnection != null) {
            ProxyConnection sideConnection = proxyConnection.dualConnection.getSideConnection();
            Proxy.dualConnection = null;
            try {
                sideConnection.getC2P().close();
            } catch (Throwable ignored) {
            }
        }
        try {
            this.proxyConnection.getC2P().close();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (this.proxyConnection.isForwardMode() && msg instanceof ByteBuf) {
            this.proxyConnection.getC2P().writeAndFlush(msg);
            return;
        }
        super.channelRead(ctx, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        if (this.proxyConnection.isForwardMode()) {
            throw new IllegalStateException("Unexpected packet in forward mode " + PacketUtils.toString(packet));
        }

        ProxyConnection sideConnection = null;
        ProxyConnection mainConnection = this.proxyConnection;
        DualConnection dualConnection = mainConnection.dualConnection;
        if (dualConnection != null) {

            sideConnection = dualConnection.getSideConnection();
            if (mainConnection.isClosed() && sideConnection.isClosed()) {
                return;
            }
        } else {
            if (mainConnection.isClosed()) {
                return;
            }
        }


//        if (packet instanceof UnknownPacket p) {
//            PacketRegistry reg = (PacketRegistry) ctx.channel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).get();
//            final MCPackets packetType = MCPackets.getPacket(reg.getConnectionState(), PacketDirection.CLIENTBOUND, reg.getProtocolVersion(), p.packetId);
//            Logger.raw("IN  " + "Unknown " + p.packetId + " " + packetType);
//        } else {
//            Logger.raw("IN  " + PacketUtils.toString(packet));
//        }

        if (!handleCompression(packet, ctx.channel())) {
            return;
        }
        handleDisconnect(packet);
        if (packet instanceof S2CLoginHelloPacket p) {
            handleS2CLoginHello(p);
            return;
        }
        synchronized (Proxy.ioLocker) {
            final List<ChannelFutureListener> listeners = new ArrayList<>(2);
            listeners.add(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            if (packet instanceof S2CLoginGameProfilePacket) {
                handleLoginGameProfile(dualConnection, (S2CLoginGameProfilePacket) packet, listeners);
            }

            if (packet instanceof S2CConfigFinishConfigurationPacket || packet instanceof S2CPlayStartConfigurationPacket) {
                if (dualConnection != null) {
                    //disable read server->proxy packets, until two clients enter the PLAY state. Look ConfigurationPacketHandler.java
                    dualConnection.disableAutoRead2();//for main and side connection
                }
            }
            if (packet instanceof UnknownPacket p && p.packetId == joinGamePacketId) {
                handleJoinGame(p, listeners);
            }
            if (packet instanceof S2CTransferPacket p) {
                handleTransfer(p);
                listeners.add(ChannelFutureListener.CLOSE);
            }

            for (PacketHandler packetHandler : mainConnection.getPacketHandlers()) {
                packetHandler.handleP2S(packet, listeners);
            }


            if (sideConnection != null) {
                for (PacketHandler packetHandler : sideConnection.getPacketHandlers()) {
                    packetHandler.handleP2S(packet, listeners);
                }
                if (!mainConnection.isClosed()) {
                    mainConnection.sendToClient(packet, listeners);
                }
                if (!sideConnection.isClosed()) {
                    sideConnection.sendToClient(packet);
                }
            } else {
                mainConnection.sendToClient(packet, listeners);
            }
        }
    }

    private void handleS2CLoginHello(S2CLoginHelloPacket loginHelloPacket) {
        try {
            //only for main connection!
            final PublicKey publicKey = CryptUtil.decodeRsaPublicKey(loginHelloPacket.publicKey);
            final SecretKey secretKey = CryptUtil.generateSecretKey();
            final String serverHash = new BigInteger(CryptUtil.computeServerIdHash(loginHelloPacket.serverId, publicKey, secretKey)).toString(16);
            int version = this.proxyConnection.getVersion();
            boolean auth = version < MCVersion.v1_20_5 || loginHelloPacket.authenticate;

            if (auth) {
                ExternalInterface.joinServer(serverHash, this.proxyConnection);
            }

            final byte[] encryptedSecretKey = CryptUtil.encryptData(publicKey, secretKey.getEncoded());
            final byte[] encryptedNonce = CryptUtil.encryptData(publicKey, loginHelloPacket.nonce);

            final C2SLoginKeyPacket loginKey = new C2SLoginKeyPacket(encryptedSecretKey, encryptedNonce);
            if (version >= MCVersion.v1_19 && this.proxyConnection.getLoginHelloPacket().key != null) {
                ExternalInterface.signNonce(loginHelloPacket.nonce, loginKey, this.proxyConnection);
            }

//            this.proxyConnection.sendToServer(loginKey, ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            this.channel.writeAndFlush(loginKey).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
//            this.proxyConnection.setP2sEncryption(new AESEncryption(secretKey));
            this.channel.attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(new AESEncryption(secretKey));
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
        }
    }

    private void handleLoginGameProfile(DualConnection dualConnection, S2CLoginGameProfilePacket gameProfilePacket, List<ChannelFutureListener> listeners) {
        if (dualConnection == null) {
            throw new RuntimeException("DualConnection cannot be null!");
        }

        ProxyConnection mainConnection = dualConnection.getMainConnection();
        ProxyConnection sideConnection = dualConnection.getSideConnection();
        final ConnectionState nextState = mainConnection.getVersion() >= MCVersion.v1_20_2 ? ConnectionState.CONFIGURATION : ConnectionState.PLAY;
        GameProfile gameProfile = new GameProfile(gameProfilePacket.uuid, gameProfilePacket.name);
        mainConnection.setGameProfile(gameProfile);
        sideConnection.setGameProfile(gameProfile);
        Logger.u_info("session " + Integer.toUnsignedString(mainConnection.hashCode(), 16), mainConnection, "Connected successfully! Switching to " + nextState + " state");
        Logger.u_info("session " + Integer.toUnsignedString(sideConnection.hashCode(), 16), sideConnection, "Connected successfully! Switching to " + nextState + " state");

        dualConnection.disableAutoRead2();
        // restore in ConfigurationPacketHandler.java, C2SLoginAcknowledgedPacket and C2SPlayConfigurationAcknowledgedPacket
        // or here
        listeners.add(f -> {
            if (f.isSuccess() && nextState != ConnectionState.CONFIGURATION) {
                mainConnection.setC2pConnectionState(nextState);
                mainConnection.setP2sConnectionState(nextState);

                sideConnection.setC2pConnectionState(nextState);
                sideConnection.setP2sConnectionState(nextState);

                dualConnection.restoreAutoRead();
                dualConnection.restoreAutoRead();
            }
        });

        setCompression(dualConnection);
    }

    private void setCompression(DualConnection dualConnection) {
        ProxyConnection mainConnection = dualConnection.getMainConnection();
        ProxyConnection sideConnection = dualConnection.getSideConnection();
        if (mainConnection.getVersion() >= (MCVersion.v1_8)) {
            int compressionThreshold = Proxy.compressionThreshold;
            if (compressionThreshold < 0) {
                return;
            }
            int mainCompression = mainConnection.getC2P().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get();
            int sideCompression = sideConnection.getC2P().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get();
            if (mainCompression == -1) {
                dualConnection.disableAutoRead();
            }
            if (sideCompression == -1) {
                dualConnection.disableAutoRead();
            }
            if (mainCompression == -1) {
                mainConnection.sendToClient(new S2CLoginCompressionPacket(compressionThreshold), ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE, (ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        mainConnection.getC2P().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(compressionThreshold);
                        dualConnection.restoreAutoRead();
                    }
                });
            }
            if (sideCompression == -1) {
                sideConnection.sendToClient(new S2CLoginCompressionPacket(compressionThreshold), ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE, (ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        sideConnection.getC2P().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(compressionThreshold);
                        dualConnection.restoreAutoRead();
                    }
                });
            }
        }
    }

    public boolean handleCompression(Packet packet, Channel channel) {
        if (packet instanceof S2CPlaySetCompressionPacket p) {
            channel.attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(p.compressionThreshold);
            return false;
        } else if (packet instanceof S2CLoginCompressionPacket p) {
            channel.attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(p.compressionThreshold);
            return false;
        }
        return true;
    }

    public void handleTransfer(S2CTransferPacket transferPacket) {
        final InetSocketAddress newAddress = MinecraftServerAddress.ofResolved(transferPacket.host, transferPacket.port);
        TransferDataHolder.addTempRedirect(this.proxyConnection.getC2P(), newAddress);
        Logger.u_warn("transfer", this.proxyConnection, "Transfer to " + newAddress.getHostName() + ":" + newAddress.getPort() + "   (" + transferPacket.host + ":" + transferPacket.port + ")");
        if (Proxy.transferAddress != null) {
            if (Proxy.transfer_redirect != 0) {
                Proxy.stopTransferRedirectDelay(2000);
            }
        }
        Proxy.transferAddress = newAddress;
        Proxy.startTransferRedirect();
        Proxy.resumeRedirect();
    }

    public void handleJoinGame(UnknownPacket packet, List<ChannelFutureListener> listeners) {
        if (this.proxyConnection.getC2pConnectionState() == ConnectionState.PLAY) {
            DualConnection dualConnection = this.proxyConnection.dualConnection;
            if (packet.packetId == this.joinGamePacketId) {
                if (!dualConnection.isBothPlayState() && dualConnection.hasDualConnections()) {
                    Logger.warn("\nNot both connections in play state!\n");
                }
                if (dualConnection.isFirstSwap()) {
                    dualConnection.swapController();
                }

                dualConnection.entityId = Unpooled.wrappedBuffer(packet.data).readInt();
                Logger.info("Join game, playerID = " + dualConnection.entityId);
                if (dualConnection.isP2sEncrypted() && dualConnection.getChatSession1_19_3() != null) {
                    final ChatSession1_19_3 chatSession = dualConnection.getChatSession1_19_3();
                    listeners.add(f -> {
                        if (f.isSuccess()) {
                            final ByteBuf chatSessionUpdate = Unpooled.buffer();
                            PacketTypes.writeVarInt(chatSessionUpdate, this.chatSessionUpdatePacketId);
                            PacketTypes.writeUuid(chatSessionUpdate, chatSession.getSessionId()); // session id
                            chatSession.getProfileKey().write(chatSessionUpdate);
                            //this.proxyConnection.sendToServer(chatSessionUpdate, ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                            this.channel.writeAndFlush(chatSessionUpdate).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                        }
                    });

                }
            }

        }

    }

    public void handleDisconnect(Packet packet) {
        if (packet instanceof S2CLoginDisconnectPacket loginDisconnectPacket) {
            Logger.u_info("server disconnect", this.proxyConnection, loginDisconnectPacket.reason.asLegacyFormatString());
        } else if (packet instanceof S2CDisconnectPacket disconnectPacket) {
            Logger.u_info("server disconnect", this.proxyConnection, disconnectPacket.reason.asLegacyFormatString());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ExceptionUtil.handleNettyException(ctx, cause, this.proxyConnection, false);
    }

}
