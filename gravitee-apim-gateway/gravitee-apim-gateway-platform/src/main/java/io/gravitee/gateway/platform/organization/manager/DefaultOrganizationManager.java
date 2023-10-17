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
package io.gravitee.gateway.platform.organization.manager;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.platform.organization.event.OrganizationEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultOrganizationManager implements OrganizationManager {

    private final Map<String, ReactableOrganization> organizations = new ConcurrentHashMap<>();
    private final EventManager eventManager;

    @Override
    public boolean register(final ReactableOrganization reactableOrganization) {
        if (isRegistrable(reactableOrganization)) {
            log.info("Register organization {}", reactableOrganization);
            organizations.put(reactableOrganization.getId(), reactableOrganization);
            eventManager.publishEvent(OrganizationEvent.REGISTER, reactableOrganization);
            return true;
        }
        return false;
    }

    private boolean isRegistrable(final ReactableOrganization reactableOrganization) {
        ReactableOrganization registeredReactableOrganization = organizations.get(reactableOrganization.getId());
        return (
            registeredReactableOrganization == null ||
            !registeredReactableOrganization.equals(reactableOrganization) ||
            registeredReactableOrganization.getDeployedAt() == null ||
            registeredReactableOrganization.getDeployedAt().before(reactableOrganization.getDeployedAt())
        );
    }

    @Override
    public void unregister(String organizationId) {
        ReactableOrganization removed = organizations.remove(organizationId);
        if (removed != null) {
            log.info("Unregister organization {}", organizationId);
            eventManager.publishEvent(OrganizationEvent.UNREGISTER, removed);
        }
    }

    public ReactableOrganization getOrganization(final String organizationId) {
        if (organizationId != null) {
            return organizations.get(organizationId);
        }
        return null;
    }
}
