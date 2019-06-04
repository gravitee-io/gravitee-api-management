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
package io.gravitee.rest.api.services.sync;

import io.gravitee.common.service.AbstractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ScheduledSyncService extends AbstractService implements Runnable {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ScheduledSyncService.class);

    @Autowired
    private TaskScheduler scheduler;

    @Value("${services.sync.cron:*/5 * * * * *}")
    private String cronTrigger;

    @Value("${services.sync.enabled:true}")
    private boolean enabled;

    @Value("${services.local.enabled:false}")
    private boolean localRegistryEnabled;

    @Autowired
    private SyncManager syncStateManager;

    private final AtomicLong counter = new AtomicLong(0);

    @Override
    protected void doStart() throws Exception {
        if (! localRegistryEnabled) {
            if (enabled) {
                super.doStart();
                logger.info("Sync service has been initialized with cron [{}]", cronTrigger);
                // Sync must start only when doStart() is invoked, that's the reason why we are not
                // using @Scheduled annotation on doSync() method.
                scheduler.schedule(this, new CronTrigger(cronTrigger));
            } else {
                logger.warn("Sync service has been disabled");
            }
        } else {
            logger.warn("Sync service is disabled because local registry mode is enabled");
        }
    }

    @Override
    public void run() {
        doSync();
    }

    /**
     * Synchronization done when Gravitee node is starting.
     * This sync phase must be done by all node before starting.
     */
    private void doSync() {
        logger.debug("Synchronization #{} started at {}", counter.incrementAndGet(), Instant.now().toString());

        syncStateManager.refresh();

        logger.debug("Synchronization #{} ended at {}", counter.get(), Instant.now().toString());
    }

    @Override
    protected String name() {
        return "Sync Service";
    }
}
