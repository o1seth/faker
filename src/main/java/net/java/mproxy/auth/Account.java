package net.java.mproxy.auth;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;

import java.util.UUID;

public abstract class Account {

    private long lastRefresh = 0L;

    public Account() {
    }

    public abstract JsonObject toJson();

    public abstract String getName();

    public abstract UUID getUUID();

    public GameProfile getGameProfile() {
        return new GameProfile(this.getUUID(), this.getName());
    }

    public abstract String getDisplayString();

    public boolean refresh() throws Exception {
        if (System.currentTimeMillis() - this.lastRefresh < 10_000L) {
            return false;
        }
        this.lastRefresh = System.currentTimeMillis();
        return true;
    }

}
