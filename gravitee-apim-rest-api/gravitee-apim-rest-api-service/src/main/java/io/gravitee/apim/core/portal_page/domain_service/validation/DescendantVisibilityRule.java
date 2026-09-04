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

import io.gravitee.apim.core.portal.domain_service.navigation.PortalNavigationValidator.PendingUpdate;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.exception.ConflictingNavigationItemStateException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Rejects an update that flips an item to PRIVATE while any descendant still resolves to PUBLIC.
 * The Automation API must not cascade — that would rewrite rows GKO still declares PUBLIC and
 * produce permanent drift. Same-batch updates on descendants are honored via pendingUpdatesByExistingId,
 * so a caller can flip parent and children in one call.
 */
@RequiredArgsConstructor
public class DescendantVisibilityRule implements UpdatePortalNavigationItemValidationRule {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return (
            PortalVisibility.PRIVATE.equals(toUpdate.getVisibility()) && !PortalVisibility.PRIVATE.equals(existingItem.getVisibility())
        );
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        var pendingUpdates = ctx.pendingUpdatesByExistingId();
        Deque<PortalNavigationItem> frontier = new ArrayDeque<>(
            navigationItemsQueryService.findByParentIdAndEnvironmentId(existingItem.getEnvironmentId(), existingItem.getId())
        );
        while (!frontier.isEmpty()) {
            var descendant = frontier.pop();
            if (effectiveVisibility(descendant, pendingUpdates) == PortalVisibility.PUBLIC) {
                throw ConflictingNavigationItemStateException.descendantsMustBePrivateFirst(existingItem.getId().toString());
            }
            frontier.addAll(
                navigationItemsQueryService.findByParentIdAndEnvironmentId(existingItem.getEnvironmentId(), descendant.getId())
            );
        }
    }

    private static PortalVisibility effectiveVisibility(PortalNavigationItem item, Map<?, PendingUpdate> pendingUpdates) {
        var pending = pendingUpdates.get(item.getId());
        if (pending != null && pending.toUpdate().getVisibility() != null) {
            return pending.toUpdate().getVisibility();
        }
        return item.getVisibility();
    }
}
