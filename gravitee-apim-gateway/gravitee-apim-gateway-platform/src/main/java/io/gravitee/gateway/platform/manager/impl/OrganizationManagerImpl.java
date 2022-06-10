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
package io.gravitee.gateway.platform.manager.impl;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.Policy;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.platform.Organization;
import io.gravitee.gateway.platform.manager.OrganizationEvent;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.jupiter.platform.PlatformPolicyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OrganizationManagerImpl implements OrganizationManager {

    private final Logger logger = LoggerFactory.getLogger(OrganizationManagerImpl.class);

    private final PlatformPolicyManager policyManager;

    private Organization currentOrganization;
    private final io.gravitee.gateway.platform.PlatformPolicyManager v3policyManager;
    private EventManager eventManager;

    @Autowired
    GatewayConfiguration gatewayConfiguration;

    public OrganizationManagerImpl(
        PlatformPolicyManager policyManager,
        io.gravitee.gateway.platform.PlatformPolicyManager v3policyManager,
        EventManager eventManager
    ) {
        this.policyManager = policyManager;
        this.v3policyManager = v3policyManager;
        this.eventManager = eventManager;
    }

    @Override
    public boolean register(Organization organization) {
        if (
            currentOrganization == null ||
            !currentOrganization.equals(organization) ||
            currentOrganization.getUpdatedAt() == null ||
            currentOrganization.getUpdatedAt().before(organization.getUpdatedAt())
        ) {
            if (
                currentOrganization == null ||
                (currentOrganization != organization && currentOrganization.getUpdatedAt().compareTo(organization.getUpdatedAt()) < 0)
            ) {
                logger.info("Register organization {}", organization);
                currentOrganization = organization;
                eventManager.publishEvent(OrganizationEvent.REGISTER, organization);
                policyManager.setDependencies(currentOrganization.dependencies(Policy.class));
                v3policyManager.setDependencies(currentOrganization.dependencies(Policy.class));
                return true;
            }
        }
        return false;
    }

    @Override
    public void unregister(String orgId) {
        logger.info("Unregister organization {}", orgId);
        eventManager.publishEvent(OrganizationEvent.UNREGISTER, currentOrganization);
        currentOrganization = null;
    }

    public Organization getCurrentOrganization() {
        return currentOrganization;
    }
}
