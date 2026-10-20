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
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Lists an API in the portal and publishes its documentation with it. The documentation is not moved:
 * it stays stored against the API and is shown under the navigation entry when the tree is read.
 */
@UseCase
@RequiredArgsConstructor
public class PublishApiToPortalUseCase {

    private final ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;
    private final PortalNavigationItemValidatorService validatorService;
    private final PortalNavigationItemDomainService domainService;

    public Output execute(Input input) {
        if (input.sectionId() == null) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("sectionId");
        }
        var listings = apiOwnedNavigationDomainService.findStandaloneListings(input.environmentId(), input.apiId());
        if (listings.stream().anyMatch(listing -> Boolean.TRUE.equals(listing.getPublished()))) {
            throw InvalidPortalNavigationItemDataException.apiIdAlreadyExists(input.apiId());
        }
        var section = apiOwnedNavigationDomainService.requirePublishLocation(input.environmentId(), input.sectionId());

        // A hidden listing, typically one the portal editor added, means the API is not published: it is reused
        var listing = listings.isEmpty() ? createListing(input, section) : showListing(input, listings.getFirst(), section);

        return new Output(listing, section);
    }

    private PortalNavigationApi createListing(Input input, PortalNavigationFolder section) {
        var listingToCreate = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.API)
            .apiId(input.apiId())
            .area(PortalArea.TOP_NAVBAR)
            .parentId(section.getId())
            .published(true)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
        validatorService.validateAll(List.of(listingToCreate), input.environmentId());

        publishOwnedItems(input);
        return (PortalNavigationApi) domainService.create(input.organizationId(), input.environmentId(), listingToCreate);
    }

    private PortalNavigationApi showListing(Input input, PortalNavigationApi hiddenListing, PortalNavigationFolder section) {
        boolean staysInPlace = section.getId().equals(hiddenListing.getParentId());
        var listingToUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.API)
            .title(hiddenListing.getTitle())
            .parentId(section.getId())
            // Moved to another section, the listing takes the last position there and a segment free among its new siblings
            .order(staysInPlace ? hiddenListing.getOrder() : null)
            .segment(staysInPlace ? hiddenListing.getSegment() : null)
            .categoryIds(hiddenListing.getCategoryIds())
            // A public item cannot sit under a private section
            .visibility(PortalVisibility.PRIVATE.equals(section.getVisibility()) ? PortalVisibility.PRIVATE : hiddenListing.getVisibility())
            .published(true)
            .build();
        validatorService.validateToUpdate(listingToUpdate, hiddenListing);

        publishOwnedItems(input);
        // Pages the portal editor stored under the listing are part of what the API shows, and are published with it
        return (PortalNavigationApi) domainService.update(listingToUpdate, hiddenListing, true);
    }

    /**
     * Nothing here is transactional. The items are published before the listing is written, so that a failure
     * leaves an API that is not listed, which publishing again repairs, rather than a listed API with part of
     * its documentation hidden and no way to publish it again.
     */
    private void publishOwnedItems(Input input) {
        apiOwnedNavigationDomainService.setPublished(input.environmentId(), input.apiId(), true);
    }

    public record Input(String organizationId, String environmentId, String apiId, PortalNavigationItemId sectionId) {}

    public record Output(PortalNavigationApi listing, PortalNavigationFolder section) {}
}
