package com.google.inject.extensions.neo4j;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import org.neo4j.graphdb.Transaction;

import java.util.LinkedList;

public class TransactionScope implements Scope {

    public static final TransactionScope TRANSACTIONAL = new TransactionScope();
    private final ThreadLocal<LinkedList<Transaction>> value = new ThreadLocal<>();

    private TransactionScope() {
    }

    public static Provider<Transaction> transactionProvider() {
        return new Provider<Transaction>() {
            public Transaction get() {
                throw new IllegalStateException("If you got here then it means that your code asked for scoped " +
                        "object which should have been explicitly initialized in this scope");
            }
        };
    }

    public void enter(Transaction transaction) {
        LinkedList<Transaction> transactions = value.get();
        if (transactions == null) {
            value.set(transactions = new LinkedList<>());
        }
        transactions.add(null);

        put(transaction);
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

    public void put(Transaction transaction) {
        final LinkedList<Transaction> transactions = value.get();
        if (transactions.getLast() != null) {
            throw new IllegalStateException("Transaction was already set in this scope.");
        }
        transactions.set(transactions.size() - 1, transaction);
    }

    public Transaction currentTransaction() {
        return value.get().getLast();
    }

    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return new Provider<T>() {
            @SuppressWarnings("unchecked") public T get() {

                LinkedList current = (LinkedList) value.get();
                if (current == null) {
                    current = (LinkedList) unscoped.get();
                    value.set(current);
                }
                return (T) current.getLast();
            }
        };
    }
}
