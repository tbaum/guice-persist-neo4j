package com.google.inject.extensions.neo4j;

import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author tbaum
 * @since 17.04.2014
 */
public interface MutableResourceIterable<T> extends ResourceIterable<T> {

    default Stream<T> stream() {
        return asList().stream();
    }

    default List<T> asList() {
        List<T> list = new ArrayList<>();
        try (ResourceIterator<T> t = iterator()) {
            t.forEachRemaining(list::add);
        }
        return list;
    }

    <C> ResourceIterator<C> columnAs(String n);
}
