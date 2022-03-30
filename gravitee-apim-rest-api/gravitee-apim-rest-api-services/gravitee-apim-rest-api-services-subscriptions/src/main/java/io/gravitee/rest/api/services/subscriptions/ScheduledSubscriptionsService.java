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
package io.gravitee.rest.api.services.subscriptions;

import io.gravitee.common.service.AbstractService;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ScheduledSubscriptionsService extends AbstractService implements Runnable {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ScheduledSubscriptionsService.class);

    @Autowired
    private TaskScheduler scheduler;

    @Value("${services.subscriptions.cron:0 1 * * * *}")
    private String cronTrigger;

    @Value("${services.subscriptions.enabled:true}")
    private boolean enabled;

    private final AtomicLong counter = new AtomicLong(0);

    @Autowired
    private ApiService apiService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Override
    protected String name() {
        return "Subscriptions Refresher Service";
    }

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();
            logger.info("Subscriptions Refresher service has been initialized with cron [{}]", cronTrigger);
            scheduler.schedule(this, new CronTrigger(cronTrigger));
        } else {
            logger.warn("Subscriptions Refresher service has been disabled");
        }
    }

    @Override
    public void run() {
        logger.debug("Refresh subscriptions #{} started at {}", counter.incrementAndGet(), Instant.now().toString());
        final SubscriptionQuery query = new SubscriptionQuery();
        query.setStatuses(Collections.singleton(SubscriptionStatus.ACCEPTED));
        query.setEndingAtBefore(new Date().getTime());

        subscriptionService
            .search(GraviteeContext.getExecutionContext(), query)
            .forEach(subscription -> subscriptionService.close(GraviteeContext.getExecutionContext(), subscription.getId()));

        logger.debug("Refresh subscriptions #{} ended at {}", counter.get(), Instant.now().toString());
    }
}
