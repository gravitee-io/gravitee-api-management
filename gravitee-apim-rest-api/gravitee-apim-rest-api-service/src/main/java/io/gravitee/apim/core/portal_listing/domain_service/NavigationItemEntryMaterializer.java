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
package io.gravitee.apim.core.portal_listing.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_documentation.domain_service.navigation.DocumentationNavigationPageMapper;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.domain_service.ApiDocumentationSyncDomainService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.Slug;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/** Owns the {@link PortalNavigationApi} row lifecycle (upsert at the deterministic id, cascade-dematerialize). */
@DomainService
@RequiredArgsConstructor
class NavigationItemEntryMaterializer {

    private static final PortalArea AREA = PortalArea.TOP_NAVBAR;

    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final ApiDocumentationSyncDomainService apiDocumentationSyncDomainService;

    PortalNavigationApi upsert(AuditInfo auditInfo, PortalId portalId, String apiId, PortalListingApiEntry entry) {
        var navApiId = rowId(auditInfo, portalId, apiId);
        var parent = resolveParent(auditInfo, portalId.toString(), entry.location());

        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navApiId);
        if (existing instanceof PortalNavigationApi navApi) {
            applyUpdate(navApi, apiId, entry, parent);
            return (PortalNavigationApi) navigationItemCrudService.update(navApi);
        }
        rejectIfSegmentTakenByForeignItem(auditInfo, parent, Slug.from(entry.apiHrid()).value(), navApiId, entry.location());
        var create = CreatePortalNavigationItem.builder()
            .id(navApiId)
            .title(entry.apiHrid())
            .segment(Slug.from(entry.apiHrid()).value())
            .area(AREA)
            .type(PortalNavigationItemType.API)
            .order(entry.order() != null ? entry.order() : 0)
            .apiId(apiId)
            .visibility(PortalVisibility.PUBLIC)
            .published(true)
            .build();
        return (PortalNavigationApi) navigationItemCrudService.create(
            PortalNavigationItem.from(create, auditInfo.organizationId(), auditInfo.environmentId(), parent)
        );
    }

    void dematerialize(AuditInfo auditInfo, PortalId portalId, String apiId) {
        apiDocumentationSyncDomainService.cleanupNavApi(auditInfo, rowId(auditInfo, portalId, apiId));
    }

    PortalNavigationItemId rowId(AuditInfo auditInfo, PortalId portalId, String apiId) {
        return PortalNavigationItemId.forListingApi(auditInfo, portalId.toString(), apiId);
    }

    PortalNavigationItem findExistingRow(AuditInfo auditInfo, PortalNavigationItemId navApiId) {
        return navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navApiId);
    }

    void validateUpsertConflict(AuditInfo auditInfo, PortalId portalId, String apiId, PortalListingApiEntry entry) {
        var navApiId = rowId(auditInfo, portalId, apiId);
        var parent = resolveParent(auditInfo, portalId.toString(), entry.location());
        rejectIfSegmentTakenByForeignItem(auditInfo, parent, Slug.from(entry.apiHrid()).value(), navApiId, entry.location());
    }

    private void rejectIfSegmentTakenByForeignItem(
        AuditInfo auditInfo,
        PortalNavigationItemContainer parent,
        String segment,
        PortalNavigationItemId expectedId,
        String location
    ) {
        var parentId = Optional.ofNullable(parent).map(PortalNavigationItemContainer::getId).orElse(null);
        navigationItemsQueryService
            .findByParentIdAndSegment(auditInfo.environmentId(), parentId, segment)
            .filter(sibling -> !sibling.getId().equals(expectedId))
            .ifPresent(squatter -> {
                throw PathConflictException.listingEntry(location);
            });
    }

    private static void applyUpdate(
        PortalNavigationApi navApi,
        String apiId,
        PortalListingApiEntry entry,
        PortalNavigationItemContainer parent
    ) {
        navApi.setApiId(apiId);
        navApi.setOrder(entry.order() != null ? entry.order() : 0);
        navApi.setTitle(entry.apiHrid());
        navApi.setSegment(Slug.from(entry.apiHrid()).value());
        navApi.setVisibility(PortalVisibility.PUBLIC);
        navApi.setPublished(true);
        if (parent == null) {
            navApi.markAsRoot();
        } else {
            navApi.updateParent(parent);
        }
    }

    private PortalNavigationItemContainer resolveParent(AuditInfo auditInfo, String portalId, String location) {
        var folderId = PortalNavigationItemId.forPortalFolder(auditInfo, portalId, location);
        if (folderId == null) {
            return null;
        }
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), folderId);
        if (existing instanceof PortalNavigationItemContainer container) {
            return container;
        }
        return DocumentationNavigationPageMapper.phantomParent(folderId);
    }
}
