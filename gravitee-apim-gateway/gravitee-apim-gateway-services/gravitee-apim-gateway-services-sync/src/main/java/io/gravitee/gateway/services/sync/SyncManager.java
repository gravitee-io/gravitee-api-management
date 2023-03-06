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
package io.gravitee.gateway.services.sync;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.services.sync.synchronizer.Synchronizer;
import io.gravitee.node.api.cluster.ClusterManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SyncManager extends AbstractService<SyncManager> {

    private final Logger logger = LoggerFactory.getLogger(SyncManager.class);

    /**
     * Add 30s delay before and after to avoid problem with out of sync clocks.
     */
    public static final int TIMEFRAME_BEFORE_DELAY = 30000;
    public static final int TIMEFRAME_AFTER_DELAY = 30000;

    @Autowired
    private List<Synchronizer> synchronizers;

    @Autowired
    private ClusterManager clusterManager;

    @Autowired
    @Qualifier("syncExecutor")
    private ThreadPoolExecutor executor;

    @Value("${services.sync.distributed:false}")
    private boolean distributed;

    private final AtomicLong counter = new AtomicLong(0);
    private long lastRefreshAt = -1;
    private int totalErrors = 0;
    private int errors = 0;
    private String lastErrorMessage;
    private final ThreadPoolTaskScheduler scheduler;
    private ScheduledFuture<?> scheduledFuture;
    private boolean synced;
    private boolean allApisSync = false;
    private List<String> environments;

    public SyncManager() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("gio.sync-master");
        // Ensure every execution is done before running next execution
        scheduler.setPoolSize(1);
        scheduler.initialize();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        synchronizers.forEach(
            synchronizer -> {
                try {
                    synchronizer.start();
                } catch (Exception e) {
                    logger.error("An error occurs while starting synchronizer {}", synchronizer.getClass().getSimpleName(), e);
                }
            }
        );
    }

    @Override
    protected void doStop() throws Exception {
        synchronizers.forEach(
            synchronizer -> {
                try {
                    synchronizer.stop();
                } catch (Exception e) {
                    logger.error("An error occurs while stoping synchronizer {}", synchronizer.getClass().getSimpleName(), e);
                }
            }
        );

        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }

        if (scheduler != null) {
            scheduler.shutdown();
        }

        super.doStop();
    }

    public void startScheduler(int delay, TimeUnit unit) {
        scheduledFuture = scheduler.scheduleAtFixedRate(this::refresh, Duration.ofMillis(unit.toMillis(delay)));
    }

    public void refresh() {
        final long nextLastRefreshAt = System.currentTimeMillis();
        AtomicBoolean error = new AtomicBoolean(false);

        if (clusterManager.isMasterNode() || (!clusterManager.isMasterNode() && !distributed)) {
            logger.debug("Synchronization #{} started at {}", counter.incrementAndGet(), Instant.now());
            logger.debug("Refreshing gateway state...");

            synchronizers.forEach(
                synchronizer -> {
                    try {
                        synchronizer.synchronize(lastRefreshAt, nextLastRefreshAt, environments);
                    } catch (Exception e) {
                        error.set(true);
                        lastErrorMessage = e.getMessage();
                        logger.error("An error occurs while synchronizing organizations", e);
                    }
                }
            );

            if (error.get()) {
                errors++;
                totalErrors++;
            } else {
                errors = 0;
            }
        }

        // If there was no error during the sync process, let's continue it with the next period of time
        if (!error.get()) {
            allApisSync = true;

            if (lastRefreshAt == -1) {
                // When first sync is entirely done, we can reduce number of threads to the minimum for next sync
                executor.setCorePoolSize(1);

                // A first full sync has been done
                synced = true;
            }
            // We refresh the date even if process did not run (not a master node) to ensure that we sync the same way as
            // soon as the node is becoming the master later.
            lastRefreshAt = nextLastRefreshAt;
        }

        logger.debug(
            "Synchronization #{} ended at {} (took {}ms}",
            counter.get(),
            Instant.now().toString(),
            System.currentTimeMillis() - nextLastRefreshAt
        );
    }

    public boolean isDistributed() {
        return distributed;
    }

    public void setDistributed(boolean distributed) {
        this.distributed = distributed;
    }

    public long getLastRefreshAt() {
        return lastRefreshAt;
    }

    public long getCounter() {
        return counter.longValue();
    }

    /**
     * This method returns if a first full sync occurs successfully.
     * @return
     */
    public boolean isSynced() {
        return synced;
    }

    public int getTotalErrors() {
        return totalErrors;
    }

    public int getErrors() {
        return errors;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public boolean isAllApisSync() {
        return allApisSync;
    }

    public List<String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<String> environments) {
        this.environments = environments;
    }
}
