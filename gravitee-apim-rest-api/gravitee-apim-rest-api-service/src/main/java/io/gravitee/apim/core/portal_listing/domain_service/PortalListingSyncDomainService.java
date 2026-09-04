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
import io.gravitee.apim.core.portal.domain_service.navigation.PortalNavigationValidator;
import io.gravitee.apim.core.portal.domain_service.navigation.PortalNavigationValidator.PendingUpdate;
import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.domain_service.ApiFolderSubtreeReconciler.ValidationItems;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_page.domain_service.ApiDocumentationSyncDomainService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import jakarta.annotation.Nullable;
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
    private final PortalNavigationValidator validatorService;

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
                navigationItemEntryMaterializer.upsert(auditInfo, portalId, apiId, entry);
                var currentFolders = apiFolderSubtreeReconciler.collectFolderDescendantsFrom(envFolders, apiId);
                apiFolderSubtreeReconciler.sync(auditInfo, apiId, List.of(), currentFolders);
                materializeApiDocs(auditInfo, apiId);
            }
        }
    }

    /** Reconciles the api-folder subtree for this api, then re-materializes its doc pages. */
    public void syncApiFolders(AuditInfo auditInfo, String apiId, List<NavigationPath> previousPaths) {
        var envFolders = apiFolderSubtreeReconciler.loadAllFoldersInEnv(auditInfo.environmentId());
        var currentFolders = apiFolderSubtreeReconciler.collectFolderDescendantsFrom(envFolders, apiId);
        apiFolderSubtreeReconciler.sync(auditInfo, apiId, previousPaths, currentFolders);
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
        validatorService.validate(creates, updates, auditInfo.environmentId());
    }

    private ValidationItems itemsForEntry(
        AuditInfo auditInfo,
        PortalId portalId,
        List<PortalNavigationItem> envFolders,
        PortalListingApiEntry entry
    ) {
        var apiId = entry.apiId(auditInfo);
        var existing = findExistingRow(auditInfo, portalId, apiId);
        return switch (existing) {
            case null -> validationItemsForNewRow(auditInfo, portalId, apiId, entry);
            case PortalNavigationApi navApi -> validationItemsForExistingRow(auditInfo, portalId, apiId, entry, navApi, envFolders);
            default -> throw PathConflictException.navigationIdTaken(PathConflictException.EntryKind.LISTING, entry.location());
        };
    }

    private PortalNavigationItem findExistingRow(AuditInfo auditInfo, PortalId portalId, String apiId) {
        return navigationItemEntryMaterializer.findExistingRow(
            auditInfo,
            navigationItemEntryMaterializer.rowId(auditInfo, portalId, apiId)
        );
    }

    private ValidationItems validationItemsForNewRow(AuditInfo auditInfo, PortalId portalId, String apiId, PortalListingApiEntry entry) {
        var rowCreate = navigationItemEntryMaterializer.itemForValidation(auditInfo, portalId, apiId, entry);
        var subtree = apiFolderSubtreeReconciler.itemsForValidation(auditInfo, apiId, List.of());
        var creates = new ArrayList<CreatePortalNavigationItem>(subtree.creates().size() + 1);
        creates.add(rowCreate);
        creates.addAll(subtree.creates());
        return new ValidationItems(creates, subtree.updates());
    }

    private ValidationItems validationItemsForExistingRow(
        AuditInfo auditInfo,
        PortalId portalId,
        String apiId,
        PortalListingApiEntry entry,
        PortalNavigationApi navApi,
        List<PortalNavigationItem> envFolders
    ) {
        var currentFolders = apiFolderSubtreeReconciler.collectFolderDescendantsFrom(envFolders, apiId);
        var subtree = apiFolderSubtreeReconciler.itemsForValidation(auditInfo, apiId, currentFolders);
        var rowUpdate = navigationItemEntryMaterializer.updateForValidation(auditInfo, portalId, entry, navApi);
        var updates = new ArrayList<>(subtree.updates());
        updates.add(rowUpdate);
        return new ValidationItems(subtree.creates(), updates);
    }

    public void validateApiFolderConflictsForApi(AuditInfo auditInfo, String apiId, @Nullable List<NavigationPath> desired) {
        var safeDesired = desired == null ? List.<NavigationPath>of() : desired;
        var envFolders = apiFolderSubtreeReconciler.loadAllFoldersInEnv(auditInfo.environmentId());
        var currentFolders = apiFolderSubtreeReconciler.collectFolderDescendantsFrom(envFolders, apiId);
        var items = apiFolderSubtreeReconciler.itemsForValidation(auditInfo, apiId, safeDesired, currentFolders);
        validatorService.validate(items.creates(), items.updates(), auditInfo.environmentId());
    }

    private void materializeApiDocs(AuditInfo auditInfo, String apiId) {
        portalPageContentQueryService
            .findByReference(auditInfo.environmentId(), AutomationMetadata.ReferenceType.API, apiId)
            .forEach(pc -> apiDocumentationSyncDomainService.materialize(auditInfo, pc));
    }
}
