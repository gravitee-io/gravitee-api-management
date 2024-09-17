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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.GENERIC_NOTIFICATION_CONFIG_UPGRADER;
import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.PORTAL_NOTIFICATION_CONFIG_UPGRADER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.service.common.UuidString;
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
public class GenericNotificationConfigUpgrader implements Upgrader {

    EnvironmentRepository environmentRepository;

    GenericNotificationConfigRepository genericNotificationConfigRepository;

    public GenericNotificationConfigUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy GenericNotificationConfigRepository genericNotificationConfigRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.genericNotificationConfigRepository = genericNotificationConfigRepository;
    }

    @Override
    public boolean upgrade() {
        try {
            Set<Environment> environments = environmentRepository.findAll();

            List<GenericNotificationConfig> genericNotificationConfigs = genericNotificationConfigRepository
                .findAll()
                .stream()
                .filter(genericNotificationConfig ->
                    genericNotificationConfig.getReferenceType().equals(NotificationReferenceType.PORTAL) &&
                    genericNotificationConfig.getReferenceId().equals("DEFAULT")
                )
                .flatMap(
                    (Function<GenericNotificationConfig, Stream<GenericNotificationConfig>>) genericNotificationConfig ->
                        migrateGenericNotificationConfigToEnvironments(environments, genericNotificationConfig)
                )
                .toList();
            log.info("Migrating genericNotificationConfig: {} for environments {}", genericNotificationConfigs.size(), environments);
        } catch (Exception e) {
            log.error("Failed to apply {}", getClass().getSimpleName(), e);
            return false;
        }
        return true;
    }

    private Stream<GenericNotificationConfig> migrateGenericNotificationConfigToEnvironments(
        Set<Environment> environments,
        GenericNotificationConfig genericNotificationConfig
    ) {
        Stream<GenericNotificationConfig> stream = environments
            .stream()
            .map(environment -> {
                GenericNotificationConfig genericNotificationConfigToCreate = duplicate(environment, genericNotificationConfig);
                try {
                    return genericNotificationConfigRepository.create(genericNotificationConfigToCreate);
                } catch (TechnicalException e) {
                    log.error("Failed to duplicate portalNotificationConfig {} to {}", genericNotificationConfig, environment, e);
                    throw new TechnicalManagementException(e);
                }
            });
        try {
            genericNotificationConfigRepository.delete(genericNotificationConfig.getId());
        } catch (TechnicalException e) {
            log.error("Failed to delete genericNotificationConfig {}", genericNotificationConfig, e);
            throw new TechnicalManagementException(e);
        }
        return stream;
    }

    private GenericNotificationConfig duplicate(Environment environment, GenericNotificationConfig genericNotificationConfig) {
        return GenericNotificationConfig
            .builder()
            .id(UuidString.generateRandom())
            .config(genericNotificationConfig.getConfig())
            .notifier(genericNotificationConfig.getNotifier())
            .useSystemProxy(genericNotificationConfig.isUseSystemProxy())
            .referenceId(environment.getId())
            .referenceType(NotificationReferenceType.ENVIRONMENT)
            .hooks(genericNotificationConfig.getHooks())
            .createdAt(genericNotificationConfig.getCreatedAt())
            .updatedAt(genericNotificationConfig.getUpdatedAt())
            .build();
    }

    @Override
    public int getOrder() {
        return GENERIC_NOTIFICATION_CONFIG_UPGRADER;
    }
}
