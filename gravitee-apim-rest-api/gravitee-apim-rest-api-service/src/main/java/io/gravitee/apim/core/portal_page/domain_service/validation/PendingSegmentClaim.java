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

import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;

/**
 * A (parent, segment) slot associated with a pending create or update in the current validation batch.
 * {@link Intent#CLAIM} means the item intends to occupy the slot; {@link Intent#RELEASE} means an update is
 * about to leave the slot free (recorded so a same-batch sibling can safely take it over).
 */
public record PendingSegmentClaim(PortalNavigationItemId id, PortalNavigationItemId parentId, String segment, Intent intent) {
    public enum Intent {
        CLAIM,
        RELEASE,
    }

    public static PendingSegmentClaim forCreate(CreatePortalNavigationItem create) {
        return new PendingSegmentClaim(create.getId(), create.getParentId(), create.getSegment(), Intent.CLAIM);
    }

    public static PendingSegmentClaim forUpdate(PortalNavigationItem existing, UpdatePortalNavigationItem update) {
        return new PendingSegmentClaim(existing.getId(), update.getParentId(), update.getSegment(), Intent.CLAIM);
    }

    public static PendingSegmentClaim forRelease(PortalNavigationItem existing) {
        return new PendingSegmentClaim(existing.getId(), existing.getParentId(), existing.getSegment(), Intent.RELEASE);
    }
}
