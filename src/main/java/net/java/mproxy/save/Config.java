package net.java.mproxy.save;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import net.java.mproxy.util.logging.Logger;
import net.raphimc.netminecraft.util.MinecraftServerAddress;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Config {
    final ArrayList<Value> values = new ArrayList<>();
    private final StringValue serverAddress = registerString("ServerAddress");
    public final BooleanValue onlineMode = registerBoolean("OnlineMode");
    public final BooleanValue signChat = registerBoolean("SignChat");
    public final BooleanValue tracerouteFix = registerBoolean("TracerouteFix").defaultValue(true);
    public final BooleanValue mdnsDisable = registerBoolean("mDNSDisable").defaultValue(true);
    public final BooleanValue routerSpoof = registerBoolean("RouterSpoof").defaultValue(true);
    public final BooleanValue blockTraffic = registerBoolean("BlockTraffic").defaultValue(true);
    public final StringValue locale = registerString("Locale");
    public final StringValue targetAdapter = registerString("TargetAdapter");
    private InetSocketAddress targetHandshakeAddress = setTargetHandshakeAddress((String) null);
    private InetSocketAddress targetAddress;
    private final File file;

    public Config(File file) {
        this.file = file;
        for (Value value : this.values) {
            value.setDefault();
        }
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                JsonElement json = JsonParser.parseReader(reader);
                if (json instanceof JsonObject jsonObject) {
                    for (Value value : this.values) {
                        value.setFromJson(jsonObject.get(value.name));
                    }
                }
            } catch (Exception e) {
                Logger.LOGGER.error("Failed to parse config " + e.getMessage());
            }
        }

        setTargetHandshakeAddress(this.serverAddress.get());
    }

    public void save() {
        JsonObject json = new JsonObject();
        for (Value value : this.values) {
            if (!value.isDefault()) {
                json.add(value.name, value.toJsonElement());
            }
        }
        if (json.isEmpty()) {
            if (file.exists() && !file.delete()) {
                Logger.LOGGER.error("Failed to delete config");
            }
        } else {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                JsonWriter jsonWriter = new JsonWriter(writer);
                jsonWriter.setStrictness(Strictness.LENIENT);
                Streams.write(json, jsonWriter);
                writer.close();
            } catch (Exception e) {
                Logger.LOGGER.error("Failed to save config " + e.getMessage());
            }
        }
    }

    private BooleanValue registerBoolean(String name) {
        BooleanValue value = new BooleanValue(name);
        this.values.add(value);
        return value;
    }

    private StringValue registerString(String name) {
        StringValue value = new StringValue(name);
        this.values.add(value);
        return value;
    }

    public void setServerAddress(String host) {
        this.serverAddress.set(host.isEmpty() ? null : host);
        setTargetHandshakeAddress(host);
    }

    public InetSocketAddress getTargetHandshakeAddress() {
        return targetHandshakeAddress;
    }

    public InetSocketAddress getTargetAddress() {
        return targetAddress;
    }

    public String getServerAddress() {
        return this.serverAddress.get();
    }

    private InetSocketAddress setTargetHandshakeAddress(String host) {
        if (host == null || host.isEmpty()) {
            return setTargetHandshakeAddress((InetSocketAddress) null);
        }
        MinecraftServerAddress address = MinecraftServerAddress.ofUnresolved(host);
        return setTargetHandshakeAddress(address);
    }

    private InetSocketAddress setTargetHandshakeAddress(InetSocketAddress targetHandshakeAddress) {
        this.targetHandshakeAddress = targetHandshakeAddress;
        if (targetHandshakeAddress == null) {
            targetAddress = null;
        } else {
            targetAddress = MinecraftServerAddress.ofResolved(targetHandshakeAddress.getHostName(), targetHandshakeAddress.getPort());
        }

        return this.targetHandshakeAddress;
    }


}
