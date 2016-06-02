package com.google.inject.extensions.neo4j;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import javax.inject.Inject;

import static com.google.inject.Guice.createInjector;
import static org.junit.Assert.*;

public class RollbackOnTest {

    private A a;

    @Before public void setup() {
        a = createInjector(new ImpermanentNeo4JPersistenceModule()).getInstance(A.class);
    }

    @Test
    public void testRollback1() {
        a.callRollbackException1(new RuntimeException());
        assertTrue(a.wasExecuted());

        a.callRollbackException1(new Exception1());
        assertFalse(a.wasExecuted());

        a.callRollbackException1(new Exception2());
        assertTrue(a.wasExecuted());

        a.callRollbackException1(new ExceptionExtends2());
        assertTrue(a.wasExecuted());
    }

    @Test
    public void testRollback2() {
        a.callRollbackException2(new RuntimeException());
        assertTrue(a.wasExecuted());

        a.callRollbackException2(new Exception1());
        assertTrue(a.wasExecuted());

        a.callRollbackException2(new Exception2());
        assertFalse(a.wasExecuted());

        a.callRollbackException2(new ExceptionExtends2());
        assertFalse(a.wasExecuted());
    }

    @Test
    public void testRollback3() {
        a.callRollbackExceptionExtends2(new RuntimeException());
        assertTrue(a.wasExecuted());

        a.callRollbackExceptionExtends2(new Exception1());
        assertTrue(a.wasExecuted());

        a.callRollbackExceptionExtends2(new Exception2());
        assertTrue(a.wasExecuted());

        a.callRollbackExceptionExtends2(new ExceptionExtends2());
        assertFalse(a.wasExecuted());
    }

    @Test
    public void testRollback4() {
        a.callRollbackList(new RuntimeException());
        assertTrue(a.wasExecuted());

        a.callRollbackList(new Exception1());
        assertFalse(a.wasExecuted());

        a.callRollbackList(new Exception2());
        assertTrue(a.wasExecuted());

        a.callRollbackList(new ExceptionExtends2());
        assertFalse(a.wasExecuted());
    }

    @Test
    public void testNoRollbackFor1() {
        a.callNoRollbackForException1(new RuntimeException());
        assertFalse(a.wasExecuted());

        a.callNoRollbackForException1(new Exception1());
        assertTrue(a.wasExecuted());

        a.callNoRollbackForException1(new Exception2());
        assertFalse(a.wasExecuted());

        a.callNoRollbackForException1(new ExceptionExtends2());
        assertFalse(a.wasExecuted());
    }

    @Test
    public void testNoRollbackFor2() {
        a.callNoRollbackForException2(new RuntimeException());
        assertFalse(a.wasExecuted());

        a.callNoRollbackForException2(new Exception1());
        assertFalse(a.wasExecuted());

        a.callNoRollbackForException2(new Exception2());
        assertTrue(a.wasExecuted());

        a.callNoRollbackForException2(new ExceptionExtends2());
        assertTrue(a.wasExecuted());
    }

    @Test
    public void testNoRollbackFor3() {
        a.callNoRollbackForExceptionExtends2(new RuntimeException());
        assertFalse(a.wasExecuted());

        a.callNoRollbackForExceptionExtends2(new Exception1());
        assertFalse(a.wasExecuted());

        a.callNoRollbackForExceptionExtends2(new Exception2());
        assertFalse(a.wasExecuted());

        a.callNoRollbackForExceptionExtends2(new ExceptionExtends2());
        assertTrue(a.wasExecuted());
    }

    @Test
    public void testNoRollbackFor4() {
        a.callNoRollbackForList(new RuntimeException());
        assertFalse(a.wasExecuted());

        a.callNoRollbackForList(new Exception1());
        assertTrue(a.wasExecuted());

        a.callNoRollbackForList(new Exception2());
        assertFalse(a.wasExecuted());

        a.callNoRollbackForList(new ExceptionExtends2());
        assertTrue(a.wasExecuted());
    }

