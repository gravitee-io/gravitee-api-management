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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationTreeWalkerTest {

    private static final PortalNavigationItemId ROOT = PortalNavigationItemId.of("11111111-1111-1111-1111-111111111111");
    private static final PortalNavigationItemId CHILD_A = PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final PortalNavigationItemId CHILD_B = PortalNavigationItemId.of("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final PortalNavigationItemId GRANDCHILD = PortalNavigationItemId.of("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Test
    void walk_from_visits_descendants_in_pre_order_without_visiting_root() {
        var items = List.<PortalNavigationItem>of(
            folder(ROOT, null, "root", 0),
            folder(CHILD_A, ROOT, "a", 0),
            folder(GRANDCHILD, CHILD_A, "g", 0),
            folder(CHILD_B, ROOT, "b", 1)
        );
        var visited = new ArrayList<String>();
        var visitor = new PortalNavigationVisitor() {
            @Override
            public void visitFolder(PortalNavigationFolder f, NavigationPath parentPath) {
                visited.add(f.getSegment());
            }
        };

        PortalNavigationTreeWalker.walkFrom(items, ROOT, visitor);

        assertThat(visited).containsExactly("a", "g", "b");
    }

    @Test
    void walk_from_with_unknown_root_is_a_no_op() {
        var items = List.<PortalNavigationItem>of(folder(ROOT, null, "root", 0), folder(CHILD_A, ROOT, "a", 0));
        var visited = new ArrayList<String>();
        var visitor = new PortalNavigationVisitor() {
            @Override
            public void visitFolder(PortalNavigationFolder f, NavigationPath parentPath) {
                visited.add(f.getSegment());
            }
        };

        PortalNavigationTreeWalker.walkFrom(items, PortalNavigationItemId.of("00000000-0000-0000-0000-000000000000"), visitor);

        assertThat(visited).isEmpty();
    }

    @Test
    void walk_visits_api_product_and_its_children() {
        var apiProduct = apiProduct(ROOT, null, "product", 0);
        var items = List.<PortalNavigationItem>of(apiProduct, folder(CHILD_A, ROOT, "child", 0));
        var visited = new ArrayList<String>();
        var visitor = new PortalNavigationVisitor() {
            @Override
            public void visitApiProduct(PortalNavigationApiProduct product, NavigationPath parentPath) {
                visited.add(product.getSegment());
            }

            @Override
            public void visitFolder(PortalNavigationFolder folder, NavigationPath parentPath) {
                visited.add(parentPath.path() + "/" + folder.getSegment());
            }
        };

        PortalNavigationTreeWalker.walk(items, visitor);

        assertThat(visited).containsExactly("product", "/product/child");
    }

    private static PortalNavigationFolder folder(PortalNavigationItemId id, PortalNavigationItemId parentId, String segment, int order) {
        return PortalNavigationFolder.builder()
            .id(id)
            .organizationId("organization-id")
            .environmentId("environment-id")
            .title(segment)
            .segment(segment)
            .area(PortalArea.TOP_NAVBAR)
            .order(order)
            .parentId(parentId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private static PortalNavigationApiProduct apiProduct(
        PortalNavigationItemId id,
        PortalNavigationItemId parentId,
        String segment,
        int order
    ) {
        return PortalNavigationApiProduct.builder()
            .id(id)
            .organizationId("organization-id")
            .environmentId("environment-id")
            .title(segment)
            .segment(segment)
            .area(PortalArea.TOP_NAVBAR)
            .order(order)
            .parentId(parentId)
            .apiProductId("00000000-0000-0000-0000-000000000020")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }
}
