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

package net.java.faker.save;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import net.java.faker.util.logging.Logger;
import net.raphimc.netminecraft.util.MinecraftServerAddress;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Config {
    final ArrayList<Value> values = new ArrayList<>();
    private final StringValue serverAddress = registerString("ServerAddress");

    public final StringValue account = registerString("Account");
    public final BooleanValue onlineMode = registerBoolean("OnlineMode");
    public final BooleanValue signChat = registerBoolean("SignChat");
    public final BooleanValue showKickErrors = registerBoolean("ShowKickErrors");
    public final BooleanValue tracerouteFix = registerBoolean("TracerouteFix").defaultValue(true);
    public final BooleanValue mdnsDisable = registerBoolean("mDNSDisable").defaultValue(true);
    public final BooleanValue routerSpoof = registerBoolean("RouterSpoof").defaultValue(true);
    public final BooleanValue blockTraffic = registerBoolean("BlockTraffic").defaultValue(true);
    public final BooleanValue allowDirectConnection = registerBoolean("AllowDirectConnection").defaultValue(false);
    public final BooleanValue autoLatency = registerBoolean("AutoLatency").defaultValue(false);
    public final StringValue locale = registerString("Locale");
    public final StringValue targetAdapter = registerString("TargetAdapter");
    public final StringValue dhcp_interface = registerString("DHCP_interface");
    public final StringValue dhcp_ip = registerString("DHCP_ip");
    public final StringValue dhcp_mask = registerString("DHCP_mask");
    public final StringValue dhcp_startIp = registerString("DHCP_startIp");
    public final StringValue dhcp_endIp = registerString("DHCP_endIp");
    public final StringValue dhcp_dns1 = registerString("DHCP_dns1");
    public final StringValue dhcp_dns2 = registerString("DHCP_dns2");
    public final BooleanValue dhcp_started = registerBoolean("DHCP_started");
    public final StringArrayValue lastServersValue = registerStringArray("LastServers");
    private InetSocketAddress targetHandshakeAddress = setTargetHandshakeAddress((String) null);

    public final StringValue proxy = registerString("proxy");
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
                Logger.error("Failed to parse config " + e.getMessage());
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
                Logger.error("Failed to delete config");
            }
        } else {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                JsonWriter jsonWriter = new JsonWriter(writer);
                jsonWriter.setStrictness(Strictness.LENIENT);
                Streams.write(json, jsonWriter);
                writer.close();
            } catch (Exception e) {
                Logger.error("Failed to save config " + e.getMessage());
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

    private StringArrayValue registerStringArray(String name) {
        StringArrayValue value = new StringArrayValue(name);
        this.values.add(value);
        return value;
    }

    private String serverAddressHost = "";

    public void refreshServerAddress() {
        this.setServerAddress(serverAddressHost);
    }

    public void setServerAddress(String host) {
        this.serverAddressHost = host;
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
