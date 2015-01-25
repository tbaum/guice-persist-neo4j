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
public class FulltextIndexTransactionEventHandler {

    public static NodeByLabelTransactionEventHandler fulltext(Provider<GraphDatabaseService> gds, Label label,
                                                              String indexName, String indexKey, String... properties) {
        return new NodeByLabelTransactionEventHandler(label, (updated, deleted) -> {
            Index<Node> index = gds.get().index().forNodes(indexName, stringMap(PROVIDER, "lucene", "type", "fulltext"));
            deleted.forEach(index::remove);
            updated.forEach(index::remove);
            List<String> props = asList(properties);
            updated.forEach((node) ->
                    props.stream().filter(node::hasProperty).map(node::getProperty)
                            .forEach(value -> index.add(node, indexKey, value)));
        });
    }
}
