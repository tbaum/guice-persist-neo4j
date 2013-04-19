package com.google.inject.extensions.neo4j;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

/**
 * @author tbaum
 * @since 20.02.12
 */
public class LocalTxnInterceptor implements MethodInterceptor {
    @Inject private final Provider<GraphDatabaseService> gdb = null;
    private final ThreadLocal<Transaction> currentTransaction = new ThreadLocal<Transaction>();

    @Override public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        if (currentTransaction.get() != null) {
            return methodInvocation.proceed();
        }

        final Transaction transaction = gdb.get().beginTx();
        currentTransaction.set(transaction);
        try {
            Object result = methodInvocation.proceed();
            transaction.success();
            return result;
        } finally {
            currentTransaction.remove();
            transaction.finish();
        }
    }
}
