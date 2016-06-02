package com.google.inject.extensions.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;

import java.util.Collection;

import static org.neo4j.helpers.collection.Iterables.asCollection;

/**
 * @author tbaum
 * @since 04.10.2013
 */
public class PatchIndexeTransactionEventHandler implements TransactionEventHandler<Object> {

    private final GraphDatabaseService graphDatabase;

    public PatchIndexeTransactionEventHandler(GraphDatabaseService graphDatabase) {
        this.graphDatabase = graphDatabase;
    }

    @Override public Object beforeCommit(TransactionData data) throws Exception {
        Collection<Node> deletedNodes = asCollection(data.deletedNodes());
        Collection<Relationship> deletedRels = asCollection(data.deletedRelationships());

        IndexManager index = graphDatabase.index();
        deletedNodes.forEach((node) -> {
            for (String s : index.nodeIndexNames()) {
                index.forNodes(s).remove(node);
            }
        });
        deletedRels.forEach((relationship) -> {
            for (String s : index.relationshipIndexNames()) {
                index.forRelationships(s).remove(relationship);
            }
        });
        return null;
    }

    @Override public void afterCommit(TransactionData data, Object state) {
    }

    @Override public void afterRollback(TransactionData data, Object state) {
    }
}
