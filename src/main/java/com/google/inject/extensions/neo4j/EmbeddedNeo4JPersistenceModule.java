package com.google.inject.extensions.neo4j;

import com.google.inject.Injector;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class EmbeddedNeo4JPersistenceModule extends Neo4JPersistenceModule {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedNeo4JPersistenceModule.class);
    private final File dbLocation;

    public EmbeddedNeo4JPersistenceModule(File dbLocation) {
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
        return new GraphDatabaseFactory().newEmbeddedDatabase(dbLocation);
    }
}

