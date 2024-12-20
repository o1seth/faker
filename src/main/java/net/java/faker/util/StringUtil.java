package net.java.faker.util;

public class StringUtil {

    public static String emptyIfNull(final String s) {
        return s == null ? "" : s;
    }

}
