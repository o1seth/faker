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


import io.netty.channel.Channel;
import net.java.faker.Proxy;
import net.java.faker.WinRedirect;
import net.java.faker.proxy.event.SwapEvent;
import net.java.faker.proxy.packet.*;
import net.java.faker.proxy.packet.keepalive.*;
import net.java.faker.proxy.packet.pingpong.*;
import net.java.faker.proxy.util.ChannelUtil;
import net.java.faker.proxy.util.LatencyMode;
import net.java.faker.proxy.util.chat.ChatSession1_19_3;
import net.java.faker.util.logging.Logger;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.constants.MCVersion;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DualConnection {
    protected final ProxyConnection mainConnection;
    protected ProxyConnection sideConnection;
    //    public final Object waiter = new Object();
    public double prevX;
    public double prevY;
    public double prevZ;
    public float prevYaw;
    public float prevPitch;
    public boolean prevOnGround;
    public double playerX;
    public double playerY;
    public double playerZ;
    public float playerYaw;
    public float clientPlayerYaw;// client will change yaw for: yaw = yaw % 360
    public float playerPitch;
    public boolean onGround;
    public C2SPlayerCommand.Action shiftState = C2SPlayerCommand.Action.RELEASE_SHIFT_KEY;
    public C2SPlayerCommand.Action sprintState = C2SPlayerCommand.Action.STOP_SPRINTING;
    public int entityId;
    public int vehicleId = -1;
    public S2CSetPassengers setPassengersPacket;
    private ChatSession1_19_3 chatSession1_19_3;
    private final Object controllerLocker;
    private boolean firstSwap = true;
    private List<C2SAbstractPong> skipPongs = Collections.emptyList();
    private long lastSwapControllerTime;
    private int connectTime;
    private LatencyChecker latencyChecker;
    private int ttl;
    private int avgLatency;
    private int minLatency = Integer.MAX_VALUE;
    final ArrayList<Pong> pongs = new ArrayList<>();
//    final ArrayList<Teleport> teleports = new ArrayList<>();
    int nextPongNum;
    int nextKeepAliveNum;
    int nextTeleportNum;

    public static class Pong {
        final C2SAbstractResponse packet;
        volatile boolean sent;
        final int num;

        private Pong(C2SAbstractResponse packet, int num) {
            this.packet = packet;
            this.num = num;
        }
    }

    public static class Teleport {
        final C2SAcceptTeleport packet;
        volatile boolean sent;
        final int num;

        private Teleport(C2SAcceptTeleport packet, int num) {
            this.packet = packet;
            this.num = num;
        }
    }

    public DualConnection(ProxyConnection mainConnection) {
        this.mainConnection = mainConnection;
        this.controllerLocker = mainConnection.controllerLocker;
    }

    public void startLatencyChecker() {
        if (this.latencyChecker != null) {
            return;
        }
        if (WinRedirect.isSupported()) {
            this.latencyChecker = new LatencyChecker();
            this.latencyChecker.start();
        }
        this.connectTime = mainConnection.getConnectTime();
    }

    public boolean isP2sEncrypted() {
        return mainConnection.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).get() != null;
    }

    public Channel getChannel() {
        return mainConnection.getChannel();
    }

    public void disableAutoRead() {
        ChannelUtil.disableAutoRead(this.getChannel());
    }

    public void disableAutoRead2() {
        ChannelUtil.disableAutoRead2(this.getChannel());
    }

    public void restoreAutoRead() {
        ChannelUtil.restoreAutoRead(this.getChannel());
    }

    public void setSideConnection(ProxyConnection sideConnection) {
        sideConnection.controllerLocker = this.controllerLocker;
        this.sideConnection = sideConnection;
//        synchronized (waiter) {
//            waiter.notifyAll();
//        }
    }

    public boolean isFirstSwap() {
        return this.firstSwap;
    }

    boolean skipPong(C2SAbstractPong pong) {
        if (skipPongs.isEmpty()) {
            return false;
        }
        if (System.currentTimeMillis() - this.lastSwapControllerTime > 3500) {
            skipPongs.clear();
            return false;
        }
        return skipPongs.remove(pong);
    }

    public synchronized void swapController() {
        this.firstSwap = false;
        synchronized (controllerLocker) {
            ProxyConnection follower = this.getFollower0();
            ProxyConnection controller = this.getController0();
            if (follower == null || controller == null || follower.isClosed()) {
                return;
            }
            //rare cases. Swap while moving
            if (shiftState == C2SPlayerCommand.Action.PRESS_SHIFT_KEY) {
                C2SPlayerCommand releaseShift = new C2SPlayerCommand();
                releaseShift.id = entityId;
                releaseShift.action = C2SPlayerCommand.Action.RELEASE_SHIFT_KEY;
                controller.sendToServer(releaseShift);
                shiftState = C2SPlayerCommand.Action.RELEASE_SHIFT_KEY;
            }

            if (sprintState == C2SPlayerCommand.Action.START_SPRINTING) {
                C2SPlayerCommand stopSprint = new C2SPlayerCommand();
                stopSprint.id = entityId;
                stopSprint.action = C2SPlayerCommand.Action.STOP_SPRINTING;
                controller.sendToServer(stopSprint);
                sprintState = C2SPlayerCommand.Action.STOP_SPRINTING;
            }
            if (isPassenger() && mainConnection.getVersion() >= MCVersion.v1_9) {
                if (!follower.isPassenger) {
                    follower.sendToClient(this.setPassengersPacket);
                    follower.isPassenger = true;
                }
                if (controller.isPassenger) {
                    controller.sendToClient(new S2CSetPassengers(vehicleId));
                    controller.isPassenger = false;
                }
            }


//            List<Packet> controllerPackets = controller.getSentPackets();
//            List<Packet> followerPackets = follower.getSentPackets();
//            if (controllerPackets.size() == followerPackets.size()) {
//                Iterator<Packet> citerator = controllerPackets.iterator();
//                Iterator<Packet> fiterator = followerPackets.iterator();
//                int i = 0;
//                while (citerator.hasNext()) {
//                    Packet cp = citerator.next();
//                    Packet fp = fiterator.next();
//                    Logger.raw(i + "  " + PacketUtils.toString(cp) + " " + PacketUtils.toString(fp));
//                    i++;
//                }
//            }
            if (!Proxy.getConfig().newPingCorrection.get()) {
                Logger.raw("SWAP OLD");
                C2SAbstractPong lastSentPong = controller.getLastSentPong();
                List<C2SAbstractPong> notSentPongs = follower.getPongPacketsAfter(lastSentPong);
                for (C2SAbstractPong pong : notSentPongs) {
                    controller.getChannel().writeAndFlush(pong).syncUninterruptibly();
                }
                this.skipPongs = controller.getPongPacketsAfter(follower.getLastSentPong());
            }

//            if (!notSentPongs.isEmpty()) {
//                Logger.raw("NOT SENT PONGS " + notSentPongs);
//            }
//            if (!this.skipPongs.isEmpty()) {
//                Logger.raw("SKIP     PONGS " + this.skipPongs);
//            }

//            if (this.entityId != 0) {
//                S2CSetEntityMotion motion = new S2CSetEntityMotion();
//                motion.entityId = entityId;
//                motion.motionX = playerX - prevX;
//                motion.motionY = playerY - prevY;
//                motion.motionZ = playerZ - prevZ;
//                follower.sendToClient(motion);
//            }
            this.lastSwapControllerTime = System.currentTimeMillis();
            follower.isController = true;
            controller.isController = false;
            Proxy.event(new SwapEvent(follower));
        }
    }


    public ProxyConnection getFollower() {
        synchronized (controllerLocker) {
            return this.getFollower0();
        }
    }

    public ProxyConnection getController() {
        synchronized (controllerLocker) {
            return this.getController0();
        }
    }

    public boolean isClosed() {
        synchronized (controllerLocker) {
            ProxyConnection follower = this.getFollower0();
            ProxyConnection controller = this.getController0();
            boolean followerClosed = true;
            boolean controllerClosed = true;
            if (follower != null && !follower.isClosed()) {
                followerClosed = false;
            }
            if (controller != null && !controller.isClosed()) {
                controllerClosed = false;
            }
            return followerClosed && controllerClosed;
        }

    }

    private ProxyConnection getController0() {
        if (mainConnection.isController) {
            return mainConnection;
        }
        if (sideConnection != null && sideConnection.isController) {
            return sideConnection;
        }
        return null;
    }

    private ProxyConnection getFollower0() {
        if (!mainConnection.isController) {
            return mainConnection;
        }
        if (sideConnection != null && !sideConnection.isController) {
            return sideConnection;
        }
        return null;
    }

    public boolean isBothConnectionCreated() {
        return this.sideConnection != null;
    }

    public ProxyConnection getSideConnection() {
//        if (sideConnection == null) {
//            try {
//                Logger.raw("Wait for side...");
//                synchronized (waiter) {
//                    waiter.wait();
//                }
//                Logger.raw("Side created!");
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
        return this.sideConnection;
    }

    public boolean hasDualConnections() {
        if (mainConnection.isClosed()) {
            return false;
        }
        if (sideConnection == null || sideConnection.isClosed()) {
            return false;
        }
        return true;
    }

    private C2SAbstractResponse createPong(S2CAbstractRequest packet) {
        if (packet instanceof S2CPing p) {
            C2SPong pong = new C2SPong();
            pong.id = p.id;
            return pong;
        } else if (packet instanceof S2CPlayKeepAlive p) {
            C2SPlayKeepAlive keepAlive = new C2SPlayKeepAlive();
            keepAlive.id = p.id;
            return keepAlive;
        } else if (packet instanceof S2CConfigKeepAlive p) {
            C2SConfigKeepAlive keepAlive = new C2SConfigKeepAlive();
            keepAlive.id = p.id;
            return keepAlive;
        } else if (packet instanceof S2CWindowConfirmation p) {
            if (!p.accepted) {
                //TODO: check openContainer id
                C2SWindowConfirmation confirm = new C2SWindowConfirmation();
                confirm.windowId = p.windowId;
                confirm.uid = p.uid;
                confirm.accepted = true;
                return confirm;
            }
        }
        return null;
    }

    public Pong getPongByNum(int num, C2SAbstractResponse packet) {
        synchronized (pongs) {
            int index = getPongIndexByNum(num, packet);
            if (index >= 0) {
                return pongs.get(index);
            }
        }
        return null;
    }

    int getPongIndexByNum(int num, C2SAbstractResponse packet) {
        synchronized (pongs) {
            for (int i = 0, size = pongs.size(); i < size; i++) {
                Pong p = pongs.get(i);
                if (packet.getClass().isAssignableFrom(p.packet.getClass())) {
                    if (p.num == num) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

//    public boolean handleTeleport(S2CPlayerPosition packet) {
//        if (mainConnection.getVersion() < MCVersion.v1_9) {
//            return false;
//        }
//        synchronized (teleports) {
//            int num = nextTeleportNum;
//            nextTeleportNum++;
//
//            Teleport tp = new Teleport(new C2SAcceptTeleport(packet.getTeleportId()), num);
//            teleports.add(tp);
//            if (teleports.size() > 128) {
//                teleports.remove(0);
//            }
//        }
//        return false;
//    }

    public boolean handleAbstractRequest(S2CAbstractRequest packet) {
        if (!Proxy.getConfig().newPingCorrection.get()) {
            return false;
        }
        C2SAbstractResponse pongPacket = createPong(packet);
        if (pongPacket != null) {
            synchronized (pongs) {
                int num = -1;
                if (packet instanceof S2CAbstractPing) {
                    num = nextPongNum;
                    nextPongNum++;
                } else if (packet instanceof S2CAbstractKeepAlive) {
                    num = nextKeepAliveNum;
                    nextKeepAliveNum++;
                }
                if (num >= 0) {
                    Pong pong = new Pong(pongPacket, num);
                    pongs.add(pong);
                    if (pongs.size() > 384) {
                        pongs.remove(0);
                    }
                    //this code is used for bypass 2x ping when you play from device with latency
//                    synchronized (controllerLocker) {
//                        ProxyConnection controller = getController0();
//                        if (controller != null && controller.getLatency() > 0) {
//                            int to = pongs.size() - 1;
//                            for (int i = Math.max(0, to - 30); i < to; i++) {
//                                Pong p = pongs.get(i);
//                                if (!p.sent) {
//                                    Logger.raw("Send unsent packet (dual) " + pong.packet);
//                                    p.sent = true;
//                                    mainConnection.getChannel().writeAndFlush(p.packet);
//                                }
//                            }
//                            pong.sent = true;
//                            Logger.raw("Send new unsent packet (dual) " + pong.packet + " " + controller.getLatency());
//                            mainConnection.getChannel().writeAndFlush(pong.packet);
//                        }
//                    }
                } else {
                    Logger.raw("Unknown request packet " + packet);
                }
            }
        }
        return false;
    }

    public boolean isBothAutoRead() {
        if (!mainConnection.getChannel().config().isAutoRead()) {
            return false;
        }
        if (sideConnection != null && !sideConnection.getChannel().config().isAutoRead()) {
            return false;
        }
        return true;
    }

    public boolean isBothPlayState() {
        if (mainConnection.isClosed()) {
            return false;
        }
        if (sideConnection == null || sideConnection.isClosed()) {
            return false;
        }
        return mainConnection.getC2pConnectionState() == ConnectionState.PLAY && sideConnection.getC2pConnectionState() == ConnectionState.PLAY;
    }

    public void clearVehicle() {
        vehicleId = -1;
        setPassengersPacket = null;
        mainConnection.isPassenger = false;
        if (sideConnection != null) {
            sideConnection.isPassenger = false;
        }
    }

    public boolean isPassenger() {
        return this.vehicleId >= 0;
    }

    public ProxyConnection getMainConnection() {
        return mainConnection;
    }

    public void setChatSession1_19_3(ChatSession1_19_3 chatSession1_19_3) {
        this.chatSession1_19_3 = chatSession1_19_3;
    }

    public int getConnectTime() {
        return connectTime;
    }

    public ChatSession1_19_3 getChatSession1_19_3() {
        return chatSession1_19_3;
    }


    private class LatencyChecker extends Thread {
        @Override
        public void run() {
            Channel channel = mainConnection.getChannel();
            int fromPort = ((InetSocketAddress) channel.localAddress()).getPort();
            InetSocketAddress connectAddress = (InetSocketAddress) mainConnection.getServerAddress();
            String toIp = connectAddress.getAddress().getHostAddress();
            int toPort = connectAddress.getPort();
            int latencySum = 0;
            int latencyCount = 0;
            minLatency = Integer.MAX_VALUE;
            int newLatency = 0;
            int count = 0;
            int lastTtl = 0;
            while (!isInterrupted()) {
                if (!channel.isOpen()) {
                    break;
                }
                if (count % 4 == 0) {
                    ttl = WinRedirect.getTtl(toIp, toPort, null, fromPort);
                    if (Proxy.getConfig().tracerouteFix.get()) {
                        if (lastTtl != ttl) {
                            WinRedirect.setTtlOverride(Proxy.forward_redirect, ttl - 1);
                            WinRedirect.setTtlOverride(Proxy.redirect, ttl);
                            if (Proxy.transfer_forward_redirect != 0) {
                                WinRedirect.setTtlOverride(Proxy.transfer_forward_redirect, ttl - 1);
                            }
                            if (Proxy.transfer_redirect != 0) {
                                WinRedirect.setTtlOverride(Proxy.transfer_redirect, ttl);
                            }
                            int paththroughTtl = 0;
                            if (63 - ttl > 0) {
                                paththroughTtl = 63 - ttl;
                            } else if (127 - ttl > 0) {
                                paththroughTtl = 128 - ttl;
                            } else if (255 - ttl > 0) {
                                paththroughTtl = 255 - ttl;
                            }
                            if (paththroughTtl < 24) {
                                WinRedirect.setTtlPaththrough(Proxy.forward_redirect, paththroughTtl);
                                if (Proxy.transfer_forward_redirect != 0) {
                                    WinRedirect.setTtlPaththrough(Proxy.transfer_forward_redirect, paththroughTtl);
                                }
                            }
                            lastTtl = ttl;
                        }
                    } else {
                        if (lastTtl != 0) {
                            WinRedirect.setTtlOverride(Proxy.forward_redirect, 0);
                            WinRedirect.setTtlOverride(Proxy.redirect, 0);
                            if (Proxy.transfer_forward_redirect != 0) {
                                WinRedirect.setTtlOverride(Proxy.transfer_forward_redirect, 0);
                            }
                            if (Proxy.transfer_redirect != 0) {
                                WinRedirect.setTtlOverride(Proxy.transfer_redirect, 0);
                            }
                            WinRedirect.setTtlPaththrough(Proxy.forward_redirect, 0);
                            if (Proxy.transfer_forward_redirect != 0) {
                                WinRedirect.setTtlPaththrough(Proxy.transfer_forward_redirect, 0);
                            }
                            lastTtl = 0;
                        }
                    }
                }

                int latency = WinRedirect.getLatency(null, fromPort, toIp, toPort);
                if (latency < getConnectTime() * 3 && latency > 2) {
                    latencySum += latency;
                    latencyCount++;
                    avgLatency = latencySum / latencyCount;
                    if (latency < minLatency) {
                        minLatency = latency;
                    }
                }
                newLatency = (minLatency + avgLatency) / 2;
                if (mainConnection.getLatencyMode() == LatencyMode.AUTO) {
                    mainConnection.setLatency(newLatency);
                }
                if (sideConnection != null && sideConnection.getLatencyMode() == LatencyMode.AUTO) {
                    sideConnection.setLatency(newLatency);
                }
                if (Proxy.getConfig().autoLatency.get()) {
                    WinRedirect.redirectSetDefaultLatency(Proxy.forward_redirect, newLatency);
                    if (Proxy.transfer_forward_redirect != 0) {
                        WinRedirect.redirectSetDefaultLatency(Proxy.transfer_forward_redirect, newLatency);
                    }
                }
                try {
                    Thread.sleep(750);
                } catch (Exception ignored) {

                }
                count++;
            }

        }
    }
}
