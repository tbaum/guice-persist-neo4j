package com.google.inject.extensions.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.neo4j.kernel.configuration.Settings.BOOLEAN;
import static org.neo4j.kernel.configuration.Settings.setting;

public class ImpermanentNeo4JPersistenceModule extends Neo4JPersistenceModule {
    @Override protected GraphDatabaseService createGraphDatabase() {
        GraphDatabaseBuilder builder = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder();
        builder.setConfig(setting("online_backup_enabled", BOOLEAN, ""), "false");
        return builder.newGraphDatabase();
    }
}
