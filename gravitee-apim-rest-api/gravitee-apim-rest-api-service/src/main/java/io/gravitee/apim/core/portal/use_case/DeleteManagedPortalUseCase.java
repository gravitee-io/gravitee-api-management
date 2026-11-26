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
package io.gravitee.apim.core.portal.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.crud_service.PortalListingCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteManagedPortalUseCase {

    private final PortalCrudService portals;
    private final PortalNavigationItemsQueryService navigationQuery;
    private final PortalNavigationItemCrudService navigationCrud;
    private final PortalPageContentCrudService contentCrud;
    private final PortalPageContentQueryService contentQuery;
    private final ApiOwnedNavigationDomainService apiNavigation;
    private final PortalListingCrudService listings;

    public record Input(String environmentId, PortalId portalId) {}

    public void execute(Input input) {
        var portal = portals
            .findByIdAndEnvironmentId(input.portalId(), input.environmentId())
            .orElseThrow(() -> new PortalNotFoundException(input.portalId().toString()));
        if (portal.isDefault()) {
            throw new ValidationDomainException("The default portal cannot be deleted", "portal.default.notDeletable");
        }
        var owner = new NavigationItemReference.PortalReference(input.portalId());
        var all = navigationQuery.search(PortalNavigationItemQueryCriteria.builder().environmentId(input.environmentId()).build());
        var owned = all
            .stream()
            .filter(item -> owner.equals(item.getReference()))
            .toList();
        var ownedIds = owned.stream().map(PortalNavigationItem::getId).collect(Collectors.toSet());

        var legacyRoots = all
            .stream()
            .filter(item -> !ownedIds.contains(item.getId()) && ownedIds.contains(item.getParentId()))
            .toList();
        var legacySubtrees = new ArrayList<List<PortalNavigationItem>>();
        for (var root : legacyRoots) {
            var parent = all
                .stream()
                .filter(item -> item.getId().equals(root.getParentId()))
                .findFirst()
                .orElseThrow();
            if (
                !(parent instanceof PortalNavigationApi api) ||
                !(root.getReference() instanceof NavigationItemReference.ApiReference ownerApi) ||
                !api.getApiId().equals(ownerApi.apiId()) ||
                apiNavigation.isInApiProductContext(input.environmentId(), api) ||
                !apiNavigation.isDocumentation(root)
            ) {
                throw new ValidationDomainException("Portal navigation contains items belonging to another owner");
            }
            if (
                all
                    .stream()
                    .anyMatch(
                        item ->
                            !item.getId().equals(root.getId()) &&
                            (item.getParentId() == null || legacyRoots.contains(item)) &&
                            root.getReference().equals(item.getReference()) &&
                            root.getArea().equals(item.getArea()) &&
                            Objects.equals(root.getSegment(), item.getSegment())
                    )
            ) {
                throw new ValidationDomainException("API documentation contains a conflicting root segment");
            }
            var subtree = new ArrayList<PortalNavigationItem>();
            var pending = new ArrayDeque<PortalNavigationItem>();
            var visited = new HashSet<PortalNavigationItemId>();
            pending.add(root);
            while (!pending.isEmpty()) {
                var item = pending.removeFirst();
                if (!visited.add(item.getId()) || !root.getReference().equals(item.getReference())) {
                    throw new ValidationDomainException("API documentation has an invalid owner or cycle");
                }
                subtree.add(item);
                all
                    .stream()
                    .filter(child -> item.getId().equals(child.getParentId()))
                    .forEach(pending::add);
            }
            legacySubtrees.add(subtree);
        }
        Set<PortalPageContentId> shared = all
            .stream()
            .filter(item -> !ownedIds.contains(item.getId()))
            .filter(PortalNavigationPage.class::isInstance)
            .map(PortalNavigationPage.class::cast)
            .map(PortalNavigationPage::getPortalPageContentId)
            .collect(Collectors.toSet());
        var contents = owned
            .stream()
            .filter(PortalNavigationPage.class::isInstance)
            .map(PortalNavigationPage.class::cast)
            .map(PortalNavigationPage::getPortalPageContentId)
            .filter(id -> !shared.contains(id))
            .collect(Collectors.toSet());
        contentQuery
            .findByReference(input.environmentId(), AutomationMetadata.ReferenceType.PORTAL, input.portalId().toString())
            .stream()
            .map(content -> content.getId())
            .filter(id -> !shared.contains(id))
            .forEach(contents::add);
        for (var id : contents) {
            contentQuery
                .findById(id)
                .ifPresent(content -> {
                    if (
                        !portal.getOrganizationId().equals(content.getOrganizationId()) ||
                        !input.environmentId().equals(content.getEnvironmentId())
                    ) {
                        throw new ValidationDomainException("Portal page content belongs to another environment");
                    }
                });
        }
        contents.removeIf(id ->
            contentQuery
                .findById(id)
                .map(content -> {
                    var metadata = content.getAutomationMetadata();
                    return (
                        metadata != null &&
                        (metadata.referenceType() != AutomationMetadata.ReferenceType.PORTAL ||
                            !input.portalId().toString().equals(metadata.referenceId()))
                    );
                })
                .orElse(false)
        );

        for (var subtree : legacySubtrees) {
            var root = subtree.getFirst();
            for (var item : subtree.subList(1, subtree.size())) {
                item.setRootId(root.getId());
                navigationCrud.update(item);
            }
            root.markAsRoot();
            navigationCrud.update(root);
        }

        var affectedApis = owned
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .map(PortalNavigationApi::getApiId)
            .collect(Collectors.toSet());
        for (var apiId : affectedApis) {
            var targetListings = apiNavigation
                .findStandaloneListings(input.environmentId(), apiId)
                .stream()
                .filter(item -> ownedIds.contains(item.getId()))
                .toList();
            if (targetListings.isEmpty()) continue;
            targetListings
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getPublished()))
                .forEach(item -> {
                    item.setPublished(false);
                    navigationCrud.update(item);
                });
            var visible = apiNavigation
                .findStandaloneListings(input.environmentId(), apiId)
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getPublished()))
                .toList();
            if (visible.isEmpty()) {
                apiNavigation.setPublished(input.environmentId(), apiId, false);
                if (
                    apiNavigation
                        .findStandaloneListings(input.environmentId(), apiId)
                        .stream()
                        .anyMatch(item -> Boolean.TRUE.equals(item.getPublished()))
                ) {
                    apiNavigation.setPublished(input.environmentId(), apiId, true);
                }
            }
        }
        contents.forEach(contentCrud::delete);
        if (!ownedIds.isEmpty()) {
            navigationCrud.deleteByIds(List.copyOf(ownedIds));
        }
        listings
            .findAllByPortalIdAndEnvironmentId(input.portalId(), input.environmentId())
            .forEach(listing -> listings.delete(listing.getId()));
        portals.delete(input.portalId());
    }
}
