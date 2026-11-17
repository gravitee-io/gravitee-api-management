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
public class BulkCreatePortalNavigationItemUseCase {

    private final PortalNavigationAccessDomainService accessService;

    private final PortalNavigationItemDomainService domainService;
    private final PortalNavigationItemValidatorService validatorService;
    private final PortalNavigationItemCreationExpansionDomainService creationExpansionDomainService;
    private final PortalNavigationDefaultPageDomainService defaultPageDomainService;
    private final PortalNavigationItemSourceDomainService sourceDomainService;
    private final PortalNavigationItemRenderingDomainService renderingDomainService;

    public Output execute(Input input) {
        var scope = new NavigationItemReference.PortalReference(input.portalId());
        accessService.requireScope(input.organizationId(), input.environmentId(), scope);
        accessService.requireCreateParents(input.organizationId(), input.environmentId(), scope, input.items());
        var items = input
            .items()
            .stream()
            .map(item -> item.toBuilder().reference(scope).build())
            .toList();
        return createItems(new Input(input.organizationId(), input.environmentId(), items, input.portalId()));
    }

    private Output createItems(Input input) {
        final var organizationId = input.organizationId();
        final var environmentId = input.environmentId();
        final var expansion = creationExpansionDomainService.expand(input.items(), environmentId);

        domainService.resolveOwners(organizationId, environmentId, expansion.itemsToCreate());
        validatorService.validateAll(expansion.itemsToCreate(), environmentId);
        var renderedParents = renderingDomainService.prepareRenderingParents(environmentId, expansion.itemsToCreate());

        var result = domainService.createAll(organizationId, environmentId, expansion.itemsToCreate());

        var defaultPageNavigationItemIds = Stream.concat(
            result.stream().filter(PortalNavigationApiProduct.class::isInstance).map(PortalNavigationItem::getId),
            expansion.generatedApiNavigationItemIds().stream()
        ).toList();
        defaultPageDomainService.seedDefaultPages(organizationId, environmentId, defaultPageNavigationItemIds);

        var renderedItems = renderingDomainService.renderCreatedItems(expansion.itemsToCreate(), result, renderedParents);
        var createdItems = expansion.selectRequestedItems(renderedItems);
        createdItems
            .stream()
            .map(PortalNavigationItem::getSource)
            .filter(Objects::nonNull)
            .forEach(sourceDomainService::removeSensitiveData);

        return new BulkCreatePortalNavigationItemUseCase.Output(createdItems);
    }

    public record Input(String organizationId, String environmentId, List<CreatePortalNavigationItem> items, PortalId portalId) {
        public Input {
            Objects.requireNonNull(portalId, "portalId");
        }
    }

    public record Output(List<PortalNavigationItem> items) {}
}
