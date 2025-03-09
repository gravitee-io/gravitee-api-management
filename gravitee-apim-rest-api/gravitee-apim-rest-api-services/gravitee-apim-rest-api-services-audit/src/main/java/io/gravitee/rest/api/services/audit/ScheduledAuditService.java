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
package io.gravitee.rest.api.services.audit;

import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import io.gravitee.apim.core.audit.use_case.CleanupEventsUseCase;
import io.gravitee.common.service.AbstractService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

public class ScheduledAuditService extends AbstractService implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(ScheduledAuditService.class);

    private final CleanupEventsUseCase cleanupEventsUseCase;
    private final OrganizationService organizationService;
    private final EnvironmentService environmentService;
    private final TaskScheduler scheduler;

    private final String cronTrigger;
    private final int eventsKeep;
    private final boolean enabled;

    @Autowired
    public ScheduledAuditService(
        CleanupEventsUseCase cleanupEventsUseCase,
        OrganizationService organizationService,
        EnvironmentService environmentService,
        @Qualifier("auditTaskScheduler") TaskScheduler scheduler,
        @Value("${services.audit.cron:0 1 * * * *}") String cronTrigger,
        @Value("${services.audit.keep:5}") int eventsKeep,
        @Value("${services.audit.enabled:true}") boolean enabled
    ) {
        this.cleanupEventsUseCase = cleanupEventsUseCase;
        this.organizationService = organizationService;
        this.environmentService = environmentService;
        this.scheduler = scheduler;
        this.cronTrigger = cronTrigger;
        this.eventsKeep = eventsKeep;
        this.enabled = enabled;
    }

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();
            logger.info("Audit event cleaner service has been initialized with cron [{}]", cronTrigger);
            scheduler.schedule(this, new CronTrigger(cronTrigger));
        } else {
            logger.warn("Audit event cleaner Refresher service has been disabled");
        }
    }

    @Override
    public void run() {
        var environments = stream(organizationService.findAll())
            .flatMap(o -> stream(environmentService.findByOrganization(o.getId())))
            .toList();
        for (var environment : environments) {
            logger.info(
                "Start cleanup environment: {} ({}) from organisation {}",
                environment.getName(),
                environment.getId(),
                environment.getOrganizationId()
            );
            cleanupEventsUseCase.execute(new CleanupEventsUseCase.Input(environment.getId(), eventsKeep));
        }
    }
}
