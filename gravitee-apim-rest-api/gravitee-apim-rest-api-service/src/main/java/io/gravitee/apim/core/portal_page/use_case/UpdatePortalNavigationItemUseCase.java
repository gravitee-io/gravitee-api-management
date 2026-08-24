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
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdatePortalNavigationItemUseCase {

    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationItemValidatorService validatorService;
    private final PortalNavigationItemDomainService domainService;
    private final PortalNavigationItemSourceDomainService sourceDomainService;

    public Output execute(Input input) {
        var toUpdate = input.updatePortalNavigationItem;
        PortalNavigationItem existing = portalNavigationItemsQueryService.findByIdAndEnvironmentId(
            input.environmentId(),
            PortalNavigationItemId.of(input.navigationItemId)
        );
        if (existing == null) {
            throw new PortalNavigationItemNotFoundException(input.navigationItemId);
        }
        // Restore the masked secrets before validation: the plugin must validate the configuration it will be given
        if (existing.getSource() != null && toUpdate.getSource() != null) {
            sourceDomainService.mergeSensitiveData(existing.getSource(), toUpdate.getSource());
        }
        // The import marker is server-owned: carried over from the stored source, never taken from a payload
        if (toUpdate.getSource() != null) {
            toUpdate.getSource().setSubtreeImport(existing.getSource() != null && existing.getSource().isSubtreeImport());
        }
        validatorService.validateToUpdate(toUpdate, existing);
        var updatedItem = domainService.update(toUpdate, existing, input.propagatePublishToChildren());
        if (updatedItem.getSource() != null) {
            sourceDomainService.removeSensitiveData(updatedItem.getSource());
        }
        return new Output(updatedItem);
    }

    @Builder
    public record Input(
        String organizationId,
        String environmentId,
        String navigationItemId,
        UpdatePortalNavigationItem updatePortalNavigationItem,
        boolean propagatePublishToChildren
    ) {}

    public record Output(PortalNavigationItem updatedItem) {}
}
