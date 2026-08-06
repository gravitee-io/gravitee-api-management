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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/** Walks down the subtree collecting every PAGE descendant carrying its own external source. */
@RequiredArgsConstructor
public class SourcedPageDescendantsFinder {

    private final PortalNavigationItemsQueryService queryService;

    public List<PortalNavigationPage> findSourcedPageDescendants(String environmentId, PortalNavigationItemId itemId) {
        var result = new ArrayList<PortalNavigationPage>();
        collect(environmentId, itemId, 0, new HashSet<>(), result);
        return result;
    }

    private void collect(
        String environmentId,
        PortalNavigationItemId parentId,
        int depth,
        Set<PortalNavigationItemId> visited,
        List<PortalNavigationPage> result
    ) {
        // visited, not just the depth bound: a cycle would otherwise collect — and so fetch — a page several times
        if (ValidationDepth.exceeded(depth, parentId, "sourced pages below are not collected") || !visited.add(parentId)) {
            return;
        }
        for (var child : queryService.findByParentIdAndEnvironmentId(environmentId, parentId)) {
            if (child instanceof PortalNavigationPage page && page.getSource() != null && !visited.contains(page.getId())) {
                result.add(page);
            }
            collect(environmentId, child.getId(), depth + 1, visited, result);
        }
    }
}
