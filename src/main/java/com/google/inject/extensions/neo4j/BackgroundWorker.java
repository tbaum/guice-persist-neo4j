package com.google.inject.extensions.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.ErrorState;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedList;

/**
 * @author tbaum
 * @since 07.11.2014
 */
@Singleton
public class BackgroundWorker extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(BackgroundWorker.class);
    private static int instance = 0;
    private final LinkedList<Runnable> queue;

    @Inject public BackgroundWorker(GraphDatabaseService gds) {
        this.queue = new LinkedList<>();
        new Thread(() -> {
            while (true) {
                try {
                    final Runnable work;
                    LOG.debug("waiting");
                    synchronized (queue) {
                        while (queue.isEmpty()) {
                            queue.wait();

                            // shutdown
                            if (isInterrupted() && queue.isEmpty()) {
                                LOG.info("shutdown worker");
                                return;
                            }
                        }
                        work = queue.getFirst();
                    }
                    LOG.debug("run job {}", work);

                    try (Transaction tx = gds.beginTx()) {
                        work.run();
                        tx.success();
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                    synchronized (queue) {
                        queue.remove(work);
                        queue.notifyAll();
                    }
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }, "BackgroundWorker:" + (instance++)).start();

        gds.registerKernelEventHandler(new KernelEventHandler() {
            @Override public void beforeShutdown() {
                shutdown();
            }

            @Override public void kernelPanic(ErrorState error) {
                shutdown();
            }

            @Override public Object getResource() {
                return null;
            }

            @Override public ExecutionOrder orderComparedTo(KernelEventHandler other) {
                return ExecutionOrder.DOESNT_MATTER;
            }
        });
    }

    public void shutdown() {
        synchronized (queue) {
            interrupt();
            queue.notifyAll();
        }
    }

    public void addJob(Runnable job) {
        synchronized (queue) {
            queue.add(job);
            queue.notifyAll();
        }
    }

    protected void _waitForQueue() throws InterruptedException {
        synchronized (queue) {
            while (!queue.isEmpty()) queue.wait();
        }
    }
}
