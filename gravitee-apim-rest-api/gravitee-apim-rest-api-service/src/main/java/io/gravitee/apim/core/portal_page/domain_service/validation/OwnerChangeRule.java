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

import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A move that hands an item, and everything below it, to another owner. Automation names the owner of what
 * it manages and would no longer recognise it, and the documentation of an API cannot itself list an API
 * or an API Product.
 */
public class OwnerChangeRule implements UpdatePortalNavigationItemValidationRule {

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return toUpdate.getReference() != null;
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        boolean becomesApiDocumentation = toUpdate.getReference() instanceof NavigationItemReference.ApiReference;
        for (var moved : withDescendants(existingItem, ctx.navigationItems())) {
            if (moved.getAutomationMetadata() != null) {
                throw InvalidPortalNavigationItemDataException.automationManagedItemCannotChangeOwner(moved.getId().json());
            }
            if (becomesApiDocumentation && (moved instanceof PortalNavigationApi || moved instanceof PortalNavigationApiProduct)) {
                throw InvalidPortalNavigationItemDataException.apiDocumentationCannotHoldListing(moved.getId().json());
            }
        }
    }

    private static List<PortalNavigationItem> withDescendants(PortalNavigationItem item, List<PortalNavigationItem> allItems) {
        Map<PortalNavigationItemId, List<PortalNavigationItem>> childrenByParent = allItems
            .stream()
            .filter(candidate -> candidate.getParentId() != null)
            .collect(Collectors.groupingBy(PortalNavigationItem::getParentId));

        var subtree = new ArrayList<PortalNavigationItem>();
        var visitedIds = new HashSet<PortalNavigationItemId>();
        var toVisit = new ArrayDeque<PortalNavigationItem>();
        toVisit.push(item);
        while (!toVisit.isEmpty()) {
            var current = toVisit.pop();
            if (!visitedIds.add(current.getId())) {
                continue;
            }
            subtree.add(current);
            childrenByParent.getOrDefault(current.getId(), List.of()).forEach(toVisit::push);
        }
        return subtree;
    }
}
