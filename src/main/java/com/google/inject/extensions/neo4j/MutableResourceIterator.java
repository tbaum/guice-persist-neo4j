package com.google.inject.extensions.neo4j;

import org.neo4j.graphdb.ResourceIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author tbaum
 * @since 17.04.2014
 */
public interface MutableResourceIterator<T> extends ResourceIterator<T> {

    default List<T> asList() {
        ArrayList<T> list = new ArrayList<>();
        forEachRemaining(list::add);
        return list;
    }

    static <T, R> MutableResourceIterator<R> convert(ResourceIterator<T> from, Function<T, R> converter) {
        return new MutableResourceIterator<R>() {
            @Override public boolean hasNext() {
                return from.hasNext();
            }

            @Override public R next() {
                return converter.apply(from.next());
            }

            @Override public void remove() {
                from.remove();
            }

            @Override public void close() {
                from.close();
            }
        };
    }

    default <R> MutableResourceIterator<R> convert(Function<T, R> converter) {
        return convert(this, converter);
    }
}
