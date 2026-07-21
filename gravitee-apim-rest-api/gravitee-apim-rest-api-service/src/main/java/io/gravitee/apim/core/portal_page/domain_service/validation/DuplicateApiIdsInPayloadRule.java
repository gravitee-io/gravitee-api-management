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
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Bulk create rule: ensures no duplicate apiIds in the same navigation context among API items in the request.
 */
public class DuplicateApiIdsInPayloadRule implements BulkCreatePortalNavigationItemValidationRule {

    @Override
    public void validate(List<CreatePortalNavigationItem> items, String environmentId, CreateValidationContext ctx) {
        var seenApiReferences = new HashSet<ApiReference>();
        for (CreatePortalNavigationItem item : items) {
            if (item.getType() != PortalNavigationItemType.API) {
                continue;
            }
            String apiId = item.getApiId();
            if (apiId == null || apiId.isBlank()) {
                continue;
            }
            var apiProductId = ApiProductAncestorValidation.findApiProductId(item.getParentId(), ctx).orElse(null);
            if (!seenApiReferences.add(new ApiReference(apiId, apiProductId))) {
                throw InvalidPortalNavigationItemDataException.apiIdAlreadyExists(apiId);
            }
        }
    }

    private record ApiReference(String apiId, String apiProductId) {
        private ApiReference {
            Objects.requireNonNull(apiId);
        }
    }
}
