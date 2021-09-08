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
package io.gravitee.gateway.debug.sync;

import io.gravitee.common.service.AbstractService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugSyncManager extends AbstractService<DebugSyncManager> {

    private final Logger logger = LoggerFactory.getLogger(DebugSyncManager.class);

    @Autowired
    private DebugApiSynchronizer debugApiSynchronizer;

    protected final AtomicLong counter = new AtomicLong(0);
    private long lastRefreshAt = -1;
    private final ThreadPoolTaskScheduler scheduler;
    private ScheduledFuture<?> scheduledFuture;
    private List<String> environments;

    public DebugSyncManager() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("gio.sync-debug-");
        // Ensure every execution is done before running next execution
        scheduler.setPoolSize(1);
        scheduler.initialize();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        debugApiSynchronizer.start();
    }

    @Override
    protected void doStop() throws Exception {
        debugApiSynchronizer.stop();

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
        logger.debug("Synchronization for debug #{} started at {}", counter.incrementAndGet(), Instant.now().toString());
        logger.debug("Refreshing gateway state...");

        try {
            debugApiSynchronizer.synchronize(lastRefreshAt, nextLastRefreshAt, environments);
            lastRefreshAt = nextLastRefreshAt;
        } catch (Exception ex) {
            logger.error("An error occurs while synchronizing debug APIs", ex);
        }

        logger.debug("Synchronization for debug #{} ended at {}", counter.get(), Instant.now().toString());
    }

    public void setEnvironments(List<String> environments) {
        this.environments = environments;
    }
}
