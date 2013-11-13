package com.google.inject.extensions.neo4j;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tbaum
 * @since 04.10.2013
 */
public class LocalTxnInterceptor implements MethodInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(LocalTxnInterceptor.class);
    @Inject private final Provider<GraphDatabaseService> gdb = null;
    @Inject private final Provider<TransactionScope> transactionScopeProvider = null;

    @Override public Object invoke(MethodInvocation methodInvocation) throws Throwable {

        LOG.trace("create new transaction");
        final Transaction transaction = gdb.get().beginTx();

        final TransactionScope transactionScope = transactionScopeProvider.get();
        transactionScope.enter(transaction);
        try {
            Object result = methodInvocation.proceed();
            LOG.trace("marking transaction success");
            transaction.success();
            return result;
        } finally {
            transactionScope.exit();
            LOG.trace("finish transaction");
            transaction.finish();
        }
    }
}
