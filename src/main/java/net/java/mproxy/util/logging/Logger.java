package net.java.mproxy.util.logging;

import com.mojang.authlib.GameProfile;
import net.java.mproxy.proxy.session.ProxyConnection;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;


import java.io.PrintStream;
import java.net.SocketAddress;
import java.util.Locale;

public class Logger {

    public static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger("Proxy");

    public static final PrintStream SYSOUT = System.out;
    public static final PrintStream SYSERR = System.err;
    private static LoggerPrintStream out = new LoggerPrintStream("STDOUT", SYSOUT);
    private static LoggerPrintStream err = new LoggerPrintStream("STDERR", SYSERR);

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
        LOGGER.log(level, "[" + title.toUpperCase(Locale.ROOT) + "] (" + address + " | " + (gameProfile != null ? gameProfile.getName() : "null") + ") " + msg);
    }

}
