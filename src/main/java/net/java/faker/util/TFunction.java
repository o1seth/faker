package net.java.faker.util;

@FunctionalInterface
public interface TFunction<T, R> {

    R apply(T t) throws Throwable;

}
