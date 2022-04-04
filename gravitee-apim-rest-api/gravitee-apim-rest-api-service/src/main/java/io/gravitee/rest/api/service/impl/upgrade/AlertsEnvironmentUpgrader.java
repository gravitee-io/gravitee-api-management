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
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.settings.Alert;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class AlertsEnvironmentUpgrader extends OneShotUpgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertsEnvironmentUpgrader.class);

    private final AlertTriggerRepository alertTriggerRepository;

    private final EnvironmentRepository environmentRepository;

    @Autowired
    public AlertsEnvironmentUpgrader(AlertTriggerRepository alertTriggerRepository, EnvironmentRepository environmentRepository) {
        super(InstallationService.ALERTS_ENVIRONMENT_UPGRADER);
        this.alertTriggerRepository = alertTriggerRepository;
        this.environmentRepository = environmentRepository;
    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    protected void processOneShotUpgrade(ExecutionContext executionContext) throws TechnicalException {
        alertTriggerRepository
            .findAll()
            .stream()
            .filter(alert -> "PLATFORM".equals(alert.getReferenceType()))
            .forEach(this::handleEnvironmentAlert);
    }

    private void handleEnvironmentAlert(AlertTrigger alertTrigger) {
        try {
            // update the current alert : set it the default environment
            alertTrigger.setReferenceType(AlertReferenceType.ENVIRONMENT.name());
            alertTrigger.setReferenceId(GraviteeContext.getDefaultEnvironment());
            alertTriggerRepository.update(alertTrigger);

            // duplicate it on all others environments
            environmentRepository
                .findAll()
                .stream()
                .filter(environment -> !GraviteeContext.getDefaultEnvironment().equals(environment.getId()))
                .forEach(
                    environment -> {
                        try {
                            createAlertTrigger(environment, alertTrigger);
                        } catch (TechnicalException e) {
                            LOGGER.error("Failed to duplicate alert {} to environment {}", alertTrigger.getId(), environment.getId(), e);
                        }
                    }
                );
        } catch (TechnicalException e) {
            LOGGER.error("Failed to handle alert {}", alertTrigger.getId(), e);
        }
    }

    private void createAlertTrigger(Environment environment, AlertTrigger alertTrigger) throws TechnicalException {
        AlertTrigger newAlertTrigger = new AlertTrigger(alertTrigger);
        newAlertTrigger.setId(null);
        newAlertTrigger.setReferenceId(environment.getId());
        alertTriggerRepository.create(newAlertTrigger);
    }
}
