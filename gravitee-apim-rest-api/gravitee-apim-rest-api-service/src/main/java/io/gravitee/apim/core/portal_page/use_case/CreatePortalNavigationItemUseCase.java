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
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationDefaultPageDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemCreationExpansionDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemRenderingDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class CreatePortalNavigationItemUseCase {

    private final PortalNavigationAccessDomainService accessService;
    private final ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;

    private final PortalNavigationItemDomainService domainService;
    private final PortalNavigationItemValidatorService validatorService;
    private final PortalNavigationItemCreationExpansionDomainService creationExpansionDomainService;
    private final PortalNavigationDefaultPageDomainService defaultPageDomainService;
    private final PortalNavigationItemSourceDomainService sourceDomainService;
    private final PortalNavigationItemRenderingDomainService renderingDomainService;

    public Output execute(Input input) {
        var scope = input.scope();
        accessService.requireScope(input.organizationId(), input.environmentId(), scope);
        accessService.requireCreateParents(input.organizationId(), input.environmentId(), scope, List.of(input.item()));
        var item = input.item().toBuilder().reference(scope).build();
        if (scope instanceof NavigationItemReference.ApiReference api) {
            item = apiOwnedNavigationDomainService.claimForApi(input.environmentId(), api.apiId(), item);
        }
        return createItem(new Input(input.organizationId(), input.environmentId(), item, scope));
    }

    private Output createItem(Input input) {
        final var organizationId = input.organizationId();
        final var environmentId = input.environmentId();
        final var expansion = creationExpansionDomainService.expand(List.of(input.item()), environmentId);

        domainService.resolveOwners(organizationId, environmentId, expansion.itemsToCreate());
        validatorService.validateAll(expansion.itemsToCreate(), environmentId);
        var renderedParents = renderingDomainService.prepareRenderingParents(environmentId, expansion.itemsToCreate());

        var createdItems = domainService.createAll(organizationId, environmentId, expansion.itemsToCreate());

        var defaultPageNavigationItemIds = Stream.concat(
            createdItems.stream().filter(PortalNavigationApiProduct.class::isInstance).map(PortalNavigationItem::getId),
            expansion.generatedApiNavigationItemIds().stream()
        ).toList();
        defaultPageDomainService.seedDefaultPages(organizationId, environmentId, defaultPageNavigationItemIds);

        var renderedItems = renderingDomainService.renderCreatedItems(expansion.itemsToCreate(), createdItems, renderedParents);
        var createdItem = expansion.selectRequestedItems(renderedItems).getFirst();
        if (createdItem.getSource() != null) {
            sourceDomainService.removeSensitiveData(createdItem.getSource());
        }
        return new CreatePortalNavigationItemUseCase.Output(createdItem);
    }

    public record Input(String organizationId, String environmentId, CreatePortalNavigationItem item, NavigationItemReference scope) {
        public Input {
            Objects.requireNonNull(scope, "scope");
        }
    }

    public record Output(PortalNavigationItem item) {}
}
