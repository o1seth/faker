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

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import net.java.faker.auth.Account;
import net.java.faker.auth.MicrosoftAccount;
import net.java.faker.util.logging.Logger;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountManager {
    private final File file;
    List<Account> accounts = new ArrayList<>();

    public AccountManager(File file) {
        this.file = file;

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                JsonElement json = JsonParser.parseReader(reader);
                if (json instanceof JsonArray array) {
                    for (JsonElement e : array) {
                        if (e instanceof JsonObject serializedSession) {
                            try {
                                StepFullJavaSession.FullJavaSession loadedSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(serializedSession);
                                MicrosoftAccount microsoftAccount = new MicrosoftAccount(loadedSession);
                                this.accounts.add(microsoftAccount);
                            } catch (Exception ex) {
                                Logger.error("Failed to parse account " + ex.getMessage());
                            }

                        }
                    }
                }
            } catch (Exception e) {
                Logger.error("Failed to parse accounts " + e.getMessage());
            }
        }

    }

    public void save() {
        JsonArray array = new JsonArray();
        for (Account a : this.accounts) {
            if (a instanceof MicrosoftAccount microsoftAccount) {
                JsonObject serializedSession = microsoftAccount.toJson();
                array.add(serializedSession);
            }
        }
        if (array.isEmpty()) {
            if (file.exists() && !file.delete()) {
                Logger.error("Failed to delete accounts");
            }
        } else {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                JsonWriter jsonWriter = new JsonWriter(writer);
                jsonWriter.setStrictness(Strictness.LENIENT);
                Streams.write(array, jsonWriter);
                writer.close();
            } catch (Exception e) {
                Logger.error("Failed to save accounts " + e.getMessage());
            }
        }
    }

    public void removeAccount(Account account) {
        this.accounts.remove(account);
    }

    public void addAccount(Account account) {
        this.accounts.add(account);
    }

    public void addAccount(int index, Account account) {
        this.accounts.add(index, account);
    }


    public List<Account> getAccounts() {
        return Collections.unmodifiableList(this.accounts);
    }


}
