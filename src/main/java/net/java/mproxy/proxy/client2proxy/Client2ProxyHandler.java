package net.java.mproxy.proxy.client2proxy;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import net.java.mproxy.Proxy;
import net.java.mproxy.WinRedirect;
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
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class Client2ProxyHandler extends SimpleChannelInboundHandler<Packet> {
    public static final AttributeKey<Client2ProxyHandler> CLIENT_2_PROXY_ATTRIBUTE_KEY = AttributeKey.valueOf("proxy_connection");
    private ProxyConnection proxyConnection;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        if (ctx.channel().localAddress() instanceof InetSocketAddress isa) {
            if (Proxy.connectedAddresses.contains(isa)) {
                Logger.u_info("disconnect", "Loopback connection closed " + isa);
                ctx.close();
            }
        }

        InetSocketAddress remote = (InetSocketAddress) ctx.channel().remoteAddress();
        InetSocketAddress[] addresses = new InetSocketAddress[2];
        if (!WinRedirect.redirectGetRealAddresses(Proxy.forward_redirect, remote.getAddress().getHostAddress(), remote.getPort(), addresses)) {
            WinRedirect.redirectGetRealAddresses(Proxy.redirect, remote.getAddress().getHostAddress(), remote.getPort(), addresses);
        }
        System.out.println("NEW CONNECTION " + remote + " " + ctx.channel().localAddress());
        final Supplier<ChannelHandler> handlerSupplier = Proxy2ServerHandler::new;
        this.proxyConnection = new ProxyConnection(handlerSupplier, Proxy2ServerChannelInitializer::new, ctx.channel(), addresses[0], addresses[1]);
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
        Proxy.getConnectedClients().remove(ctx.channel());
    }

    static IntConsumer addSkipPort = port -> {
        if (Proxy.redirect != 0) {
            WinRedirect.redirectAddSkipPort(Proxy.redirect, port);
            System.out.println("SKIP PORT ADDED " + port);
        }

    };
    static ChannelFutureListener removeSkipPort = f -> {
        if (Proxy.redirect != 0) {
            InetSocketAddress localAddress = (InetSocketAddress) f.channel().localAddress();
            int port = localAddress.getPort();
            WinRedirect.redirectRemoveSkipPort(Proxy.redirect, port);
            System.out.println("SKIP PORT REMOVED " + port);
        }
    };

    private boolean shouldEnablePortForward(C2SHandshakingClientIntentionPacket handshakingPacket) {
        if (Proxy.dualConnection != null) {
            if (Proxy.dualConnection.isBothConnectionCreated()) {
                return true;
            }
            ProxyConnection mainConnection = Proxy.dualConnection.getMainConnection();
            if (mainConnection.getVersion() != handshakingPacket.protocolVersion) {
                return true;
            }
        }
        InetSocketAddress targetHandshake = Proxy.getTargetHandshakeAddress();
        if (targetHandshake != null) {
            if (targetHandshake.getHostName().equals(handshakingPacket.address) && targetHandshake.getPort() == handshakingPacket.port) {
                return false;
            }
        }

        InetSocketAddress target = Proxy.getTargetAddress();
        if (target != null) {
            if (target.getHostName().equals(handshakingPacket.address) && target.getPort() == handshakingPacket.port) {
                return false;
            }
        }
        return false;
    }

    public void onFailedHandshake(ChannelHandlerContext ctx, Throwable t) {
        if (this.proxyConnection.isRedirected()) {
            Logger.u_info("handshake", "Failed handshake. Set port-forward. " + t.getClass() + " " + t.getMessage());
            InetSocketAddress connectAddress = this.proxyConnection.getRealDstAddress();
            Proxy.connectedAddresses.add(connectAddress);
            proxyConnection.connectToServer(connectAddress, addSkipPort).addListeners(removeSkipPort, (ThrowingChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    if (f.channel().remoteAddress() instanceof InetSocketAddress isa) {
                        Proxy.connectedAddresses.remove(isa);
                    }
                }
            }).syncUninterruptibly();
            proxyConnection.setForwardMode();

        } else {
            ctx.close();
            Logger.u_info("handshake", "Failed handshake. Close. " + t.getClass() + " " + t.getMessage());
        }
    }

    public boolean onHandshake(ChannelHandlerContext ctx, C2SHandshakingClientIntentionPacket handshakingPacket) {
        if (shouldEnablePortForward(handshakingPacket)) {
            System.out.println(PacketUtils.toString(handshakingPacket) + " SET TO FORWARD");
            C2SHandshakingClientIntentionPacket handshake;
            InetSocketAddress connectAddress;
            if (this.proxyConnection.isRedirected()) {
                handshake = handshakingPacket;
                connectAddress = this.proxyConnection.getRealDstAddress();
            } else {
                //direct connection to proxy. etc 127.0.0.1:25565, localhost:25565, 192.168.1.2:25565
                handshake = new C2SHandshakingClientIntentionPacket();
                handshake.intendedState = handshakingPacket.intendedState;
                handshake.protocolVersion = handshakingPacket.protocolVersion;
                InetSocketAddress targetAddress = Proxy.getTargetAddress();
                InetSocketAddress targetHandshakeAddress;
                if (targetAddress != null) {
                    connectAddress = targetAddress;
                    if (handshakingPacket.intendedState == IntendedState.LOGIN) {
                        targetHandshakeAddress = targetAddress;
                    } else {
                        targetHandshakeAddress = Proxy.getTargetHandshakeAddress();
                    }
                } else {
                    connectAddress = new InetSocketAddress(handshakingPacket.address, handshakingPacket.port);
                    targetHandshakeAddress = connectAddress;
                }
                handshake.address = targetHandshakeAddress.getHostName();
                handshake.port = targetHandshakeAddress.getPort();
            }

            if (checkLoopbackConnection(connectAddress)) {
                return true;
            }
            Logger.u_info("port forward connect", this.proxyConnection, "[" + handshakingPacket.protocolVersion + "] Connecting to " + connectAddress);
            Proxy.connectedAddresses.add(connectAddress);
            proxyConnection.connectToServer(connectAddress, addSkipPort).addListeners(removeSkipPort, (ThrowingChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    if (f.channel().remoteAddress() instanceof InetSocketAddress isa) {
                        Proxy.connectedAddresses.remove(isa);
                    }
                }
            }).syncUninterruptibly();

            proxyConnection.getChannel().writeAndFlush(handshake).syncUninterruptibly();
            proxyConnection.setForwardMode();
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


        if (packet.intendedState == null) {
            throw CloseAndReturn.INSTANCE;
        }
//        final int version = packet.protocolVersion;
//        this.proxyConnection.setVersion(version);
//        this.proxyConnection.setC2pConnectionState(packet.intendedState.getConnectionState());

        InetSocketAddress connectAddress;
        InetSocketAddress handshakeAddress;
        //connect and handshake addresses may be different for SRV server addresses.
        if (this.proxyConnection.isRedirected()) {
            connectAddress = this.proxyConnection.getRealDstAddress();
            handshakeAddress = new InetSocketAddress(packet.address, packet.port);
        } else {
            //direct connection to proxy. etc 127.0.0.1:25565, localhost:25565, 192.168.1.2:25565
            InetSocketAddress targetAddress = Proxy.getTargetAddress();
            if (targetAddress != null) {
                connectAddress = targetAddress;
                if (packet.intendedState == IntendedState.LOGIN) {
                    handshakeAddress = Proxy.getTargetAddress();
                } else {
                    handshakeAddress = Proxy.getTargetHandshakeAddress();
                }
            } else {
                if (Proxy.dualConnection != null) {
                    connectAddress = (InetSocketAddress) Proxy.dualConnection.getMainConnection().getChannel().remoteAddress();
                    //handshake with address will not be sent to server
                    handshakeAddress = Proxy.dualConnection.getMainConnection().getClientHandshakeAddress();
                } else {
                    //connect to what?
                    connectAddress = null;
                    handshakeAddress = null;
                }
            }

            //not tested. Should be implemented in future
            if (packet.intendedState.getConnectionState() == ConnectionState.LOGIN && TransferDataHolder.hasTempRedirect(this.proxyConnection.getC2P())) {
                connectAddress = TransferDataHolder.removeTempRedirect(this.proxyConnection.getC2P());
                System.out.println("tempRedirect  " + connectAddress);
            }
        }
        this.proxyConnection.setClientHandshakeAddress(new InetSocketAddress(packet.address, packet.port));
        ChannelUtil.disableAutoRead(this.proxyConnection.getC2P());
        System.out.println("Connect...");
        this.connect(connectAddress, handshakeAddress, packet, Proxy.getAccount());
    }

    private void connect(InetSocketAddress connectAddress, InetSocketAddress handshakeAddress, C2SHandshakingClientIntentionPacket clientHandshake, Account account) {
        final int version = clientHandshake.protocolVersion;
        final IntendedState intendedState = clientHandshake.intendedState;


        this.proxyConnection.getC2P().attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY).set(this.proxyConnection);
        this.proxyConnection.setVersion(version);

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

        this.proxyConnection.getPacketHandlers().add(new UnexpectedPacketHandler(this.proxyConnection));
        if (!this.proxyConnection.isController() && this.proxyConnection.dualConnection != null) {
            Logger.u_info("connect", this.proxyConnection, "cancel connect to server");
            this.proxyConnection.setP2sConnectionState(intendedState.getConnectionState());
            ChannelUtil.restoreAutoRead(this.proxyConnection.getC2P());

//            WinRedirect.redirectPause(Proxy.forward_redirect);
//            WinRedirect.redirectPause(Proxy.redirect);
            return;
        }
        if (connectAddress.equals(handshakeAddress)) {
            Logger.u_info("connect", this.proxyConnection, "[" + version + "] Connecting to " + connectAddress);
        } else {
            Logger.u_info("connect", this.proxyConnection, "[" + version + "] Connecting to " + connectAddress + " (" + handshakeAddress + ")");
        }

        final C2SHandshakingClientIntentionPacket newHandshake;
        if (this.proxyConnection.isRedirected()) {
            newHandshake = clientHandshake;
        } else {
            final String address = handshakeAddress.getHostString();
            final int port = handshakeAddress.getPort();
            newHandshake = new C2SHandshakingClientIntentionPacket(version, address, port, intendedState);
        }
        if (checkLoopbackConnection(connectAddress)) {
            return;
        }

        Proxy.connectedAddresses.add(connectAddress);
        this.proxyConnection.connectToServer(connectAddress, addSkipPort).addListeners(removeSkipPort, (ThrowingChannelFutureListener) f -> {
            if (f.isSuccess()) {
                f.channel().eventLoop().submit(() -> { // Reschedule so the packets get sent after the channel is fully initialized and active

                    this.proxyConnection.sendToServer(newHandshake, ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE, f2 -> {
                        if (f2.isSuccess()) {
//                            final UserConnection userConnection = this.proxyConnection.getUserConnection();
//                            if (userConnection.has(CookieStorage.class) && TransferDataHolder.hasCookieStorage(this.proxyConnection.getC2P())) {
//                                userConnection.get(CookieStorage.class).cookies().putAll(TransferDataHolder.removeCookieStorage(this.proxyConnection.getC2P()).cookies());
//                                System.out.println("COOKIES?");
//                            }
                            this.proxyConnection.setP2sConnectionState(intendedState.getConnectionState());
                            ChannelUtil.restoreAutoRead(this.proxyConnection.getC2P());
                            if (intendedState == IntendedState.LOGIN) {
                                //Disable read server->proxy packets until the second client is connected.
                                //Used to synchronize incoming packets between two connections
                                //will be restored in LoginPacketHandler
                                ChannelUtil.disableAutoRead(this.proxyConnection.getChannel());
                            }
                        }
                    });
                });
            }
        }, (ThrowingChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                if (f.channel().remoteAddress() instanceof InetSocketAddress isa) {
                    Proxy.connectedAddresses.remove(isa);
                }
                if (f.cause() instanceof ConnectException || f.cause() instanceof UnresolvedAddressException) {
                    this.proxyConnection.kickClient("Could not connect to the backend server!");
                } else {
                    Logger.LOGGER.error("Error while connecting to the backend server", f.cause());
                    this.proxyConnection.kickClient("An error occurred while connecting to the backend server: " + f.cause().getMessage() + "\nCheck the console for more information.");
                }
            }
        });
    }

    private boolean checkLoopbackConnection(InetSocketAddress serverAddress) {
        if (Proxy.proxyAddress.getAddress().isAnyLocalAddress()) {
            if (serverAddress.getAddress().isLoopbackAddress() && Proxy.proxyAddress.getPort() == serverAddress.getPort()) {
                Logger.u_info("disconnect", this.proxyConnection, "Cancel loopback connection");
                this.proxyConnection.getC2P().close();
                return true;
            }
        } else if (Proxy.proxyAddress.equals(serverAddress)) {
            Logger.u_info("disconnect", this.proxyConnection, "Cancel loopback connection");
            this.proxyConnection.getC2P().close();
            return true;
        }
        return false;
    }

}
