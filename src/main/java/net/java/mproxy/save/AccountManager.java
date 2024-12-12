package net.java.mproxy.save;

import net.java.mproxy.auth.Account;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class SaveManager {

    public void removeAccount(Account account) {

    }

    public void addAccount(Account account) {

    }

    public void addAccount(int index, Account account) {

    }

    public void put(String setting, String value) {

    }

    public String get(String setting) {
        if (setting.equals("locale")) {
            return "en_US";
        }
        return setting + ".value";
    }

    public List<Account> getAccounts() {
        return Collections.emptyList();
//        return Collections.unmodifiableList(this.accounts);
    }


    public void save() {

    }

    public void loadTextField(String serverAddress, JTextField serverAddress1) {
    }


}
