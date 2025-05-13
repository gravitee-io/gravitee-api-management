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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.AlertTriggerConverter;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Before this upgrader runs :
 *  - environment_id in alertTrigger collection are not set
 *  - PLATFORM alerts, edited at environment level, are shared across all environments
 *
 * For each platform alert, this upgrader will :
 *  - update its referenceType to ENVIRONMENT
 *  - update its referenceId to the DEFAULT environment id
 *
 * For each application or api alert, this upgrader will :
 *  - find the related api or application, to get its environment id
 *  - update alert environmentId in alertTrigger collection
 *
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class AlertsEnvironmentUpgrader implements Upgrader {

    private final AlertTriggerRepository alertTriggerRepository;

    private final AlertTriggerConverter alertTriggerConverter;

    private final ApiRepository apiRepository;

    private final ApplicationRepository applicationRepository;

    @Autowired
    public AlertsEnvironmentUpgrader(
        @Lazy AlertTriggerRepository alertTriggerRepository,
        AlertTriggerConverter alertTriggerConverter,
        @Lazy ApiRepository apiRepository,
        @Lazy ApplicationRepository applicationRepository
    ) {
        this.alertTriggerRepository = alertTriggerRepository;
        this.alertTriggerConverter = alertTriggerConverter;
        this.apiRepository = apiRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.ALERTS_ENVIRONMENT_UPGRADER;
    }

    @Override
    public boolean upgrade() {
        try {
            Set<AlertTrigger> alertTriggers = alertTriggerRepository.findAll();
            for (AlertTrigger alertTrigger : alertTriggers) {
                switch (alertTrigger.getReferenceType()) {
                    case "PLATFORM":
                        handlePlatformAlert(alertTrigger);
                        break;
                    case "API":
                        handleApiAlert(alertTrigger);
                        break;
                    case "APPLICATION":
                        handleApplicationAlert(alertTrigger);
                        break;
                    default:
                        log.error("unsupported reference type {}", alertTrigger.getReferenceType());
                        break;
                }
            }
        } catch (Exception e) {
            log.error("Error applying upgrader", e);
            return false;
        }

        return true;
    }

    private void handlePlatformAlert(AlertTrigger alertTrigger) {
        try {
            updateEnvironmentAlertReferences(GraviteeContext.getDefaultEnvironment(), alertTrigger);
        } catch (TechnicalException e) {
            log.error("Failed to handle alert {}", alertTrigger.getId(), e);
        }
    }

    private void handleApiAlert(AlertTrigger alertTrigger) throws TechnicalException {
        Api api = apiRepository
            .findById(alertTrigger.getReferenceId())
            .orElseThrow(() -> new ApiNotFoundException(alertTrigger.getReferenceId()));
        updateAlertEnvironment(api.getEnvironmentId(), alertTrigger);
    }

    private void handleApplicationAlert(AlertTrigger alertTrigger) throws TechnicalException {
        Application application = applicationRepository
            .findById(alertTrigger.getReferenceId())
            .orElseThrow(() -> new ApplicationNotFoundException(alertTrigger.getReferenceId()));
        updateAlertEnvironment(application.getEnvironmentId(), alertTrigger);
    }

    private void updateAlertEnvironment(String environmentId, AlertTrigger alertTrigger) throws TechnicalException {
        AlertTriggerEntity alertTriggerEntity = alertTriggerConverter.toAlertTriggerEntity(alertTrigger);
        alertTriggerEntity.setEnvironmentId(environmentId);
        alertTriggerRepository.update(alertTriggerConverter.toAlertTrigger(alertTriggerEntity));
    }

    private void updateEnvironmentAlertReferences(String environmentId, AlertTrigger alertTrigger) throws TechnicalException {
        alertTrigger.setReferenceType(AlertReferenceType.ENVIRONMENT.name());
        AlertTriggerEntity alertTriggerEntity = alertTriggerConverter.toAlertTriggerEntity(alertTrigger);
        alertTriggerEntity.setReferenceType(AlertReferenceType.ENVIRONMENT);
        alertTriggerEntity.setReferenceId(environmentId);
        alertTriggerEntity.setEnvironmentId(environmentId);
        alertTriggerRepository.update(alertTriggerConverter.toAlertTrigger(alertTriggerEntity));
    }
}
