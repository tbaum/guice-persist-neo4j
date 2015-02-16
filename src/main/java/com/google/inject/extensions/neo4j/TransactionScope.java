package com.google.inject.extensions.neo4j;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import org.neo4j.graphdb.Transaction;

import java.util.LinkedList;

public class TransactionScope implements Scope, AutoCloseable {

    public static final TransactionScope TRANSACTIONAL = new TransactionScope();
    private final ThreadLocal<LinkedList<Transaction>> value = new ThreadLocal<>();
    private final ThreadLocal<Boolean> failed = ThreadLocal.withInitial(() -> false);

    private TransactionScope() {
    }

    public static Provider<Transaction> transactionProvider() {
        return () -> {
            throw new IllegalStateException("If you got here then it means that your code asked for scoped " +
                    "object which should have been explicitly initialized in this scope");
        };
    }

    public TransactionScope enter(Transaction transaction) {
        LinkedList<Transaction> transactions = value.get();
        if (transactions == null) {
            value.set(transactions = new LinkedList<>());
            failed.set(false);
        }
        transactions.add(transaction);
        return this;
    }

    public void exit() {
        LinkedList<Transaction> transactions = value.get();
        if (transactions == null) {
            throw new IllegalStateException("No scoping block in progress");
        }

        transactions.removeLast();

        if (transactions.isEmpty()) {
            value.set(null);
        }
    }

    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return () -> {
            LinkedList<Transaction> current = value.get();
            if (current == null) {
                throw new IllegalStateException();
            }
            //noinspection unchecked
            return (T) current.getLast();
        };
    }

    @Override public void close() {
        exit();
    }

    boolean inScope() {
        LinkedList<Transaction> transactions = value.get();
        return transactions != null && !transactions.isEmpty();
    }

    public Integer getCurrentDepth() {
        LinkedList<Transaction> transactions = value.get();
        return transactions != null ? transactions.size() : null;
    }

    public void markFailed() {
        if (!inScope()) {
            throw new IllegalStateException("No scoping block in progress");
        }
        value.get().getFirst().failure();
        failed.set(true);
    }

    public boolean isFailed() {
        return failed.get();
    }
}
