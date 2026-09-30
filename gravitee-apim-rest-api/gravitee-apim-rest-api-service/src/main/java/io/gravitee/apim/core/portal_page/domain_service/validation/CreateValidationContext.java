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
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context built once per create validation (single or bulk) to hold shared data and avoid repeated fetches.
 *
 * {@code pendingContentIds} lets a caller declare that a content id, though not yet persisted, is about to be
 * written by the same operation this validation is gating — see {@link PageContentExistsRule}.
 *
 * {@code itemIdsBeingReplaced} lets a caller exclude an existing item's id from a conflict check — see
 * {@link UniqueItemIdRule} and {@link HomepageUniquenessRule}.
 */
public record CreateValidationContext(
    List<PortalNavigationItem> navigationItems,
    Map<PortalNavigationItemId, PortalNavigationItem> itemsById,
    Map<PortalNavigationItemId, CreatePortalNavigationItem> pendingItemsById,
    Map<PortalNavigationItemId, PendingUpdate> pendingUpdatesByExistingId,
    List<PendingSegmentClaim> pendingSegmentClaims,
    Set<PortalPageContentId> pendingContentIds,
    Set<PortalNavigationItemId> itemIdsBeingReplaced
) {
    public CreateValidationContext(
        List<PortalNavigationItem> navigationItems,
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById,
        Map<PortalNavigationItemId, CreatePortalNavigationItem> pendingItemsById,
        Map<PortalNavigationItemId, PendingUpdate> pendingUpdatesByExistingId,
        List<PendingSegmentClaim> pendingSegmentClaims
    ) {
        this(navigationItems, itemsById, pendingItemsById, pendingUpdatesByExistingId, pendingSegmentClaims, Set.of(), Set.of());
    }

    public static CreateValidationContext empty() {
        return new CreateValidationContext(List.of(), Map.of(), Map.of(), Map.of(), List.of());
    }

    public static CreateValidationContext replacing(Set<PortalNavigationItemId> itemIdsBeingReplaced) {
        return new CreateValidationContext(List.of(), Map.of(), Map.of(), Map.of(), List.of(), Set.of(), itemIdsBeingReplaced);
    }
}
