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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.PORTAL_NOTIFICATION_CONFIG_UPGRADER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PortalNotificationConfigUpgrader implements Upgrader {

    EnvironmentRepository environmentRepository;

    PortalNotificationConfigRepository portalNotificationConfigRepository;

    public PortalNotificationConfigUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy PortalNotificationConfigRepository portalNotificationConfigRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.portalNotificationConfigRepository = portalNotificationConfigRepository;
    }

    @Override
    public boolean upgrade() {
        try {
            Set<Environment> environments = environmentRepository.findAll();

            List<PortalNotificationConfig> portalNotificationConfigs = portalNotificationConfigRepository
                .findAll()
                .stream()
                .filter(portalNotificationConfig ->
                    portalNotificationConfig.getReferenceType().equals(NotificationReferenceType.PORTAL) &&
                    portalNotificationConfig.getReferenceId().equals("DEFAULT")
                )
                .flatMap(
                    (Function<PortalNotificationConfig, Stream<PortalNotificationConfig>>) portalNotificationConfig ->
                        migratePortalNotificationConfigToEnvironments(environments, portalNotificationConfig)
                )
                .toList();
            log.info("Migrating portalNotificationConfig: {} for environments {}", portalNotificationConfigs.size(), environments);
        } catch (Exception e) {
            log.error("Failed to apply {}", getClass().getSimpleName(), e);
            return false;
        }
        return true;
    }

    private Stream<PortalNotificationConfig> migratePortalNotificationConfigToEnvironments(
        Set<Environment> environments,
        PortalNotificationConfig portalNotificationConfig
    ) {
        Stream<PortalNotificationConfig> stream = environments
            .stream()
            .map(environment -> {
                PortalNotificationConfig portalNotificationConfigToCreate = duplicate(environment, portalNotificationConfig);
                try {
                    return portalNotificationConfigRepository.create(portalNotificationConfigToCreate);
                } catch (TechnicalException e) {
                    log.error("Failed to duplicate portalNotificationConfig {} to {}", portalNotificationConfig, environment, e);
                    throw new TechnicalManagementException(e);
                }
            });
        try {
            portalNotificationConfigRepository.delete(portalNotificationConfig);
        } catch (TechnicalException e) {
            log.error("Failed to delete portalNotificationConfig {}", portalNotificationConfig, e);
            throw new TechnicalManagementException(e);
        }
        return stream;
    }

    private PortalNotificationConfig duplicate(Environment environment, PortalNotificationConfig portalNotificationConfig) {
        return PortalNotificationConfig
            .builder()
            .referenceId(environment.getId())
            .referenceType(NotificationReferenceType.ENVIRONMENT)
            .hooks(portalNotificationConfig.getHooks())
            .user(portalNotificationConfig.getUser())
            .createdAt(portalNotificationConfig.getCreatedAt())
            .updatedAt(portalNotificationConfig.getUpdatedAt())
            .build();
    }

    @Override
    public int getOrder() {
        return PORTAL_NOTIFICATION_CONFIG_UPGRADER;
    }
}
