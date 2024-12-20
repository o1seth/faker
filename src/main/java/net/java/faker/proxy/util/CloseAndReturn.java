package net.java.faker.proxy.util;

public class CloseAndReturn extends RuntimeException {

    public static final CloseAndReturn INSTANCE = new CloseAndReturn();

    private CloseAndReturn() {
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
