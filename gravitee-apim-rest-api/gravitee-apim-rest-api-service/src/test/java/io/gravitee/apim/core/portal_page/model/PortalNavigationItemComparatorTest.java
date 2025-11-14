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
package io.gravitee.apim.core.portal_page.model;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalNavigationItemFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemComparatorTest {

    @Test
    void should_order_items_by_parentId_nulls_first_then_by_order_nulls_last() {
        var parent = PortalNavigationItemId.random();

        var itemWithNullParent = PortalNavigationItemFixtures.aFolder(PortalNavigationItemId.random().toString(), "A");
        itemWithNullParent.setOrder(2);

        var itemWithParentOrderNull = PortalNavigationItemFixtures.aFolder(PortalNavigationItemId.random().toString(), "B", parent);
        itemWithParentOrderNull.setOrder(null);

        var itemWithParentOrder1 = PortalNavigationItemFixtures.aFolder(PortalNavigationItemId.random().toString(), "C", parent);
        itemWithParentOrder1.setOrder(1);

        var itemWithParentOrder2 = PortalNavigationItemFixtures.aFolder(PortalNavigationItemId.random().toString(), "D", parent);
        itemWithParentOrder2.setOrder(2);

        List<PortalNavigationItem> items = new ArrayList<>(
            List.of(itemWithParentOrder2, itemWithNullParent, itemWithParentOrder1, itemWithParentOrderNull)
        );

        items.sort(PortalNavigationItemComparator.byNullableParentIdThenNullableOrder());

        assertThat(items.getFirst()).isEqualTo(itemWithNullParent); // null parent first
        // then items with same parent ordered by order (nulls last -> itemWithParentOrder1 (1), itemWithParentOrder2 (2), itemWithParentOrderNull (null))
        assertThat(items.subList(1, 4)).containsExactly(itemWithParentOrder1, itemWithParentOrder2, itemWithParentOrderNull);
    }
}
