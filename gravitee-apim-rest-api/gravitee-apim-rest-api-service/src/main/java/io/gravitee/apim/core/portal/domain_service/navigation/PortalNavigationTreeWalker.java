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

import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import jakarta.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Pre-order traversal, children sorted by {@code order}. */
public final class PortalNavigationTreeWalker {

    private static final NavigationPath ROOT = new NavigationPath("", null);
    private static final Comparator<PortalNavigationItem> BY_ORDER = Comparator.comparingInt(PortalNavigationItem::getOrder);

    private PortalNavigationTreeWalker() {}

    public static void walk(List<PortalNavigationItem> items, PortalNavigationVisitor visitor) {
        walkFrom(items, null, visitor);
    }

    public static void walkFrom(
        List<PortalNavigationItem> items,
        @Nullable PortalNavigationItemId rootId,
        PortalNavigationVisitor visitor
    ) {
        final var childrenByParent = childrenByParent(items);
        final List<PortalNavigationItem> startingNodes = rootId == null
            ? topLevelElements(items)
            : childrenByParent.getOrDefault(rootId, List.of());
        startingNodes.forEach(node -> traverse(node, ROOT, childrenByParent, visitor));
    }

    private static Map<PortalNavigationItemId, List<PortalNavigationItem>> childrenByParent(List<PortalNavigationItem> items) {
        return items
            .stream()
            .filter(i -> i.getParentId() != null)
            .sorted(BY_ORDER)
            .collect(Collectors.groupingBy(PortalNavigationItem::getParentId));
    }

    private static List<PortalNavigationItem> topLevelElements(List<PortalNavigationItem> items) {
        return items
            .stream()
            .filter(i -> i.getParentId() == null)
            .sorted(BY_ORDER)
            .toList();
    }

    private static void traverse(
        PortalNavigationItem node,
        NavigationPath parentPath,
        Map<PortalNavigationItemId, List<PortalNavigationItem>> childrenByParent,
        PortalNavigationVisitor visitor
    ) {
        dispatch(node, parentPath, visitor);
        final var nodePath = parentPath.descend(node.getEffectiveSegment());
        childrenByParent.getOrDefault(node.getId(), List.of()).forEach(child -> traverse(child, nodePath, childrenByParent, visitor));
    }

    private static void dispatch(PortalNavigationItem node, NavigationPath parentPath, PortalNavigationVisitor visitor) {
        switch (node) {
            case PortalNavigationFolder f -> visitor.visitFolder(f, parentPath);
            case PortalNavigationPage p -> visitor.visitPage(p, parentPath);
            case PortalNavigationApi a -> visitor.visitApi(a, parentPath);
            case PortalNavigationLink l -> visitor.visitLink(l, parentPath);
        }
    }
}
