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
package io.gravitee.rest.api.services.fetcher;

import io.gravitee.common.service.AbstractService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.services.fetcher.spring.AutoFetchConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ScheduledAutoFetchService extends AbstractService implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledAutoFetchService.class);

    @Autowired
    private TaskScheduler scheduler;

    @Autowired
    private AutoFetchConfiguration configuration;

    @Autowired
    private PageService pageService;

    private final AtomicLong counter = new AtomicLong(0);

    @Override
    protected String name() {
        return "Auto Fetch Service";
    }

    @Override
    protected void doStart() throws Exception {
        if (configuration.isEnabled()) {
            super.doStart();
            LOGGER.info("Auto Fetch service has been initialized with cron [{}]", configuration.getCronTrigger());
            scheduler.schedule(this, new CronTrigger(configuration.getCronTrigger()));
        } else {
            LOGGER.warn("Auto Fetch service has been disabled");
        }
    }

    @Override
    public void run() {
        LOGGER.debug("Auto Fetch #{} started at {}", counter.incrementAndGet(), Instant.now());
        pageService.execAutoFetch();
        LOGGER.debug("Auto Fetch #{} ended at {}", counter.get(), Instant.now());
    }

}