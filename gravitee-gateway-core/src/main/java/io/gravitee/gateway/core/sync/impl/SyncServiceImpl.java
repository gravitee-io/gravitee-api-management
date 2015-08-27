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
package io.gravitee.gateway.core.sync.impl;

import io.gravitee.gateway.core.service.AbstractService;
import io.gravitee.gateway.core.sync.SyncService;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Polling sync service.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class SyncServiceImpl extends AbstractService implements SyncService, Runnable {

    /**
     * Logger
     */
    private final Logger LOGGER = LoggerFactory.getLogger(SyncServiceImpl.class);

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private TaskScheduler scheduler;

    @Value("${sync.cron:*/5 * * * * *}")
    private String cronTrigger;

    @Autowired
    private SyncStateManager syncStateManager;

    private final AtomicLong counter = new AtomicLong(0);

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        // Sync must start only when doStart() is invoked, that's the reason why we are not
        // using @Scheduled annotation on doSync() method.
        LOGGER.info("Sync service has been initialized with cron [{}]", cronTrigger);
        scheduler.schedule(this, new CronTrigger(cronTrigger));
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
        long count = counter.incrementAndGet();

        LOGGER.info("Synchronization #{} started at {}", count, Instant.now().toString());

        try {
            Set<Api> apis = apiRepository.findAll();
            apis.stream().forEach(api -> syncStateManager.handle(api));
        } catch (TechnicalException te) {
            te.printStackTrace();
        }

        LOGGER.info("Synchronization #{} ended at {}", count, Instant.now().toString());
    }
}
