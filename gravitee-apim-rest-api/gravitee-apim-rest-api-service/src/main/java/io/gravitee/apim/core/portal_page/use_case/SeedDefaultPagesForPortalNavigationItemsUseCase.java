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
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationDefaultPageDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SeedDefaultPagesForPortalNavigationItemsUseCase {

    private final PortalNavigationAccessDomainService accessService;

    private final PortalNavigationDefaultPageDomainService defaultPageDomainService;

    public Output execute(Input input) {
        var scope = new NavigationItemReference.PortalReference(input.portalId());
        accessService.requireScope(input.organizationId(), input.environmentId(), scope);
        for (var id : input.navigationItemIds()) {
            var item = accessService.requireItem(input.organizationId(), input.environmentId(), scope, id);
            accessService.requireSubtree(input.organizationId(), input.environmentId(), scope, item, true);
        }
        return new Output(
            defaultPageDomainService.seedDefaultPages(input.organizationId(), input.environmentId(), input.navigationItemIds())
        );
    }

    public record Input(String organizationId, String environmentId, List<PortalNavigationItemId> navigationItemIds, PortalId portalId) {
        public Input {
            Objects.requireNonNull(portalId, "portalId");
        }
    }

    public record Output(List<PortalNavigationItemId> seededNavigationItemIds) {}
}
