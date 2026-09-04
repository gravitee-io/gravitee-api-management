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
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.slug.model.Slug;
import jakarta.annotation.Nullable;
import java.util.Objects;
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
        Integer order,
        PortalVisibility callerVisibility
    ) {
        return upsert(
            auditInfo,
            PortalNavigationItemId.forPortalLink(auditInfo, portalId, linkHrid),
            resolveParent(auditInfo, location, portalId),
            linkHrid,
            name,
            href,
            location,
            order,
            callerVisibility,
            new AutomationMetadata(AutomationMetadata.ReferenceType.PORTAL, portalId, null, Optional.ofNullable(location), Optional.empty())
        );
    }

    private PortalNavigationLink upsert(
        AuditInfo auditInfo,
        PortalNavigationItemId linkId,
        PortalNavigationItemContainer parent,
        String linkHrid,
        String name,
        String href,
        String location,
        Integer order,
        PortalVisibility callerVisibility,
        AutomationMetadata automationMetadata
    ) {
        var segment = Slug.from(linkHrid).value();
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), linkId);
        var existingLink = existing instanceof PortalNavigationLink link ? link : null;
        var parentId = parent == null ? null : parent.getId();

        // Segment is derived from the stable linkHrid, never from the mutable name — so a conflict can
        // only newly arise on create or on an actual relocation. Skipping the check on an in-place update
        // is safe: if this link already legitimately owns (parent, segment), no foreign item could have
        // taken that same slot in the meantime without failing this same check itself.
        if (existingLink == null || !Objects.equals(existingLink.getParentId(), parentId)) {
            rejectIfSegmentTakenByForeignItem(auditInfo, parent, segment, linkId, location, automationMetadata.reference());
        }

        var visibility = PortalVisibility.resolve(callerVisibility, parent == null ? null : parent.getVisibility());

        if (existingLink != null) {
            var toUpdate = UpdatePortalNavigationItem.builder()
                .title(name)
                .segment(segment)
                .order(order != null ? order : 0)
                .url(href)
                .visibility(visibility)
                .published(true)
                .build();
            existingLink.update(toUpdate, automationMetadata);
            if (parent == null) {
                existingLink.markAsRoot();
            } else {
                existingLink.updateParent(parent);
            }
            return (PortalNavigationLink) navigationItemCrudService.update(existingLink);
        }

        var create = CreatePortalNavigationItem.builder()
            .id(linkId)
            .title(name)
            .segment(segment)
            .area(AREA)
            .type(PortalNavigationItemType.LINK)
            .order(order != null ? order : 0)
            .url(href)
            .visibility(visibility)
            .reference(automationMetadata.reference())
            .automationMetadata(automationMetadata)
            .published(true)
            .build();

        return (PortalNavigationLink) navigationItemCrudService.create(
            PortalNavigationItem.from(create, auditInfo.organizationId(), auditInfo.environmentId(), parent)
        );
    }

    public void validateForConflicts(AuditInfo auditInfo, String portalId, String linkHrid, String location) {
        rejectIfSegmentTakenByForeignItem(
            auditInfo,
            resolveParent(auditInfo, location, portalId),
            Slug.from(linkHrid).value(),
            PortalNavigationItemId.forPortalLink(auditInfo, portalId, linkHrid),
            location,
            new NavigationItemReference.PortalReference(PortalId.of(portalId))
        );
    }

    @Nullable
    public PortalNavigationItemId parentIdFor(AuditInfo auditInfo, String portalId, String location) {
        var parent = resolveParent(auditInfo, location, portalId);
        return parent == null ? null : parent.getId();
    }

    /**
     * The API-attached counterpart of {@link #materialize}. {@code location} resolves against the
     * API's own {@code portalNavigation} folder subtree — the same tree API-attached documentation
     * uses — so the parent is derived from the API rather than from a portal folder.
     */
    public PortalNavigationLink materializeForApi(
        AuditInfo auditInfo,
        String apiId,
        String linkHrid,
        String name,
        String href,
        String location,
        Integer order,
        PortalVisibility callerVisibility
    ) {
        return upsert(
            auditInfo,
            PortalNavigationItemId.forApiLink(auditInfo, apiId, linkHrid),
            resolveApiParent(auditInfo, location, apiId),
            linkHrid,
            name,
            href,
            location,
            order,
            callerVisibility,
            new AutomationMetadata(AutomationMetadata.ReferenceType.API, apiId, null, Optional.ofNullable(location), Optional.empty())
        );
    }

    public void validateForConflictsForApi(AuditInfo auditInfo, String apiId, String linkHrid, String location) {
        rejectIfSegmentTakenByForeignItem(
            auditInfo,
            resolveApiParent(auditInfo, location, apiId),
            Slug.from(linkHrid).value(),
            PortalNavigationItemId.forApiLink(auditInfo, apiId, linkHrid),
            location,
            new NavigationItemReference.ApiReference(apiId)
        );
    }

    @Nullable
    public PortalNavigationItemId parentIdForApi(AuditInfo auditInfo, String apiId, String location) {
        var parent = resolveApiParent(auditInfo, location, apiId);
        return parent == null ? null : parent.getId();
    }

    public void dematerialize(AuditInfo auditInfo, PortalNavigationItemId linkId) {
        if (navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), linkId) != null) {
            navigationItemCrudService.delete(linkId);
        }
    }

    /**
     * An API-attached link lives in the API's own navigation subtree, so its location resolves in the
     * API's key space and an absent location makes the link a root of that subtree. Rendering splices
     * the subtree in under every portal row that lists the API; nothing here needs to know about portals.
     */
    private PortalNavigationItemContainer resolveApiParent(AuditInfo auditInfo, String location, String apiId) {
        if (location == null || location.isBlank() || "/".equals(location)) {
            return null;
        }
        var folderId = PortalNavigationItemId.forApiFolder(auditInfo, apiId, location);
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), folderId);
        if (existing instanceof PortalNavigationItemContainer container) {
            return container;
        }
        return PortalNavigationItemContainer.phantom(folderId);
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
        String location,
        NavigationItemReference reference
    ) {
        var parentId = Optional.ofNullable(parent).map(PortalNavigationItemContainer::getId).orElse(null);
        navigationItemsQueryService
            .findByParentIdAndSegment(auditInfo.environmentId(), parentId, segment, reference)
            .filter(sibling -> !sibling.getId().equals(expectedId))
            .ifPresent(squatter -> {
                throw PathConflictException.segmentTaken(PathConflictException.EntryKind.LINK, location);
            });
    }
}
