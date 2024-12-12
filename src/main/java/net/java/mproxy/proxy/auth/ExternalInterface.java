package net.java.mproxy.proxy.auth;

import net.java.mproxy.Proxy;
import net.java.mproxy.proxy.session.DualConnection;
import net.raphimc.minecraftauth.step.java.StepPlayerCertificates;
import net.raphimc.netminecraft.constants.MCVersion;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket;
import net.java.mproxy.proxy.session.ProxyConnection;
import net.java.mproxy.proxy.util.chat.ChatSession1_19_3;
import net.java.mproxy.proxy.util.chat.ProfileKey;
import net.java.mproxy.auth.Account;
import net.java.mproxy.auth.MicrosoftAccount;
import net.java.mproxy.util.logging.Logger;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public class ExternalInterface {

    public static void fillPlayerData(final ProxyConnection proxyConnection) {
        Logger.u_info("auth", proxyConnection, "Filling player data");
        try {
            if (proxyConnection.getAccount() != null) {
                final Account account = proxyConnection.getAccount();
//                ViaProxy.getSaveManager().accountsSave.ensureRefreshed(account);

                proxyConnection.setGameProfile(account.getGameProfile());
                //final UserConnection user = proxyConnection.getUserConnection();

                if (Proxy.getConfig().signChat.get() && proxyConnection.getVersion() >= (MCVersion.v1_19) && account instanceof MicrosoftAccount microsoftAccount) {
                    final StepPlayerCertificates.PlayerCertificates playerCertificates = microsoftAccount.getPlayerCertificates();
                    final Instant expiresAt = Instant.ofEpochMilli(playerCertificates.getExpireTimeMs());
                    final long expiresAtMillis = playerCertificates.getExpireTimeMs();
                    final PublicKey publicKey = playerCertificates.getPublicKey();
                    final byte[] publicKeyBytes = publicKey.getEncoded();
                    final byte[] keySignature = playerCertificates.getPublicKeySignature();
                    final PrivateKey privateKey = playerCertificates.getPrivateKey();
                    final UUID uuid = proxyConnection.getGameProfile().getId();

                    byte[] loginHelloKeySignature = keySignature;
                    if (proxyConnection.getVersion() == (MCVersion.v1_19)) {
                        loginHelloKeySignature = playerCertificates.getLegacyPublicKeySignature();
                    }
                    proxyConnection.setLoginHelloPacket(new C2SLoginHelloPacket(proxyConnection.getGameProfile().getName(), expiresAt, publicKey, loginHelloKeySignature, proxyConnection.getGameProfile().getId()));
                    ChatSession1_19_3 chatSession1_19_3 = new ChatSession1_19_3(uuid, privateKey, new ProfileKey(expiresAtMillis, publicKeyBytes, keySignature));
//                    ChatSession1_19_0 chatSession1_19_0 = new ChatSession1_19_0(uuid, privateKey, new ProfileKey(expiresAtMillis, publicKeyBytes, playerCertificates.getLegacyPublicKeySignature()));
//                    ChatSession1_19_1 chatSession1_19_1 = new ChatSession1_19_1(uuid, privateKey, new ProfileKey(expiresAtMillis, publicKeyBytes, keySignature));
//                    user.put(chatSession1_19_0);
//                    user.put(chatSession1_19_1);
//                    user.put(chatSession1_19_3);
                    proxyConnection.dualConnection.setChatSession1_19_3(chatSession1_19_3);
//                    proxyConnection.setChatSession1_19_1(chatSession1_19_1);
//                    proxyConnection.setChatSession1_19_0(chatSession1_19_0);
                }
            }


        } catch (Throwable e) {
            Logger.LOGGER.error("Failed to fill player data", e);
            proxyConnection.kickClient("§cFailed to fill player data. This might be caused by outdated account tokens or rate limits. Wait a couple of seconds and try again. If the problem persists, remove and re-add your account.");
        }

        proxyConnection.getLoginHelloPacket().name = proxyConnection.getGameProfile().getName();
        proxyConnection.getLoginHelloPacket().uuid = proxyConnection.getGameProfile().getId();
    }

    public static void joinServer(final String serverIdHash, final ProxyConnection proxyConnection) throws InterruptedException, ExecutionException {
        Logger.u_info("auth", proxyConnection, "Trying to join online mode server");
        if (proxyConnection.getAccount() instanceof MicrosoftAccount microsoftAccount) {
            try {
                AuthLibServices.SESSION_SERVICE.joinServer(microsoftAccount.getGameProfile(), microsoftAccount.getMcProfile().getMcToken().getAccessToken(), serverIdHash);
            } catch (Throwable e) {
                proxyConnection.kickClient("§cFailed to authenticate with Mojang servers! Please try again in a couple of seconds.");
            }
        } else {
            proxyConnection.kickClient("§cThis server is in online mode and requires a valid authentication mode.");
        }
    }

    public static void signNonce(final byte[] nonce, final C2SLoginKeyPacket packet, final ProxyConnection proxyConnection) throws InterruptedException, ExecutionException, SignatureException {
        Logger.u_info("auth", proxyConnection, "Requesting nonce signature");
        //final UserConnection user = proxyConnection.getUserConnection();
        DualConnection dualConnection = proxyConnection.dualConnection;
        if (dualConnection.getChatSession1_19_3() != null) {
            final long salt = ThreadLocalRandom.current().nextLong();
            packet.signature = dualConnection.getChatSession1_19_3().sign(updater -> {
                updater.accept(nonce);
                updater.accept(toByteArray(salt));
            });
            packet.salt = salt;
        } else {
            proxyConnection.kickClient("§cThis server requires a signed nonce. Please enable chat signing in the config and select a valid authentication mode.");
        }
    }

    private static byte[] toByteArray(long value) {
        // Note that this code needs to stay compatible with GWT, which has known
        // bugs when narrowing byte casts of long values occur.
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xffL);
            value >>= 8;
        }
        return result;
    }
}
