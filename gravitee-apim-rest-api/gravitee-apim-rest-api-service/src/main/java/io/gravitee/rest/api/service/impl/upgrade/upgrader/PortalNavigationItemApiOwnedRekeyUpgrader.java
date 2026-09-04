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
 * One-time realignment of API-attached navigation rows created before the API's subtree moved off the
 * per-listing {@code PortalNavigationApi} row and onto the API itself.
 * <p>
 * Every such row keeps the id it already has. {@code forApiFolder}, {@code forApiDocumentation} and
 * {@code forApiLink} have always been keyed on the API, never on the nav-api row a {@code PortalListing}
 * creates, so no identity moves here — only {@code parentId}, {@code rootId} and the stamped
 * {@code reference}, which legacy rows inherited from the nav-api row they hung under. That makes this
 * migration a pure in-place field update: it never creates or deletes a row, so a re-run is a no-op and
 * a run that died half-way is safe to resume.
 * <p>
 * Folders have no automation-ownership signal of their own — they never carried
 * {@code automationMetadata} — so they are discovered by walking down from each nav-api row (where they
 * were parented) and claimed only when their stored id matches {@code forApiFolder} for their own
 * reconstructed path, the same identity test the runtime uses to reject a foreign item. See
 * {@link ApiNavigationSubtreePaths}. Doc pages and links carry {@code automationMetadata} whenever
 * automation created them and never otherwise, so they are found by a direct scan instead of a tree
 * walk — which also finds a page phantom-parented at a folder id with no row behind it, unreachable by
 * any walk.
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

        Map<String, String> folderIdToRootId = new HashMap<>();

        var apiRows = items
            .stream()
            .filter(item -> item.getType() == PortalNavigationItem.Type.API)
            .toList();
        for (var apiRow : apiRows) {
            realignFolders(organizationId, environmentId, apiRow, childrenByParentId, folderIdToRootId);
        }

        realignDocPages(organizationId, environmentId, items, folderIdToRootId);
        realignApiLinks(organizationId, environmentId, items, folderIdToRootId);
    }

    private void realignFolders(
        String organizationId,
        String environmentId,
        PortalNavigationItem apiRow,
        Map<String, List<PortalNavigationItem>> childrenByParentId,
        Map<String, String> folderIdToRootId
    ) throws TechnicalException {
        var apiId = apiRow.getApiId();
        var pathedFolders = ApiNavigationSubtreePaths.collect(organizationId, environmentId, apiRow.getId(), apiId, childrenByParentId);

        // Pre-order: every ancestor of a folder appears earlier in this list, so its root is already
        // in folderIdToRootId by the time a descendant needs it.
        for (var pathed : pathedFolders) {
            var path = pathed.path();
            var folderId = pathed.folder().getId();

            var parentAndRoot = parentAndRootForFolder(organizationId, environmentId, apiId, path, folderId, folderIdToRootId);
            folderIdToRootId.put(folderId, parentAndRoot.rootId());

            realign(pathed.folder(), parentAndRoot, apiId);
        }
    }

    private void realignDocPages(
        String organizationId,
        String environmentId,
        List<PortalNavigationItem> items,
        Map<String, String> folderIdToRootId
    ) throws TechnicalException {
        var candidates = items
            .stream()
            .filter(item -> item.getType() == PortalNavigationItem.Type.PAGE)
            .filter(PortalNavigationItemApiOwnedRekeyUpgrader::isApiAutomationManaged)
            .map(this::toDocPageCandidate)
            // A page whose configuration will not parse has something wrong with it beyond a stale
            // parent; leave such a row exactly as it is rather than rewriting fields on it.
            .filter(candidate -> candidate.contentId() != null)
            .toList();

        for (var candidate : candidates) {
            realignAtLocation(organizationId, environmentId, candidate.page(), candidate.apiId(), candidate.location(), folderIdToRootId);
        }
    }

    private void realignApiLinks(
        String organizationId,
        String environmentId,
        List<PortalNavigationItem> items,
        Map<String, String> folderIdToRootId
    ) throws TechnicalException {
        var links = items
            .stream()
            .filter(item -> item.getType() == PortalNavigationItem.Type.LINK)
            .filter(PortalNavigationItemApiOwnedRekeyUpgrader::isApiAutomationManaged)
            .toList();

        for (var link : links) {
            var metadata = link.getAutomationMetadata();
            realignAtLocation(organizationId, environmentId, link, metadata.getReferenceId(), metadata.getLocation(), folderIdToRootId);
        }
    }

    private void realignAtLocation(
        String organizationId,
        String environmentId,
        PortalNavigationItem item,
        String apiId,
        String location,
        Map<String, String> folderIdToRootId
    ) throws TechnicalException {
        var parentAndRoot = parentAndRootForLocation(organizationId, environmentId, apiId, location, item.getId(), folderIdToRootId);
        realign(item, parentAndRoot, apiId);
    }

    private static boolean isApiAutomationManaged(PortalNavigationItem item) {
        var metadata = item.getAutomationMetadata();
        return metadata != null && metadata.getReferenceType() == AutomationTargetReferenceType.API;
    }

    private DocPageCandidate toDocPageCandidate(PortalNavigationItem page) {
        var metadata = page.getAutomationMetadata();
        return new DocPageCandidate(page, metadata.getReferenceId(), metadata.getLocation(), extractContentId(page));
    }

    private record DocPageCandidate(PortalNavigationItem page, String apiId, String location, String contentId) {}

    /**
     * The row keeps its id; only these three fields move into the API-owned scheme. Skipping a row that
     * already carries them is what makes a second run touch nothing.
     */
    private void realign(PortalNavigationItem item, ParentAndRoot parentAndRoot, String apiId) throws TechnicalException {
        if (isAlreadyAligned(item, parentAndRoot, apiId)) {
            return;
        }
        item.setParentId(parentAndRoot.parentId());
        item.setRootId(parentAndRoot.rootId());
        item.setReferenceType(PortalNavigationReferenceType.API);
        item.setReferenceId(apiId);
        portalNavigationItemRepository.update(item);
    }

    private static boolean isAlreadyAligned(PortalNavigationItem item, ParentAndRoot parentAndRoot, String apiId) {
        if (!Objects.equals(item.getParentId(), parentAndRoot.parentId())) {
            return false;
        }
        if (!Objects.equals(item.getRootId(), parentAndRoot.rootId())) {
            return false;
        }
        if (item.getReferenceType() != PortalNavigationReferenceType.API) {
            return false;
        }
        return apiId.equals(item.getReferenceId());
    }

    private record ParentAndRoot(String parentId, String rootId) {}

    /**
     * A top-level folder becomes a root of its own; a nested one keeps pointing at its parent folder —
     * an id that does not change either — and inherits that parent's root.
     */
    private static ParentAndRoot parentAndRootForFolder(
        String organizationId,
        String environmentId,
        String apiId,
        String path,
        String folderId,
        Map<String, String> folderIdToRootId
    ) {
        var lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return new ParentAndRoot(null, folderId);
        }
        var parentId = folderId(organizationId, environmentId, apiId, path.substring(0, lastSlash));
        return new ParentAndRoot(parentId, folderIdToRootId.getOrDefault(parentId, parentId));
    }

    /**
     * A blank/null/"/" location makes the item a root of its own — matching
     * {@code ApiDocumentationSyncDomainService#resolveParent}'s same three-way check. Otherwise the
     * parent is the deterministic folder id for that location; if that folder was realigned earlier in
     * this same pass its real root is known, and if not — the folder does not exist, exactly the phantom
     * case the runtime itself tolerates — the item's root is the folder id itself, mirroring
     * {@code PortalNavigationItemContainer#phantom}, whose root is its own placeholder id.
     */
    private static ParentAndRoot parentAndRootForLocation(
        String organizationId,
        String environmentId,
        String apiId,
        String location,
        String ownId,
        Map<String, String> folderIdToRootId
    ) {
        if (location == null || location.isBlank() || "/".equals(location)) {
            return new ParentAndRoot(null, ownId);
        }
        var folderId = folderId(organizationId, environmentId, apiId, normalizeLocation(location));
        return new ParentAndRoot(folderId, folderIdToRootId.getOrDefault(folderId, folderId));
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
            log.warn("Unable to parse configuration for portal navigation item {}; skipping api-owned realignment for it", item.getId(), e);
            return null;
        }
    }
}