    @Test
    public void testDefault() {
        a.callDefault(new RuntimeException());
        assertFalse(a.wasExecuted());

        a.callDefault(new Exception1());
        assertFalse(a.wasExecuted());

        a.callDefault(new Exception2());
        assertFalse(a.wasExecuted());

        a.callDefault(new ExceptionExtends2());
        assertFalse(a.wasExecuted());
    }


    private static class Exception1 extends RuntimeException {
    }

    static class Exception2 extends RuntimeException {
    }

    private static class ExceptionExtends2 extends Exception2 {
    }


    private static class A {
        final Node node;

        @Inject A(GraphDatabaseService gds) {
            try (Transaction tx = gds.beginTx()) {
                node = gds.createNode();
                tx.success();
            }
        }

        @Transactional Transaction _default(RuntimeException t) {
            node.setProperty("v", true);
            throw t;
        }

        @Transactional(rollbackOn = Exception1.class) Transaction _rollbackException1(RuntimeException t) {
            node.setProperty("v", true);
            throw t;
        }

        @Transactional(rollbackOn = Exception2.class) Transaction _rollbackException2(RuntimeException t) {
            node.setProperty("v", true);
            throw t;
        }

        @Transactional(rollbackOn = ExceptionExtends2.class)
        Transaction _rollbackExceptionExtends2(RuntimeException t) {
            node.setProperty("v", true);
            throw t;
        }

        @Transactional(rollbackOn = {Exception1.class, ExceptionExtends2.class})
        Transaction _rollbackList(RuntimeException t) {
            node.setProperty("v", true);
            throw t;
        }

        @Transactional(noRollbackFor = Exception1.class) Transaction _noRollbackException1(RuntimeException t) {
            node.setProperty("v", true);
            throw t;
        }

        @Transactional(noRollbackFor = Exception2.class) Transaction _noRollbackException2(RuntimeException t) {
            node.setProperty("v", true);
            throw t;
        }

        @Transactional(noRollbackFor = ExceptionExtends2.class)
        Transaction _noRollbackExceptionExtends2(RuntimeException t) {
            node.setProperty("v", true);
            throw t;
        }

        @Transactional(noRollbackFor = {Exception1.class, ExceptionExtends2.class})
        Transaction _noRollbackList(RuntimeException t) {
            node.setProperty("v", true);
            throw t;
        }

        @Transactional boolean wasExecuted() {
            try {
                return (boolean) node.getProperty("v", false);
            } finally {
                node.setProperty("v", false);
            }
        }

        private void callDefault(RuntimeException t) {
            try {
                _default(t);
                fail();
            } catch (Exception ignored) {
            }
        }


        private void callRollbackException1(RuntimeException t) {
            try {
                _rollbackException1(t);
                fail();
            } catch (Exception ignored) {
            }
        }


        private void callRollbackException2(RuntimeException t) {
            try {
                _rollbackException2(t);
                fail();
            } catch (Exception ignored) {
            }
        }

        private void callRollbackExceptionExtends2(RuntimeException t) {
            try {
                _rollbackExceptionExtends2(t);
                fail();
            } catch (Exception ignored) {
            }
        }

        private void callRollbackList(RuntimeException t) {
            try {
                _rollbackList(t);
                fail();
            } catch (Exception ignored) {
            }
        }

        private void callNoRollbackForException1(RuntimeException t) {
            try {
                _noRollbackException1(t);
                fail();
            } catch (Exception ignored) {
            }
        }


        private void callNoRollbackForException2(RuntimeException t) {
            try {
                _noRollbackException2(t);
                fail();
            } catch (Exception ignored) {
            }
        }

        private void callNoRollbackForExceptionExtends2(RuntimeException t) {
            try {
                _noRollbackExceptionExtends2(t);
                fail();
            } catch (Exception ignored) {
            }
        }

        private void callNoRollbackForList(RuntimeException t) {
            try {
                _noRollbackList(t);
                fail();
            } catch (Exception ignored) {
            }
        }
    }
}
