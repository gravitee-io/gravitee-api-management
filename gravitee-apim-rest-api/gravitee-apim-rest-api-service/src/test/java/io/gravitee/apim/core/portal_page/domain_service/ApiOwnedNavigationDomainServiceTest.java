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

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.aFolder;
import static fixtures.core.model.PortalNavigationItemFixtures.aPage;
import static fixtures.core.model.PortalNavigationItemFixtures.anApi;
import static fixtures.core.model.PortalNavigationItemFixtures.anApiProduct;
import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiOwnedNavigationDomainServiceTest {

    private static final String API_ID = "api-a";
    private static final String OTHER_API_ID = "api-b";

    private final PortalNavigationItemsQueryServiceInMemory queryService = new PortalNavigationItemsQueryServiceInMemory();
    private final ApiOwnedNavigationDomainService service = new ApiOwnedNavigationDomainService(queryService);

    @Nested
    class FindOwnedItems {

        @Test
        void should_return_nothing_when_api_owns_no_item() {
            queryService.initWith(List.of(aFolder("Guides")));

            assertThat(service.findOwnedItems(ENV_ID, API_ID)).isEmpty();
        }

        @Test
        void should_return_owned_items_at_any_depth() {
            var folder = aFolder("Auth").toBuilder().reference(ownedBy(API_ID)).build();
            var nestedFolder = aFolder("OAuth", folder.getId()).toBuilder().reference(ownedBy(API_ID)).build();
            var nestedPage = aPage("Setup", nestedFolder.getId()).toBuilder().reference(ownedBy(API_ID)).build();
            queryService.initWith(List.of(folder, nestedFolder, nestedPage));

            assertThat(service.findOwnedItems(ENV_ID, API_ID))
                .extracting(PortalNavigationItem::getId)
                .containsExactlyInAnyOrder(folder.getId(), nestedFolder.getId(), nestedPage.getId());
        }

        @Test
        void should_not_return_items_owned_by_another_api_or_by_the_portal() {
            var owned = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).build();
            var foreign = aPage("Other overview", null).toBuilder().reference(ownedBy(OTHER_API_ID)).build();
            var section = aFolder("APIs");
            var listing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
            var storedUnderListing = aPage("Legacy page", listing.getId());
            queryService.initWith(List.of(owned, foreign, section, listing, storedUnderListing));

            assertThat(service.findOwnedItems(ENV_ID, API_ID)).extracting(PortalNavigationItem::getId).containsExactly(owned.getId());
        }
    }

    @Nested
    class FindOwnedItemsInAnInconsistentTree {

        @Test
        void should_not_return_a_child_owned_by_someone_else_stored_under_an_owned_folder() {
            var folder = aFolder("Auth").toBuilder().reference(ownedBy(API_ID)).build();
            var ownedPage = aPage("Setup", folder.getId()).toBuilder().reference(ownedBy(API_ID)).build();
            var portalPage = aPage("Portal page", folder.getId());
            var foreignFolder = aFolder("Foreign", folder.getId()).toBuilder().reference(ownedBy(OTHER_API_ID)).build();
            var pageUnderForeignFolder = aPage("Nested", foreignFolder.getId()).toBuilder().reference(ownedBy(API_ID)).build();
            queryService.initWith(List.of(folder, ownedPage, portalPage, foreignFolder, pageUnderForeignFolder));

            assertThat(service.findOwnedItems(ENV_ID, API_ID))
                .extracting(PortalNavigationItem::getId)
                .containsExactlyInAnyOrder(folder.getId(), ownedPage.getId());
        }

        @Test
        @Timeout(5)
        void should_return_each_item_once_when_the_hierarchy_loops() {
            var folder = aFolder("Auth").toBuilder().reference(ownedBy(API_ID)).build();
            var page = aPage("Setup", folder.getId()).toBuilder().reference(ownedBy(API_ID)).build();
            // A corrupted hierarchy in which the folder is also reported as a child of its own page
            var loopingQueryService = new PortalNavigationItemsQueryServiceInMemory(List.of(folder, page)) {
                @Override
                public List<PortalNavigationItem> findByParentIdAndEnvironmentId(String environmentId, PortalNavigationItemId parentId) {
                    return page.getId().equals(parentId) ? List.of(folder) : super.findByParentIdAndEnvironmentId(environmentId, parentId);
                }
            };

            var ownedItems = new ApiOwnedNavigationDomainService(loopingQueryService).findOwnedItems(ENV_ID, API_ID);

            assertThat(ownedItems).extracting(PortalNavigationItem::getId).containsExactlyInAnyOrder(folder.getId(), page.getId());
        }

        @Test
        void should_walk_a_hierarchy_deeper_than_the_call_stack_allows() {
            var items = new ArrayList<PortalNavigationItem>();
            PortalNavigationItemId parentId = null;
            for (int depth = 0; depth < 20_000; depth++) {
                var folder = aFolder("Level " + depth, parentId).toBuilder().reference(ownedBy(API_ID)).build();
                items.add(folder);
                parentId = folder.getId();
            }
            var childrenByParentId = items
                .stream()
                .filter(PortalNavigationItem::hasParent)
                .collect(Collectors.toMap(PortalNavigationItem::getParentId, List::of));
            var indexedQueryService = new PortalNavigationItemsQueryServiceInMemory(items) {
                @Override
                public List<PortalNavigationItem> findByParentIdAndEnvironmentId(String environmentId, PortalNavigationItemId parentId) {
                    return childrenByParentId.getOrDefault(parentId, List.of());
                }
            };

            assertThat(new ApiOwnedNavigationDomainService(indexedQueryService).findOwnedItems(ENV_ID, API_ID)).hasSize(20_000);
        }
    }

    @Nested
    class FindStandaloneListings {

        @Test
        void should_find_no_listing_row_for_an_unlisted_api() {
            queryService.initWith(List.of(aFolder("APIs")));

            assertThat(service.findStandaloneListings(ENV_ID, API_ID)).isEmpty();
        }

        @Test
        void should_find_the_listing_row_of_a_listed_api() {
            var section = aFolder("APIs");
            var listing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
            queryService.initWith(List.of(section, listing));

            assertThat(service.findStandaloneListings(ENV_ID, API_ID)).containsExactly(listing);
        }

        @Test
        void should_not_find_the_listing_row_of_another_api() {
            var section = aFolder("APIs");
            var listing = anApi(PortalNavigationItemId.random().json(), "Api B", section.getId(), OTHER_API_ID);
            queryService.initWith(List.of(section, listing));

            assertThat(service.findStandaloneListings(ENV_ID, API_ID)).isEmpty();
        }

        @Test
        void should_ignore_a_listing_row_under_an_api_product() {
            var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-id");
            var folderInProduct = aFolder("Members", product.getId());
            var listing = anApi(PortalNavigationItemId.random().json(), "Api A", folderInProduct.getId(), API_ID);
            queryService.initWith(List.of(product, folderInProduct, listing));

            assertThat(service.findStandaloneListings(ENV_ID, API_ID)).isEmpty();
        }

        @Test
        void should_find_every_standalone_row_of_the_api() {
            var section = aFolder("APIs");
            var otherSection = aFolder("Partners");
            var listing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
            var otherListing = anApi(PortalNavigationItemId.random().json(), "Api A", otherSection.getId(), API_ID);
            queryService.initWith(List.of(section, otherSection, listing, otherListing));

            assertThat(service.findStandaloneListings(ENV_ID, API_ID)).containsExactlyInAnyOrder(listing, otherListing);
        }

        @Test
        void should_find_the_standalone_row_when_api_is_also_listed_under_an_api_product() {
            var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-id");
            var productListing = anApi(PortalNavigationItemId.random().json(), "Api A", product.getId(), API_ID);
            var section = aFolder("APIs");
            var standaloneListing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
            queryService.initWith(List.of(product, productListing, section, standaloneListing));

            assertThat(service.findStandaloneListings(ENV_ID, API_ID)).containsExactly(standaloneListing);
        }
    }

    private static NavigationItemReference ownedBy(String apiId) {
        return new NavigationItemReference.ApiReference(apiId);
    }
}
