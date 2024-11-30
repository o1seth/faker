package net.java.mproxy.proxy.external_interface;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import java.net.Proxy;
import java.util.UUID;

public class AuthLibServices {

    public static final YggdrasilAuthenticationService AUTHENTICATION_SERVICE = new YggdrasilAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString());
    public static final MinecraftSessionService SESSION_SERVICE = AUTHENTICATION_SERVICE.createMinecraftSessionService();
    public static final GameProfileRepository PROFILE_REPOSITORY = AUTHENTICATION_SERVICE.createProfileRepository();

}
