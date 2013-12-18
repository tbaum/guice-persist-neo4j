package com.google.inject.extensions.neo4j;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.Node;

import java.util.Iterator;

import static com.google.inject.Guice.createInjector;
import static com.google.inject.extensions.neo4j.ConvertingCypherIterable.convertingCypherIterable;
import static com.google.inject.extensions.neo4j.ConvertingCypherIterable.singleCypherResult;
import static org.junit.Assert.*;
import static org.neo4j.helpers.collection.MapUtil.map;

public class ConvertingCypherIterableTest {

    private ExecutionEngine cypher;

    @Before public void setup() {
        Injector injector = createInjector(new ImpermanentNeo4JPersistenceModule());
        cypher = injector.getInstance(ExecutionEngine.class);
        cypher.execute("START n=node(0) CREATE (n)-[:REL]->(n1 {name:'n1'}),(n)-[:REL]->(n2 {name:'n2'})");

    }

    @Test
    public void testNoResult() {
        Iterable<Node> r = convertingCypherIterable(cypher, "START n=node(0) MATCH n-[:NOTHING]->r RETURN r",
                ResultMapConverter.<Node>key("r"));

        Iterator<Node> iterator = r.iterator();

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testMultipleResults() {
        Iterable<Node> r = convertingCypherIterable(cypher, "START n=node(0) MATCH n-[:REL]->r RETURN r",
                ResultMapConverter.<Node>key("r"));

        Iterator<Node> iterator = r.iterator();

        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testSingleResult() {
        Iterable<Node> r = convertingCypherIterable(cypher, "START n=node(0) MATCH n-[:REL]->r WHERE r.name='n1' RETURN r",
                ResultMapConverter.<Node>key("r"));

        Iterator<Node> iterator = r.iterator();

        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        assertFalse(iterator.hasNext());
    }


    @Test
    public void testSingleResult1() {
        Node r = singleCypherResult(cypher, "START n=node(0) MATCH n-[:REL]->r WHERE r.name='n1' RETURN r",
                map(), ResultMapConverter.<Node>key("r"));

        assertNotNull(r);
    }

    @Test
    public void testSingleResult2() {
        Node r = singleCypherResult(cypher, "START n=node(0) MATCH n-[:NOTHING]->r RETURN r",
                map(), ResultMapConverter.<Node>key("r"));
        assertNull(r);
    }


}
