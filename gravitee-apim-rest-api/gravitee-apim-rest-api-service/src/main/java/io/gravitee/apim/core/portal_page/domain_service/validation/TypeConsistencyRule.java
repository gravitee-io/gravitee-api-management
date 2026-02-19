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
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;

/**
 * Ensures the update payload type matches the existing item's type (type cannot be changed).
 */
public class TypeConsistencyRule implements UpdatePortalNavigationItemValidationRule {

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return true;
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        PortalNavigationItemType existingType = switch (existingItem) {
            case PortalNavigationFolder ignored -> PortalNavigationItemType.FOLDER;
            case PortalNavigationPage ignored -> PortalNavigationItemType.PAGE;
            case PortalNavigationLink ignored -> PortalNavigationItemType.LINK;
            case PortalNavigationApi ignored -> PortalNavigationItemType.API;
        };
        if (existingType != toUpdate.getType()) {
            throw InvalidPortalNavigationItemDataException.typeMismatch(toUpdate.getType().toString(), existingType.toString());
        }
    }
}
