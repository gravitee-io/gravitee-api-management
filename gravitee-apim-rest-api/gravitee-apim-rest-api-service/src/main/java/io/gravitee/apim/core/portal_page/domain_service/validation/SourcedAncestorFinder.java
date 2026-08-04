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

import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Walks up the parent chain looking for an item carrying an external source — every item below a
 * sourced FOLDER is managed by the fetcher and read-only.
 */
@RequiredArgsConstructor
public class SourcedAncestorFinder {

    private final PortalNavigationItemsQueryService queryService;

    public Optional<PortalNavigationItem> findSourcedAncestor(String environmentId, PortalNavigationItem item) {
        return findSourcedAncestorFrom(environmentId, item.getParentId());
    }

    /** Same walk from a parent id, so an item about to be created can be checked against its target parent. */
    public Optional<PortalNavigationItem> findSourcedAncestorFrom(String environmentId, PortalNavigationItemId parentId) {
        int depth = 0;
        while (parentId != null) {
            if (ValidationDepth.exceeded(depth++, parentId, "the read-only restriction is not enforced above")) {
                return Optional.empty();
            }
            var parent = queryService.findByIdAndEnvironmentId(environmentId, parentId);
            if (parent == null) {
                return Optional.empty();
            }
            if (parent.getSource() != null) {
                return Optional.of(parent);
            }
            parentId = parent.getParentId();
        }
        return Optional.empty();
    }
}
