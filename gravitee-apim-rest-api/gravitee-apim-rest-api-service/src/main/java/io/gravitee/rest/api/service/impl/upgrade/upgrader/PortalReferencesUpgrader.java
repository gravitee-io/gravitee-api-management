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

import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.domain_service.PortalContentOwnershipDomainService;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Moves the navigation items that name no existing portal of their environment onto its
 * portal. An environment without a usable target is skipped. Without a portal, its content moves when its
 * first portal is created. With several portals and none default, its content stays where it is: nothing
 * moves it later, so it needs a manual fix before PORTAL-345 removes the zero UUID reference.
 */
@Component
@CustomLog
public class PortalReferencesUpgrader implements Upgrader {

    private final EnvironmentRepository environmentRepository;
    private final PortalContentOwnershipDomainService portalContentOwnershipDomainService;
    private final PortalCrudService portalCrudService;

    public PortalReferencesUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        PortalContentOwnershipDomainService portalContentOwnershipDomainService,
        PortalCrudService portalCrudService
    ) {
        this.environmentRepository = environmentRepository;
        this.portalContentOwnershipDomainService = portalContentOwnershipDomainService;
        this.portalCrudService = portalCrudService;
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
                .ifPresentOrElse(moved -> logMoved(environmentId, moved), () -> logSkipped(environmentId));
        }
        return true;
    }

    private void logSkipped(String environmentId) {
        if (portalCrudService.findByEnvironmentId(environmentId).isEmpty()) {
            log.warn(
                "Environment [{}] skipped: it has no portal yet. Its navigation items will move onto its first portal when it is created",
                environmentId
            );
        } else {
            log.warn(
                "Environment [{}] skipped: it has several portals and no default one. Its navigation items that name no existing portal stay as they are and need a manual fix: re-point them onto one of its portals",
                environmentId
            );
        }
    }

    private void logMoved(String environmentId, PortalContentOwnershipDomainService.Moved moved) {
        if (!moved.isEmpty()) {
            log.info("Moved {} navigation item(s) of environment [{}] onto its portal", moved.navigationItems(), environmentId);
        }
    }
}
