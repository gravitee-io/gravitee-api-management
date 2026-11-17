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

import static fixtures.core.model.PortalNavigationItemFixtures.PORTAL_REFERENCE;

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.net.URI;
import lombok.SneakyThrows;

final class PortalNavigationUseCaseTestFixtures {

    private PortalNavigationUseCaseTestFixtures() {}

    @SneakyThrows
    static PortalNavigationAccessDomainService access(
        String organizationId,
        String environmentId,
        PortalNavigationItemsQueryService navigation,
        PortalPageContentQueryService contents,
        ApiCrudServiceInMemory apis
    ) {
        var portals = new PortalCrudServiceInMemory();
        portals.create(
            Portal.of(
                ((NavigationItemReference.PortalReference) PORTAL_REFERENCE).portalId(),
                environmentId,
                organizationId,
                "Developer Portal",
                PortalNavigationStructure.empty(),
                null,
                true,
                URI.create("https://portal.example.com").toURL()
            )
        );
        return new PortalNavigationAccessDomainService(
            portals,
            apis,
            navigation,
            contents,
            new ApiOwnedNavigationDomainService(navigation, new PortalNavigationItemsCrudServiceInMemory()),
            new ApiQueryServiceInMemory(apis)
        );
    }
}
