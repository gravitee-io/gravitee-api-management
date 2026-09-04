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
 * Reconstructs the {@code portalNavigation} path of every automation-owned {@code FOLDER} descendant
 * of a nav-api row, so the migration can derive that folder's parent, root and reference in the
 * API-owned scheme.
 * <p>
 * Legacy folders are discovered by walking down from the nav-api row because that is where they were
 * <em>parented</em>. Their <em>id</em>, however, was never nav-api-derived: {@code forApiFolder} has
 * always been keyed on the API, so a legacy folder already sits at the id it will keep. Only its
 * parent, root and reference are stale.
 * <p>
 * "Automation-owned" is decided the way the runtime itself decides it when rejecting a conflicting
 * foreign item ({@code NavigationSyncPlanExecutor#rejectIfSegmentTakenByForeignItem}): by id, not by
 * metadata. Folders never carried {@code automationMetadata}, and pre-migration folders never carried
 * a stamped {@code reference} either, so a hand-created folder is indistinguishable from an automation
 * folder on metadata alone. An automation folder's id is deterministic — {@code forApiFolder} for its
 * own reconstructed path — so a candidate whose stored id does not match that formula was never
 * written by automation, whatever it is. Its subtree is left alone, exactly as a hand-created folder
 * standing where automation planned to write one is left alone today.
 * <p>
 * This is also why paths are not chained through parent folder ids: the formula takes one full path
 * string per folder, never nested per ancestor.
 */
@CustomLog
final class ApiNavigationSubtreePaths {

    private ApiNavigationSubtreePaths() {}

    record PathedFolder(PortalNavigationItem folder, String path) {}

    static List<PathedFolder> collect(
        String organizationId,
        String environmentId,
        String navApiRowId,
        String apiId,
        Map<String, List<PortalNavigationItem>> childrenByParentId
    ) {
        var result = new ArrayList<PathedFolder>();
        walk(organizationId, environmentId, navApiRowId, apiId, navApiRowId, "", childrenByParentId, result);
        return result;
    }

    private static void walk(
        String organizationId,
        String environmentId,
        String navApiRowId,
        String apiId,
        String parentId,
        String parentPath,
        Map<String, List<PortalNavigationItem>> childrenByParentId,
        List<PathedFolder> result
    ) {
        var matches = childrenByParentId
            .getOrDefault(parentId, List.of())
            .stream()
            .filter(child -> child.getType() == PortalNavigationItem.Type.FOLDER)
            .map(child -> matchAutomationFolder(organizationId, environmentId, navApiRowId, apiId, parentPath, child))
            .flatMap(Optional::stream)
            .toList();

        for (var match : matches) {
            result.add(match);
            walk(organizationId, environmentId, navApiRowId, apiId, match.folder().getId(), match.path(), childrenByParentId, result);
        }
    }

    private static Optional<PathedFolder> matchAutomationFolder(
        String organizationId,
        String environmentId,
        String navApiRowId,
        String apiId,
        String parentPath,
        PortalNavigationItem child
    ) {
        var segment = child.getSegment();
        if (segment == null || segment.isBlank()) {
            log.warn(
                "Navigation folder [id={}] under legacy nav-api row [id={}] has a blank segment; not realigning it or its descendants",
                child.getId(),
                navApiRowId
            );
            return Optional.empty();
        }
        var path = parentPath + "/" + segment;
        var expectedId = HRIDToUUID.navigation().context(organizationId, environmentId).api(apiId).folder(path).id();
        return expectedId.equals(child.getId()) ? Optional.of(new PathedFolder(child, path)) : Optional.empty();
    }
}
