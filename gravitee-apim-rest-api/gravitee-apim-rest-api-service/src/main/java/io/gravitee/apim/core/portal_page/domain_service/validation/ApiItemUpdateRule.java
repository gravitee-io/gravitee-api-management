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
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.Map;

/**
 * For existing API items: validates parentId required, area TOP_NAVBAR, no API in ancestors.
 * Does not check apiId uniqueness (apiId cannot be updated).
 */
public class ApiItemUpdateRule implements UpdatePortalNavigationItemValidationRule {

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return existingItem instanceof PortalNavigationApi;
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        var payloadParentId = toUpdate.getParentId();

        if (payloadParentId == null) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("parentId");
        }
        if (existingItem.getArea() != PortalArea.TOP_NAVBAR) {
            throw InvalidPortalNavigationItemDataException.apiMustBeInTopNavbar();
        }
        ApiAncestorValidation.ensureNoApiInAncestors(payloadParentId, ctx.itemsById());
    }
}
