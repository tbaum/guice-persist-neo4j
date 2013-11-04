package com.google.inject.extensions.neo4j;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.currentTimeMillis;

/**
 * @author tbaum
 * @since 04.10.2013
 */
@Singleton
public class GuicedExecutionEngine extends ExecutionEngine {

    private static final Logger LOG = LoggerFactory.getLogger(GuicedExecutionEngine.class);

    @Inject
    public GuicedExecutionEngine(GraphDatabaseService gds) {
        super(gds);
    }

    @Override public ExecutionResult execute(String query) {
        return execute(query, new HashMap<String, Object>());
    }

    @Transactional @Override
    public ExecutionResult execute(String query, Map<String, Object> parameters) {
        LOG.debug("Execute: '{}' params:{}", query, parameters);
        long start = currentTimeMillis();
        try {
            return super.execute(query, parameters);
        } finally {
            long time = currentTimeMillis() - start;
            if (time > 50) {
                LOG.warn("cypherstatement took {}ms query:'{}' params:{}", time, query, parameters);
            }
        }
    }
}
