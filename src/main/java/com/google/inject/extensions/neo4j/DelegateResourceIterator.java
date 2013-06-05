package com.google.inject.extensions.neo4j;

import java.util.Iterator;

import static java.util.Arrays.asList;

//public class DelegateResourceIterator<T> implements ResourceIterator<T> {
public class DelegateResourceIterator<T> implements Iterator<T> {

    private final Iterator<T> deleage;

    public DelegateResourceIterator(Iterator<T> deleage) {
        this.deleage = deleage;
    }

    public DelegateResourceIterator(Iterable<T> deleage) {
        this.deleage = deleage.iterator();
    }

    public static <T> DelegateResourceIterator<T> iterator(T element) {
        return new DelegateResourceIterator<T>(asList(element));
    }

    public static <T> DelegateResourceIterator<T> iterator(T... element) {
        return new DelegateResourceIterator<T>(asList(element));
    }

//    @Override public void close() {
//    }

    @Override public boolean hasNext() {
        return deleage.hasNext();
    }

    @Override public T next() {
        return deleage.next();
    }

    @Override public void remove() {
        deleage.remove();
    }
}
