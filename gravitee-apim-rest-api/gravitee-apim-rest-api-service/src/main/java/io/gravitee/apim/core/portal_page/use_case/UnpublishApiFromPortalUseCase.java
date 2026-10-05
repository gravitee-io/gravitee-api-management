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
package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import lombok.RequiredArgsConstructor;

/**
 * Removes an API from every portal listing it. Its documentation is kept, unpublished, against the API,
 * ready to be published again; only the navigation entries listing the API are deleted.
 */
@UseCase
@RequiredArgsConstructor
public class UnpublishApiFromPortalUseCase {

    private final ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;
    private final PortalNavigationItemDomainService domainService;

    public void execute(Input input) {
        var listings = apiOwnedNavigationDomainService.findStandaloneListings(input.environmentId(), input.apiId());
        if (listings.isEmpty()) {
            throw InvalidPortalNavigationItemDataException.apiIsNotListed(input.apiId());
        }

        // The documentation is shared by every portal listing the API, so unpublishing it leaves no listing behind
        listings.forEach(domainService::deleteWithDescendants);
        apiOwnedNavigationDomainService.setPublished(input.environmentId(), input.apiId(), false);
    }

    public record Input(String environmentId, String apiId) {}
}
