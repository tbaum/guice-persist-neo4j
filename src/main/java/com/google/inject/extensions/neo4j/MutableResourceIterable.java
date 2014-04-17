package com.google.inject.extensions.neo4j;

import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author tbaum
 * @since 17.04.2014
 */
public interface MutableResourceIterable<T> extends ResourceIterable<T> {

    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    default List<T> asList() {
        return stream().collect(Collectors.toList());
    }

    <T> ResourceIterator<T> columnAs(String n);
}
