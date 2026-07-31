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
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemCreationExpansionDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class CreatePortalNavigationItemUseCase {

    private final PortalNavigationItemDomainService domainService;
    private final PortalNavigationItemValidatorService validatorService;
    private final PortalNavigationItemCreationExpansionDomainService creationExpansionDomainService;

    public Output execute(Input input) {
        final var organizationId = input.organizationId();
        final var environmentId = input.environmentId();
        final var expansion = creationExpansionDomainService.expand(List.of(input.item()), environmentId);

        validatorService.validateAll(expansion.itemsToCreate(), environmentId);

        var createdItems = new ArrayList<PortalNavigationItem>(expansion.itemsToCreate().size());
        for (var itemToCreate : expansion.itemsToCreate()) {
            createdItems.add(domainService.create(organizationId, environmentId, itemToCreate));
        }

        return new CreatePortalNavigationItemUseCase.Output(expansion.selectRequestedItems(createdItems).getFirst());
    }

    public record Input(String organizationId, String environmentId, CreatePortalNavigationItem item) {}

    public record Output(PortalNavigationItem item) {}
}
