package net.java.mproxy.util;

@FunctionalInterface
public interface TFunction<T, R> {

    R apply(T t) throws Throwable;

}
