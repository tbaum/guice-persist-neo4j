package com.google.inject.extensions.neo4j;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.extensions.neo4j.util.CypherExportService;
import org.junit.Before;
import org.junit.Test;

import static com.google.inject.Guice.createInjector;

public class CypherExportTest {

    private GuicedExecutionEngine cypher;

    @Before public void setup() {
        Injector injector = createInjector(new ImpermanentNeo4JPersistenceModule(), new AbstractModule() {
            @Override protected void configure() {
                requestStaticInjection(CypherExportService.class);
            }
        });
        cypher = injector.getInstance(GuicedExecutionEngine.class);
        cypher.execute("MATCH (n) OPTIONAL MATCH n-[o]-() DELETE o,n");
    }

    @Test
    public void testExport() {
        cypher.execute("CREATE INDEX ON :Matrix(name)");
        cypher.execute("CREATE CONSTRAINT ON (i:Crew) ASSERT i.name IS UNIQUE");
        cypher.execute("CREATE (:Crew { name: 'Trinity' })<-[:LOVES]-(:Crew { name:'Neo' })-[:KNOWS {since: 1990}]->" +
                        "(:Crew { name: 'Morpheus' }),(:Crew:Matrix { name: 'Cypher' })");

        CypherExportService.assertGraphConstraintsEquals("CREATE CONSTRAINT ON (c:Crew) ASSERT c.name IS UNIQUE;\n" +
                "CREATE INDEX ON :Matrix(name);\n");

        CypherExportService.assertGraphEquals("create \n" +
                "(Crew_1:Crew  {name:\"Neo\"}),\n" +
                "(Crew_2:Crew  {name:\"Trinity\"}),\n" +
                "(Crew_3:Crew  {name:\"Morpheus\"}),\n" +
                "(Matrix_1:Matrix:Crew  {name:\"Cypher\"}),\n" +
                "(Crew_1)-[:KNOWS {since:1990}]->(Crew_3),\n" +
                "(Crew_1)-[:LOVES]->(Crew_2)");
        System.err.println(CypherExportService.export());
    }
}
