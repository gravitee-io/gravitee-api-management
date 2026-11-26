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
package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal.domain_service.NavigationPortalDomainService;
import io.gravitee.apim.core.portal.exception.NavigationPortalNotResolvedException;
import io.gravitee.apim.core.portal_page.domain_service.PortalStarterContentDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class CreateDefaultPortalNavigationItemsUseCase {

    private final PortalStarterContentDomainService portalStarterContentDomainService;
    private final NavigationPortalDomainService navigationPortalDomainService;

    /**
     * Creates the starter navigation items, and the homepage, the environment's default portal is still missing (see
     * {@link PortalStarterContentDomainService#seed}).
     * <p>
     * Nothing is created in an environment without a default portal (see {@link #defaultPortalOf}).
     */
    public void execute(String organizationId, String environmentId) {
        defaultPortalOf(organizationId, environmentId).ifPresent(owner ->
            portalStarterContentDomainService.seed(organizationId, environmentId, owner)
        );
    }

    /** Creates the "APIs" section when the environment's default portal has none. */
    public void ensureApisSection(String organizationId, String environmentId) {
        defaultPortalOf(organizationId, environmentId).ifPresent(owner ->
            portalStarterContentDomainService.ensureApisSection(organizationId, environmentId, owner)
        );
    }

    /**
     * Seeding runs at start-up and on environment creation, so a refusal must not stop it: the navigation of an
     * Automation API portal belongs to the Automation API, and a postponed default portal gets an empty
     * navigation that admins fill once its URL is fixed.
     */
    private Optional<NavigationItemReference.PortalReference> defaultPortalOf(String organizationId, String environmentId) {
        try {
            return Optional.of(navigationPortalDomainService.resolveReference(organizationId, environmentId));
        } catch (NavigationPortalNotResolvedException e) {
            log.warn("Default navigation of environment [{}] not created: {}", environmentId, e.getMessage());
            return Optional.empty();
        }
    }
}
