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
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * External sources are mutually exclusive with automation-managed navigation, because both write the
 * content of the pages they own. Automation ownership is carried by {@code AutomationMetadata} on the
 * page content.
 * <p>
 * The conflict area is exactly what a source would own: the item itself and everything below it.
 * Ancestors are never checked — only a PAGE can be automation-managed, and a PAGE is never a container.
 */
@RequiredArgsConstructor
public class SourceAutomationExclusivityRule implements CreatePortalNavigationItemValidationRule, UpdatePortalNavigationItemValidationRule {

    private final PortalNavigationItemsQueryService queryService;
    private final PortalPageContentQueryService contentQueryService;

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getSource() != null;
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        // The item does not exist yet, so it owns no subtree: only the content passed in the payload can conflict
        if (item.getType() == PortalNavigationItemType.PAGE && isAutomationManaged(item.getPortalPageContentId())) {
            throw InvalidPortalNavigationItemDataException.sourceNotAllowedOnAutomationManagedItem(
                item.getPortalPageContentId().toString()
            );
        }
    }

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return toUpdate.getSource() != null;
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        if (isAutomationManaged(existingItem)) {
            throw InvalidPortalNavigationItemDataException.sourceNotAllowedOnAutomationManagedItem(existingItem.getId().json());
        }
        findAutomationManagedItemBelow(existingItem.getEnvironmentId(), existingItem.getId(), 0).ifPresent(conflicting -> {
            throw InvalidPortalNavigationItemDataException.sourceNotAllowedOnAutomationManagedItem(conflicting.getId().json());
        });
    }

    private Optional<PortalNavigationItem> findAutomationManagedItemBelow(
        String environmentId,
        PortalNavigationItemId parentId,
        int depth
    ) {
        if (ValidationDepth.exceeded(depth, parentId, "the automation exclusivity is not enforced deeper")) {
            return Optional.empty();
        }
        List<PortalNavigationItem> children = queryService.findByParentIdAndEnvironmentId(environmentId, parentId);
        return children
            .stream()
            .map(child ->
                isAutomationManaged(child) ? Optional.of(child) : findAutomationManagedItemBelow(environmentId, child.getId(), depth + 1)
            )
            .flatMap(Optional::stream)
            .findFirst();
    }

    private boolean isAutomationManaged(PortalNavigationItem item) {
        return item instanceof PortalNavigationPage page && isAutomationManaged(page.getPortalPageContentId());
    }

    private boolean isAutomationManaged(PortalPageContentId contentId) {
        return (
            contentId != null &&
            contentQueryService
                .findById(contentId)
                .map(content -> content.getAutomationMetadata() != null)
                .orElse(false)
        );
    }
}
