package com.google.inject.extensions.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
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

    @Inject public BackgroundWorker() {
        this.queue = new LinkedList<>();
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Runnable work;

                        synchronized (queue) {
                            while (queue.isEmpty()) queue.wait();
                            work = queue.getFirst();
                        }
                        try {
                            work.run();
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
            }
        };

        thread.setName("BackgroundWorker:" + (instance++));
        thread.start();
    }

    public void addJob(Runnable job) {
        synchronized (queue) {
            queue.add(job);
            queue.notifyAll();
        }
    }

    public void waitForQueue() throws InterruptedException {
        synchronized (queue) {
            while (!queue.isEmpty()) queue.wait();
        }
    }
}
