package com.google.inject.extensions.neo4j;

import com.google.inject.Injector;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Runtime.getRuntime;

public class EmbeddedNeo4JPersistenceModule extends Neo4JPersistenceModule {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedNeo4JPersistenceModule.class);
    private final String dbLocation;

    public EmbeddedNeo4JPersistenceModule(String dbLocation) {
        this.dbLocation = dbLocation;
    }

    public static void handleShutdown(Injector injector) {
        try {
            GraphDatabaseService gds = injector.getInstance(GraphDatabaseService.class);
            gds.shutdown();
        } catch (Exception e) {
            LOG.warn("unable to shutdown GraphDatabaseService", e);
        }
    }

    @Override protected GraphDatabaseService createGraphDatabase() {

        final GraphDatabaseService gdb = new GraphDatabaseFactory().newEmbeddedDatabase(dbLocation);

        getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                gdb.shutdown();
            }
        });

        return gdb;
    }
}

