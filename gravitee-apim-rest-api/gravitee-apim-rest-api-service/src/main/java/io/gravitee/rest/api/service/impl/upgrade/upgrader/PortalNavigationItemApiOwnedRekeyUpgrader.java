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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.PORTAL_NAVIGATION_ITEM_API_OWNED_REKEY_UPGRADER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.model.AutomationTargetReferenceType;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalNavigationReferenceType;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * One-time re-key of API-attached navigation rows created before their identity moved off the
 * per-listing {@code PortalNavigationApi} row and onto the API itself. Folders, doc pages and
 * links keyed on a nav-api row are moved into the API-owned key space; a legacy link's id never changed
 * (it was already keyed on the API), so it is only re-parented and re-referenced in place.
 * <p>
 * Folders have no reliable automation-ownership signal of their own — they never carried
 * {@code automationMetadata} — so a folder is treated as legacy-automation-owned only if its stored id
 * matches the pre-re-key {@code forApiFolder} formula for its own reconstructed path, the same identity
 * test the runtime itself uses to reject a foreign item. See {@link ApiNavigationSubtreePaths}. Doc
 * pages and links carry {@code automationMetadata} whenever automation created them and never otherwise,
 * so they are found by a direct scan instead of a tree walk — which also finds a page phantom-parented
 * at a folder id with no row behind it, unreachable by any walk.
 * <p>
 * Must run after {@link PortalNavigationItemAutomationMetadataUpgrader}: doc-page discovery here relies
 * on {@code automationMetadata} already being backfilled onto every automation-managed page.
 */
@Component
@CustomLog
public class PortalNavigationItemApiOwnedRekeyUpgrader implements Upgrader {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PORTAL_PAGE_CONTENT_ID = "portalPageContentId";

    private final PortalNavigationItemRepository portalNavigationItemRepository;

    public PortalNavigationItemApiOwnedRekeyUpgrader(@Lazy PortalNavigationItemRepository portalNavigationItemRepository) {
        this.portalNavigationItemRepository = portalNavigationItemRepository;
    }

