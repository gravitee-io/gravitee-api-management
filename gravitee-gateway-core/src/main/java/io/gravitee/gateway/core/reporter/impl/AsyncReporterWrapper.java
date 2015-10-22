/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.reporter.impl;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.metrics.Metrics;
import io.gravitee.gateway.api.reporter.MetricsReporter;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public final class AsyncReporterWrapper extends AbstractService implements MetricsReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncReporterWrapper.class);

    private final MetricsReporter metricsReporter;

    private BlockingQueue<Metrics> queue;

    private transient WriterThread thread;

    private boolean warnedFull;

    private String reporterName;

    private int queueCapacity;

    private long pollingTimeout;

    public AsyncReporterWrapper(MetricsReporter metricsReporter) {
        this.metricsReporter = metricsReporter;
    }

    @Override
    public synchronized void doStart() throws Exception {
        super.doStart();
        LOGGER.info("Start async reporter for {}", getReporterName());
        LOGGER.info("\tQueue capacity: {}", getQueueCapacity());
        LOGGER.info("\tQueue polling timeout: {} ms", getPollingTimeout());
        this.queue = new BlockingArrayQueue<>(getQueueCapacity());
        this.thread = new WriterThread();
        this.thread.start();

        this.metricsReporter.start();
        LOGGER.info("Start async reporter for {} : DONE", getReporterName());
    }

    @Override
    public synchronized void doStop() throws Exception {
        LOGGER.info("Stop async reporter for {}", getReporterName());
        thread.terminate();
        thread.join();
        super.doStop();
        thread = null;
        this.metricsReporter.stop();
        LOGGER.info("Stop async reporter for {} : DONE", getReporterName());
    }

    @Override
    public void report(Metrics metrics) {
        if (!this.queue.offer(metrics)) {
            if (this.warnedFull) {
                // TODO: provide a programmatic overflow to disk feature
                LOGGER.warn("Async reporter's queue overflow !");
            }
            this.warnedFull = true;
        }
    }

    public long getPollingTimeout() {
        return pollingTimeout;
    }

    public void setPollingTimeout(long pollingTimeout) {
        this.pollingTimeout = pollingTimeout;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    private class WriterThread extends Thread {
        private volatile boolean running = true;

        WriterThread() {
            this.setName("async-reporter-" + getReporterName());
        }

        public void terminate() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Metrics metrics = AsyncReporterWrapper.this.queue.poll(getPollingTimeout(), TimeUnit.MILLISECONDS);
                    if (metrics != null) {
                        AsyncReporterWrapper.this.metricsReporter.report(metrics);
                    }

                    while (!AsyncReporterWrapper.this.queue.isEmpty()) {
                        metrics = AsyncReporterWrapper.this.queue.poll();
                        if (metrics != null) {
                            AsyncReporterWrapper.this.metricsReporter.report(metrics);
                        }
                    }
                } catch (InterruptedException ie) {
                    running = false;
                }
            }
        }
    }
}
