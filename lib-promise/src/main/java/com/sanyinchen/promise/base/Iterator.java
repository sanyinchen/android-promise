package com.sanyinchen.promise.base;

public interface Iterator<T> {
    boolean hasNext();

    boolean hasPre();

    T next();

    T pre();
}
