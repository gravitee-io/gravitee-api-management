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
package io.gravitee.rest.api.service.impl;

import io.gravitee.apim.core.application_certificate.use_case.ProcessPendingCertificateTransitionsUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.common.service.AbstractService;
import io.gravitee.rest.api.service.ScheduledCertificateRevocationService;
import java.util.concurrent.ScheduledFuture;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class ScheduledCertificateRevocationServiceImpl
    extends AbstractService<ScheduledCertificateRevocationServiceImpl>
    implements ScheduledCertificateRevocationService<ScheduledCertificateRevocationServiceImpl> {

    private final TaskScheduler scheduler;
    private final String cronTrigger;
    private final boolean enabled;
    private final ProcessPendingCertificateTransitionsUseCase processPendingCertificateTransitionsUseCase;
    private static final AuditActor SYSTEM_ACTOR = AuditActor.builder().userId("system").build();

    private ScheduledFuture<?> scheduledFuture;
    private boolean started = false;

    public ScheduledCertificateRevocationServiceImpl(
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
            if (started) {
                return;
            }
            started = true;
            super.doStart();
            try {
                scheduledFuture = scheduler.schedule(this::executeRevocation, new CronTrigger(cronTrigger));
                log.info("Certificate Revocation service has been initialized with cron [{}]", cronTrigger);
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
    protected void doStop() throws Exception {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        super.doStop();
    }

    void executeRevocation() {
        if (!enabled) {
            log.debug("Certificate revocation processing is disabled");
            return;
        }
        log.debug("Certificate revocation processing started");

        try {
            processPendingCertificateTransitionsUseCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(SYSTEM_ACTOR));
        } catch (Exception e) {
            log.error("Error during certificate revocation processing", e);
        }

        log.debug("Certificate revocation processing ended");
    }
}
