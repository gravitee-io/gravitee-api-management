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
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationBulkImportDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemSourceException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Creates a folder bound to a file-listing source and imports the remote documentation tree below
 * it. Unlike the bulk creation endpoint, which creates items from an explicit list, the import
 * builds that list from the remote repository.
 */
@RequiredArgsConstructor
@UseCase
public class ImportPortalNavigationUseCase {

    private final PortalNavigationItemValidatorService validatorService;
    private final PortalNavigationItemDomainService domainService;
    private final PortalNavigationItemSourceDomainService sourceDomainService;
    private final PortalNavigationBulkImportDomainService bulkImportDomainService;
    private final PortalNavigationItemsQueryService queryService;

    public Output execute(Input input) {
        // The caller never picks an area: under a parent it can only be the parent's, and the
        // area validators would otherwise reject a mismatch the caller has no way to understand.
        // An unknown parent falls back to the default so ParentRule reports it, instead of an NPE here.
        var parent = input.parentId() == null ? null : queryService.findByIdAndEnvironmentId(input.environmentId(), input.parentId());
        var area = parent == null ? PortalArea.TOP_NAVBAR : parent.getArea();
        var itemToCreate = CreatePortalNavigationItem.builder()
            .title(input.title())
            .type(PortalNavigationItemType.FOLDER)
            .area(area)
            .parentId(input.parentId())
            .visibility(input.visibility() == null ? PortalVisibility.PRIVATE : input.visibility())
            .published(false)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .source(input.source())
            .build();

        validatorService.validateAll(List.of(itemToCreate), input.environmentId());
        if (!sourceDomainService.supportsFileListing(input.source())) {
            throw InvalidPortalNavigationItemSourceException.sourceCannotListFiles(input.source().getSourceType());
        }
        // The marker is what later routes a fetch of this folder to a re-import
        input.source().setSubtreeImport(true);

        var rootFolder = (PortalNavigationFolder) domainService.create(input.organizationId(), input.environmentId(), itemToCreate);
        var result = bulkImportDomainService.importSubtree(rootFolder);

        // The import stamps the fetch state on the root folder: return the persisted version
        var importedRoot = (PortalNavigationFolder) queryService.findByIdAndEnvironmentId(input.environmentId(), rootFolder.getId());
        if (importedRoot.getSource() != null) {
            sourceDomainService.removeSensitiveData(importedRoot.getSource());
        }
        return new Output(importedRoot, result);
    }

    @Builder
    public record Input(
        String organizationId,
        String environmentId,
        String title,
        @Nullable PortalNavigationItemId parentId,
        @Nullable PortalVisibility visibility,
        PortalNavigationItemSource source
    ) {}

    public record Output(PortalNavigationFolder rootFolder, PortalNavigationBulkImportDomainService.BulkImportResult result) {}
}
