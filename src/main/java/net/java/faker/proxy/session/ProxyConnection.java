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

package net.java.faker.proxy.session;

import com.google.gson.JsonPrimitive;
import com.mojang.authlib.GameProfile;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import net.java.faker.Proxy;
import net.java.faker.WinRedirect;
import net.java.faker.auth.Account;
import net.java.faker.proxy.PacketRegistry;
import net.java.faker.proxy.packet.C2SAbstractPong;
import net.java.faker.proxy.packet.C2SMovePlayer;
import net.java.faker.proxy.packethandler.PacketHandler;
import net.java.faker.proxy.util.CloseAndReturn;
import net.java.faker.proxy.util.LatencyMode;
import net.java.faker.util.logging.Logger;
import net.lenni0451.mcstructs.text.components.StringComponent;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.NetClient;
import net.raphimc.netminecraft.netty.crypto.AESEncryption;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.configuration.S2CConfigDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.play.S2CPlayDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.status.S2CStatusResponsePacket;
import net.raphimc.netminecraft.util.ChannelType;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class ProxyConnection extends NetClient {

    public static final AttributeKey<ProxyConnection> PROXY_CONNECTION_ATTRIBUTE_KEY = AttributeKey.valueOf("proxy_connection");

    private final Channel c2p;
    private final List<PacketHandler> packetHandlers = new ArrayList<>();

    private SocketAddress serverAddress;

    private int version;
    volatile boolean isController = true;
    //    public InetSocketAddress connectAddress;
    public DualConnection dualConnection;
    private InetSocketAddress clientHandshakeAddress;
    private GameProfile gameProfile;
    private C2SLoginHelloPacket loginHelloPacket;
//    private C2SHandshakingClientIntentionPacket handshakePacket;


    private Account account;

    private ConnectionState c2pConnectionState = ConnectionState.HANDSHAKING;
    private ConnectionState p2sConnectionState = ConnectionState.HANDSHAKING;
    private static final int MAX_SENT_PACKETS = 64;
    private final LinkedList<Packet> sentPackets = new LinkedList<>();

    Object controllerLocker = new Object();
    public int syncPosState;
    public static final int SYNC_POS_SENT = 1;
    public static final int SYNC_POS_RECEIVED = 2;

    public volatile boolean isPassenger;
    private boolean isForwardMode;
    private final boolean isRedirected;
    private final InetSocketAddress realSrcAddress;
    private final InetSocketAddress realDstAddress;
    private LatencyMode latencyMode = LatencyMode.DISABLED;
    private int latency;
    private int connectTime = -1;// only for first ProxyConnection
    private Consumer<ProxyConnection> latencyChange;

    public ProxyConnection(final ChannelInitializer<Channel> channelInitializerSupplier, final Channel c2p) {
        this(channelInitializerSupplier, c2p, null, null);
    }

    public ProxyConnection(final ChannelInitializer<Channel> channelInitializerSupplier, final Channel c2p, InetSocketAddress src, InetSocketAddress dst) {
        super(channelInitializerSupplier);
        this.c2p = c2p;
        if (src != null && dst != null) {
            this.isRedirected = true;
            this.realSrcAddress = src;
            this.realDstAddress = dst;
        } else {
            this.isRedirected = false;
            if (c2p.localAddress() instanceof InetSocketAddress) {
                this.realSrcAddress = (InetSocketAddress) c2p.localAddress();
            } else {
                this.realSrcAddress = null;
            }
            if (c2p.remoteAddress() instanceof InetSocketAddress) {
                this.realDstAddress = (InetSocketAddress) c2p.remoteAddress();
            } else {
                this.realDstAddress = null;
            }
        }
        updateLatencyMode();
    }

    public static ProxyConnection fromChannel(final Channel channel) {
        return channel.attr(PROXY_CONNECTION_ATTRIBUTE_KEY).get();
    }

    private void updateLatencyMode() {
        if (!WinRedirect.isSupported()) {
            this.latencyMode = LatencyMode.DISABLED;
            this.latency = 0;
            return;
        }
        InetSocketAddress remote = (InetSocketAddress) this.c2p.remoteAddress();
        String ip = remote.getAddress().getHostAddress();
        int port = remote.getPort();
        int latency = WinRedirect.getRedirectLatency(Proxy.forward_redirect, ip, port);
        if (latency == -1) {
            latency = WinRedirect.getRedirectLatency(Proxy.redirect, ip, port);
        }
        if (latency > 0) {
            this.latencyMode = LatencyMode.AUTO;
            this.latency = latency;
        }
    }

    @Override
    @Deprecated
    public ChannelFuture connect(final SocketAddress address) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initialize(final ChannelType channelType, final Bootstrap bootstrap) {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Proxy.connectTimeout);
        bootstrap.attr(PROXY_CONNECTION_ATTRIBUTE_KEY, this);
        super.initialize(channelType, bootstrap);
    }

    public ChannelFuture connectToServer(final SocketAddress serverAddress, IntConsumer onBind) {
        this.serverAddress = serverAddress;
        if (this.channelFuture == null) {
            this.initialize(ChannelType.get(serverAddress), new Bootstrap());
        }

        this.getChannel().bind(new InetSocketAddress(0)).syncUninterruptibly();
        InetSocketAddress localAddress = (InetSocketAddress) this.getChannel().localAddress();
        int port = localAddress.getPort();
        if (onBind != null) {
            onBind.accept(port);
        }

        return this.getChannel().connect(serverAddress);
    }

    public Channel getC2P() {
        return this.c2p;
    }

    public List<PacketHandler> getPacketHandlers() {
        return this.packetHandlers;
    }

    public <T> T getPacketHandler(final Class<T> packetHandlerType) {
        for (final PacketHandler packetHandler : this.packetHandlers) {
            if (packetHandlerType.isInstance(packetHandler)) {
                return packetHandlerType.cast(packetHandler);
            }
        }
        return null;
    }

