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
package io.gravitee.apim.core.portal.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.portal.domain_service.navigation.PortalNavigationTreeWalker;
import io.gravitee.apim.core.portal.domain_service.navigation.PortalNavigationVisitor;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationListingDomainService {

    private final PortalNavigationItemsQueryService queryService;

    public List<NavigationPath> listAsNavigationPaths(String environmentId) {
        final var folders = queryService.search(
            PortalNavigationItemQueryCriteria.builder()
                .environmentId(environmentId)
                .area(PortalArea.TOP_NAVBAR)
                .type(PortalNavigationItemType.FOLDER)
                .build()
        );
        final var collector = new PathCollector();
        PortalNavigationTreeWalker.walk(folders, collector);
        return collector.paths;
    }

    private static final class PathCollector implements PortalNavigationVisitor {

        private final List<NavigationPath> paths = new ArrayList<>();

        @Override
        public void visitFolder(PortalNavigationFolder folder, NavigationPath parentPath) {
            final var nodePath = parentPath.descend(folder.getEffectiveSegment());
            final var displayName = !folder.getEffectiveSegment().equals(folder.getTitle()) ? folder.getTitle() : null;
            paths.add(new NavigationPath(nodePath.path(), displayName));
        }
    }
}
