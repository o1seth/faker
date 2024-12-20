package net.java.faker.save;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanValue extends Value {
    private boolean value;
    private boolean defaultValue;

    BooleanValue(String name) {
        super(name);
    }

    public boolean get() {
        return this.value;
    }

    public BooleanValue set(boolean value) {
        this.value = value;
        return this;
    }

    public BooleanValue defaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public void setDefault() {
        this.value = this.defaultValue;
    }

    public boolean isDefault() {
        return this.value == this.defaultValue;
    }

    @Override
    public void setFromJson(JsonElement element) {
        if (element instanceof JsonPrimitive primitive) {
            this.set(primitive.getAsBoolean());
        }
    }

    @Override
    public JsonElement toJsonElement() {
        return new JsonPrimitive(this.value);
    }
}
