/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.services.genericevents;

import io.gravitee.apim.core.generic_event.use_case.PurgeGenericEventsUseCase;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.cluster.ClusterManager;
import java.time.Duration;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Deletes expired generic events and purges the unpublished ones, from the primary node only.
 */
@CustomLog
public class ScheduledGenericEventsPurgeService extends AbstractService implements Runnable {

    private final PurgeGenericEventsUseCase purgeGenericEventsUseCase;
    private final TaskScheduler scheduler;
    private final ClusterManager clusterManager;
    private final String cronTrigger;
    private final boolean enabled;
    private final PurgeGenericEventsUseCase.Input input;

    @Autowired
    public ScheduledGenericEventsPurgeService(
        PurgeGenericEventsUseCase purgeGenericEventsUseCase,
        @Qualifier("genericEventsPurgeTaskScheduler") TaskScheduler scheduler,
        ClusterManager clusterManager,
        @Value("${services.genericEvents.cron:0 * * * * *}") String cronTrigger,
        @Value("${services.genericEvents.enabled:true}") boolean enabled,
        @Value("${services.genericEvents.deletedRetention:10080}") long deletedRetentionMinutes
    ) {
        this.purgeGenericEventsUseCase = purgeGenericEventsUseCase;
        this.scheduler = scheduler;
        this.clusterManager = clusterManager;
        this.cronTrigger = cronTrigger;
        this.enabled = enabled;
        this.input = new PurgeGenericEventsUseCase.Input(Duration.ofMinutes(deletedRetentionMinutes));
    }

    @Override
    protected String name() {
        return "Generic events purge service";
    }

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();
            log.info("Generic events purge service has been initialized with cron [{}]", cronTrigger);
            scheduler.schedule(this, new CronTrigger(cronTrigger));
        } else {
            log.warn("Generic events purge service has been disabled");
        }
    }

    @Override
    public void run() {
        if (!clusterManager.self().primary()) {
            return;
        }
        try {
            var output = purgeGenericEventsUseCase.execute(input);
            log.debug("Generic events purge: {} expired deleted, {} unpublished purged", output.expired(), output.purged());
        } catch (Exception e) {
            log.error("Generic events purge failed", e);
        }
    }
}
