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
package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalPageNavigationId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPortalNavigationItemsUseCase {

    private final PortalNavigationItemsQueryService queryService;

    public Output execute(Input input) {
        var directItems = input
            .parentId()
            .map(parentId -> queryService.findByParentIdAndEnvironmentId(input.environmentId(), parentId))
            .orElse(queryService.findTopLevelItemsByEnvironmentId(input.environmentId(), input.portalArea()));

        var items = new ArrayList<>(directItems);

        if (input.loadChildren()) {
            for (var item : directItems) {
                if (item instanceof PortalNavigationFolder) {
                    addChildrenRecursively(items, item.getId(), input.environmentId());
                }
            }
        }

        return new Output(items);
    }

    private void addChildrenRecursively(List<PortalNavigationItem> items, PortalPageNavigationId parentId, String environmentId) {
        var children = queryService.findByParentIdAndEnvironmentId(environmentId, parentId);
        for (var child : children) {
            items.add(child);
            if (child instanceof PortalNavigationFolder) {
                addChildrenRecursively(items, child.getId(), environmentId);
            }
        }
    }

    public record Output(List<PortalNavigationItem> items) {}

    public record Input(String environmentId, PortalArea portalArea, Optional<PortalPageNavigationId> parentId, boolean loadChildren) {}
}
