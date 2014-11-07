package com.google.inject.extensions.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.IteratorUtil;

import java.util.Collection;

/**
 * @author tbaum
 * @since 04.10.2013
 */
public class PatchIndexeTransactionEventHandler implements TransactionEventHandler<Object> {

    private final GraphDatabaseService graphDatabase;
    private final BackgroundWorker backgroundWorker;

    public PatchIndexeTransactionEventHandler(GraphDatabaseService graphDatabase, BackgroundWorker backgroundWorker) {
        this.graphDatabase = graphDatabase;
        this.backgroundWorker = backgroundWorker;
    }

    @Override public Object beforeCommit(TransactionData data) throws Exception {
        Collection<Node> deletedNodes = IteratorUtil.asCollection(data.deletedNodes());
        Collection<Relationship> deletedRels = IteratorUtil.asCollection(data.deletedRelationships());

        backgroundWorker.addJob(() -> {
            try (Transaction tx = graphDatabase.beginTx()) {
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
                tx.success();
            }
        });
        return null;
    }

    @Override public void afterCommit(TransactionData data, Object state) {
    }

    @Override public void afterRollback(TransactionData data, Object state) {
    }
}
