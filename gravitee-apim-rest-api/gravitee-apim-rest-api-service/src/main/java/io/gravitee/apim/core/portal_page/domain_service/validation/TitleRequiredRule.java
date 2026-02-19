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
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;

/**
 * Ensures the payload has a non-null, non-blank title (create and update).
 * On create, API items are excluded because the domain sets title from the API name.
 */
public class TitleRequiredRule implements CreatePortalNavigationItemValidationRule, UpdatePortalNavigationItemValidationRule {

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getType() != PortalNavigationItemType.API;
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        requireNonBlankTitle(item.getTitle());
    }

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return true;
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        requireNonBlankTitle(toUpdate.getTitle());
    }

    private static void requireNonBlankTitle(String title) {
        if (title == null || title.isBlank()) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("title");
        }
    }
}
