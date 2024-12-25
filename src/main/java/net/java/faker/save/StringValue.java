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
