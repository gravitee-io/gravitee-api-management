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

import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;

/**
 * For AGENT type: parentId required, area TOP_NAVBAR, apiId required.
 */
public class AgentItemCreateRule implements CreatePortalNavigationItemValidationRule {

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getType() == PortalNavigationItemType.AGENT;
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        if (item.getParentId() == null) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("parentId");
        }
        if (item.getArea() != PortalArea.TOP_NAVBAR) {
            throw InvalidPortalNavigationItemDataException.apiMustBeInTopNavbar();
        }
        if (item.getApiId() == null || item.getApiId().isBlank()) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("apiId");
        }
    }
}
