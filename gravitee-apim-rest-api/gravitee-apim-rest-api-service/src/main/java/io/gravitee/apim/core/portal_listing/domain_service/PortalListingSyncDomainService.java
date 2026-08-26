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
import io.gravitee.apim.core.portal.domain_service.navigation.PortalNavigationValidator.PendingUpdate;
import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.domain_service.ApiFolderSubtreeReconciler.ValidationItems;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_page.domain_service.ApiDocumentationSyncDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Materializes a {@link PortalListing} into the navigation tree. Delegates row CRUD to
 * {@link NavigationItemEntryMaterializer} and api-folder subtree reconciliation to {@link ApiFolderSubtreeReconciler};
 * fan-out doc-page materialization is delegated to {@link ApiDocumentationSyncDomainService}.
 */
@DomainService
@RequiredArgsConstructor
public class PortalListingSyncDomainService {

    private final PortalPageContentQueryService portalPageContentQueryService;
    private final ApiDocumentationSyncDomainService apiDocumentationSyncDomainService;
    private final NavigationItemEntryMaterializer navigationItemEntryMaterializer;
    private final ApiFolderSubtreeReconciler apiFolderSubtreeReconciler;
    private final PortalNavigationItemValidatorService validatorService;

    public void sync(AuditInfo auditInfo, PortalId portalId, List<PortalListingApiEntry> previousEntries, PortalListing listing) {
        Set<String> newHrids = listing.getApis().stream().map(PortalListingApiEntry::apiHrid).collect(Collectors.toSet());
        List<PortalListingApiEntry> entriesToRemove = previousEntries
            .stream()
            .filter(entry -> !newHrids.contains(entry.apiHrid()))
            .toList();
        for (var entry : entriesToRemove) {
            navigationItemEntryMaterializer.dematerialize(auditInfo, portalId, entry.apiId(auditInfo));
        }

        if (!listing.getApis().isEmpty()) {
            var envFolders = apiFolderSubtreeReconciler.loadAllFoldersInEnv(auditInfo.environmentId());
            for (var entry : listing.getApis()) {
                var apiId = entry.apiId(auditInfo);
                var navApi = navigationItemEntryMaterializer.upsert(auditInfo, portalId, apiId, entry);
                var currentFolders = apiFolderSubtreeReconciler.collectFolderDescendantsFrom(envFolders, navApi.getId());
                apiFolderSubtreeReconciler.sync(auditInfo, navApi, apiId, List.of(), currentFolders);
                materializeApiDocs(auditInfo, apiId);
            }
        }
    }

    /** Reconciles the api-folder subtree under every nav-api row for this api, then re-materializes its doc pages. */
    public void syncApiFolders(AuditInfo auditInfo, String apiId, List<NavigationPath> previousPaths) {
        var navApis = apiFolderSubtreeReconciler.navApiRowsFor(auditInfo, apiId);
        if (!navApis.isEmpty()) {
            var envFolders = apiFolderSubtreeReconciler.loadAllFoldersInEnv(auditInfo.environmentId());
            for (var navApi : navApis) {
                var currentFolders = apiFolderSubtreeReconciler.collectFolderDescendantsFrom(envFolders, navApi.getId());
                apiFolderSubtreeReconciler.sync(auditInfo, navApi, apiId, previousPaths, currentFolders);
            }
        }
        materializeApiDocs(auditInfo, apiId);
    }

    public void dematerialize(AuditInfo auditInfo, PortalId portalId, PortalListing listing) {
        for (var entry : listing.getApis()) {
            navigationItemEntryMaterializer.dematerialize(auditInfo, portalId, entry.apiId(auditInfo));
        }
    }

    public void validateForConflicts(AuditInfo auditInfo, PortalId portalId, PortalListing listing) {
        if (listing.getApis().isEmpty()) return;
        var envFolders = apiFolderSubtreeReconciler.loadAllFoldersInEnv(auditInfo.environmentId());
        List<CreatePortalNavigationItem> creates = new ArrayList<>();
        List<PendingUpdate> updates = new ArrayList<>();
        for (var entry : listing.getApis()) {
            var items = itemsForEntry(auditInfo, portalId, envFolders, entry);
            creates.addAll(items.creates());
            updates.addAll(items.updates());
        }
        validatorService.validateAll(creates, auditInfo.environmentId());
        validatorService.validateAllUpdates(updates);
    }

    private ValidationItems itemsForEntry(
        AuditInfo auditInfo,
        PortalId portalId,
        List<PortalNavigationItem> envFolders,
        PortalListingApiEntry entry
    ) {
        var apiId = entry.apiId(auditInfo);
        var existing = navigationItemEntryMaterializer.findExistingRow(
            auditInfo,
            navigationItemEntryMaterializer.rowId(auditInfo, portalId, apiId)
        );
        return switch (existing) {
            case null -> new ValidationItems(
                List.of(navigationItemEntryMaterializer.itemForValidation(auditInfo, portalId, apiId, entry)),
                List.of()
            );
            case PortalNavigationApi navApi -> mergeRowUpdate(
                apiFolderSubtreeReconciler.itemsForValidation(
                    auditInfo,
                    apiId,
                    navApi,
                    apiFolderSubtreeReconciler.collectFolderDescendantsFrom(envFolders, navApi.getId())
                ),
                navigationItemEntryMaterializer.updateForValidation(auditInfo, portalId, entry, navApi)
            );
            default -> throw PathConflictException.navigationIdTaken(PathConflictException.EntryKind.LISTING, entry.location());
        };
    }

    private static ValidationItems mergeRowUpdate(ValidationItems subtreeItems, PendingUpdate rowUpdate) {
        var updates = new ArrayList<>(subtreeItems.updates());
        updates.add(rowUpdate);
        return new ValidationItems(subtreeItems.creates(), updates);
    }

    public void validateApiFolderConflictsForApi(AuditInfo auditInfo, String apiId, List<NavigationPath> desired) {
        var navApis = apiFolderSubtreeReconciler.navApiRowsFor(auditInfo, apiId);
        if (navApis.isEmpty()) return;
        var envFolders = apiFolderSubtreeReconciler.loadAllFoldersInEnv(auditInfo.environmentId());
        for (var navApi : navApis) {
            var currentFolders = apiFolderSubtreeReconciler.collectFolderDescendantsFrom(envFolders, navApi.getId());
            apiFolderSubtreeReconciler.validateConflicts(auditInfo, apiId, desired, currentFolders);
        }
    }

    private void materializeApiDocs(AuditInfo auditInfo, String apiId) {
        portalPageContentQueryService
            .findByReference(auditInfo.environmentId(), AutomationMetadata.ReferenceType.API, apiId)
            .forEach(pc -> apiDocumentationSyncDomainService.materialize(auditInfo, pc));
    }
}
