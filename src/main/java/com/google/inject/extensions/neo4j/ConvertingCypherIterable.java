package com.google.inject.extensions.neo4j;

import ch.lambdaj.function.convert.Converter;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.neo4j.helpers.collection.MapUtil.map;

public abstract class ConvertingCypherIterable<T> implements ResourceIterable<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertingCypherIterable.class);
    private final ExecutionEngine cypher;
    private final String query;
    private final Map<String, Object> params;

    private ConvertingCypherIterable(ExecutionEngine cypher, String query, Map<String, Object> params) {
        this.cypher = cypher;
        this.query = query;
        this.params = params;
    }

    public static <T> ConvertingCypherIterable<T> convertingCypherIterable(ExecutionEngine cypher, String query,
                                                                           Converter<ResultMap, T> converter) {
        return convertingCypherIterable(cypher, query, map(), converter);
    }

    public static <T> ConvertingCypherIterable<T> convertingCypherIterable(ExecutionEngine cypher,
                                                                           String query, Map<String, Object> params,
                                                                           final Converter<ResultMap, T> converter) {
        return new ConvertingCypherIterable<T>(cypher, query, params) {
            @Override protected T convert(ResultMap from) {
                return converter.convert(from);
            }
        };
    }

    public static <R> R singleCypherResult(ExecutionEngine cypher, String query, Map<String, Object> params,
                                           Converter<ResultMap, R> converter) {
        try (ResourceIterator<R> it = convertingCypherIterable(cypher, query, params, converter).iterator()) {
            if (it.hasNext()) {
                R result = it.next();
                if (it.hasNext()) {
                    LOG.error("more than one result found!, query:{}, params:{}", query, params);
                    LOG.debug("results {}", cypher.execute(query, params).toString());
                    throw new IllegalStateException("more than one result found!");
                }
                return result;
            }
            return converter.convert(new ResultMap(map()));
        }
    }

    @Override public ResourceIterator<T> iterator() {
        final ResourceIterator<ResultMap> iterator = toIterator();
        return new ResourceIterator<T>() {
            @Override public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override public T next() {
                return convert(iterator.next());
            }

            @Override public void remove() {
                iterator.remove();
            }

            @Override public void close() {
                iterator.close();
            }
        };
    }

    protected abstract T convert(ResultMap from);

    protected ResourceIterator<ResultMap> toIterator() {
        return new ResultMapIterator(cypher.execute(query, params));
    }

    public static class ResultMap {
        private final Map<String, Object> map;

        public ResultMap(final Map<String, Object> next) {
            this.map = new HashMap<>(next);
        }

        public static ResultMap empty() {
            return new ResultMap(Collections.<String, Object>emptyMap());
        }

        @SuppressWarnings("unchecked") public <T> T get(String o) {
            return (T) map.get(o);
        }

        public boolean contains(String o) {
            return map.get(o) != null;
        }

        @Override public String toString() {
            return map.toString();
        }
    }

    public static class ResultMapIterator implements ResourceIterator<ResultMap> {
        private final ResourceIterator<Map<String, Object>> iterator;

        public ResultMapIterator(ResourceIterable<Map<String, Object>> iterable) {
            this.iterator = iterable.iterator();
        }

        public ResultMapIterator(ResourceIterator<Map<String, Object>> iterator) {
            this.iterator = iterator;
        }

        @Override public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override public ResultMap next() {
            return new ResultMap(iterator.next());

        }

        @Override public void remove() {
            iterator.remove();
        }

        @Override public void close() {
            iterator.close();
        }
    }
}
