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
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.Objects;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdatePortalNavigationItemUseCase {

    private final PortalNavigationAccessDomainService accessService;

    private final PortalNavigationItemValidatorService validatorService;
    private final PortalNavigationItemDomainService domainService;
    private final PortalNavigationItemSourceDomainService sourceDomainService;

    public Output execute(Input input) {
        var scope = input.scope();
        var existing = accessService.requireItem(
            input.organizationId(),
            input.environmentId(),
            scope,
            PortalNavigationItemId.of(input.navigationItemId())
        );
        accessService.requireParent(input.organizationId(), input.environmentId(), scope, input.updatePortalNavigationItem().getParentId());
        accessService.requireSubtree(input.organizationId(), input.environmentId(), scope, existing, true);
        return updateItem(input, existing, scope instanceof NavigationItemReference.PortalReference portal ? portal : null);
    }

    private Output updateItem(Input input, PortalNavigationItem existing, NavigationItemReference.PortalReference portal) {
        var toUpdate = domainService.resolveDestination(
            input.updatePortalNavigationItem(),
            existing,
            input.scope() instanceof NavigationItemReference.ApiReference,
            portal
        );
        // Restore the masked secrets before validation: the plugin must validate the configuration it will be given
        if (existing.getSource() != null && toUpdate.getSource() != null) {
            sourceDomainService.mergeSensitiveData(existing.getSource(), toUpdate.getSource());
        }
        // The import marker is server-owned: carried over from the stored source, never taken from a payload
        if (toUpdate.getSource() != null) {
            toUpdate.getSource().setSubtreeImport(existing.getSource() != null && existing.getSource().isSubtreeImport());
        }
        validatorService.validateToUpdateCascadingVisibility(toUpdate, existing);
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
        boolean propagatePublishToChildren,
        NavigationItemReference scope
    ) {
        public Input {
            Objects.requireNonNull(scope, "scope");
        }
    }

    public record Output(PortalNavigationItem updatedItem) {}
}
