package com.google.inject.extensions.neo4j.handler;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

import javax.inject.Provider;
import java.util.List;

import static java.util.Arrays.asList;
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

    public FulltextIndexTransactionEventHandler(Label label, Provider<GraphDatabaseService> gds, String name,
                                                String key, String... properties) {
        super(label);
        this.gds = gds;
        this.name = name;
        this.key = key;
        this.properties = asList(properties);
    }

    public static NodeByLabelTransactionEventHandler fulltext(Label label, Provider<GraphDatabaseService> gds,
                                                              String name, String key, String... properties) {
        return new FulltextIndexTransactionEventHandler(label, gds, name, key, properties);
    }

    protected Index<Node> index() {
        return gds.get().index().forNodes(name, stringMap(PROVIDER, "lucene", "type", "fulltext"));
    }

    @Override protected void onUpdate(Node node) {
        final Index<Node> index = index();
        index.remove(node);
        properties.stream()
                .map(name -> node.getProperty(name, null))
                .filter(value -> value != null)
                .forEach(value -> index.add(node, key, value));
    }

    @Override protected void onDelete(Node node) {
        index().remove(node);
    }

}
