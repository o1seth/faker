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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;

public class StringArrayValue extends Value {
    private final ArrayList<String> value;

    StringArrayValue(String name) {
        super(name);
        this.value = new ArrayList<>();
    }

    public String get(int i) {
        return this.value.get(i);
    }

    public void add(String value) {
        this.value.add(value);
    }

    public void remove(String value) {
        this.value.remove(value);
    }

    public void removeIgnoreCase(String value) {
        this.value.removeIf(s -> s.equalsIgnoreCase(value));
    }

    public String[] reverseArray() {
        String[] array = new String[this.value.size()];
        for (int i = 0, j = array.length; i < array.length; i++) {
            array[--j] = this.value.get(i);
        }
        return array;
    }

    public int size() {
        return this.value.size();
    }

    public void remove(int i) {
        this.value.remove(i);
    }

    public void setDefault() {
        this.value.clear();
    }

    public boolean isDefault() {
        return this.value == null;
    }

    @Override
    public void setFromJson(JsonElement element) {
        if (element instanceof JsonArray array) {
            this.value.clear();
            for (int i = 0; i < array.size(); i++) {
                this.value.add(array.get(i).getAsString());
            }
        }
    }

    @Override
    public JsonElement toJsonElement() {
        if (this.value.isEmpty()) {
            return null;
        }
        JsonArray array = new JsonArray(this.value.size());
        for (String s : this.value) {
            array.add(new JsonPrimitive(s));
        }
        return array;
    }
}
