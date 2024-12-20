package net.java.faker.util.logging;

import com.mojang.authlib.GameProfile;
import net.java.faker.Proxy;
import net.java.faker.proxy.session.ProxyConnection;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;


import java.io.File;
import java.io.PrintStream;
import java.net.SocketAddress;
import java.util.Locale;

public class Logger {

    static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("Faker");


    public static final PrintStream SYSOUT = System.out;
    public static final PrintStream SYSERR = System.err;
    private static LoggerFileStream out = new LoggerFileStream("STDOUT", SYSOUT, new File(Proxy.getFakerDirectory(), "std.log"));
    private static LoggerFileStream err = new LoggerFileStream("STDERR", SYSERR, new File(Proxy.getFakerDirectory(), "err.log"));

    public static void setup() {
        System.setErr(err);
        System.setOut(out);
    }

    public static void raw(Object msg) {
        out.print(msg);
        out.print('\n');
        out.flush();
    }

    public static void u_info(final String title, final String msg) {
        u_log(Level.INFO, title, null, msg);
    }

    public static void u_info(final String title, final ProxyConnection proxyConnection, final String msg) {
        u_log(Level.INFO, title, proxyConnection, msg);
    }

    public static void u_warn(final String title, final ProxyConnection proxyConnection, final String msg) {
        u_log(Level.WARN, title, proxyConnection, msg);
    }

    public static void u_err(final String title, final ProxyConnection proxyConnection, final String msg) {
        u_log(Level.INFO, title, proxyConnection, msg);
    }

    public static void u_log(final Level level, final String title, final ProxyConnection proxyConnection, final String msg) {
        if (proxyConnection == null) {
            u_log(level, title, null, null, msg);
            return;
        }
        final SocketAddress address = proxyConnection.getC2P().remoteAddress();
        final GameProfile gameProfile = proxyConnection.getGameProfile();
        u_log(level, title, address, gameProfile, msg);
    }

    public static void u_log(final Level level, final String title, final SocketAddress address, final GameProfile gameProfile, final String msg) {
        String message = "[" + title.toUpperCase(Locale.ROOT) + "] (" + address + " | " + (gameProfile != null ? gameProfile.getName() : "null") + ") " + msg;
        LOGGER.atLevel(level).log(message);
    }

    public static void debug(Object msg) {
        LOGGER.debug(msg.toString());
    }

    public static void info(Object msg) {
        LOGGER.info(msg.toString());
    }

    public static void warn(Object msg) {
        LOGGER.warn(msg.toString());
    }

    public static void error(Object msg) {
        LOGGER.error(msg.toString());
    }

    public static void error(Object msg, Throwable t) {
        LOGGER.error(msg.toString(), t);
    }
}
