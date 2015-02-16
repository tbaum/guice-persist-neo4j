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
public class TransactionInterceptor implements MethodInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionInterceptor.class);
    @Inject private final Provider<GraphDatabaseService> gdb = null;
    @Inject private final Provider<TransactionScope> transactionScopeProvider = null;

    @Override public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        TransactionScope transactionScope = transactionScopeProvider.get();
        if (transactionScope.inScope()) {
            LOG.debug("join transaction {}", transactionScope.getCurrentDepth());
            try {
                return methodInvocation.proceed();
            } finally {
                LOG.debug("leaving transaction {}", transactionScope.getCurrentDepth());
            }
        }

        LOG.debug("create new transaction");
        try (Transaction transaction = gdb.get().beginTx()) {
            try (TransactionScope ignored = transactionScope.enter(transaction)) {
                final Object result = methodInvocation.proceed();
                if (transactionScope.isFailed()) {
                    LOG.debug("marking transaction failed");
                    transaction.failure();
                } else {
                    LOG.debug("marking transaction success");
                    transaction.success();
                }
                return result;
            } catch (Throwable throwable) {
                final Class<? extends Throwable> throwableClass = throwable.getClass();
                if (noRollback(methodInvocation, throwableClass)) {
                    LOG.debug("marking transaction success (catched exception {})", throwableClass);
                    transaction.success();
                }
                throw throwable;
            } finally {
                LOG.debug("leaving transaction");
            }
        }
    }

    private boolean noRollback(MethodInvocation methodInvocation, Class<? extends Throwable> throwable) {
        final Transactional annotation = methodInvocation.getMethod().getAnnotation(Transactional.class);

        return annotation != null &&
                (isAssignableFrom(throwable, annotation.noRollbackFor()) || !isAssignableFrom(throwable, annotation.rollbackOn()));

    }

    private boolean isAssignableFrom(Class subClass, Class... classes) {
        for (Class<?> aClass : classes) {
            if (aClass.isAssignableFrom(subClass)) {
                return true;
            }
        }
        return false;
    }
}
