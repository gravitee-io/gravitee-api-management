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

import io.gravitee.apim.core.portal_page.exception.ApiProductNavigationItemAlreadyExistsException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import java.util.HashSet;
import java.util.List;

public class DuplicateApiProductIdsInPayloadRule implements BulkCreatePortalNavigationItemValidationRule {

    @Override
    public void validate(List<CreatePortalNavigationItem> items, String environmentId, CreateValidationContext ctx) {
        var seenApiProductIds = new HashSet<String>();
        items
            .stream()
            .filter(item -> item.getType() == PortalNavigationItemType.API_PRODUCT)
            .map(CreatePortalNavigationItem::getApiProductId)
            .filter(apiProductId -> apiProductId != null && !apiProductId.isBlank())
            .forEach(apiProductId -> {
                if (!seenApiProductIds.add(apiProductId)) {
                    throw new ApiProductNavigationItemAlreadyExistsException(apiProductId);
                }
            });
    }
}
