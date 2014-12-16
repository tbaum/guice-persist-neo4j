package com.google.inject.extensions.neo4j.handler;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

import javax.inject.Provider;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.neo4j.graphdb.index.IndexManager.PROVIDER;
import static org.neo4j.helpers.collection.MapUtil.stringMap;

/**
 * @author tbaum
 * @since 15.04.2014
 */
public class FulltextIndexTransactionEventHandler extends NodeByLabelTransactionEventHandler {

    private final Provider<GraphDatabaseService> gds;
    private final String name;
    private final String key;
    private final List<String> properties;

    private FulltextIndexTransactionEventHandler(Provider<GraphDatabaseService> gds,
                                                 Label label, String name, String key, String... properties) {
        super(label);
        this.gds = gds;
        this.name = name;
        this.key = key;
        this.properties = asList(properties);
    }

    public static NodeByLabelTransactionEventHandler fulltext(Provider<GraphDatabaseService> gds, Label label,
                                                              String name, String key, String... properties) {
        return new FulltextIndexTransactionEventHandler(gds, label, name, key, properties);
    }

    protected Index<Node> index() {
        return gds.get().index().forNodes(name, stringMap(PROVIDER, "lucene", "type", "fulltext"));
    }

    @Override protected void onChange(Collection<Node> updated, Collection<Node> deleted) {
        Map<Node, List<Object>> vals = Stream.concat(
                updated.stream()
                        .map((node) -> new Tuple<>(node,
                                properties.stream()
                                        .map(name -> node.getProperty(name, null))
                                        .filter(value -> value != null)
                                        .collect(toList()))),
                deleted.stream()
                        .map((node) -> new Tuple<>(node, asList())))
                .collect(toMap(t -> t.a, t -> t.b));

        Index<Node> index = index();
        for (Map.Entry<Node, List<Object>> e : vals.entrySet()) {
            Node node = e.getKey();
            index.remove(node);
            e.getValue().forEach(value -> index.add(node, key, value));
        }
    }

    static class Tuple<A, B> {
        final A a;
        final B b;

        public Tuple(A a, B b) {
            this.a = a;
            this.b = b;
        }
    }

}
