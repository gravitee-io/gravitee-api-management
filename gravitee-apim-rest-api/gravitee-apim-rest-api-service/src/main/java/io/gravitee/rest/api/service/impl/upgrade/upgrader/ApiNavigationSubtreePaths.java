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

import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.CustomLog;

/**
 * Reconstructs the {@code portalNavigation} path of every legacy, automation-owned {@code FOLDER}
 * descendant of a nav-api row — the path {@code PortalNavigationItemId#forApiFolder} needs to derive
 * that same folder's id in the API-owned key space.
 * <p>
 * "Automation-owned" is decided the way the runtime itself decides it when rejecting a conflicting
 * foreign item ({@code NavigationSyncPlanExecutor#rejectIfSegmentTakenByForeignItem}): by id, not by
 * metadata. Folders never carried {@code automationMetadata}, and pre-re-key folders never carried a
 * stamped {@code reference} either, so a hand-created folder is indistinguishable from a legacy
 * automation folder on metadata alone. A legacy folder's id is deterministic — the pre-re-key
 * {@code forApiFolder} formula, keyed on the nav-api row rather than on the API — so a candidate whose
 * stored id does not match that formula for its own reconstructed path was never written by automation,
 * whatever it is. Its subtree is left alone, exactly as a hand-created folder standing where automation
 * planned to write one is left alone today.
 * <p>
 * This is also why paths are not chained through parent folder ids: the legacy formula takes one full
 * path string per folder, computed straight from the nav-api row, never nested per ancestor.
 */
@CustomLog
final class ApiNavigationSubtreePaths {

    private ApiNavigationSubtreePaths() {}

    record PathedFolder(PortalNavigationItem folder, String path) {}

    static List<PathedFolder> collect(
        String organizationId,
        String environmentId,
        String navApiRowId,
        Map<String, List<PortalNavigationItem>> childrenByParentId
    ) {
        var result = new ArrayList<PathedFolder>();
        walk(organizationId, environmentId, navApiRowId, navApiRowId, "", childrenByParentId, result);
        return result;
    }

    private static void walk(
        String organizationId,
        String environmentId,
        String navApiRowId,
        String parentId,
        String parentPath,
        Map<String, List<PortalNavigationItem>> childrenByParentId,
        List<PathedFolder> result
    ) {
        var matches = childrenByParentId
            .getOrDefault(parentId, List.of())
            .stream()
            .filter(child -> child.getType() == PortalNavigationItem.Type.FOLDER)
            .map(child -> matchLegacyFolder(organizationId, environmentId, navApiRowId, parentPath, child))
            .flatMap(Optional::stream)
            .toList();

        for (var match : matches) {
            result.add(match);
            walk(organizationId, environmentId, navApiRowId, match.folder().getId(), match.path(), childrenByParentId, result);
        }
    }

    private static Optional<PathedFolder> matchLegacyFolder(
        String organizationId,
        String environmentId,
        String navApiRowId,
        String parentPath,
        PortalNavigationItem child
    ) {
        var segment = child.getSegment();
        if (segment == null || segment.isBlank()) {
            log.warn(
                "Navigation folder [id={}] under legacy nav-api row [id={}] has a blank segment; not re-keying it or its descendants",
                child.getId(),
                navApiRowId
            );
            return Optional.empty();
        }
        var path = parentPath + "/" + segment;
        var legacyId = HRIDToUUID.navigation().context(organizationId, environmentId).api(navApiRowId).folder(path).id();
        return legacyId.equals(child.getId()) ? Optional.of(new PathedFolder(child, path)) : Optional.empty();
    }
}
