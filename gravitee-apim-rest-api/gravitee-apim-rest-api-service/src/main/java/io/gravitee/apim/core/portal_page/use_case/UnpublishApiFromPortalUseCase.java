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
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import lombok.RequiredArgsConstructor;

/**
 * Takes an API out of every portal listing it, without removing anything: the navigation entries listing
 * the API are hidden, and so is its documentation, ready to be published again.
 */
@UseCase
@RequiredArgsConstructor
public class UnpublishApiFromPortalUseCase {

    private final ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;
    private final PortalNavigationItemValidatorService validatorService;
    private final PortalNavigationItemDomainService domainService;

    public void execute(Input input) {
        // A hidden listing means the API is not published, so only the visible ones count
        var visibleListings = apiOwnedNavigationDomainService
            .findStandaloneListings(input.environmentId(), input.apiId())
            .stream()
            .filter(listing -> Boolean.TRUE.equals(listing.getPublished()))
            .toList();
        if (visibleListings.isEmpty()) {
            throw InvalidPortalNavigationItemDataException.apiIsNotPublished(input.apiId());
        }

        // Nothing here is transactional. The items are hidden before the listings, so that a failure leaves the
        // API still published and unpublishing again finishes the job.
        apiOwnedNavigationDomainService.setPublished(input.environmentId(), input.apiId(), false);
        visibleListings.forEach(this::hide);
    }

    private void hide(PortalNavigationApi listing) {
        var listingToUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.API)
            .title(listing.getTitle())
            .parentId(listing.getParentId())
            .order(listing.getOrder())
            .segment(listing.getSegment())
            .categoryIds(listing.getCategoryIds())
            .visibility(listing.getVisibility())
            .published(false)
            .build();
        validatorService.validateToUpdate(listingToUpdate, listing);
        // Hiding an item hides what is stored under it, which stays in place
        domainService.update(listingToUpdate, listing);
    }

    public record Input(String environmentId, String apiId) {}
}
