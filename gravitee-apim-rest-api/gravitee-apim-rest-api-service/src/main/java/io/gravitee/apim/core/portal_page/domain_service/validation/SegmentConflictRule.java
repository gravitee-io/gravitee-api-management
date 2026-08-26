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

import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/** Rejects items whose (parent, segment) is already claimed — by the DB or by another item in the same batch. */
@RequiredArgsConstructor
public class SegmentConflictRule implements CreatePortalNavigationItemValidationRule, UpdatePortalNavigationItemValidationRule {

    private static final Set<PortalNavigationItemType> SEGMENT_CHECKED_TYPES = EnumSet.of(
        PortalNavigationItemType.FOLDER,
        PortalNavigationItemType.LINK,
        PortalNavigationItemType.API,
        PortalNavigationItemType.API_PRODUCT
    );

    private final PortalNavigationItemsQueryService navigationItemsQueryService;

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        if (item.getSegment() == null || item.getId() == null) {
            return false;
        }
        return SEGMENT_CHECKED_TYPES.contains(item.getType());
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        if (collidesWithPendingBatch(item, ctx) || collidesWithPersistedSibling(item, environmentId)) {
            throw exceptionFor(item);
        }
    }

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        if (toUpdate.getSegment() == null) {
            return false;
        }
        return SEGMENT_CHECKED_TYPES.contains(existingItem.getType());
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        if (
            collidesWithPersistedSibling(
                existingItem.getId(),
                toUpdate.getParentId(),
                toUpdate.getSegment(),
                existingItem.getEnvironmentId()
            )
        ) {
            throw exceptionFor(existingItem);
        }
    }

    private static boolean collidesWithPendingBatch(CreatePortalNavigationItem item, CreateValidationContext ctx) {
        return ctx
            .pendingItemsById()
            .values()
            .stream()
            .anyMatch(
                other ->
                    !Objects.equals(other.getId(), item.getId()) &&
                    Objects.equals(other.getParentId(), item.getParentId()) &&
                    Objects.equals(other.getSegment(), item.getSegment())
            );
    }

    private boolean collidesWithPersistedSibling(CreatePortalNavigationItem item, String environmentId) {
        return collidesWithPersistedSibling(item.getId(), item.getParentId(), item.getSegment(), environmentId);
    }

    private boolean collidesWithPersistedSibling(
        io.gravitee.apim.core.portal_page.model.PortalNavigationItemId itemId,
        io.gravitee.apim.core.portal_page.model.PortalNavigationItemId parentId,
        String segment,
        String environmentId
    ) {
        return navigationItemsQueryService
            .findByParentIdAndSegment(environmentId, parentId, segment)
            .filter(sibling -> !sibling.getId().equals(itemId))
            .isPresent();
    }

    private static PathConflictException exceptionFor(CreatePortalNavigationItem item) {
        var location = item.getAutomationMetadata() != null
            ? item.getAutomationMetadata().location().orElse(item.getSegment())
            : item.getSegment();
        return exceptionFor(item.getType(), location);
    }

    private static PathConflictException exceptionFor(PortalNavigationItem existing) {
        var location = existing.getAutomationMetadata() != null
            ? existing.getAutomationMetadata().location().orElse(existing.getSegment())
            : existing.getSegment();
        return exceptionFor(existing.getType(), location);
    }

    private static PathConflictException exceptionFor(PortalNavigationItemType type, String location) {
        return switch (type) {
            case FOLDER -> PathConflictException.folderPath(location);
            case LINK -> PathConflictException.segmentTaken(PathConflictException.EntryKind.LINK, location);
            case API, API_PRODUCT -> PathConflictException.segmentTaken(PathConflictException.EntryKind.LISTING, location);
            default -> throw new IllegalStateException("SegmentConflictRule does not apply to " + type);
        };
    }
}
