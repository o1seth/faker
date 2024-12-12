package net.java.mproxy.save;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Objects;

public class StringValue extends Value {
    private String value;
    private String defaultValue;

    StringValue(String name) {
        super(name);
    }

    public String get() {
        return this.value;
    }

    public StringValue set(String value) {
        this.value = value;
        return this;
    }

    public StringValue defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public void setDefault() {
        this.value = this.defaultValue;
    }

    public boolean isDefault() {
        return Objects.equals(this.value, this.defaultValue);
    }

    @Override
    public void setFromJson(JsonElement element) {
        if (element instanceof JsonPrimitive primitive) {
            this.set(primitive.getAsString());
        }
    }

    @Override
    public JsonElement toJsonElement() {
        return new JsonPrimitive(this.value);
    }
}
