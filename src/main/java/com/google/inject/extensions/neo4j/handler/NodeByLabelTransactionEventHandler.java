package com.google.inject.extensions.neo4j.handler;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.event.LabelEntry;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author tbaum
 * @since 15.04.2014
 */
public class NodeByLabelTransactionEventHandler implements TransactionEventHandler {

    private final ThreadLocal<Boolean> active = ThreadLocal.withInitial(() -> false);
    private final Label label;
    private final Consumer consumer;

    public NodeByLabelTransactionEventHandler(Label label, Consumer consumer) {
        this.label = label;
        this.consumer = consumer;
    }

    @Override public Object beforeCommit(TransactionData data) throws Exception {
        if (active.get()) {
            return null;
        }
        active.set(true);
        try {

            Set<Node> updated = new HashSet<>();
            Set<Node> deleted = new HashSet<>();

            data.deletedNodes().forEach(deleted::add);
            toStream(data.removedLabels()).filter(this::matchesLabel).map(LabelEntry::node).forEach(deleted::add);
            data.createdNodes().forEach(updated::add);
            toStream(data.assignedNodeProperties()).map(PropertyEntry::entity).forEach(updated::add);
            toStream(data.removedNodeProperties()).map(PropertyEntry::entity).forEach(updated::add);
            toStream(data.assignedLabels()).filter(this::matchesLabel).map(LabelEntry::node).forEach(updated::add);


            final Set<Node> collect = updated.stream().filter(this::hasLabel).collect(Collectors.toSet());
            if (!collect.isEmpty() || !deleted.isEmpty()) {
                consumer.onChange(collect, deleted);
            }
            return null;
        } finally {
            active.set(false);
        }
    }

    private boolean hasLabel(Node n) {
        return n.hasLabel(label);
    }

    private boolean matchesLabel(LabelEntry l) {
        return l.label().equals(label);
    }

    private <T> Stream<T> toStream(Iterable<T> labelEntries) {
        return StreamSupport.stream(labelEntries.spliterator(), false);
    }

    @Override public void afterCommit(TransactionData data, Object state) {
    }

    @Override public void afterRollback(TransactionData data, Object state) {
    }

    public interface Consumer {
        void onChange(Collection<Node> updated, Collection<Node> deleted);
    }
}
