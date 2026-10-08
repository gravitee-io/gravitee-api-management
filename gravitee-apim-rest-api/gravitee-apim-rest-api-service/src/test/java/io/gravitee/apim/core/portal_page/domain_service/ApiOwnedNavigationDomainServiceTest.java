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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

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

        @Test
        @Timeout(5)
        void should_reject_a_listing_with_cyclic_parent_hierarchy() {
            var folder = aFolder("Cyclic folder");
            folder.updateParent(folder);
            var listing = anApi(PortalNavigationItemId.random().json(), "Api A", folder.getId(), API_ID);
            queryService.initWith(List.of(folder, listing));

            assertThatThrownBy(() -> service.findStandaloneListings(ENV_ID, API_ID))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessage(InvalidPortalNavigationItemDataException.cyclicParentHierarchy().getMessage());
        }
    }

    @Nested
    class IsInApiProductContext {

        @Test
        void should_include_the_api_product_itself() {
            var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-id");

            assertThat(service.isInApiProductContext(ENV_ID, product)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(booleans = { false, true })
        void should_recognize_direct_and_folder_nested_api_product_members(boolean nested) {
            var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-id");
            var folder = aFolder("Members", product.getId());
            var listing = anApi(PortalNavigationItemId.random().json(), "Api A", nested ? folder.getId() : product.getId(), API_ID);
            queryService.initWith(List.of(product, folder, listing));

            assertThat(service.isInApiProductContext(ENV_ID, listing)).isTrue();
        }

        @Test
        void should_return_false_for_an_item_without_a_parent() {
            assertThat(service.isInApiProductContext(ENV_ID, aFolder("APIs"))).isFalse();
        }

        @Test
        void should_return_false_when_the_parent_is_missing() {
            var listing = anApi(PortalNavigationItemId.random().json(), "Api A", PortalNavigationItemId.random(), API_ID);

            assertThat(service.isInApiProductContext(ENV_ID, listing)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(booleans = { false, true })
        void should_cache_the_entire_chain_and_read_shared_ancestors_once(boolean productContext) {
            PortalNavigationItem root = productContext
                ? anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-id")
                : aFolder("APIs");
            var sharedAncestor = aFolder("Shared ancestor", root.getId());
            var sharedParent = aFolder("Shared parent", sharedAncestor.getId());
            var first = anApi(PortalNavigationItemId.random().json(), "First listing", sharedParent.getId(), API_ID);
            var second = anApi(PortalNavigationItemId.random().json(), "Second listing", sharedParent.getId(), API_ID);
            var countingQueryService = spy(
                new PortalNavigationItemsQueryServiceInMemory(List.of(root, sharedAncestor, sharedParent, first, second))
            );
            var countingService = new ApiOwnedNavigationDomainService(countingQueryService);
            var contextById = new HashMap<PortalNavigationItemId, Boolean>();

            assertThat(countingService.isInApiProductContext(ENV_ID, first, contextById)).isEqualTo(productContext);
            assertThat(countingService.isInApiProductContext(ENV_ID, second, contextById)).isEqualTo(productContext);
            assertThat(countingService.isInApiProductContext(ENV_ID, sharedAncestor, contextById)).isEqualTo(productContext);
            assertThat(countingService.isInApiProductContext(ENV_ID, first, contextById)).isEqualTo(productContext);

            assertThat(contextById)
                .hasSize(5)
                .containsEntry(root.getId(), productContext)
                .containsEntry(sharedAncestor.getId(), productContext)
                .containsEntry(sharedParent.getId(), productContext)
                .containsEntry(first.getId(), productContext)
                .containsEntry(second.getId(), productContext);
            verify(countingQueryService).findByIdAndEnvironmentId(ENV_ID, sharedParent.getId());
            verify(countingQueryService).findByIdAndEnvironmentId(ENV_ID, sharedAncestor.getId());
            verify(countingQueryService).findByIdAndEnvironmentId(ENV_ID, root.getId());
            verifyNoMoreInteractions(countingQueryService);
        }

        @Test
        void should_cache_a_shared_missing_parent_as_non_product_context() {
            var missingParentId = PortalNavigationItemId.random();
            var first = aFolder("First", missingParentId);
            var second = aFolder("Second", missingParentId);
            var countingQueryService = spy(new PortalNavigationItemsQueryServiceInMemory(List.of(first, second)));
            var countingService = new ApiOwnedNavigationDomainService(countingQueryService);
            var contextById = new HashMap<PortalNavigationItemId, Boolean>();

            assertThat(countingService.isInApiProductContext(ENV_ID, first, contextById)).isFalse();
            assertThat(countingService.isInApiProductContext(ENV_ID, second, contextById)).isFalse();

            assertThat(contextById)
                .hasSize(3)
                .containsEntry(first.getId(), false)
                .containsEntry(second.getId(), false)
                .containsEntry(missingParentId, false);
            verify(countingQueryService).findByIdAndEnvironmentId(ENV_ID, missingParentId);
            verifyNoMoreInteractions(countingQueryService);
        }

        @ParameterizedTest
        @ValueSource(booleans = { false, true })
        @Timeout(5)
        void should_reject_self_and_multi_node_cycles_without_caching_a_result(boolean multipleNodes) {
            var first = aFolder("First");
            var second = aFolder("Second", first.getId());
            first.updateParent(multipleNodes ? second : first);
            var listing = anApi(PortalNavigationItemId.random().json(), "Api A", first.getId(), API_ID);
            queryService.initWith(List.of(first, second, listing));
            var contextById = new HashMap<PortalNavigationItemId, Boolean>();

            assertThatThrownBy(() -> service.isInApiProductContext(ENV_ID, listing, contextById))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessage(InvalidPortalNavigationItemDataException.cyclicParentHierarchy().getMessage());
            assertThat(contextById).isEmpty();
        }

        @Test
        void should_resolve_current_ancestry_on_each_call_without_a_shared_cache() {
            var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-id");
            var folder = aFolder("Members", product.getId());
            var listing = anApi(PortalNavigationItemId.random().json(), "Api A", folder.getId(), API_ID);
            queryService.initWith(List.of(product, folder, listing));

            assertThat(service.isInApiProductContext(ENV_ID, listing)).isTrue();
            folder.markAsRoot();
            assertThat(service.isInApiProductContext(ENV_ID, listing)).isFalse();
            folder.updateParent(product);
            assertThat(service.isInApiProductContext(ENV_ID, listing)).isTrue();
        }
    }

    @Nested
    class RequireOwnedItem {

        @Test
        void should_return_an_item_owned_by_the_api() {
            var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).build();
            queryService.initWith(List.of(page));

            assertThat(service.requireOwnedItem(ENV_ID, API_ID, page.getId())).isEqualTo(page);
        }

        @Test
        void should_reject_an_unknown_item() {
            var unknownId = PortalNavigationItemId.random();

            assertThatThrownBy(() -> service.requireOwnedItem(ENV_ID, API_ID, unknownId)).isInstanceOf(
                PortalNavigationItemNotFoundException.class
            );
        }

        @Test
        void should_reject_an_item_owned_by_another_api() {
            var foreign = aPage("Other overview", null).toBuilder().reference(ownedBy(OTHER_API_ID)).build();
            queryService.initWith(List.of(foreign));

            assertThatThrownBy(() -> service.requireOwnedItem(ENV_ID, API_ID, foreign.getId())).isInstanceOf(
                PortalNavigationItemNotFoundException.class
            );
        }

        @Test
        void should_reject_a_portal_owned_item_stored_under_the_listing_row() {
            var section = aFolder("APIs");
            var listing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
            var storedUnderListing = aPage("Legacy page", listing.getId());
            queryService.initWith(List.of(section, listing, storedUnderListing));

            assertThatThrownBy(() -> service.requireOwnedItem(ENV_ID, API_ID, storedUnderListing.getId())).isInstanceOf(
                PortalNavigationItemNotFoundException.class
            );
        }

        @Test
        void should_reject_the_listing_row_of_the_api() {
            var section = aFolder("APIs");
            var listing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
            queryService.initWith(List.of(section, listing));

            assertThatThrownBy(() -> service.requireOwnedItem(ENV_ID, API_ID, listing.getId())).isInstanceOf(
                PortalNavigationItemNotFoundException.class
            );
        }
    }

    @Nested
    class ClaimForApi {

        @Test
        void should_stamp_the_api_as_owner() {
            var claimed = service.claimForApi(ENV_ID, API_ID, aPageToCreate().build());

            assertThat(claimed.getReference()).isEqualTo(ownedBy(API_ID));
        }

        @Test
        void should_replace_an_owner_set_by_the_caller() {
            var claimed = service.claimForApi(ENV_ID, API_ID, aPageToCreate().reference(ownedBy(OTHER_API_ID)).build());

            assertThat(claimed.getReference()).isEqualTo(ownedBy(API_ID));
        }

        @Test
        void should_force_unpublished_even_when_published_is_requested() {
            var claimed = service.claimForApi(ENV_ID, API_ID, aPageToCreate().published(true).build());

            assertThat(claimed.getPublished()).isFalse();
        }

        @Test
        void should_force_the_top_navigation_area() {
            var claimed = service.claimForApi(ENV_ID, API_ID, aPageToCreate().area(PortalArea.HOMEPAGE).build());

            assertThat(claimed.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
        }

        @Test
        void should_accept_a_parent_owned_by_the_same_api() {
            var folder = aFolder("Auth").toBuilder().reference(ownedBy(API_ID)).build();
            queryService.initWith(List.of(folder));

            var claimed = service.claimForApi(ENV_ID, API_ID, aPageToCreate().parentId(folder.getId()).build());

            assertThat(claimed.getParentId()).isEqualTo(folder.getId());
        }

        @Test
        void should_reject_a_parent_owned_by_another_api() {
            var foreignFolder = aFolder("Auth").toBuilder().reference(ownedBy(OTHER_API_ID)).build();
            queryService.initWith(List.of(foreignFolder));
            var item = aPageToCreate().parentId(foreignFolder.getId()).build();

            assertThatThrownBy(() -> service.claimForApi(ENV_ID, API_ID, item)).isInstanceOf(PortalNavigationItemNotFoundException.class);
        }

        @Test
        void should_reject_a_portal_owned_parent() {
            var section = aFolder("APIs");
            queryService.initWith(List.of(section));
            var item = aPageToCreate().parentId(section.getId()).build();

            assertThatThrownBy(() -> service.claimForApi(ENV_ID, API_ID, item)).isInstanceOf(PortalNavigationItemNotFoundException.class);
        }

        @ParameterizedTest
        @EnumSource(value = PortalNavigationItemType.class, names = { "API", "API_PRODUCT" })
        void should_reject_a_type_that_is_not_documentation(PortalNavigationItemType type) {
            var item = aPageToCreate().type(type).build();

            assertThatThrownBy(() -> service.claimForApi(ENV_ID, API_ID, item)).isInstanceOf(
                InvalidPortalNavigationItemDataException.class
            );
        }

        private CreatePortalNavigationItem.CreatePortalNavigationItemBuilder aPageToCreate() {
            return CreatePortalNavigationItem.builder()
                .title("Overview")
                .type(PortalNavigationItemType.PAGE)
                .area(PortalArea.TOP_NAVBAR)
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN);
        }
    }

    private static NavigationItemReference ownedBy(String apiId) {
        return new NavigationItemReference.ApiReference(apiId);
    }
}
