package com.google.inject.extensions.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.neo4j.kernel.configuration.Settings.BOOLEAN;
import static org.neo4j.kernel.configuration.Settings.setting;

public class ImpermanentNeo4JPersistenceModule extends Neo4JPersistenceModule {
    @Override protected GraphDatabaseService createGraphDatabase() {
        return new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                .setConfig(setting("online_backup_enabled", BOOLEAN, ""), "false")
                .newGraphDatabase();
    }
}
