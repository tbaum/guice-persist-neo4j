package com.google.inject.extensions.neo4j;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.neo4j.cypher.SyntaxException;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.EmbeddedReadOnlyGraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.currentTimeMillis;

/**
 * @author tbaum
 * @since 20.02.12
 */
public abstract class Neo4JPersistenceModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(Neo4JPersistenceModule.class);

    @Override protected void configure() {
        LocalTxnInterceptor tx = new LocalTxnInterceptor();
        requestInjection(tx);
        bindInterceptor(annotatedWith(Transactional.class), any(), tx);
        bindInterceptor(any(), annotatedWith(Transactional.class), tx);
    }

    @Provides @Singleton ExecutionEngine getExecutionEngine(GraphDatabaseService gds) {
        final Pattern limit = Pattern.compile("(.+LIMIT )(\\d+)(.*)");
        final Pattern skip = Pattern.compile("(.+SKIP )(\\d+)(.*)");

        return new ExecutionEngine(gds) {
            @Override public ExecutionResult execute(String query) throws SyntaxException {
                return execute(query, new HashMap<String, Object>());
            }

            @Override public ExecutionResult execute(String query, Map<String, Object> parameters) {
//                query = query.replaceAll(" RELATE ", " CREATE UNIQUE ");

                parameters = new HashMap<String, Object>(parameters);
                Matcher maSkip = skip.matcher(query);
                if (maSkip.matches()) {
                    parameters.put("__skip", Integer.valueOf(maSkip.group(2)));
                    query = maSkip.group(1) + " {__skip} " + maSkip.group(3);
                }

                Matcher maLimit = limit.matcher(query);
                if (maLimit.matches()) {
                    parameters.put("__limit", Integer.valueOf(maLimit.group(2)));
                    query = maLimit.group(1) + " {__limit} " + maLimit.group(3);
                }

//                System.err.println("CYPHER: " + query);
//                System.err.println("PARAMS: " + p);
                long start = currentTimeMillis();
                try {
                    return super.execute(query, parameters);
                } finally {
                    long time = currentTimeMillis() - start;
                    if (time > 50) {
                        LOG.warn("cypherstatement took {}ms query:{}, params:{}", time, query, parameters);
                    }
                }
            }
        };
    }

    @Provides @Singleton public GraphDatabaseService getGraphDatabaseService() {
        final GraphDatabaseService graphDatabase = createGraphDatabase();

        getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                graphDatabase.shutdown();
            }
        });

        if (!(graphDatabase instanceof EmbeddedReadOnlyGraphDatabase)) {
            graphDatabase.registerTransactionEventHandler(new TransactionEventHandler<Object>() {
                @Override public Object beforeCommit(TransactionData data) throws Exception {
                    IndexManager index = graphDatabase.index();
                    for (Node node : data.deletedNodes()) {
                        for (String s : index.nodeIndexNames()) {
                            index.forNodes(s).remove(node);
                        }
                    }
                    for (Relationship relationship : data.deletedRelationships()) {
                        for (String s : index.relationshipIndexNames()) {
                            index.forRelationships(s).remove(relationship);
                        }
                    }
                    return null;
                }

                @Override public void afterCommit(TransactionData data, Object state) {
                }

                @Override public void afterRollback(TransactionData data, Object state) {
                }
            });
        }

        return graphDatabase;
    }

    protected abstract GraphDatabaseService createGraphDatabase();
}
