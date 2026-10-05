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
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListApiDocumentationUseCase {

    private final ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;
    private final PortalNavigationItemsQueryService queryService;
    private final PortalNavigationItemSourceDomainService sourceDomainService;

    public Output execute(Input input) {
        var items = apiOwnedNavigationDomainService.findOwnedItems(input.environmentId(), input.apiId());
        items.stream().map(PortalNavigationItem::getSource).filter(Objects::nonNull).forEach(sourceDomainService::removeSensitiveData);

        var publications = apiOwnedNavigationDomainService
            .findStandaloneListings(input.environmentId(), input.apiId())
            .stream()
            .map(listing -> new Publication(listing, queryService.findByIdAndEnvironmentId(input.environmentId(), listing.getParentId())))
            .toList();

        return new Output(items, publications);
    }

    public record Input(String environmentId, String apiId) {}

    /**
     * @param publications where the API is listed, one per navigation entry; empty when it is not listed
     */
    public record Output(List<PortalNavigationItem> items, List<Publication> publications) {}

    /**
     * @param listing the API's navigation entry; listed does not imply visible, the entry may be unpublished
     * @param section the item holding the entry
     */
    public record Publication(PortalNavigationApi listing, PortalNavigationItem section) {
        public PortalId portalId() {
            return ((NavigationItemReference.PortalReference) listing.getReference()).portalId();
        }
    }
}
