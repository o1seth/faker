package net.java.mproxy.save;

import com.google.gson.JsonElement;

public abstract class Value {
    public final String name;

    Value(String name) {
        this.name = name;
    }

    public abstract boolean isDefault();

    public abstract void setDefault();

    public abstract void setFromJson(JsonElement element);

    public abstract JsonElement toJsonElement();
}
