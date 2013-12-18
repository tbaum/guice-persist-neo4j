package com.google.inject.extensions.neo4j;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;

import java.util.Iterator;

import static com.google.inject.Guice.createInjector;
import static com.google.inject.extensions.neo4j.ConvertingCypherIterable.convertingCypherIterable;
import static com.google.inject.extensions.neo4j.ConvertingCypherIterable.singleCypherResult;
import static org.junit.Assert.*;
import static org.neo4j.helpers.collection.MapUtil.map;

public class ConvertingCypherIterableTest {

    private ExecutionEngine cypher;
    private Node ref;

    @Before public void setup() {
        Injector injector = createInjector(new ImpermanentNeo4JPersistenceModule());
        cypher = injector.getInstance(ExecutionEngine.class);
        try (ResourceIterator<Node> n = cypher.execute(
                "CREATE (n)-[:REL]->(n1 {name:'n1'}),(n)-[:REL]->(n2 {name:'n2'}) RETURN n").columnAs("n")) {
            ref = n.next();
        }
        System.err.println(ref);
    }

    @Test
    public void testNoResult() {
        Iterable<Node> r = convertingCypherIterable(cypher, "START n=node({ref}) MATCH n-[:NOTHING]->r RETURN r",
                map("ref", ref), ResultMapConverter.<Node>key("r"));

        Iterator<Node> iterator = r.iterator();

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testMultipleResults() {
        Iterable<Node> r = convertingCypherIterable(cypher, "START n=node({ref}) MATCH n-[:REL]->r RETURN r",
                map("ref", ref), ResultMapConverter.<Node>key("r"));

        Iterator<Node> iterator = r.iterator();

        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testSingleResult() {
        Iterable<Node> r = convertingCypherIterable(cypher, "START n=node({ref}) MATCH n-[:REL]->r WHERE r.name='n1' RETURN r",
                map("ref", ref), ResultMapConverter.<Node>key("r"));

        Iterator<Node> iterator = r.iterator();

        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        assertFalse(iterator.hasNext());
    }


    @Test
    public void testSingleResult1() {
        Node r = singleCypherResult(cypher, "START n=node({ref}) MATCH n-[:REL]->r WHERE r.name='n1' RETURN r",
                map("ref", ref), ResultMapConverter.<Node>key("r"));

        assertNotNull(r);
    }

    @Test
    public void testSingleResult2() {
        Node r = singleCypherResult(cypher, "START n=node({ref}) MATCH n-[:NOTHING]->r RETURN r",
                map("ref", ref), ResultMapConverter.<Node>key("r"));
        assertNull(r);
    }


}