//    public void setHandshakePacket(C2SHandshakingClientIntentionPacket handshakePacket) {
//        this.handshakePacket = handshakePacket;
//    }
//
//    public C2SHandshakingClientIntentionPacket getHandshakePacket() {
//        return handshakePacket;
//    }


    public SocketAddress getServerAddress() {
        return this.serverAddress;
    }

    public int getVersion() {
        return this.version;
    }


    public void setVersion(final int version) {
        this.version = version;
        this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(new PacketRegistry(false, version));
    }

    public InetSocketAddress getClientHandshakeAddress() {
        return this.clientHandshakeAddress;
    }

    public void setClientHandshakeAddress(final InetSocketAddress clientHandshakeAddress) {
        this.clientHandshakeAddress = clientHandshakeAddress;
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public void setGameProfile(final GameProfile gameProfile) {
        this.gameProfile = gameProfile;
    }

    public C2SLoginHelloPacket getLoginHelloPacket() {
        return this.loginHelloPacket;
    }

    public void setLoginHelloPacket(final C2SLoginHelloPacket loginHelloPacket) {
        this.loginHelloPacket = loginHelloPacket;
    }


    public Account getAccount() {
        return this.account;
    }

    public void setAccount(final Account account) {
        this.account = account;
    }

    public ConnectionState getC2pConnectionState() {
        return this.c2pConnectionState;
    }

    private boolean skipPacket(Object packet) {
        if (packet instanceof C2SAbstractPong pong) {
            if (dualConnection != null && dualConnection.skipPong(pong)) {
//                Logger.raw("SKIP: " + PacketUtils.toString(pong));
                return true;
            }
        }
        return false;
    }

    private ChannelFuture sendServer(Object msg) {
        return getChannel().writeAndFlush(msg);
    }

    private void addSentPacket(Packet packet) {
        this.sentPackets.addLast(packet);
        while (this.sentPackets.size() > MAX_SENT_PACKETS) {
            this.sentPackets.removeFirst();
        }
    }

    public boolean preReceivePacket(Packet packet) {
        if (dualConnection != null) {
            if (syncPosState == ProxyConnection.SYNC_POS_RECEIVED) {
                if (packet instanceof C2SMovePlayer p) {
                    syncPosState = 0;
                    double y = ((C2SMovePlayer) packet).getY(dualConnection.playerY);
                    int intY = (int) y;
                    if (intY == y || y - intY == 0.5D || y - intY == 0.25D || y - intY == 0.75D) {
                        p.setOnGround(true);
                    } else {
                        p.setOnGround(dualConnection.onGround);
                    }
                    if (p instanceof C2SMovePlayer.Status) {
                        return false;
                    }
                }
            }
            if (syncPosState == ProxyConnection.SYNC_POS_SENT) {
                if (packet instanceof C2SMovePlayer.PosRot) {
                    syncPosState = ProxyConnection.SYNC_POS_RECEIVED;
                    return false;
                }
            }
        }
        return true;
    }

    public void sendToServer(Packet packet, List<ChannelFutureListener> listeners) {
        synchronized (controllerLocker) {
            if (!isController) {
                addSentPacket(packet);
                return;
            }
            addSentPacket(packet);
            if (skipPacket(packet)) {
                return;
            }
            if (listeners == null) {
                sendServer(packet);
            } else {
                sendServer(packet).addListeners(listeners.toArray(new ChannelFutureListener[listeners.size()]));
            }
        }
    }

    public void sendToServer(Packet packet, ChannelFutureListener listener) {
        synchronized (controllerLocker) {
            if (!isController) {
                addSentPacket(packet);
                return;
            }
            addSentPacket(packet);
            if (skipPacket(packet)) {
                return;
            }
            if (listener == null) {
                sendServer(packet);
            } else {
                sendServer(packet).addListener(listener);
            }
        }
    }

    public void sendToServer(ByteBuf packet, ChannelFutureListener listener) {
        synchronized (controllerLocker) {
            if (!isController) {
                return;
            }
            if (skipPacket(packet)) {
                return;
            }
            if (listener == null) {
                sendServer(packet);
            } else {
                sendServer(packet).addListener(listener);
            }
        }
    }

    public void sendToServer(Packet packet, ChannelFutureListener... listeners) {
        synchronized (controllerLocker) {
            if (!isController) {
                addSentPacket(packet);
                return;
            }
            if (skipPacket(packet)) {
                return;
            }
            if (listeners == null) {
                sendServer(packet);
            } else {
                sendServer(packet).addListeners(listeners);
            }
        }
    }

    private ChannelFuture sendClient(Packet packet) {
        return this.c2p.writeAndFlush(packet);
    }

    public void sendToClient(Packet packet) {
        sendClient(packet);
    }

    public void sendToClient(Packet packet, List<ChannelFutureListener> listeners) {
        if (listeners == null) {
            sendClient(packet);
        } else {
            sendClient(packet).addListeners(listeners.toArray(new ChannelFutureListener[listeners.size()]));
        }
    }

    public void sendToClient(Packet packet, ChannelFutureListener listener) {
        if (listener == null) {
            sendClient(packet);
        } else {
            sendClient(packet).addListener(listener);
        }
    }

    public void sendToClient(Packet packet, ChannelFutureListener... listeners) {
        if (listeners == null) {
            sendClient(packet);
        } else {
            sendClient(packet).addListeners(listeners);
        }
    }

    public void setC2pConnectionState(final ConnectionState connectionState) {
        this.c2pConnectionState = connectionState;
        this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).get().setConnectionState(connectionState);
    }

    public void setP2sConnectionState(final ConnectionState connectionState) {
        this.p2sConnectionState = connectionState;
        this.getChannel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).get().setConnectionState(connectionState);
    }


    public void setC2pEncryption(AESEncryption encryption) {
        this.getC2P().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(encryption);
    }


    public C2SAbstractPong getLastSentPong() {
        C2SAbstractPong last = null;
        for (Packet p : this.sentPackets) {
            if (p instanceof C2SAbstractPong) {
                last = (C2SAbstractPong) p;
            }
        }
        return last;
    }

    public List<C2SAbstractPong> getPongPacketsAfter(C2SAbstractPong after) {
        if (after == null) {
            return Collections.emptyList();
        }

        List<C2SAbstractPong> pongs = new ArrayList<>(6);
        boolean add = false;
        for (Packet p : this.sentPackets) {
            if (p instanceof C2SAbstractPong pong) {
                if (add) {
                    pongs.add(pong);
                }
                if (after.getId() == pong.getId()) {
                    add = true;
                    pongs.clear();
                }
            }
        }
        return pongs;
    }

    public List<Packet> getSentPackets() {
        return sentPackets;
    }

    public boolean isClosed() {
        return !this.c2p.isOpen() || (this.getChannel() != null && !this.getChannel().isOpen());
    }

    public void setChannel(ProxyConnection proxyConnection) {
        this.channelFuture = proxyConnection.getChannelFuture();
    }

    public boolean isController() {
        synchronized (controllerLocker) {
            return isController;
        }
    }

    public void setController(boolean controller) {
        synchronized (controllerLocker) {
            this.isController = controller;
        }
    }

    public void setForwardMode() {
        try {
            removeHandlers(getC2P());
        } catch (Exception e) {
            e.printStackTrace(Logger.SYSERR);
            Logger.u_err("Set forward mode c2p", this, e.getMessage());
        }
        try {
            removeHandlers(getChannel());
        } catch (Exception e) {
            e.printStackTrace(Logger.SYSERR);
            Logger.u_err("Set forward mode p2s", this, e.getMessage());
        }
        this.isForwardMode = true;
    }

    public InetSocketAddress getRealDstAddress() {
        return realDstAddress;
    }

    public InetSocketAddress getRealSrcAddress() {
        return realSrcAddress;
    }

    public boolean isRedirected() {
        return isRedirected;
    }

    public boolean isForwardMode() {
        return isForwardMode;
    }

    public LatencyMode getLatencyMode() {
        return latencyMode;
    }

    public int getLatency() {
        return latency;
    }

    public void setLatencyMode(LatencyMode latencyMode) {
        this.latencyMode = latencyMode;
        if (this.latencyMode == LatencyMode.DISABLED) {
            this.latency = 0;
            if (latencyChange != null) {
                latencyChange.accept(this);
            }
        }
    }

    public void setLatencyChangeListener(Consumer<ProxyConnection> latencyChange) {
        this.latencyChange = latencyChange;
    }

    public void setLatency(int latency) {
        if (this.latencyMode != LatencyMode.DISABLED) {
            if (this.latency != latency) {
                this.latency = latency;
                InetSocketAddress remote = (InetSocketAddress) this.c2p.remoteAddress();
                String ip = remote.getAddress().getHostAddress();
                int port = remote.getPort();
                if (!WinRedirect.setRedirectLatency(Proxy.forward_redirect, ip, port, this.latency)) {
                    WinRedirect.setRedirectLatency(Proxy.redirect, ip, port, this.latency);
                }
                if (latencyChange != null) {
                    latencyChange.accept(this);
                }
            }
        }

    }

    public void setConnectTime(int connectTime) {
        this.connectTime = connectTime;
    }

    public int getConnectTime() {
        return connectTime;
    }


    private void removeHandlers(Channel channel) {
        if (channel == null) {
            return;
        }
        channel.attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(null);
        channel.attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(null);
        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(null);

        channel.pipeline().remove(MCPipeline.ENCRYPTION_HANDLER_NAME);
        channel.pipeline().remove(MCPipeline.SIZER_HANDLER_NAME);
        channel.pipeline().remove(MCPipeline.FLOW_CONTROL_HANDLER_NAME);
        channel.pipeline().remove(MCPipeline.COMPRESSION_HANDLER_NAME);
        channel.pipeline().remove(MCPipeline.PACKET_CODEC_HANDLER_NAME);
//        if(channel.pipeline().get(HandshakeCodec.HANDSHAKE_HANDLER_NAME) != null) {
//            channel.pipeline().remove(HandshakeCodec.HANDSHAKE_HANDLER_NAME);
//        }
    }

    public void kickClient(final String message) throws CloseAndReturn {
        Logger.u_err("proxy kick", this, message);

        final ChannelFuture future;
        if (!Proxy.getConfig().showKickErrors.get() || message == null) {
            future = this.c2p.newSucceededFuture();
        } else if (this.c2pConnectionState == ConnectionState.STATUS) {
            future = this.c2p.writeAndFlush(new S2CStatusResponsePacket("{\"players\":{\"max\":0,\"online\":0},\"description\":" + new JsonPrimitive(message) + ",\"version\":{\"protocol\":-1,\"name\":\"Proxy\"}}"));
        } else if (this.c2pConnectionState == ConnectionState.LOGIN) {
            future = this.c2p.writeAndFlush(new S2CLoginDisconnectPacket(new StringComponent(message)));
        } else if (this.c2pConnectionState == ConnectionState.CONFIGURATION) {
            future = this.c2p.writeAndFlush(new S2CConfigDisconnectPacket(new StringComponent(message)));
        } else if (this.c2pConnectionState == ConnectionState.PLAY) {
            future = this.c2p.writeAndFlush(new S2CPlayDisconnectPacket(new StringComponent(message)));
        } else {
            future = this.c2p.newSucceededFuture();
        }

        future.addListener(ChannelFutureListener.CLOSE);
        throw CloseAndReturn.INSTANCE;
    }
}
