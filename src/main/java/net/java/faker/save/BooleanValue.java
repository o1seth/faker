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
