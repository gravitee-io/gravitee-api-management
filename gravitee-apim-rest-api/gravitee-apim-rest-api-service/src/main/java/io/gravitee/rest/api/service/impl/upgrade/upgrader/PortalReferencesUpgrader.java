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

import io.gravitee.apim.core.portal.domain_service.PortalContentOwnershipDomainService;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Moves the navigation items and listings that name no existing portal of their environment onto its
 * portal. An environment without a usable target is skipped: without a portal, its content follows the first
 * one created; with several and none default, it stays where it is.
 */
@Component
@CustomLog
public class PortalReferencesUpgrader implements Upgrader {

    private final EnvironmentRepository environmentRepository;
    private final PortalContentOwnershipDomainService portalContentOwnershipDomainService;

    public PortalReferencesUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        PortalContentOwnershipDomainService portalContentOwnershipDomainService
    ) {
        this.environmentRepository = environmentRepository;
        this.portalContentOwnershipDomainService = portalContentOwnershipDomainService;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.PORTAL_REFERENCES_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        for (var environment : environmentRepository.findAll()) {
            var environmentId = environment.getId();
            portalContentOwnershipDomainService
                .moveOrphans(environmentId)
                .ifPresentOrElse(
                    moved -> logMoved(environmentId, moved),
                    () ->
                        log.warn(
                            "Environment [{}] skipped: it has no portal yet, or several portals and none is the default one. Its navigation items and listings stay as they are: without a portal, they follow the first one created; with several, they need a default portal to be set",
                            environmentId
                        )
                );
        }
        return true;
    }

    private void logMoved(String environmentId, PortalContentOwnershipDomainService.Moved moved) {
        if (!moved.isEmpty()) {
            log.info(
                "Moved {} navigation item(s) and {} listing(s) of environment [{}] onto its portal",
                moved.navigationItems(),
                moved.listings(),
                environmentId
            );
        }
    }
}
