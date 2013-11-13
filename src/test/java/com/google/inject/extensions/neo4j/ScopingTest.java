package com.google.inject.extensions.neo4j;

import com.google.inject.*;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.PlaceboTransaction;
import org.neo4j.kernel.TopLevelTransaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import javax.inject.Inject;
import javax.inject.Provider;

import static com.google.inject.Guice.createInjector;
import static com.google.inject.extensions.neo4j.TransactionScope.TRANSACTIONAL;
import static com.google.inject.extensions.neo4j.TransactionScope.transactionProvider;
import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

public class ScopingTest {

    private Injector injector;

    @Before public void setup() {
        injector = createInjector(new AbstractModule() {
            @Override protected void configure() {
                install(new AbstractModule() {

                    @Override protected void configure() {
                        MethodInterceptor tx = new LocalTxnInterceptor();
                        requestInjection(tx);
                        bindInterceptor(any(), annotatedWith(Transactional.class), tx);

                        bindScope(Transactional.class, TRANSACTIONAL);
                        bind(TransactionScope.class).toInstance(TRANSACTIONAL);
                        bind(Transaction.class).toProvider(transactionProvider()).in(TRANSACTIONAL);
                    }

                    @Provides @Singleton public GraphDatabaseService getGraphDatabaseService() {
                        return new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
                    }

                });
            }
        });

    }

    @Test(expected = ProvisionException.class)
    public void notScoped() {
        injector.getInstance(A.class).inTx();
    }

    @Test
    public void scoped() {
        final Transaction tx = injector.getInstance(B.class).inTx();
        Assert.assertEquals(TopLevelTransaction.class, tx.getClass());

    }

    @Test
    public void testSimpleScoping() {
        final Transaction[] tx = injector.getInstance(D.class).inTx();
        Assert.assertEquals(TopLevelTransaction.class, tx[0].getClass());
        Assert.assertEquals(PlaceboTransaction.class, tx[1].getClass());
    }

    static class A {
        @Inject Transaction tx;

        Transaction inTx() {
            return tx;
        }
    }

    static class B {
        @Inject Provider<A> aProvider;

        @Transactional Transaction inTx() {
            return aProvider.get().inTx();
        }
    }

    static class C {
        @Inject Provider<A> aProvider;
        @Inject Transaction tx;

        @Transactional Transaction[] inTx() {
            return new Transaction[]{tx, aProvider.get().inTx()};
        }
    }

    static class D {
        @Inject Provider<C> bProvider;

        @Transactional Transaction[] inTx() {
            return bProvider.get().inTx();
        }
    }
}
