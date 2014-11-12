package com.google.inject.extensions.neo4j;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static java.lang.System.currentTimeMillis;
import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author tbaum
 * @since 04.10.2013
 */
@Singleton
public class GuicedExecutionEngine {

    private static final Logger LOG = LoggerFactory.getLogger(GuicedExecutionEngine.class);
    private final ExecutionEngine delegate;

    @Inject
    public GuicedExecutionEngine(GraphDatabaseService gds) {
        delegate = new ExecutionEngine(gds);
    }

    @Transactional
    public MutableResourceIterable<ResultMap> execute(String query) {
        return execute(query, (r) -> r);
    }

    public MutableResourceIterable<ResultMap> execute(String query, Map<String, Object> params) {
        return execute(query, params, (r) -> r);
    }

    public <T> MutableResourceIterable<T> execute(String query, Function<ResultMap, T> converter) {
        return execute(query, new HashMap<>(), converter);
    }

    public <T> MutableResourceIterable<T> execute(String query, Map<String, Object> params,
                                                  Function<ResultMap, T> converter) {
        Function<Map<String, Object>, T> compose = converter.compose(ResultMap::new);
        ExecutionResult maps = executeInternal(query, params);

        return new MutableResourceIterable<T>() {
            private final AtomicBoolean consumed = new AtomicBoolean(false);

            private ExecutionResult _result() {
                return (consumed.getAndSet(true) ? executeInternal(query, params) : maps);
            }

            @Override public <E> ResourceIterator<E> columnAs(String n) {
                return _result().columnAs(n);
            }


            @Override public ResourceIterator<T> iterator() {
                return MutableResourceIterator.convert(_result().iterator(), compose);
            }
        };
    }

    public <T> T singleResult(String query, Map<String, Object> params,
                              Function<ResultMap, T> converter) {
        try (ResourceIterator<T> it = execute(query, params, converter).iterator()) {
            if (it.hasNext()) {
                T result = it.next();
                if (it.hasNext()) {
                    LOG.error("more than one result found!, query:{}, params:{}", query, params);
                    LOG.debug("results {}", executeInternal(query, params).toString());
                    throw new IllegalStateException("more than one result found!");
                }
                return result;
            }
            return converter.apply(new ResultMap(map()));
        }
    }

    public <T> T singleResult(String query, Function<ResultMap, T> converter) {
        return singleResult(query, new HashMap<>(), converter);
    }

    public ResultMap singleResult(String query) {
        return singleResult(query, new HashMap<>(), (r) -> r);
    }

    public ExecutionResult executeInternal(String query, Map<String, Object> parameters) {
        LOG.debug("Execute: '{}' params:{}", query, parameters);
        long start = currentTimeMillis();
        try {
            return delegate.execute(query, parameters);
        } finally {
            long time = currentTimeMillis() - start;
            if (time > 50) {
                LOG.warn("cypherstatement took {}ms query:'{}' params:{}", time, query, parameters);
            }
        }
    }

    public <T> ListResult<T> asListPresentation(String whereQuery, int start, int limit,
                                                Function<ResultMap, T> converter,
                                                String countColumn, String returnColumns,
                                                Map<String, Object> parameter) {

        String countQuery = whereQuery + " RETURN count(" + countColumn + ") as count";
        String listQuery = whereQuery + " " + returnColumns;
        parameter = new HashMap<>(parameter);

        if (start > 0) {
            listQuery += " skip {___skip}";
            parameter.put("___skip", start);
        }
        if (limit != Integer.MAX_VALUE) {
            listQuery += " limit {___limit}";
            parameter.put("___limit", limit);
        }

        return new ListResult<>(
                execute(listQuery, parameter, converter).asList(),
                singleResult(countQuery, parameter, (r) -> r.<Number>get("count")).intValue()
        );
    }
}
