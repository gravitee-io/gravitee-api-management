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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Resolves the API that scopes documentation templates for a navigation item by walking {@link PortalNavigationItem#getParentId()}.
 */
@DomainService
@RequiredArgsConstructor
public class PortalNavigationEnclosingApiDomainService {

    private final PortalNavigationItemsQueryService queryService;

    public Optional<String> findEnclosingApiId(String environmentId, PortalNavigationItem item) {
        PortalNavigationItem current = item;
        while (current != null && current.getParentId() != null) {
            current = queryService.findByIdAndEnvironmentId(environmentId, current.getParentId());
            if (current instanceof PortalNavigationApi api) {
                return Optional.of(api.getApiId());
            }
        }
        return Optional.empty();
    }
}
