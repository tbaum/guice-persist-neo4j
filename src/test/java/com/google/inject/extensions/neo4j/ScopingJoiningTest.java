package com.google.inject.extensions.neo4j;

import com.google.inject.*;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.TopLevelTransaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import static com.google.inject.Guice.createInjector;
import static com.google.inject.extensions.neo4j.ScopingTest.*;
import static com.google.inject.extensions.neo4j.TransactionScope.TRANSACTIONAL;
import static com.google.inject.extensions.neo4j.TransactionScope.transactionProvider;
import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

public class ScopingJoiningTest {

    private Injector injector;

    @Before public void setup() {
        injector = createInjector(new AbstractModule() {
            @Override protected void configure() {
                install(new AbstractModule() {

                    @Override protected void configure() {

                        MethodInterceptor tx = new JoiningLocalTxnInterceptor();
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
        Assert.assertEquals(tx[0], tx[1]);
    }
}
