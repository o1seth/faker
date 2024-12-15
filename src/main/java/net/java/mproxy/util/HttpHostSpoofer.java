package net.java.mproxy.util;

import net.java.mproxy.proxy.util.ExceptionUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpHostSpoofer extends Thread {

    private final String fromHost;
    private final String toHost;
    private final InetAddress connectAddress;
    ServerSocket server;

    public HttpHostSpoofer(InetAddress connectAddress, String fromHost, String toHost) {
        super(null, null, "HttpHostSpoofer", 96 * 1024);
        this.connectAddress = connectAddress;
        this.fromHost = fromHost;
        this.toHost = toHost;

    }

    @Override
    public synchronized void start() {
        try {
            server = new ServerSocket(80);
        } catch (IOException e) {
            ExceptionUtil.throwException(e);
        }
        super.start();
    }

    @Override
    public void interrupt() {
        try {
            this.server.close();
        } catch (Throwable ignored) {

        }
        super.interrupt();
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                try {
                    Socket accepted = server.accept();
                    Socket socket = new Socket(connectAddress, 80);
                    accepted.setSoTimeout(30_000);
                    socket.setSoTimeout(30_000);
                    new RedirectThread(accepted, socket, "Host: " + fromHost, "Host: " + toHost).start();
                    new RedirectThread(socket, accepted, null, null).start();
                } catch (ConnectException e) {

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class RedirectThread extends Thread {
        final Socket from;
        final Socket to;
        final String fromHost;
        final String toHost;

        RedirectThread(Socket from, Socket to, String fromHost, String toHost) {
            super(null, null, "RedirectThread " + from + "->" + to, 96 * 1024);
            this.from = from;
            this.to = to;
            this.fromHost = fromHost;
            this.toHost = toHost;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[8192];
                int n;
                InputStream in = from.getInputStream();
                OutputStream out = to.getOutputStream();

                while ((n = in.read(buffer)) >= 0) {
//                    System.out.write(buffer,0,n);
//                    System.out.flush();
                    if (fromHost != null && toHost != null) {
                        byte[] newBuffer = replace(buffer, n, fromHost, toHost);
                        if (newBuffer != buffer) {
                            out.write(newBuffer);
                            continue;
                        }
                    }
                    out.write(buffer, 0, n);
                }
            } catch (IOException e) {
                try {
                    from.close();
                } catch (Exception ignored) {

                }
                try {
                    to.close();
                } catch (Exception ignored) {

                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static int findArray(byte[] largeArray, int largeLen, byte[] subArray) {
        if (subArray.length == 0) {
            return -1;
        }
        int limit = largeLen - subArray.length;
        next:
        for (int i = 0; i <= limit; i++) {
            for (int j = 0; j < subArray.length; j++) {
                if (subArray[j] != largeArray[i + j]) {
                    continue next;
                }
            }
            /* Sub array found - return its index */
            return i;
        }
        /* Return default value */
        return -1;
    }


    public static byte[] replace(byte[] dest, int len, String from, String to) {
        byte[] fromBytes = from.getBytes();
        byte[] toBytes = to.getBytes();
        if (fromBytes.length == toBytes.length) {
            do {
                int index = findArray(dest, len, fromBytes);
                if (index < 0) {
                    return dest;
                }
                for (int i = 0; i < toBytes.length; i++) {
                    dest[index + i] = toBytes[i];
                }
            } while (true);
        }
        do {
            int index = findArray(dest, len, fromBytes);
            if (index < 0) {
                return dest;
            }
            byte[] newBytes = new byte[len - fromBytes.length + toBytes.length];
            int i = 0;
            System.arraycopy(dest, 0, newBytes, 0, index);
            i += index;
            System.arraycopy(toBytes, 0, newBytes, i, toBytes.length);
            i += toBytes.length;
            System.arraycopy(dest, index + fromBytes.length, newBytes, i, len - (index + fromBytes.length));
            dest = newBytes;
            len = dest.length;
        } while (true);
    }
}


