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
package io.gravitee.apim.core.portal.domain_service.navigation.plan;

import io.gravitee.apim.core.portal.domain_service.navigation.actions.FolderActions.CreateFolder;
import io.gravitee.apim.core.portal.domain_service.navigation.actions.FolderActions.DeleteFolder;
import io.gravitee.apim.core.portal.domain_service.navigation.actions.FolderActions.DesiredFolder;
import io.gravitee.apim.core.portal.domain_service.navigation.actions.FolderActions.FolderMutation;
import io.gravitee.apim.core.portal.domain_service.navigation.actions.FolderActions.UpdateFolder;
import io.gravitee.apim.core.portal.domain_service.navigation.actions.NavigationAction;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Pure — no side effects, no I/O. Folders match by path, not by ID. */
public final class NavigationSyncPlanner {

    private static final String PATH_DELIMITER = "/";

    private NavigationSyncPlanner() {}

    /**
     * Diffs the desired navigation against what is currently in the tree, restricting deletes
     * to paths that were previously persisted on the Portal entity.
     *
     * <p>Folders present in the tree but not in {@code previouslyPersisted} are treated as
     * "unmanaged" (created outside the automation API) and are left untouched.
     */
    public static NavigationSyncPlan plan(
        List<NavigationPath> desired,
        List<PortalNavigationItem> currentFolders,
        List<NavigationPath> previouslyPersisted
    ) {
        final var desiredFolders = computeDesiredFolders(desired);
        final var currentByPath = indexByPath(currentFolders);
        final var desiredPaths = desiredFolders.stream().map(DesiredFolder::path).collect(Collectors.toSet());
        final var managedPaths = expandToFullPaths(previouslyPersisted);
        final var depthOrder = Comparator.comparingInt((FolderMutation m) -> PathExpander.depth(m.desired().path()));

        final var mutations = desiredFolders
            .stream()
            .<FolderMutation>map(df ->
                currentByPath.containsKey(df.path()) ? new UpdateFolder(currentByPath.get(df.path()), df) : new CreateFolder(df)
            )
            .sorted(depthOrder);

        final var deletes = currentByPath
            .entrySet()
            .stream()
            .filter(e -> !desiredPaths.contains(e.getKey()))
            .filter(e -> managedPaths.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .map(DeleteFolder::new);

        return new NavigationSyncPlan(Stream.<NavigationAction>concat(mutations, deletes).toList());
    }

    private static Set<String> expandToFullPaths(List<NavigationPath> previouslyPersisted) {
        if (previouslyPersisted == null) return Set.of();
        return previouslyPersisted
            .stream()
            .flatMap(e -> PathExpander.expand(e.path()).stream())
            .map(PathExpander.PathEntry::fullPath)
            .collect(Collectors.toSet());
    }

    /** Reconstructs the path of every current folder by walking from its root, using {@code segment} (fallback to {@code title}). */
    private static Map<String, PortalNavigationFolder> indexByPath(List<PortalNavigationItem> currentFolders) {
        final var byId = currentFolders
            .stream()
            .filter(PortalNavigationFolder.class::isInstance)
            .map(PortalNavigationFolder.class::cast)
            .collect(Collectors.toMap(PortalNavigationItem::getId, f -> f));
        final var result = new HashMap<String, PortalNavigationFolder>();
        for (PortalNavigationFolder folder : byId.values()) {
            result.put(reconstructPath(folder, byId), folder);
        }
        return result;
    }

    private static String reconstructPath(PortalNavigationFolder folder, Map<PortalNavigationItemId, PortalNavigationFolder> byId) {
        return PATH_DELIMITER + String.join(PATH_DELIMITER, walkSegmentChain(folder, byId));
    }

    /** Walks the parent chain from {@code folder} up to the root, returning segments in root-first order. */
    private static Deque<String> walkSegmentChain(PortalNavigationFolder folder, Map<PortalNavigationItemId, PortalNavigationFolder> byId) {
        List<String> segments = Stream.iterate(folder, Objects::nonNull, item -> item.hasParent() ? byId.get(item.getParentId()) : null)
            .map(PortalNavigationItem::getEffectiveSegment)
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(segments);
        return new ArrayDeque<>(segments);
    }

    private static List<DesiredFolder> computeDesiredFolders(List<NavigationPath> input) {
        final var displayNames = explicitDisplayNames(input);
        final var uniqueEntries = deduplicateByPath(input);
        final var orderByPath = assignSiblingOrder(uniqueEntries);
        return uniqueEntries
            .stream()
            .map(pe -> buildDesiredFolder(pe, displayNames, orderByPath.get(pe.fullPath())))
            .toList();
    }

    private static Map<String, String> explicitDisplayNames(List<NavigationPath> input) {
        return input
            .stream()
            .filter(e -> e.displayName() != null)
            .collect(Collectors.toMap(NavigationPath::path, NavigationPath::displayName, (first, second) -> first));
    }

    private static Collection<PathExpander.PathEntry> deduplicateByPath(List<NavigationPath> input) {
        return input
            .stream()
            .flatMap(e -> PathExpander.expand(e.path()).stream())
            .collect(Collectors.toMap(PathExpander.PathEntry::fullPath, pe -> pe, (first, second) -> first, LinkedHashMap::new))
            .values();
    }

    private static Map<String, Integer> assignSiblingOrder(Collection<PathExpander.PathEntry> entries) {
        final var counter = new HashMap<String, Integer>();
        final var orderByPath = new HashMap<String, Integer>();
        entries.forEach(pe -> {
            final var parentKey = pe.parentPath() != null ? pe.parentPath() : "";
            orderByPath.put(pe.fullPath(), counter.merge(parentKey, 1, Integer::sum) - 1);
        });
        return orderByPath;
    }

    private static DesiredFolder buildDesiredFolder(PathExpander.PathEntry pe, Map<String, String> displayNames, int order) {
        return new DesiredFolder(
            pe.fullPath(),
            pe.parentPath(),
            pe.segment(),
            displayNames.getOrDefault(pe.fullPath(), pe.segment().value()),
            order
        );
    }
}
