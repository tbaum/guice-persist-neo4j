package com.google.inject.extensions.neo4j;

import ch.lambdaj.function.convert.Converter;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypherdsl.Identifier;
import org.neo4j.cypherdsl.query.Query;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.neo4j.cypherdsl.CypherQuery.identifier;
import static org.neo4j.helpers.collection.MapUtil.map;

public abstract class ConvertingCypherIterable<T> implements Iterable<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertingCypherIterable.class);
    private final ExecutionEngine cypher;
    private final String query;
    private final Map<String, Object> params;

    private ConvertingCypherIterable(ExecutionEngine cypher, String query, Map<String, Object> params) {
        this.cypher = cypher;
        this.query = query;
        this.params = params;
    }

    public static <T> ConvertingCypherIterable<T> convertingCypherIterable(ExecutionEngine cypher, Query query,
                                                                           Converter<ResultMap, T> converter) {
        return convertingCypherIterable(cypher, query, map(), converter);
    }

    public static <T> ConvertingCypherIterable<T> convertingCypherIterable(ExecutionEngine cypher, String query,
                                                                           Converter<ResultMap, T> converter) {
        return convertingCypherIterable(cypher, query, map(), converter);
    }

    public static <T> ConvertingCypherIterable<T> convertingCypherIterable(ExecutionEngine cypher,
                                                                           Query query, Map<String, Object> params,
                                                                           Converter<ResultMap, T> converter) {
        return convertingCypherIterable(cypher, query.toString(), params, converter);
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

    public static <R> R singleCypherResult(ExecutionEngine cypher, Query query, Map<String, Object> params,
                                           Converter<ResultMap, R> converter) {
        return singleCypherResult(cypher, query.toString(), params, converter);
    }

    public static <R> R singleCypherResult(ExecutionEngine cypher, String query, Map<String, Object> params,
                                           Converter<ResultMap, R> converter) {
        Iterator<R> it = convertingCypherIterable(cypher, query, params, converter).iterator();
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

    @Override public Iterator<T> iterator() {
        final Iterator<ResultMap> iterator = toIterator();
        return new Iterator<T>() {
            @Override public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override public T next() {
                return convert(iterator.next());
            }

            @Override public void remove() {
                iterator.remove();
            }
        };
    }

    protected abstract T convert(ResultMap from);

    protected Iterator<ResultMap> toIterator() {
        return new ResultMapIterator(cypher.execute(query, params));
    }

    public static class ResultMap {
        private final Map<String, Object> map;

        public ResultMap(final Map<String, Object> next) {
            this.map = new IdentifierObjectAbstractMap(next);
        }

        public static ResultMap empty() {
            return new ResultMap(Collections.<String, Object>emptyMap());
        }

        @SuppressWarnings("unchecked") public <T> T get(Identifier o) {
            return (T) map.get(o.toString());
        }

        public boolean contains(Identifier o) {
            return map.get(o.toString()) != null;
        }

        @Override public String toString() {
            return map.toString();
        }
    }

    private static class IdentifierObjectAbstractMap extends AbstractMap<String, Object> {
        private final Map<String, Object> next;

        public IdentifierObjectAbstractMap(Map<String, Object> next) {
            this.next = next;
        }

        @Override public Set<Entry<String, Object>> entrySet() {
            return new EntryAbstractSet(next.entrySet());
        }
    }

    private static class EntryIterator implements Iterator<Map.Entry<String, Object>> {
        private final Iterator<Map.Entry<String, Object>> iterator;

        public EntryIterator(Iterator<Map.Entry<String, Object>> iterator) {
            this.iterator = iterator;
        }

        @Override public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override public Map.Entry<String, Object> next() {
            return new StringObjectEntry(iterator.next());
        }

        @Override public void remove() {
            iterator.remove();
        }
    }

    private static class EntryAbstractSet extends AbstractSet<Map.Entry<String, Object>> {
        private final Set<Map.Entry<String, Object>> iterable;

        public EntryAbstractSet(Set<Map.Entry<String, Object>> iterable) {
            this.iterable = iterable;
        }

        @Override public Iterator<Map.Entry<String, Object>> iterator() {
            return new EntryIterator(iterable.iterator());
        }

        @Override public int size() {
            return iterable.size();
        }
    }

    private static class StringObjectEntry implements Map.Entry<String, Object> {
        private final Map.Entry<String, Object> next;

        public StringObjectEntry(Map.Entry<String, Object> next) {
            this.next = next;
        }

        @Override public String getKey() {
            return identifier(next.getKey()).toString();
        }

        @Override public Object getValue() {
            return next.getValue();
        }

        @Override public Object setValue(Object o) {
            return next.setValue(o);
        }
    }

    public static class ResultMapIterator implements Iterator<ResultMap> {
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
    }

}
