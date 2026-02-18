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
package io.gravitee.rest.api.services.certificate.revocation;

import io.gravitee.apim.core.application_certificate.use_case.ProcessPendingCertificateTransitionsUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.common.service.AbstractService;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public class ScheduledCertificateRevocationService extends AbstractService implements Runnable {

    private final TaskScheduler scheduler;
    private final String cronTrigger;
    private final boolean enabled;
    private final ProcessPendingCertificateTransitionsUseCase processPendingCertificateTransitionsUseCase;
    private final AtomicLong counter = new AtomicLong(0);

    public ScheduledCertificateRevocationService(
        @Qualifier("certificateRevocationTaskScheduler") TaskScheduler scheduler,
        @Value("${services.certificate-revocation.cron:0 0 0 * * *}") String cronTrigger,
        @Value("${services.certificate-revocation.enabled:true}") boolean enabled,
        ProcessPendingCertificateTransitionsUseCase processPendingCertificateTransitionsUseCase
    ) {
        this.scheduler = scheduler;
        this.cronTrigger = cronTrigger;
        this.enabled = enabled;
        this.processPendingCertificateTransitionsUseCase = processPendingCertificateTransitionsUseCase;
    }

    @Override
    protected String name() {
        return "Certificate Revocation Service";
    }

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();
            log.info("Certificate Revocation service has been initialized with cron [{}]", cronTrigger);
            try {
                scheduler.schedule(this, new CronTrigger(cronTrigger));
            } catch (IllegalArgumentException e) {
                log.error(
                    "Certificate Revocation service failed to start: invalid cron expression [{}] " +
                        "in property 'services.certificate-revocation.cron'. Service will not run.",
                    cronTrigger
                );
                throw e;
            }
        } else {
            log.warn("Certificate Revocation service has been disabled");
        }
    }

    @Override
    public void run() {
        log.debug("Certificate revocation #{} started at {}", counter.incrementAndGet(), Instant.now());

        try {
            processPendingCertificateTransitionsUseCase.execute(
                new ProcessPendingCertificateTransitionsUseCase.Input(AuditActor.builder().userId("system").build())
            );
        } catch (Exception e) {
            log.error("Error during certificate revocation processing", e);
        }

        log.debug("Certificate revocation #{} ended at {}", counter.get(), Instant.now());
    }
}
