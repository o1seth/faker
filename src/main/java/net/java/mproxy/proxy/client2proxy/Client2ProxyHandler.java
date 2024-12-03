package net.java.mproxy.proxy.client2proxy;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import net.java.mproxy.Proxy;
import net.java.mproxy.auth.Account;
import net.java.mproxy.proxy.packethandler.*;
import net.java.mproxy.proxy.proxy2server.Proxy2ServerChannelInitializer;
import net.java.mproxy.proxy.proxy2server.Proxy2ServerHandler;
import net.java.mproxy.proxy.session.DualConnection;
import net.java.mproxy.proxy.session.ProxyConnection;
import net.java.mproxy.proxy.util.*;
import net.java.mproxy.util.logging.Logger;
import net.raphimc.netminecraft.constants.*;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.netminecraft.packet.impl.handshaking.C2SHandshakingClientIntentionPacket;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Client2ProxyHandler extends SimpleChannelInboundHandler<Packet> {
    public static final AttributeKey<Client2ProxyHandler> CLIENT_2_PROXY_ATTRIBUTE_KEY = AttributeKey.valueOf("proxy_connection");
    private ProxyConnection proxyConnection;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        final Supplier<ChannelHandler> handlerSupplier = Proxy2ServerHandler::new;
        this.proxyConnection = new ProxyConnection(handlerSupplier, Proxy2ServerChannelInitializer::new, ctx.channel());
        ctx.channel().attr(CLIENT_2_PROXY_ATTRIBUTE_KEY).set(this);
//        this.proxyConnection = new DummyProxyConnection(ctx.channel());
        Proxy.getConnectedClients().add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
//        if (this.proxyConnection instanceof DummyProxyConnection) return;
        System.out.println("Closing client->proxy channel");
        DualConnection dualConnection = this.proxyConnection.dualConnection;
        if (dualConnection != null) {

            if (dualConnection.isClosed()) {
                Proxy.dualConnection = null;
                System.out.println("                                     Dual closed");
                try {
                    this.proxyConnection.getChannel().close();
                } catch (Throwable ignored) {
                }
            } else {
                dualConnection.swapController();
            }

        } else {
            try {
                this.proxyConnection.getChannel().close();
            } catch (Throwable ignored) {
            }
        }

    }

    static final boolean forwardConnect = false;

    public boolean onHandshake(C2SHandshakingClientIntentionPacket handshakingPacket) {
        if (Proxy.dualConnection != null && Proxy.dualConnection.isBothConnectionCreated()) {
            System.out.println(PacketUtils.toString(handshakingPacket));
            C2SHandshakingClientIntentionPacket newHandshake = new C2SHandshakingClientIntentionPacket();
            newHandshake.intendedState = handshakingPacket.intendedState;
            newHandshake.protocolVersion = handshakingPacket.protocolVersion;
            InetSocketAddress connectAddress = Proxy.targetAddress;
            newHandshake.address = connectAddress.getHostName();
            newHandshake.port = connectAddress.getPort();
            Logger.u_info("forward connect", this.proxyConnection, "[" + handshakingPacket.protocolVersion + "] Connecting to " + connectAddress);
            proxyConnection.connectToServer(connectAddress).syncUninterruptibly();
            proxyConnection.getChannel().writeAndFlush(newHandshake).syncUninterruptibly();
            proxyConnection.setForwardMode();
//        proxyConnection.getC2P().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).get().setConnectionState(handshakingPacket.intendedState.getConnectionState());

            return false;
        }
        return true;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (this.proxyConnection.isForwardMode() && msg instanceof ByteBuf) {
            this.proxyConnection.getChannel().writeAndFlush(msg);
            return;
        }
        super.channelRead(ctx, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        if (this.proxyConnection.isClosed()) {
            return;
        }
        if (this.proxyConnection.isForwardMode()) {
            throw new IllegalStateException("Unexpected packet in forward mode " + PacketUtils.toString(packet));
        }
        if (!(packet instanceof UnknownPacket)) {
            if (!proxyConnection.preReceivePacket(packet)) {
                return;
            }

//            if (proxyConnection.isController()) {
//                System.out.printf(Integer.toUnsignedString(proxyConnection.hashCode(), 16) + " controller send: " + PacketUtils.toString(packet) + "\n");
//            } else {
//                System.out.printf(System.currentTimeMillis() + " " + Integer.toUnsignedString(proxyConnection.hashCode(), 16) + "       side send: " + PacketUtils.toString(packet) + "\n");
//            }
        }
        if (this.proxyConnection.getC2pConnectionState() == ConnectionState.HANDSHAKING) {

            if (packet instanceof C2SHandshakingClientIntentionPacket) {
                this.handleHandshake((C2SHandshakingClientIntentionPacket) packet);
            } else {
                throw new IllegalStateException("Unexpected packet in HANDSHAKING state " + packet);
            }
            return;
        }

        final List<ChannelFutureListener> listeners = new ArrayList<>(1);
        listeners.add(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        for (PacketHandler packetHandler : this.proxyConnection.getPacketHandlers()) {
            if (!packetHandler.handleC2P(packet, listeners)) {
                return;
            }
        }
        this.proxyConnection.sendToServer(packet, listeners);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ExceptionUtil.handleNettyException(ctx, cause, this.proxyConnection, true);
    }

    private void handleHandshake(final C2SHandshakingClientIntentionPacket packet) {

        if (packet.intendedState == IntendedState.LOGIN) {
            if (Proxy.dualConnection == null) {
                Proxy.dualConnection = new DualConnection(proxyConnection);
                this.proxyConnection.setController(true);
                System.out.println("DualConnection created");
            } else {
                this.proxyConnection.setController(false);
                this.proxyConnection.setChannel(Proxy.dualConnection.getMainConnection());
                Proxy.dualConnection.setSideConnection(this.proxyConnection);
                System.out.println("side connection created");
            }
            this.proxyConnection.dualConnection = Proxy.dualConnection;
        }
        final int version = packet.protocolVersion;

        if (packet.intendedState == null) {
            throw CloseAndReturn.INSTANCE;
        }

        this.proxyConnection.setVersion(version);
        this.proxyConnection.setC2pConnectionState(packet.intendedState.getConnectionState());

        InetSocketAddress serverAddress = Proxy.targetAddress;

        if (packet.intendedState.getConnectionState() == ConnectionState.LOGIN && TransferDataHolder.hasTempRedirect(this.proxyConnection.getC2P())) {
            serverAddress = TransferDataHolder.removeTempRedirect(this.proxyConnection.getC2P());
            System.out.println("tempRedirect  " + serverAddress);
        }

        InetSocketAddress handshakeAddress = new InetSocketAddress(packet.address, packet.port);

        ChannelUtil.disableAutoRead(this.proxyConnection.getC2P());

        System.out.println("Connect...");
        this.connect(serverAddress, version, packet.intendedState, handshakeAddress, Proxy.account);
    }

    private void connect(InetSocketAddress serverAddress, int version, IntendedState intendedState, InetSocketAddress handshakeAddress, Account account) {
        this.proxyConnection.getC2P().attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY).set(this.proxyConnection);
        this.proxyConnection.setVersion(version);
        this.proxyConnection.setClientHandshakeAddress(handshakeAddress);
        this.proxyConnection.setAccount(account);
        this.proxyConnection.setC2pConnectionState(intendedState.getConnectionState());

        this.proxyConnection.getPacketHandlers().add(new LoginPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new MovePlayerPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new CarriedItemHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new CloseContainerHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new PlayerCommandHandler(this.proxyConnection));
        if (version >= MCVersion.v1_20_5) {
            this.proxyConnection.getPacketHandlers().add(new TransferPacketHandler(this.proxyConnection));
        }
        if (version >= (MCVersion.v1_20_2)) {
            this.proxyConnection.getPacketHandlers().add(new ConfigurationPacketHandler(this.proxyConnection));
        }
        if (version >= MCVersion.v1_19_3) {
            this.proxyConnection.getPacketHandlers().add(new ChatSignaturePacketHandler(this.proxyConnection));
        }
//        this.proxyConnection.getPacketHandlers().add(new ResourcePackPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new UnexpectedPacketHandler(this.proxyConnection));
        if (!this.proxyConnection.isController() && this.proxyConnection.dualConnection != null) {
            Logger.u_info("connect", this.proxyConnection, "cancel connect to server");
            this.proxyConnection.setP2sConnectionState(intendedState.getConnectionState());
            ChannelUtil.restoreAutoRead(this.proxyConnection.getC2P());

            return;
        }
        Logger.u_info("connect", this.proxyConnection, "[" + version + "] Connecting to " + serverAddress);


        this.proxyConnection.connectToServer(serverAddress).addListeners((ThrowingChannelFutureListener) f -> {
            if (f.isSuccess()) {
                f.channel().eventLoop().submit(() -> { // Reschedule so the packets get sent after the channel is fully initialized and active
                    final String address = serverAddress.getHostString();
                    final int port = serverAddress.getPort();
                    final C2SHandshakingClientIntentionPacket newHandshakePacket = new C2SHandshakingClientIntentionPacket(version, address, port, intendedState);
                    this.proxyConnection.sendToServer(newHandshakePacket, ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE, (ChannelFutureListener) f2 -> {
                        if (f2.isSuccess()) {
//                            final UserConnection userConnection = this.proxyConnection.getUserConnection();
//                            if (userConnection.has(CookieStorage.class) && TransferDataHolder.hasCookieStorage(this.proxyConnection.getC2P())) {
//                                userConnection.get(CookieStorage.class).cookies().putAll(TransferDataHolder.removeCookieStorage(this.proxyConnection.getC2P()).cookies());
//                                System.out.println("COOKIES?");
//                            }
                            this.proxyConnection.setP2sConnectionState(intendedState.getConnectionState());
                            ChannelUtil.restoreAutoRead(this.proxyConnection.getC2P());
                        }
                    });
                });
            }
        }, (ThrowingChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                if (f.cause() instanceof ConnectException || f.cause() instanceof UnresolvedAddressException) {
                    this.proxyConnection.kickClient("§cCould not connect to the backend server!");
                } else {
                    Logger.LOGGER.error("Error while connecting to the backend server", f.cause());
                    this.proxyConnection.kickClient("§cAn error occurred while connecting to the backend server: " + f.cause().getMessage() + "\n§cCheck the console for more information.");
                }
            }
        });
    }

}
