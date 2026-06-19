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

import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.Set;
import java.util.function.Function;

/**
 * Precomputed ownership signals for one Portal-scoped (or api-folder-scoped) navigation sync.
 *
 * <p>A folder at path {@code P} is automation-owned iff both:
 * <ul>
 *   <li>{@code P} is in {@link #managedFolderPaths} (path-based check)</li>
 *   <li>the folder's id equals {@link #folderIdByPath}.apply(P) (id-based check)</li>
 * </ul>
 *
 * <p>The leaf-id sets ({@link #automationManagedPageIds} and {@link #automationManagedNavApiIds}) double as
 * the {@code DeleteStrategy.shouldSkip} input — no need to re-query the navigation items service.
 */
public record NavigationOwnership(
    Set<String> managedFolderPaths,
    Function<String, PortalNavigationItemId> folderIdByPath,
    Set<PortalNavigationItemId> automationManagedPageIds,
    Set<PortalNavigationItemId> automationManagedNavApiIds
) {
    public Set<PortalNavigationItemId> automationManagedLeafIds() {
        var union = new java.util.HashSet<PortalNavigationItemId>(automationManagedPageIds.size() + automationManagedNavApiIds.size());
        union.addAll(automationManagedPageIds);
        union.addAll(automationManagedNavApiIds);
        return java.util.Set.copyOf(union);
    }

    public DeleteStrategy asDeleteStrategy() {
        var skipIds = automationManagedLeafIds();
        return new DeleteStrategy(item -> skipIds.contains(item.getId()), true);
    }
}
