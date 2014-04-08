package com.google.inject.extensions.neo4j.handler;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;

/**
 * @author tbaum
 * @since 15.04.2014
 */
public abstract class NodeByLabelTransactionEventHandler implements TransactionEventHandler {

    private final Label label;

    public NodeByLabelTransactionEventHandler(Label label) {
        this.label = label;
    }

    @Override public Object beforeCommit(TransactionData data) throws Exception {
        data.deletedNodes().forEach(this::onDelete);
        data.createdNodes().forEach(this::update);
        data.assignedNodeProperties().forEach(this::update);
        data.removedNodeProperties().forEach(this::update);
        return null;
    }

    private void update(PropertyEntry<Node> entry) {
        update(entry.entity());
    }

    private void update(Node node) {
        if (node.hasLabel(label)) onUpdate(node);
    }

    protected abstract void onUpdate(Node node);

    protected abstract void onDelete(Node node);

    @Override public void afterCommit(TransactionData data, Object state) {
    }

    @Override public void afterRollback(TransactionData data, Object state) {
    }
}
