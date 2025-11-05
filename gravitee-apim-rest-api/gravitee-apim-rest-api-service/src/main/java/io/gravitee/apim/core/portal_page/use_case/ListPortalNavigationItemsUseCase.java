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
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPortalNavigationItemsUseCase {

    private final PortalNavigationItemsQueryService queryService;
    private static final Predicate<PortalNavigationItem> IS_FOLDER_PREDICATE = i -> i instanceof PortalNavigationFolder;

    public Output execute(Input input) {
        var directItems = input
            .parentId()
            .map(parentId -> queryService.findByParentIdAndEnvironmentId(input.environmentId(), parentId))
            .orElse(queryService.findTopLevelItemsByEnvironmentIdAndPortalArea(input.environmentId(), input.portalArea()));

        var items = new ArrayList<>(directItems);

        if (!input.loadChildren()) {
            return new Output(items);
        }

        var queue = new ArrayList<>(items.stream().filter(IS_FOLDER_PREDICATE).toList());

        while (!queue.isEmpty()) {
            var current = queue.removeFirst();
            var children = queryService.findByParentIdAndEnvironmentId(current.getEnvironmentId(), current.getId());
            items.addAll(children);
            queue.addAll(children.stream().filter(IS_FOLDER_PREDICATE).toList());
        }

        return new Output(items);
    }

    public record Output(List<PortalNavigationItem> items) {}

    public record Input(String environmentId, PortalArea portalArea, Optional<PortalNavigationItemId> parentId, boolean loadChildren) {}
}
