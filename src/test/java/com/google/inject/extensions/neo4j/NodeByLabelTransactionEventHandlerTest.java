package com.google.inject.extensions.neo4j;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.extensions.neo4j.util.CypherExportService;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.event.TransactionEventHandler;

import javax.inject.Provider;
import java.util.Collection;

import static com.google.inject.Guice.createInjector;
import static com.google.inject.extensions.neo4j.handler.FulltextIndexTransactionEventHandler.fulltext;
import static com.google.inject.util.Modules.override;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.neo4j.graphdb.DynamicLabel.label;
import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author tbaum
 * @since 15.04.2014
 */
public class NodeByLabelTransactionEventHandlerTest {
    private ExecutionEngine cypher;

    @Before public void setup() {
        Injector injector = createInjector(
                override(new ImpermanentNeo4JPersistenceModule()).with(new AbstractModule() {
                    @Override protected void configure() {
                    }

                    @Provides Collection<TransactionEventHandler> transactionEventHandler(Provider<GraphDatabaseService> gds) {
                        return asList(fulltext(label("Crew"), gds, "ft", "text", "name", "addon"));
                    }
                }),
                new AbstractModule() {
                    @Override protected void configure() {
                        requestStaticInjection(CypherExportService.class);
                    }
                }
        );
        cypher = injector.getInstance(ExecutionEngine.class);
        cypher.execute("CYPHER 2.0 START n=node(*) OPTIONAL MATCH n-[o]-() DELETE o,n");
    }

    @Test
    public void testExport() {
        cypher.execute("CYPHER 2.0 CREATE " +
                        "(:Crew { name: 'Trinity' }),(:Crew { name:'Neo' })," +
                        "(:Matrix { name: 'Cypher' }),(:Matrix {name:'Neo1'}), (:Matrix {name:'Neo2'})"
        );

        cypher.execute("CYPHER 2.0 MATCH (n:Crew {name:'Trinity'}) SET n.addon='favorite'");
        cypher.execute("CYPHER 2.0 MATCH (n:Crew {name:'Trinity'}) SET n.name='trinity'");
        cypher.execute("CYPHER 2.0 MATCH (n:Crew {name:'Neo'}) OPTIONAL MATCH n-[r]-() DELETE r,n");

        //workarround for adding labels
        cypher.execute("CYPHER 2.0 MATCH (n:Matrix {name:'Neo1'}) set n:Crew SET n._trigger_update=true REMOVE n._trigger_update");
        cypher.execute("CYPHER 2.0 MATCH (n:Matrix {name:'Neo2'}) SET n:Crew");

        cypher.execute("CYPHER 2.0 MATCH (n:Matrix {name:'Cypher'}) SET n.name='cypher' ");


        assertPresent("Cypher", 0);
        assertPresent("cypher", 0);
        assertPresent("Neo", 0);
        assertPresent("Neo1", 1);
        // TODO assertPresent("Neo2", 1);
        assertPresent("trinity", 1);
        assertPresent("favorite", 1);
        assertPresent("Trinity", 0);
    }

    private void assertPresent(String key, long expectedCount) {
        ExecutionResult result = cypher.execute("CYPHER 2.0 START n=node:ft(text={p}) RETURN count(*)", map("p", key));
        assertEquals("index for " + key, expectedCount, result.columnAs("count(*)").next());
    }
}
