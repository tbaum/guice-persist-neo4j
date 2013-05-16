package com.google.inject.extensions.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

public class ImpermanentNeo4JPersistenceModule extends Neo4JPersistenceModule {
    @Override protected GraphDatabaseService createGraphDatabase() {
        return new TestGraphDatabaseFactory().newImpermanentDatabase();
    }
}
