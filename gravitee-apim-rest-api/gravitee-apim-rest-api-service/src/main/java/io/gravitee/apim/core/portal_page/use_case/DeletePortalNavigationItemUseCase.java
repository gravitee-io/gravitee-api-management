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
import io.gravitee.apim.core.parameters.model.ParameterContext;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@UseCase
@RequiredArgsConstructor
public class DeletePortalNavigationItemUseCase {

    private final PortalNavigationItemDomainService portalNavigationItemDomainService;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationSourcedItemsDomainService sourcedItemsDomainService;
    private final ParametersQueryService parametersQueryService;

    public Output execute(Input input) {
        var existing = portalNavigationItemsQueryService.findByIdAndEnvironmentId(input.environmentId(), input.navigationItemId());
        if (existing == null) {
            throw new PortalNavigationItemNotFoundException(input.navigationItemId().json());
        }

        if (existing.getSource() != null) {
            throw InvalidPortalNavigationItemDataException.sourcedItemCannotBeDeleted(existing.getId().json());
        }
        if (sourcedItemsDomainService.findSourcedAncestor(input.environmentId(), existing).isPresent()) {
            throw InvalidPortalNavigationItemDataException.childOfSourcedItemIsReadOnly(existing.getId().json());
        }
        if (existing.getType() == PortalNavigationItemType.FOLDER) {
            assertNotDefaultApiDocumentationFolder(input, existing.getId());
        }

        portalNavigationItemDomainService.deleteWithDescendants(existing);

        return new Output();
    }

    private void assertNotDefaultApiDocumentationFolder(Input input, PortalNavigationItemId folderId) {
        String defaultFolderId = parametersQueryService.findAsString(
            Key.PORTAL_NEXT_DOCUMENTATION_DEFAULT_FOLDER_ID,
            new ParameterContext(input.environmentId(), input.organizationId(), ParameterReferenceType.ENVIRONMENT)
        );
        if (StringUtils.isNotBlank(defaultFolderId) && defaultFolderId.equals(folderId.json())) {
            throw InvalidPortalNavigationItemDataException.defaultApiDocumentationFolderCannotBeDeleted();
        }
    }

    public record Output() {}

    public record Input(String organizationId, String environmentId, PortalNavigationItemId navigationItemId) {}
}
