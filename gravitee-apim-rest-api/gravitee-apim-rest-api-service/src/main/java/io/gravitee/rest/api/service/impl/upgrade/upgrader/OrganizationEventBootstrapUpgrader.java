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
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.rest.api.service.OrganizationService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Ensures every organization has a current runtime snapshot after latest-event migration. */
@Component
@CustomLog
public class OrganizationEventBootstrapUpgrader implements Upgrader {

    private final OrganizationRepository organizationRepository;
    private final EventLatestRepository eventLatestRepository;
    private final OrganizationService organizationService;

    @Autowired
    public OrganizationEventBootstrapUpgrader(
        @Lazy OrganizationRepository organizationRepository,
        @Lazy EventLatestRepository eventLatestRepository,
        OrganizationService organizationService
    ) {
        this.organizationRepository = organizationRepository;
        this.eventLatestRepository = eventLatestRepository;
        this.organizationService = organizationService;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.ORGANIZATION_EVENT_BOOTSTRAP_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return wrapException(() -> {
            organizationRepository
                .findAll()
                .forEach(organization -> {
                    boolean hasOrganizationSnapshot = eventLatestRepository
                        .findByOrganizationId(organization.getId())
                        .stream()
                        .anyMatch(event -> event.getType() == EventType.PUBLISH_ORGANIZATION);
                    if (!hasOrganizationSnapshot) {
                        log.info("Publishing missing organization runtime snapshot for {}", organization.getId());
                        organizationService.publishOrganization(organization.getId());
                    }
                });
            return true;
        });
    }
}
