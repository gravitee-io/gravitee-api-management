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
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeletePortalNavigationItemUseCase {

    private final PortalNavigationAccessDomainService accessService;

    private final PortalNavigationItemDomainService portalNavigationItemDomainService;
    private final PortalNavigationSourcedItemsDomainService sourcedItemsDomainService;

    public Output execute(Input input) {
        var scope = input.scope();
        var existing = accessService.requireItem(input.organizationId(), input.environmentId(), scope, input.navigationItemId());
        accessService
            .requireSubtree(input.organizationId(), input.environmentId(), scope, existing, false)
            .stream()
            .filter(PortalNavigationPage.class::isInstance)
            .map(PortalNavigationPage.class::cast)
            .forEach(page ->
                accessService.checkContentAccess(input.organizationId(), input.environmentId(), scope, page.getPortalPageContentId())
            );
        // What is below a synced item is managed by it, and goes only with it.
        if (sourcedItemsDomainService.findSourcedAncestor(input.environmentId(), existing).isPresent()) {
            throw InvalidPortalNavigationItemDataException.childOfSourcedItemIsReadOnly(existing.getId().json());
        }

        portalNavigationItemDomainService.deleteWithDescendants(existing);

        return new Output();
    }

    public record Output() {}

    public record Input(
        String organizationId,
        String environmentId,
        PortalNavigationItemId navigationItemId,
        NavigationItemReference scope
    ) {
        public Input {
            Objects.requireNonNull(scope, "scope");
        }
    }
}
