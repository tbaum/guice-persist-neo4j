package com.google.inject.extensions.neo4j.handler;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
        Set<Node> updated = new HashSet<>();
        Set<Node> deleted = new HashSet<>();

        data.deletedNodes().forEach(deleted::add);
        data.removedLabels().forEach((l) -> {
            if (l.label().equals(label)) deleted.add(l.node());
        });

        data.createdNodes().forEach(updated::add);
        data.assignedNodeProperties().forEach((p) -> updated.add(p.entity()));
        data.removedNodeProperties().forEach((p) -> updated.add(p.entity()));

        data.assignedLabels().forEach((l) -> {
            if (l.label().equals(label)) updated.add(l.node());
        });

        onChange(updated.stream().filter((n) -> n.hasLabel(label)).collect(Collectors.toSet()), deleted);
        return null;
    }

    protected abstract void onChange(Collection<Node> updated, Collection<Node> deleted);

    @Override public void afterCommit(TransactionData data, Object state) {
    }

    @Override public void afterRollback(TransactionData data, Object state) {
    }
}