    @Override
    public int getOrder() {
        return PORTAL_NAVIGATION_ITEM_API_OWNED_REKEY_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::migrate);
    }

    private boolean migrate() throws TechnicalException {
        // One full scan, grouped by (organizationId, environmentId) below, rather than looping
        // environments and re-querying: navigation items are few enough that this is simpler and
        // cheaper, mirroring PortalNavigationItemAutomationMetadataUpgrader#migrate.
        Map<Map.Entry<String, String>, List<PortalNavigationItem>> byOrgAndEnv = portalNavigationItemRepository
            .findAll()
            .stream()
            .collect(Collectors.groupingBy(item -> Map.entry(item.getOrganizationId(), item.getEnvironmentId())));

        for (var entry : byOrgAndEnv.entrySet()) {
            migrateEnvironment(entry.getKey().getKey(), entry.getKey().getValue(), entry.getValue());
        }
        return true;
    }

    private void migrateEnvironment(String organizationId, String environmentId, List<PortalNavigationItem> items)
        throws TechnicalException {
        Map<String, List<PortalNavigationItem>> childrenByParentId = items
            .stream()
            .filter(item -> item.getParentId() != null)
            .collect(Collectors.groupingBy(PortalNavigationItem::getParentId));

        Map<String, String> folderNewIdToRootId = new HashMap<>();

        var apiRows = items
            .stream()
            .filter(item -> item.getType() == PortalNavigationItem.Type.API)
            .toList();
        for (var apiRow : apiRows) {
            rekeyFolders(organizationId, environmentId, apiRow, childrenByParentId, folderNewIdToRootId);
        }

        rekeyDocPages(organizationId, environmentId, items, folderNewIdToRootId);
        realignApiLinkReferences(organizationId, environmentId, items, folderNewIdToRootId);
    }

    private void rekeyFolders(
        String organizationId,
        String environmentId,
        PortalNavigationItem apiRow,
        Map<String, List<PortalNavigationItem>> childrenByParentId,
        Map<String, String> folderNewIdToRootId
    ) throws TechnicalException {
        var apiId = apiRow.getApiId();
        var pathedFolders = ApiNavigationSubtreePaths.collect(organizationId, environmentId, apiRow.getId(), childrenByParentId);

        // Pre-order: every ancestor of a folder appears earlier in this list, so its new root is
        // already in folderNewIdToRootId by the time a descendant needs it.
        for (var pathed : pathedFolders) {
            var path = pathed.path();
            var newId = folderId(organizationId, environmentId, apiId, path);

            String newParentId = null;
            String newRootId = newId;
            var lastSlash = path.lastIndexOf('/');
            if (lastSlash > 0) {
                var parentPath = path.substring(0, lastSlash);
                newParentId = folderId(organizationId, environmentId, apiId, parentPath);
                newRootId = folderNewIdToRootId.getOrDefault(newParentId, newParentId);
            }
            folderNewIdToRootId.put(newId, newRootId);

            rekeyItem(pathed.folder(), newId, newParentId, newRootId, PortalNavigationReferenceType.API, apiId);
        }
    }

    private record DocPageCandidate(PortalNavigationItem page, String apiId, String location, String contentId) {}

    private void rekeyDocPages(
        String organizationId,
        String environmentId,
        List<PortalNavigationItem> items,
        Map<String, String> folderNewIdToRootId
    ) throws TechnicalException {
        var candidates = items
            .stream()
            .filter(item -> item.getType() == PortalNavigationItem.Type.PAGE)
            .filter(page -> {
                var metadata = page.getAutomationMetadata();
                return metadata != null && metadata.getReferenceType() == AutomationTargetReferenceType.API;
            })
            .map(page -> {
                var metadata = page.getAutomationMetadata();
                return new DocPageCandidate(page, metadata.getReferenceId(), metadata.getLocation(), extractContentId(page));
            })
            .filter(candidate -> candidate.contentId() != null)
            .toList();

        for (var candidate : candidates) {
            var newId = HRIDToUUID.navigation()
                .context(organizationId, environmentId)
                .api(candidate.apiId())
                .documentation(candidate.contentId())
                .id();
            var parentAndRoot = resolveNewParentAndRoot(
                organizationId,
                environmentId,
                candidate.apiId(),
                candidate.location(),
                newId,
                folderNewIdToRootId
            );

            rekeyItem(
                candidate.page(),
                newId,
                parentAndRoot.parentId(),
                parentAndRoot.rootId(),
                PortalNavigationReferenceType.API,
                candidate.apiId()
            );
        }
    }

    private record LinkCandidate(PortalNavigationItem link, String apiId, ParentAndRoot parentAndRoot) {}

    private void realignApiLinkReferences(
        String organizationId,
        String environmentId,
        List<PortalNavigationItem> items,
        Map<String, String> folderNewIdToRootId
    ) throws TechnicalException {
        var candidates = items
            .stream()
            .filter(item -> item.getType() == PortalNavigationItem.Type.LINK)
            .filter(link -> {
                var metadata = link.getAutomationMetadata();
                return metadata != null && metadata.getReferenceType() == AutomationTargetReferenceType.API;
            })
            .map(link -> {
                var metadata = link.getAutomationMetadata();
                var apiId = metadata.getReferenceId();
                var parentAndRoot = resolveNewParentAndRoot(
                    organizationId,
                    environmentId,
                    apiId,
                    metadata.getLocation(),
                    link.getId(),
                    folderNewIdToRootId
                );
                return new LinkCandidate(link, apiId, parentAndRoot);
            })
            .filter(candidate -> !isAlreadyCorrect(candidate))
            .toList();

        for (var candidate : candidates) {
            var link = candidate.link();
            var parentAndRoot = candidate.parentAndRoot();
            link.setParentId(parentAndRoot.parentId());
            link.setRootId(parentAndRoot.rootId());
            link.setReferenceType(PortalNavigationReferenceType.API);
            link.setReferenceId(candidate.apiId());
            portalNavigationItemRepository.update(link);
        }
    }

    private static boolean isAlreadyCorrect(LinkCandidate candidate) {
        var link = candidate.link();
        var parentAndRoot = candidate.parentAndRoot();
        if (!Objects.equals(link.getParentId(), parentAndRoot.parentId())) {
            return false;
        }
        if (!Objects.equals(link.getRootId(), parentAndRoot.rootId())) {
            return false;
        }
        if (link.getReferenceType() != PortalNavigationReferenceType.API) {
            return false;
        }
        return candidate.apiId().equals(link.getReferenceId());
    }

    /**
     * Applied to folders and doc pages, where the identity itself changes. If {@code newId} already
     * matches, nothing was ever legacy-keyed here and there is nothing to do — the write paths that
     * create these rows already set parent/root/reference correctly together whenever they set the id
     * correctly. If a row
     * already exists at {@code newId}, this source is a duplicate that lost the race — created by a
     * different, already-processed nav-api row's copy of the same subtree — so only the source is
     * deleted; skipping the redundant insert and always deleting the source is what makes two legacy
     * duplicates converge on one new row, and what makes a run that died mid-migration converge on retry.
     */
    private void rekeyItem(
        PortalNavigationItem original,
        String newId,
        String newParentId,
        String newRootId,
        PortalNavigationReferenceType referenceType,
        String referenceId
    ) throws TechnicalException {
        if (newId.equals(original.getId())) {
            return;
        }
        if (portalNavigationItemRepository.findById(newId).isEmpty()) {
            portalNavigationItemRepository.create(copyWithNewIdentity(original, newId, newParentId, newRootId, referenceType, referenceId));
        }
        portalNavigationItemRepository.delete(original.getId());
    }

    private static PortalNavigationItem copyWithNewIdentity(
        PortalNavigationItem original,
        String newId,
        String newParentId,
        String newRootId,
        PortalNavigationReferenceType referenceType,
        String referenceId
    ) {
        return PortalNavigationItem.builder()
            .id(newId)
            .organizationId(original.getOrganizationId())
            .environmentId(original.getEnvironmentId())
            .referenceType(referenceType)
            .referenceId(referenceId)
            .title(original.getTitle())
            .segment(original.getSegment())
            .type(original.getType())
            .area(original.getArea())
            .parentId(newParentId)
            .rootId(newRootId)
            .order(original.getOrder())
            .configuration(original.getConfiguration())
            .published(original.isPublished())
            .visibility(original.getVisibility())
            .apiId(original.getApiId())
            .apiProductId(original.getApiProductId())
            .useAutoFetch(original.isUseAutoFetch())
            .categoryIds(original.getCategoryIds())
            .automationMetadata(original.getAutomationMetadata())
            .build();
    }

    private record ParentAndRoot(String parentId, String rootId) {}

    /**
     * A blank/null/"/" location makes the item a root of its own — matching
     * {@code ApiDocumentationSyncDomainService#resolveParent}'s same three-way check. Otherwise the
     * parent is the deterministic folder id for that location; if that folder was re-keyed earlier in
     * this same pass its real root is known, and if not — the folder does not exist, exactly the phantom
     * case the runtime itself tolerates — the item's root is the folder id itself, mirroring
     * {@code PortalNavigationItemContainer#phantom}, whose root is its own placeholder id.
     */
    private static ParentAndRoot resolveNewParentAndRoot(
        String organizationId,
        String environmentId,
        String apiId,
        String location,
        String ownNewId,
        Map<String, String> folderNewIdToRootId
    ) {
        if (location == null || location.isBlank() || "/".equals(location)) {
            return new ParentAndRoot(null, ownNewId);
        }
        var folderId = folderId(organizationId, environmentId, apiId, normalizeLocation(location));
        return new ParentAndRoot(folderId, folderNewIdToRootId.getOrDefault(folderId, folderId));
    }

    private static String folderId(String organizationId, String environmentId, String apiId, String path) {
        return HRIDToUUID.navigation().context(organizationId, environmentId).api(apiId).folder(path).id();
    }

    /** Mirrors {@code PortalNavigationItemId#normalizeLocation}, which is private and not reusable from here. */
    private static String normalizeLocation(String location) {
        return location.endsWith("/") && location.length() > 1 ? location.substring(0, location.length() - 1) : location;
    }

    // Mirrors PortalNavigationItemAutomationMetadataUpgrader#extractContentId.
    private String extractContentId(PortalNavigationItem item) {
        try {
            JsonNode node = JSON.readTree(item.getConfiguration()).get(PORTAL_PAGE_CONTENT_ID);
            return node == null ? null : node.asText();
        } catch (Exception e) {
            log.warn("Unable to parse configuration for portal navigation item {}; skipping api-owned re-key for it", item.getId(), e);
            return null;
        }
    }
}
