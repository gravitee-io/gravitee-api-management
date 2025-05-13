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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.PORTAL_NOTIFICATION_CONFIG_ORIGIN_UPGRADER;

import io.gravitee.definition.model.Origin;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PortalNotificationConfigOriginUpgrader implements Upgrader {

    EnvironmentRepository environmentRepository;

    PortalNotificationConfigRepository portalNotificationConfigRepository;

    public PortalNotificationConfigOriginUpgrader(@Lazy PortalNotificationConfigRepository portalNotificationConfigRepository) {
        this.portalNotificationConfigRepository = portalNotificationConfigRepository;
    }

    @Override
    public boolean upgrade() {
        try {
            List<PortalNotificationConfig> portalNotificationConfigs = portalNotificationConfigRepository
                .findAll()
                .stream()
                .filter(portalNotificationConfig -> portalNotificationConfig.getOrigin() == null)
                .map(portalNotificationConfig -> {
                    portalNotificationConfig.setOrigin(Origin.MANAGEMENT);
                    try {
                        portalNotificationConfigRepository.update(portalNotificationConfig);
                    } catch (TechnicalException e) {
                        throw new TechnicalManagementException(e);
                    }
                    return portalNotificationConfig;
                })
                .toList();
            log.info("Migrating portalNotificationConfig: {} with origin set", portalNotificationConfigs.size());
        } catch (Exception e) {
            log.error("Error applying upgrader", e);
            return false;
        }
        return true;
    }

    @Override
    public int getOrder() {
        return PORTAL_NOTIFICATION_CONFIG_ORIGIN_UPGRADER;
    }
}
