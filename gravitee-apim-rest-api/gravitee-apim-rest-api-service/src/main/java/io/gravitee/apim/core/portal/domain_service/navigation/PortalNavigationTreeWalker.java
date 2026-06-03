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
package io.gravitee.apim.core.portal.domain_service.navigation;

import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Pre-order traversal, children sorted by {@code order}. */
public final class PortalNavigationTreeWalker {

    private static final String PATH_DELIMITER = "/";

    private PortalNavigationTreeWalker() {}

    public static void walk(List<PortalNavigationItem> items, PortalNavigationVisitor visitor) {
        final Comparator<PortalNavigationItem> byOrder = Comparator.comparingInt(PortalNavigationItem::getOrder);
        final var childrenByParent = items
            .stream()
            .filter(i -> i.getParentId() != null)
            .collect(Collectors.groupingBy(PortalNavigationItem::getParentId));
        childrenByParent.values().forEach(list -> list.sort(byOrder));

        items
            .stream()
            .filter(i -> i.getParentId() == null)
            .sorted(byOrder)
            .forEach(root -> traverse(root, "", childrenByParent, visitor));
    }

    private static void traverse(
        PortalNavigationItem node,
        String parentPath,
        Map<PortalNavigationItemId, List<PortalNavigationItem>> childrenByParent,
        PortalNavigationVisitor visitor
    ) {
        dispatch(node, parentPath, visitor);
        final var fullPath = parentPath + PATH_DELIMITER + node.getEffectiveSegment();
        childrenByParent.getOrDefault(node.getId(), List.of()).forEach(child -> traverse(child, fullPath, childrenByParent, visitor));
    }

    private static void dispatch(PortalNavigationItem node, String parentPath, PortalNavigationVisitor visitor) {
        switch (node) {
            case PortalNavigationFolder f -> visitor.visitFolder(f, parentPath);
            case PortalNavigationPage p -> visitor.visitPage(p, parentPath);
            case PortalNavigationApi a -> visitor.visitApi(a, parentPath);
            case PortalNavigationLink l -> visitor.visitLink(l, parentPath);
        }
    }
}
