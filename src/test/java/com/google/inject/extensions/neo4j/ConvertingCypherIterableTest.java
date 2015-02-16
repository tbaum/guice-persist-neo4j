package com.google.inject.extensions.neo4j;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;

import java.util.Iterator;

import static com.google.inject.Guice.createInjector;
import static org.junit.Assert.*;
import static org.neo4j.helpers.collection.MapUtil.map;

public class ConvertingCypherIterableTest {

    private GuicedExecutionEngine cypher;
    private Node ref;

    @Before public void setup() {
        Injector injector = createInjector(new ImpermanentNeo4JPersistenceModule());
        cypher = injector.getInstance(GuicedExecutionEngine.class);
        try (ResourceIterator<Node> n = cypher.execute(
                "CYPHER 2.2 CREATE (n)-[:REL]->(n1 {name:'n1'}),(n)-[:REL]->(n2 {name:'n2'}) RETURN n").columnAs("n")) {
            ref = n.next();
        }
        System.err.println(ref);
    }

    @Test
    public void testNoResult() {
        Iterable<Node> r = cypher.execute("CYPHER 2.2 MATCH n WHERE n={ref} MATCH n-[:NOTHING]->r RETURN r", map("ref", ref),
                (f) -> f.get("r"));

        Iterator<Node> iterator = r.iterator();

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testMultipleResults() {
        Iterable<Node> r = cypher.execute("CYPHER 2.2 MATCH n WHERE n={ref} MATCH n-[:REL]->r RETURN r", map("ref", ref),
                (f) -> f.get("r"));

        Iterator<Node> iterator = r.iterator();

        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testSingleResult() {
        Iterable<Node> r = cypher.execute("CYPHER 2.2 MATCH n WHERE n={ref} MATCH n-[:REL]->r WHERE r.name='n1' RETURN r",
                map("ref", ref), (f) -> f.get("r"));

        Iterator<Node> iterator = r.iterator();

        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testSingleResult1() {
        Node r = cypher.singleResult("CYPHER 2.2 MATCH n WHERE n={ref} MATCH n-[:REL]->r WHERE r.name='n1' RETURN r",
                map("ref", ref), (f) -> f.get("r"));

        assertNotNull(r);
    }

    @Test
    public void testSingleResult2() {
        Node r = cypher.singleResult("CYPHER 2.2 MATCH n WHERE n={ref} MATCH n-[:NOTHING]->r RETURN r", map("ref", ref),
                (f) -> f.get("r"));
        assertNull(r);
    }
}
