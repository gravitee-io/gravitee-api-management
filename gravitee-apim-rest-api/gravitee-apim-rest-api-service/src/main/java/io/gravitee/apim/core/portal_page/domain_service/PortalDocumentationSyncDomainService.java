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
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.domain_service.reconciliation.HomepageReconciler;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.slug.model.Slug;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalDocumentationSyncDomainService {

    private static final PortalNavigationItemType TYPE = PortalNavigationItemType.PAGE;
    private static final int DEFAULT_ORDER = 0;
    private static final boolean DEFAULT_PUBLISHED = true;

    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final HomepageReconciler homepageReconciler;
    private final PortalNavigationItemValidatorService validatorService;

    public void materialize(
        AuditInfo auditInfo,
        PortalPageContent<?> pageContent,
        PortalArea targetArea,
        PortalVisibility callerVisibility
    ) {
        var navigationItemId = PortalNavigationItemId.forPortalDocumentationContent(auditInfo, pageContent);
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navigationItemId);
        upsertNavigationPage(auditInfo, pageContent, navigationItemId, existing, targetArea, callerVisibility);
    }

    /** Validates the placement {@link #materialize} would produce, without writing anything. */
    public void validatePlacement(
        AuditInfo auditInfo,
        PortalPageContent<?> pageContent,
        PortalArea targetArea,
        PortalVisibility callerVisibility
    ) {
        var navigationItemId = PortalNavigationItemId.forPortalDocumentationContent(auditInfo, pageContent);
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navigationItemId);
        var plan = plan(auditInfo, pageContent, navigationItemId, existing, targetArea, callerVisibility);
        validate(plan, auditInfo, pageContent, targetArea, navigationItemId, existing);
    }

    public void materialize(AuditInfo auditInfo, PortalPageContent<?> pageContent) {
        var navigationItemId = PortalNavigationItemId.forPortalDocumentationContent(auditInfo, pageContent);
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navigationItemId);
        var targetArea = existing instanceof PortalNavigationPage page ? page.getArea() : PortalArea.TOP_NAVBAR;
        var storedVisibility = existing != null ? existing.getVisibility() : null;
        upsertNavigationPage(auditInfo, pageContent, navigationItemId, existing, targetArea, storedVisibility);
    }

    public void dematerialize(AuditInfo auditInfo, String portalId, PortalPageContentId pageContentId) {
        final var navigationItemId = PortalNavigationItemId.forPortalDocumentation(auditInfo, portalId, pageContentId);
        final var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navigationItemId);
        if (existing != null) {
            navigationItemCrudService.delete(navigationItemId);
        }
    }

    private void upsertNavigationPage(
        AuditInfo auditInfo,
        PortalPageContent<?> pageContent,
        PortalNavigationItemId navigationItemId,
        PortalNavigationItem existing,
        PortalArea targetArea,
        PortalVisibility callerVisibility
    ) {
        var plan = plan(auditInfo, pageContent, navigationItemId, existing, targetArea, callerVisibility);
        validate(plan, auditInfo, pageContent, targetArea, navigationItemId, existing);
        apply(plan, auditInfo, pageContent, navigationItemId, existing);
    }

    private sealed interface NavigationItemPlan {}

    private record UpdateInPlace(
        PortalNavigationPage page,
        UpdatePortalNavigationItem update,
        PortalNavigationItemContainer parent
    ) implements NavigationItemPlan {}

    private record CreateNew(CreatePortalNavigationItem create, PortalNavigationItemContainer parent) implements NavigationItemPlan {}

    private NavigationItemPlan plan(
        AuditInfo auditInfo,
        PortalPageContent<?> pageContent,
        PortalNavigationItemId navigationItemId,
        PortalNavigationItem existing,
        PortalArea targetArea,
        PortalVisibility callerVisibility
    ) {
        final var envId = auditInfo.environmentId();
        final var meta = pageContent.getAutomationMetadata();
        final var parent = resolveParent(auditInfo, meta.location().orElse(null), meta.referenceId());
        final var parentId = parent == null ? null : parent.getId();
        final var fallbackVisibility = Optional.ofNullable(parent).map(PortalNavigationItemContainer::getVisibility).orElse(null);
        final var visibility = PortalVisibility.resolve(callerVisibility, fallbackVisibility);

        if (isUpdatableInPlace(existing, targetArea)) {
            var page = (PortalNavigationPage) existing;
            final var segment = Slug.from(meta.name(), siblingsSlugs(envId, parentId, navigationItemId));
            var update = UpdatePortalNavigationItem.builder()
                .title(meta.name())
                .segment(segment.value())
                .type(TYPE)
                .order(meta.order().orElse(DEFAULT_ORDER))
                .parentId(parentId)
                .visibility(visibility)
                .published(DEFAULT_PUBLISHED)
                .build();
            return new UpdateInPlace(page, update, parent);
        }

        final var segment = Slug.from(meta.name(), siblingsSlugs(envId, parentId, navigationItemId));
        var create = CreatePortalNavigationItem.builder()
            .id(navigationItemId)
            .title(meta.name())
            .segment(segment.value())
            .area(targetArea)
            .type(TYPE)
            .order(meta.order().orElse(DEFAULT_ORDER))
            .portalPageContentId(pageContent.getId())
            .parentId(parentId)
            .reference(meta.reference())
            .visibility(visibility)
            .published(DEFAULT_PUBLISHED)
            .automationMetadata(meta.trimmedForNavItem())
            .build();
        return new CreateNew(create, parent);
    }

    private void validate(
        NavigationItemPlan plan,
        AuditInfo auditInfo,
        PortalPageContent<?> pageContent,
        PortalArea targetArea,
        PortalNavigationItemId navigationItemId,
        PortalNavigationItem existing
    ) {
        if (existing instanceof PortalNavigationPage page && page.getArea() != targetArea) {
            throw InvalidPortalNavigationItemDataException.areaCannotChange(navigationItemId.toString());
        }
        switch (plan) {
            case UpdateInPlace(var page, var update, var ignoredParent) -> validatorService.validateToUpdate(update, page);
            case CreateNew(var create, var ignoredParent) -> {
                var itemIdsBeingReplaced = new HashSet<PortalNavigationItemId>();
                if (existing != null) {
                    itemIdsBeingReplaced.add(navigationItemId);
                }
                if (targetArea == PortalArea.HOMEPAGE) {
                    homepageReconciler
                        .findStaleHomepages(auditInfo.environmentId(), pageContent.getAutomationMetadata().referenceId(), navigationItemId)
                        .forEach(stale -> itemIdsBeingReplaced.add(stale.getId()));
                }
                validatorService.validateOne(create, auditInfo.environmentId(), Set.of(pageContent.getId()), itemIdsBeingReplaced);
            }
        }
    }

    private void apply(
        NavigationItemPlan plan,
        AuditInfo auditInfo,
        PortalPageContent<?> pageContent,
        PortalNavigationItemId navigationItemId,
        PortalNavigationItem existing
    ) {
        switch (plan) {
            case UpdateInPlace(var page, var update, var parent) -> {
                page.update(update, pageContent.getAutomationMetadata().trimmedForNavItem());
                page.attachTo(parent);
                navigationItemCrudService.update(page);
            }
            case CreateNew(var create, var parent) -> {
                if (existing != null) {
                    navigationItemCrudService.delete(navigationItemId);
                }
                if (create.getArea() == PortalArea.HOMEPAGE) {
                    homepageReconciler.dropStaleHomepages(
                        auditInfo.environmentId(),
                        pageContent.getAutomationMetadata().referenceId(),
                        navigationItemId
                    );
                }
                navigationItemCrudService.create(
                    PortalNavigationItem.from(create, auditInfo.organizationId(), auditInfo.environmentId(), parent)
                );
            }
        }
    }

    private static boolean isUpdatableInPlace(PortalNavigationItem existing, PortalArea targetArea) {
        return existing instanceof PortalNavigationPage page && page.getArea() == targetArea;
    }

    private Set<Slug> siblingsSlugs(String environmentId, PortalNavigationItemId parentId, PortalNavigationItemId excludeId) {
        return navigationItemsQueryService
            .findByParentIdAndEnvironmentId(environmentId, parentId)
            .stream()
            .filter(item -> !item.getId().equals(excludeId))
            .map(PortalNavigationItem::getSegment)
            .map(Slug::new)
            .collect(Collectors.toSet());
    }

    private PortalNavigationItemContainer resolveParent(AuditInfo auditInfo, String location, String portalId) {
        var folderId = PortalNavigationItemId.forPortalFolder(auditInfo, portalId, location);
        if (folderId == null) return null;
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), folderId);
        if (existing instanceof PortalNavigationItemContainer container) {
            return container;
        }
        return PortalNavigationItemContainer.phantom(folderId);
    }
}
