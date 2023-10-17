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
package io.gravitee.gateway.reactive.platform.organization.reactor;

import io.gravitee.gateway.platform.organization.ReactableOrganization;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class OrganizationReactorRegistry {

    private final OrganizationReactorFactory organizationReactorFactory;

    private final Map<String, OrganizationReactor> registry = new ConcurrentHashMap<>();

    public OrganizationReactor get(final String organizationId) {
        log.debug("Retrieving an OrganizationReactor for organization: {}", organizationId);
        return registry.get(organizationId);
    }

    public void create(final ReactableOrganization reactableOrganization) {
        log.debug("Create a new OrganizationReactor for organization: {}", reactableOrganization.getId());
        try {
            OrganizationReactor organizationReactor = organizationReactorFactory.create(reactableOrganization);
            organizationReactor.start();
            OrganizationReactor previousOrganizationReactor = registry.put(organizationReactor.id(), organizationReactor);
            if (previousOrganizationReactor != null) {
                log.debug(
                    "The ReactableOrganization was already deployed; stopping previous OrganizationReactor for organization: {}",
                    reactableOrganization.getId()
                );
                previousOrganizationReactor.stop();
            }
        } catch (Exception ex) {
            log.error("Unable to create and start the new organization '{}' reactor", reactableOrganization.getId(), ex);
        }
    }

    public void remove(final ReactableOrganization reactableOrganization) {
        log.debug("Removing an OrganizationReactor for organization: {}", reactableOrganization.getId());
        try {
            OrganizationReactor organizationReactor = registry.remove(reactableOrganization.getId());
            if (organizationReactor != null) {
                organizationReactor.stop();
            }
        } catch (Exception ex) {
            log.error("Unable to remove and stop the organization '{}' reactor", reactableOrganization.getId(), ex);
        }
    }

    public void clear() {
        registry.forEach((id, organizationReactor) -> {
            try {
                organizationReactor.stop();
            } catch (Exception ex) {
                log.error("Unable to remove and stop the organization '{}' reactor", id, ex);
            }
        });
        registry.clear();
    }
}
