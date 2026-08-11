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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.slug.model.Slug;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Orphan-tolerant materialization of Portal Link automation resources onto the shared
 * {@link PortalNavigationLink} nav-item model. Mirrors {@link PortalDocumentationSyncDomainService}'s
 * phantom-parent resolution, but — since a link has no separate content object — the nav item
 * itself is the sole persisted representation, and its segment is derived from the stable
 * {@code linkHrid} (like {@code NavigationItemEntryMaterializer}'s API-listing entries), not the
 * mutable display name.
 */
@DomainService
@RequiredArgsConstructor
public class PortalLinkSyncDomainService {

    private static final PortalArea AREA = PortalArea.TOP_NAVBAR;

    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalNavigationItemsQueryService navigationItemsQueryService;

    public PortalNavigationLink materialize(
        AuditInfo auditInfo,
        String portalId,
        String linkHrid,
        String name,
        String href,
        String location,
        Integer order
    ) {
        var linkId = linkId(auditInfo, portalId, linkHrid);
        var segment = Slug.from(linkHrid).value();
        var parent = resolveParent(auditInfo, location, portalId);
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), linkId);

        if (existing instanceof PortalNavigationLink existingLink) {
            var toUpdate = UpdatePortalNavigationItem.builder()
                .title(name)
                .segment(segment)
                .order(order != null ? order : 0)
                .url(href)
                .visibility(PortalVisibility.PUBLIC)
                .published(true)
                .build();
            existingLink.update(toUpdate);
            if (parent == null) {
                existingLink.markAsRoot();
            } else {
                existingLink.updateParent(parent);
            }
            return (PortalNavigationLink) navigationItemCrudService.update(existingLink);
        }

        rejectIfSegmentTakenByForeignItem(auditInfo, parent, segment, linkId, location);

        var create = CreatePortalNavigationItem.builder()
            .id(linkId)
            .title(name)
            .segment(segment)
            .area(AREA)
            .type(PortalNavigationItemType.LINK)
            .order(order != null ? order : 0)
            .url(href)
            .visibility(PortalVisibility.PUBLIC)
            .published(true)
            .build();

        return (PortalNavigationLink) navigationItemCrudService.create(
            PortalNavigationItem.from(create, auditInfo.organizationId(), auditInfo.environmentId(), parent)
        );
    }

    public void dematerialize(AuditInfo auditInfo, String portalId, String linkHrid) {
        var linkId = linkId(auditInfo, portalId, linkHrid);
        if (navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), linkId) != null) {
            navigationItemCrudService.delete(linkId);
        }
    }

    private static PortalNavigationItemId linkId(AuditInfo auditInfo, String portalId, String linkHrid) {
        return PortalNavigationItemId.forPortalLink(auditInfo, portalId, linkHrid);
    }

    private PortalNavigationItemContainer resolveParent(AuditInfo auditInfo, String location, String portalId) {
        var folderId = PortalNavigationItemId.forPortalFolder(auditInfo, portalId, location);
        if (folderId == null) {
            return null;
        }
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), folderId);
        if (existing instanceof PortalNavigationItemContainer container) {
            return container;
        }
        return PortalNavigationItemContainer.phantom(folderId);
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
                throw PathConflictException.segmentTaken(PathConflictException.EntryKind.LINK, location);
            });
    }
}
