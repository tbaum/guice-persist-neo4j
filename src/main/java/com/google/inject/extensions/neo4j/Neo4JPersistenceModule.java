package com.google.inject.extensions.neo4j;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.kernel.EmbeddedReadOnlyGraphDatabase;

import java.util.Collection;

import static com.google.inject.extensions.neo4j.TransactionScope.TRANSACTIONAL;
import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;
import static java.lang.Runtime.getRuntime;
import static java.util.Arrays.asList;

/**
 * @author tbaum
 * @since 20.02.12
 */
public abstract class Neo4JPersistenceModule extends AbstractModule {

    @Override protected void configure() {
        TransactionInterceptor tx = new TransactionInterceptor();
        requestInjection(tx);
        bindInterceptor(annotatedWith(Transactional.class), any(), tx);
        bindInterceptor(any(), annotatedWith(Transactional.class), tx);
        bind(TransactionInterceptor.class).toInstance(tx);

        bindScope(Transactional.class, TRANSACTIONAL);
        bind(TransactionScope.class).toInstance(TRANSACTIONAL);
        bind(Transaction.class).toProvider(TransactionScope.transactionProvider()).in(TRANSACTIONAL);
    }

    @Provides @Singleton GraphDatabaseService getGraphDatabaseService(Collection<TransactionEventHandler> handlers) {
        final GraphDatabaseService graphDatabase = createGraphDatabase();

        getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                graphDatabase.shutdown();
            }
        });
        handlers.forEach(graphDatabase::registerTransactionEventHandler);
        if (!(graphDatabase instanceof EmbeddedReadOnlyGraphDatabase)) {
            graphDatabase.registerTransactionEventHandler(new PatchIndexeTransactionEventHandler(graphDatabase));
        }
        return graphDatabase;
    }

    @Provides @Singleton Collection<TransactionEventHandler> defaultTransactionEventHandler() {
        return asList();
    }

    protected abstract GraphDatabaseService createGraphDatabase();

}
