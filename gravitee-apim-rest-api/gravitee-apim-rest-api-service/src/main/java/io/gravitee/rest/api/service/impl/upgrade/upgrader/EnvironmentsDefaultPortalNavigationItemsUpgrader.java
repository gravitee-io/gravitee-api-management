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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.ENVIRONMENTS_DEFAULT_PORTAL_NAVIGATION_ITEMS_UPGRADER;

import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.use_case.CreateDefaultPortalNavigationItemsUseCase;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class EnvironmentsDefaultPortalNavigationItemsUpgrader implements Upgrader {

    private final EnvironmentRepository environmentRepository;
    private final CreateDefaultPortalNavigationItemsUseCase createDefaultPortalNavigationItemsUseCase;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    public EnvironmentsDefaultPortalNavigationItemsUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        CreateDefaultPortalNavigationItemsUseCase createDefaultPortalNavigationItemsUseCase,
        PortalNavigationItemsQueryService portalNavigationItemsQueryService
    ) {
        this.environmentRepository = environmentRepository;
        this.createDefaultPortalNavigationItemsUseCase = createDefaultPortalNavigationItemsUseCase;
        this.portalNavigationItemsQueryService = portalNavigationItemsQueryService;
    }

    @Override
    public int getOrder() {
        return ENVIRONMENTS_DEFAULT_PORTAL_NAVIGATION_ITEMS_UPGRADER;
    }

    /**
     * Bumped to re-run once: repairs environments left partially seeded by APIM-14865 (a classloader
     * bug that could abort seeding partway through, after creating the "Guides" folder but before the
     * Home Page).
     */
    @Override
    public String version() {
        return "v2";
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        for (final var environment : environmentRepository.findAll()) {
            // Only touch environments where seeding never completed (no Home Page yet). An environment
            // that has a Home Page was fully seeded at some point; if a user later deliberately removed
            // e.g. the "Guides" folder, re-running here must not resurrect it.
            var existingHomepage = portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea(
                environment.getId(),
                PortalArea.HOMEPAGE
            );
            if (existingHomepage.isEmpty()) {
                createDefaultPortalNavigationItemsUseCase.execute(environment.getOrganizationId(), environment.getId());
            }
        }
        return true;
    }
}
