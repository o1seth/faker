package net.java.mproxy.proxy.session;


import io.netty.channel.Channel;
import net.java.mproxy.proxy.packet.C2SPlayerCommand;
import net.java.mproxy.proxy.packet.S2CSetPassengers;
import net.java.mproxy.proxy.util.ChannelUtil;
import net.java.mproxy.proxy.util.chat.ChatSession1_19_3;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.constants.MCVersion;

public class DualConnection {
    protected final ProxyConnection mainConnection;
    protected ProxyConnection sideConnection;

    public final Object waiter = new Object();
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


    public DualConnection(ProxyConnection mainConnection) {
        this.mainConnection = mainConnection;
        this.controllerLocker = mainConnection.controllerLocker;
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

    public void restoreAutoRead() {
        ChannelUtil.restoreAutoRead(this.getChannel());
    }

    boolean firstSwap = true;

    public void onPlay() {
        if (firstSwap) {

            if (getSideConnection().getC2pConnectionState() == ConnectionState.PLAY && getMainConnection().getC2pConnectionState() == ConnectionState.PLAY) {
                swapController();
                System.out.println("FIRST SWAP!");
                firstSwap = false;
            }
        }
    }

    public void setSideConnection(ProxyConnection sideConnection) {
        sideConnection.controllerLocker = this.controllerLocker;
        this.sideConnection = sideConnection;
//        synchronized (waiter) {
//            waiter.notifyAll();
//        }
    }

    public synchronized void swapController() {
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
//            if (this.entityId != 0) {
//                S2CSetEntityMotion motion = new S2CSetEntityMotion();
//                motion.entityId = entityId;
//                motion.motionX = playerX - prevX;
//                motion.motionY = playerY - prevY;
//                motion.motionZ = playerZ - prevZ;
//                follower.sendToClient(motion);
//            }
            follower.isController = true;
            controller.isController = false;
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

    public ProxyConnection getSideConnection() {
        if (sideConnection == null) {
            try {
                synchronized (waiter) {
                    waiter.wait();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return this.sideConnection;
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


    public ChatSession1_19_3 getChatSession1_19_3() {
        return chatSession1_19_3;
    }

//    public void sendToServer(Packet packet) {
//        if (mainConnection.getChannel() != null) {
//            mainConnection.getChannel().writeAndFlush(packet);
//            return;
//        }
//        if (sideConnection != null && sideConnection.getChannel() != null) {
//            sideConnection.getChannel().writeAndFlush(packet);
//            return;
//        }
//        throw new IllegalStateException("Attempt to send packet to nothing");
//    }
//
//    public void sendToServer(Packet packet, List<ChannelFutureListener> listeners) {
//        if (mainConnection.getChannel() != null) {
//            mainConnection.getChannel().writeAndFlush(packet).addListeners(listeners.toArray(new ChannelFutureListener[listeners.size()]));
//            return;
//        }
//        if (sideConnection != null && sideConnection.getChannel() != null) {
//            sideConnection.getChannel().writeAndFlush(packet).addListeners(listeners.toArray(new ChannelFutureListener[listeners.size()]));
//            return;
//        }
//        throw new IllegalStateException("Attempt to send packet to nothing");
//    }
}
