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

import io.gravitee.apim.core.audit.use_case.RemoveOldAuditDataUseCase;
import io.gravitee.common.service.AbstractService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@Slf4j
public class ScheduledAuditCleanerService extends AbstractService implements Runnable {

    private final TaskScheduler scheduler;
    private final String cronTrigger;
    private final boolean enabled;
    private final RemoveOldAuditDataUseCase removeOldAuditDataUseCase;
    private final OrganizationService organizationService;
    private final EnvironmentService environmentService;
    private final Duration maxAge;

    public ScheduledAuditCleanerService(
        @Qualifier("auditTaskScheduler") TaskScheduler scheduler,
        @Value("${services.audit.cron:0 1 * * * *}") String cronTrigger,
        @Value("${services.audit.enabled:false}") boolean enabled,
        @Value("${services.audit.retention.days:365}") int maxAgeInDays,
        RemoveOldAuditDataUseCase removeOldAuditDataUseCase,
        OrganizationService organizationService,
        EnvironmentService environmentService
    ) {
        this.scheduler = scheduler;
        this.cronTrigger = cronTrigger;
        this.enabled = enabled;
        this.removeOldAuditDataUseCase = removeOldAuditDataUseCase;
        this.organizationService = organizationService;
        this.environmentService = environmentService;
        maxAge = Duration.ofDays(maxAgeInDays);
    }

    @Override
    protected String name() {
        return "Subscriptions Refresher Service";
    }

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();
            log.info("Audit cleaner service has been initialized with cron [{}]", cronTrigger);
            scheduler.schedule(this, new CronTrigger(cronTrigger));
        } else {
            log.warn("Audit cleaner service has been disabled");
        }
    }

    @Override
    public void run() {
        var environments = stream(organizationService.findAll())
            .flatMap(o -> stream(environmentService.findByOrganization(o.getId())))
            .toList();

        for (var environment : environments) {
            log.info(
                "Start cleanup environment: {} ({}) from organisation {}",
                environment.getName(),
                environment.getId(),
                environment.getOrganizationId()
            );
            removeOldAuditDataUseCase.execute(new RemoveOldAuditDataUseCase.Input(environment.getId(), maxAge));
        }
    }
}
