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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.ADD_ORG_ID_NOTIFICATION_CONFIGS_UPGRADER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AddOrgIdNotificationConfigsUpgrader implements Upgrader {

    private final EnvironmentRepository environmentRepository;
    private final ApiRepository apiRepository;
    private final ApplicationRepository applicationRepository;
    private final GenericNotificationConfigRepository genericNotificationConfigRepository;
    private final PortalNotificationConfigRepository portalNotificationConfigRepository;
    Set<Environment> environments;
    Set<Api> apis;
    Set<Application> applications;

    public AddOrgIdNotificationConfigsUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy ApiRepository apiRepository,
        @Lazy ApplicationRepository applicationRepository,
        @Lazy GenericNotificationConfigRepository genericNotificationConfigRepository,
        @Lazy PortalNotificationConfigRepository portalNotificationConfigRepository
    ) throws TechnicalException {
        this.environmentRepository = environmentRepository;
        this.apiRepository = apiRepository;
        this.applicationRepository = applicationRepository;
        this.genericNotificationConfigRepository = genericNotificationConfigRepository;
        this.portalNotificationConfigRepository = portalNotificationConfigRepository;
    }

    private String getEnvironmentOrgId(String envId) throws TechnicalException {
        Optional<Environment> environment = environmentRepository.findById(envId);
        return environment.map(Environment::getOrganizationId).orElse(null);
    }

    private String getApiOrgId(String apiId) throws TechnicalException {
        Optional<Api> api = apiRepository.findById(apiId);
        if (api.isPresent()) {
            String envId = api.get().getEnvironmentId();
            return getEnvironmentOrgId(envId);
        }
        return null;
    }

    private String getApplicationOrgId(String apiId) throws TechnicalException {
        Optional<Application> application = applicationRepository.findById(apiId);
        if (application.isPresent()) {
            String envId = application.get().getEnvironmentId();
            return getEnvironmentOrgId(envId);
        }
        return null;
    }

    private String getOrgId(String refId, NotificationReferenceType referenceType) throws TechnicalException {
        if (referenceType == NotificationReferenceType.ENVIRONMENT) {
            return getEnvironmentOrgId(refId);
        }
        if (referenceType == NotificationReferenceType.API) {
            return getApiOrgId(refId);
        }
        if (referenceType == NotificationReferenceType.APPLICATION) {
            return getApplicationOrgId(refId);
        }
        return null;
    }

    @Override
    public boolean upgrade() {
        try {
            var genericNotificationConfigs = genericNotificationConfigRepository.findAll();
            for (GenericNotificationConfig genericNotificationConfig : genericNotificationConfigs) {
                if (genericNotificationConfig.getReferenceType() == NotificationReferenceType.PORTAL) {
                    genericNotificationConfig.setReferenceType(NotificationReferenceType.ENVIRONMENT);
                }
                String orgId = getOrgId(genericNotificationConfig.getReferenceId(), genericNotificationConfig.getReferenceType());
                genericNotificationConfig.setOrganizationId(orgId);
                genericNotificationConfigRepository.update(genericNotificationConfig);
            }
            log.info("Migrating {} genericNotificationConfigs -> adding orgId value", genericNotificationConfigs.size());

            var portalNotificationConfigs = portalNotificationConfigRepository.findAll();
            for (PortalNotificationConfig portalNotificationConfig : portalNotificationConfigs) {
                if (portalNotificationConfig.getReferenceType() == NotificationReferenceType.PORTAL) {
                    portalNotificationConfig.setReferenceType(NotificationReferenceType.ENVIRONMENT);
                }
                String orgId = getOrgId(portalNotificationConfig.getReferenceId(), portalNotificationConfig.getReferenceType());
                portalNotificationConfig.setOrganizationId(orgId);
                portalNotificationConfigRepository.update(portalNotificationConfig);
            }
            log.info("Migrating {} portalNotificationConfigs -> adding orgId value", portalNotificationConfigs.size());

            return true;
        } catch (Exception e) {
            log.error("Failed to apply {}", getClass().getSimpleName(), e);
            return false;
        }
    }

    @Override
    public int getOrder() {
        return ADD_ORG_ID_NOTIFICATION_CONFIGS_UPGRADER;
    }
}
