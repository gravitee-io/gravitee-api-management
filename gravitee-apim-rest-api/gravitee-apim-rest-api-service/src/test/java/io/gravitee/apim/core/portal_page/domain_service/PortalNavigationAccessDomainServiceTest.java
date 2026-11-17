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
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.aFolder;
import static fixtures.core.model.PortalNavigationItemFixtures.aPage;
import static fixtures.core.model.PortalNavigationItemFixtures.anApi;
import static fixtures.core.model.PortalNavigationItemFixtures.anApiProduct;
import static fixtures.core.model.PortalPageContentFixtures.aGraviteeMarkdownPageContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class PortalNavigationAccessDomainServiceTest {

    private static final PortalId PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final PortalId OTHER_PORTAL_ID = PortalId.of("22222222-2222-2222-2222-222222222222");
    private static final NavigationItemReference.PortalReference PORTAL = new NavigationItemReference.PortalReference(PORTAL_ID);
    private static final NavigationItemReference.PortalReference OTHER_PORTAL = new NavigationItemReference.PortalReference(
        OTHER_PORTAL_ID
    );
    private static final NavigationItemReference.ApiReference API = new NavigationItemReference.ApiReference("api-a");

    private final PortalCrudService portals = mock(PortalCrudService.class);
    private final ApiCrudService apis = mock(ApiCrudService.class);
    private final ApiQueryService apiQueries = mock(ApiQueryService.class);
    private final PortalPageContentQueryService contents = mock(PortalPageContentQueryService.class);
    private final PortalNavigationItemsQueryServiceInMemory query = spy(new PortalNavigationItemsQueryServiceInMemory());
    private final ApiOwnedNavigationDomainService apiNavigation = new ApiOwnedNavigationDomainService(
        query,
        mock(PortalNavigationItemCrudService.class)
    );
    private final PortalNavigationAccessDomainService service = new PortalNavigationAccessDomainService(
        portals,
        apis,
        query,
        contents,
        apiNavigation,
        apiQueries
    );

    @BeforeEach
    void set_up() throws Exception {
        when(portals.findByIdAndEnvironmentId(PORTAL_ID, ENV_ID)).thenReturn(
            Optional.of(Portal.create(PORTAL_ID, ENV_ID, ORG_ID, "Portal A", URI.create("https://portal.example.test").toURL()))
        );
        when(apis.findById(API.apiId())).thenReturn(Optional.of(Api.builder().id(API.apiId()).environmentId(ENV_ID).build()));
    }

    @Test
    void should_allow_missing_content_only_when_its_navigation_associations_are_accessible() {
        var page = aPage("Broken", null).toBuilder().reference(PORTAL).build();
        query.initWith(List.of(page));
        when(contents.findById(page.getPortalPageContentId())).thenReturn(Optional.empty());

        service.checkContentAccess(ORG_ID, ENV_ID, PORTAL, page.getPortalPageContentId());

        assertThatThrownBy(() -> service.requireContent(ORG_ID, ENV_ID, PORTAL, page.getPortalPageContentId())).isInstanceOf(
            PageContentNotFoundException.class
        );
    }

    @Test
    void should_reject_foreign_navigation_associations_even_when_the_content_row_is_absent() {
        var own = aPage("Own", null).toBuilder().reference(PORTAL).build();
        var foreign = aPage("Foreign", null).toBuilder().reference(OTHER_PORTAL).portalPageContentId(own.getPortalPageContentId()).build();
        query.initWith(List.of(own, foreign));
        when(contents.findById(own.getPortalPageContentId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkContentAccess(ORG_ID, ENV_ID, PORTAL, own.getPortalPageContentId())).isInstanceOf(
            PageContentNotFoundException.class
        );
    }

    @Test
    void should_reject_an_existing_content_row_from_another_environment_during_access_validation() {
        var page = aPage("Wrong environment", null).toBuilder().reference(PORTAL).build();
        query.initWith(List.of(page));
        var content = aGraviteeMarkdownPageContent(page.getPortalPageContentId(), ORG_ID, "other-env", "Content");
        when(contents.findById(content.getId())).thenReturn(Optional.of(content));

        assertThatThrownBy(() -> service.checkContentAccess(ORG_ID, ENV_ID, PORTAL, content.getId())).isInstanceOf(
            PageContentNotFoundException.class
        );
    }

    @Test
    void should_accept_a_portal_owned_item() {
        var page = aPage("Home", null).toBuilder().reference(PORTAL).build();
        query.initWith(List.of(page));

        assertThat(service.requireItem(ORG_ID, ENV_ID, PORTAL, page.getId())).isSameAs(page);
    }

    @Test
    void should_hide_an_item_of_another_portal_in_the_same_environment() {
        var page = aPage("Foreign", null).toBuilder().reference(OTHER_PORTAL).build();
        query.initWith(List.of(page));

        assertThatThrownBy(() -> service.requireItem(ORG_ID, ENV_ID, PORTAL, page.getId())).isInstanceOf(
            PortalNavigationItemNotFoundException.class
        );
    }

    @Test
    void should_hide_an_item_of_another_environment() {
        var page = aPage("Foreign", null).toBuilder().reference(PORTAL).environmentId("other-env").build();
        query.initWith(List.of(page));

        assertThatThrownBy(() -> service.requireItem(ORG_ID, ENV_ID, PORTAL, page.getId())).isInstanceOf(
            PortalNavigationItemNotFoundException.class
        );
    }

    @Test
    void should_hide_an_unknown_portal() {
        assertThatThrownBy(() -> service.requireScope(ORG_ID, ENV_ID, OTHER_PORTAL)).isInstanceOf(PortalNotFoundException.class);
    }

    @Test
    void should_hide_a_portal_of_another_organization() {
        assertThatThrownBy(() -> service.requireScope("other-org", ENV_ID, PORTAL)).isInstanceOf(PortalNotFoundException.class);
    }

    @Test
    void should_hide_an_api_of_another_environment() {
        when(apis.findById(API.apiId())).thenReturn(Optional.of(Api.builder().id(API.apiId()).environmentId("other-env").build()));

        assertThatThrownBy(() -> service.requireScope(ORG_ID, ENV_ID, API)).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_accept_api_owned_documentation_without_a_portal() {
        var page = aPage("API guide", null).toBuilder().reference(API).build();
        query.initWith(List.of(page));

        assertThat(service.requireItem(ORG_ID, ENV_ID, API, page.getId())).isSameAs(page);
    }

    @Test
    void should_hide_api_product_documentation_from_the_api_scope() {
        var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-a");
        var page = aPage("Product guide", product.getId()).toBuilder().reference(PORTAL).build();
        query.initWith(List.of(product, page));

        assertThatThrownBy(() -> service.requireItem(ORG_ID, ENV_ID, API, page.getId())).isInstanceOf(
            PortalNavigationItemNotFoundException.class
        );
    }

    @Test
    void should_accept_shared_api_documentation_reached_through_a_standalone_listing() {
        var section = aFolder("APIs").toBuilder().reference(PORTAL).build();
        var listing = apiListing(section.getId(), PORTAL);
        var page = aPage("API guide", null).toBuilder().reference(API).build();
        query.initWith(List.of(section, listing, page));

        assertThat(service.requireItem(ORG_ID, ENV_ID, PORTAL, page.getId())).isSameAs(page);
    }

    @Test
    void should_ignore_a_corrupt_listing_in_another_portal_when_the_selected_portal_lists_the_api() {
        var section = aFolder("APIs").toBuilder().reference(PORTAL).build();
        var listing = apiListing(section.getId(), PORTAL);
        var broken = aFolder("Foreign loop").toBuilder().reference(OTHER_PORTAL).build();
        broken.updateParent(broken);
        var foreignListing = apiListing(broken.getId(), OTHER_PORTAL);
        var page = aPage("API guide", null).toBuilder().reference(API).build();
        query.initWith(List.of(section, listing, broken, foreignListing, page));

        assertThat(service.requireItem(ORG_ID, ENV_ID, PORTAL, page.getId())).isSameAs(page);
    }

    @Test
    void should_not_reach_shared_documentation_through_another_portals_listing() {
        var section = aFolder("APIs").toBuilder().reference(OTHER_PORTAL).build();
        var listing = apiListing(section.getId(), OTHER_PORTAL);
        var page = aPage("API guide", null).toBuilder().reference(API).build();
        query.initWith(List.of(section, listing, page));

        assertThatThrownBy(() -> service.requireItem(ORG_ID, ENV_ID, PORTAL, page.getId())).isInstanceOf(
            PortalNavigationItemNotFoundException.class
        );
    }

    @Test
    void should_not_reach_shared_documentation_through_a_product_listing() {
        var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-a")
            .toBuilder()
            .reference(PORTAL)
            .build();
        var listing = apiListing(product.getId(), PORTAL);
        var page = aPage("API guide", null).toBuilder().reference(API).build();
        query.initWith(List.of(product, listing, page));

        assertThatThrownBy(() -> service.requireItem(ORG_ID, ENV_ID, PORTAL, page.getId())).isInstanceOf(
            PortalNavigationItemNotFoundException.class
        );
    }

    @Test
    void should_hide_an_item_below_a_parent_owned_by_another_portal() {
        var parent = aFolder("Foreign").toBuilder().reference(OTHER_PORTAL).build();
        var page = aPage("Mixed", parent.getId()).toBuilder().reference(PORTAL).build();
        query.initWith(List.of(parent, page));

        assertThatThrownBy(() -> service.requireItem(ORG_ID, ENV_ID, PORTAL, page.getId())).isInstanceOf(
            PortalNavigationItemNotFoundException.class
        );
    }

    @Test
    void should_hide_an_item_in_a_cyclic_hierarchy() {
        var folder = aFolder("Loop").toBuilder().reference(PORTAL).build();
        folder.updateParent(folder);
        query.initWith(List.of(folder));

        assertThatThrownBy(() -> service.requireItem(ORG_ID, ENV_ID, PORTAL, folder.getId())).isInstanceOf(
            PortalNavigationItemNotFoundException.class
        );
    }

    @Test
    void should_require_a_navigation_page_before_returning_content() {
        var page = aPage("Home", null).toBuilder().reference(PORTAL).build();
        var content = aGraviteeMarkdownPageContent(page.getPortalPageContentId(), ORG_ID, ENV_ID, "Content");
        when(contents.findById(content.getId())).thenReturn(Optional.of(content));

        assertThatThrownBy(() -> service.requireContent(ORG_ID, ENV_ID, PORTAL, content.getId())).isInstanceOf(
            PageContentNotFoundException.class
        );
        query.initWith(List.of(page));
        assertThat(service.requireContent(ORG_ID, ENV_ID, PORTAL, content.getId())).isSameAs(content);
    }

    @Test
    void should_hide_content_with_an_inaccessible_page_association() {
        var page = aPage("Home", null).toBuilder().reference(PORTAL).build();
        var foreignPage = aPage("Foreign", null)
            .toBuilder()
            .reference(OTHER_PORTAL)
            .portalPageContentId(page.getPortalPageContentId())
            .build();
        var content = aGraviteeMarkdownPageContent(page.getPortalPageContentId(), ORG_ID, ENV_ID, "Content");
        when(contents.findById(content.getId())).thenReturn(Optional.of(content));
        query.initWith(List.of(page, foreignPage));

        assertThatThrownBy(() -> service.requireContent(ORG_ID, ENV_ID, PORTAL, content.getId())).isInstanceOf(
            PageContentNotFoundException.class
        );
    }

    @Test
    void should_reject_a_subtree_before_returning_any_foreign_descendant() {
        var folder = aFolder("Root").toBuilder().reference(PORTAL).build();
        var foreign = aFolder("Foreign", folder.getId()).toBuilder().reference(OTHER_PORTAL).build();
        query.initWith(List.of(folder, foreign));

        assertThatThrownBy(() -> service.requireSubtree(ORG_ID, ENV_ID, PORTAL, folder, false)).isInstanceOf(
            PortalNavigationItemNotFoundException.class
        );
    }

    @Test
    void should_include_shared_documentation_only_when_the_operation_requests_it() {
        var section = aFolder("APIs").toBuilder().reference(PORTAL).build();
        var listing = apiListing(section.getId(), PORTAL);
        var document = aFolder("API guide").toBuilder().reference(API).build();
        query.initWith(List.of(section, listing, document));

        assertThat(service.requireSubtree(ORG_ID, ENV_ID, PORTAL, listing, false)).containsExactly(listing);
        assertThat(service.requireSubtree(ORG_ID, ENV_ID, PORTAL, listing, true)).containsExactly(listing, document);
    }

    @Test
    void should_validate_persisted_parents_referenced_by_pending_items() {
        var parent = aFolder("Foreign").toBuilder().reference(OTHER_PORTAL).build();
        var pendingFolder = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .reference(PORTAL)
            .type(PortalNavigationItemType.FOLDER)
            .parentId(parent.getId())
            .build();
        var pendingPage = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.PAGE)
            .parentId(pendingFolder.getId())
            .build();
        query.initWith(List.of(parent));

        assertThatThrownBy(() -> service.requireCreateParents(ORG_ID, ENV_ID, PORTAL, List.of(pendingFolder, pendingPage))).isInstanceOf(
            PortalNavigationItemNotFoundException.class
        );
    }

    @Test
    void should_validate_the_rendered_parent_before_a_normalized_api_root_is_created() {
        var parent = apiListing(null, OTHER_PORTAL);
        var page = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .reference(API)
            .type(PortalNavigationItemType.PAGE)
            .renderedParentId(parent.getId())
            .build();
        query.initWith(List.of(parent));

        assertThatThrownBy(() -> service.requireCreateParents(ORG_ID, ENV_ID, PORTAL, List.of(page))).isInstanceOf(
            PortalNavigationItemNotFoundException.class
        );
    }

    @Test
    void should_accept_parents_in_the_same_request() {
        var folder = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .reference(PORTAL)
            .type(PortalNavigationItemType.FOLDER)
            .build();
        var page = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .reference(PORTAL)
            .type(PortalNavigationItemType.PAGE)
            .parentId(folder.getId())
            .build();

        service.requireCreateParents(ORG_ID, ENV_ID, PORTAL, List.of(page, folder));
    }

    @Test
    void should_reject_a_cycle_within_pending_items() {
        var folderId = PortalNavigationItemId.random();
        var pageId = PortalNavigationItemId.random();
        var folder = CreatePortalNavigationItem.builder()
            .id(folderId)
            .reference(PORTAL)
            .type(PortalNavigationItemType.FOLDER)
            .parentId(pageId)
            .build();
        var page = CreatePortalNavigationItem.builder()
            .id(pageId)
            .reference(PORTAL)
            .type(PortalNavigationItemType.PAGE)
            .parentId(folderId)
            .build();

        assertThatThrownBy(() -> service.requireCreateParents(ORG_ID, ENV_ID, PORTAL, List.of(folder, page))).isInstanceOf(
            InvalidPortalNavigationItemDataException.class
        );
    }

    @Test
    void should_reject_content_of_another_organization() {
        var page = aPage("Home", null).toBuilder().reference(PORTAL).build();
        var content = aGraviteeMarkdownPageContent(page.getPortalPageContentId(), "other-org", ENV_ID, "Content");
        when(contents.findById(content.getId())).thenReturn(Optional.of(content));
        query.initWith(List.of(page));

        assertThatThrownBy(() -> service.requireContent(ORG_ID, ENV_ID, PORTAL, content.getId())).isInstanceOf(
            PageContentNotFoundException.class
        );
    }

    @Nested
    class PortalRead {

        @Test
        void should_validate_loaded_items_without_reloading_each_item_or_portal() {
            var pages = IntStream.range(0, 100)
                .mapToObj(index -> (PortalNavigationItem) aPage("Page " + index, null).toBuilder().reference(PORTAL).build())
                .toList();
            var access = service.openPortalRead(ORG_ID, ENV_ID, PORTAL_ID);
            access.remember(pages);

            access.requireItems(pages);

            verify(portals).findByIdAndEnvironmentId(PORTAL_ID, ENV_ID);
            verify(query, never()).findByIdAndEnvironmentId(any(), any());
            verify(query, never()).search(any());
        }

        @Test
        void should_load_shared_missing_ancestors_in_batches_without_projection_filters() {
            var root = aFolder("Root").toBuilder().reference(PORTAL).area(PortalArea.HOMEPAGE).build();
            var parent = aFolder("Parent", root.getId()).toBuilder().reference(PORTAL).build();
            var pages = IntStream.range(0, 100)
                .mapToObj(index -> (PortalNavigationItem) aPage("Page " + index, parent.getId()).toBuilder().reference(PORTAL).build())
                .toList();
            query.initWith(List.of(root, parent));
            var access = service.openPortalRead(ORG_ID, ENV_ID, PORTAL_ID);
            access.remember(pages);

            access.requireItems(pages);

            var criteria = ArgumentCaptor.forClass(PortalNavigationItemQueryCriteria.class);
            verify(query, times(2)).search(criteria.capture());
            assertThat(criteria.getAllValues())
                .extracting(PortalNavigationItemQueryCriteria::getIds)
                .containsExactly(Set.of(parent.getId()), Set.of(root.getId()));
            assertThat(criteria.getAllValues()).allSatisfy(value -> {
                assertThat(value.getEnvironmentId()).isEqualTo(ENV_ID);
                assertThat(value.getReference()).isNull();
                assertThat(value.getArea()).isNull();
                assertThat(value.getRoot()).isNull();
            });
            verify(query, never()).findByIdAndEnvironmentId(any(), any());
        }

        @ParameterizedTest
        @ValueSource(strings = { "foreign-owner", "foreign-org", "foreign-env", "missing", "cycle" })
        void should_reject_invalid_ancestry(String corruption) {
            var parent = aFolder("Parent").toBuilder().reference(PORTAL).build();
            parent = switch (corruption) {
                case "foreign-owner" -> parent.toBuilder().reference(OTHER_PORTAL).build();
                case "foreign-org" -> parent.toBuilder().organizationId("other-org").build();
                case "foreign-env" -> parent.toBuilder().environmentId("other-env").build();
                default -> parent;
            };
            var page = aPage("Page", parent.getId()).toBuilder().reference(PORTAL).build();
            if (corruption.equals("cycle")) {
                parent.updateParent(parent);
            }
            query.initWith(corruption.equals("missing") ? List.of(page) : List.of(parent, page));
            var access = service.openPortalRead(ORG_ID, ENV_ID, PORTAL_ID);
            access.remember(List.of(page));

            assertThatThrownBy(() -> access.requireItems(List.of(page))).isInstanceOf(PortalNavigationItemNotFoundException.class);
        }

        @Test
        void should_reuse_the_validated_explicit_parent() {
            var parent = aFolder("Parent").toBuilder().reference(PORTAL).build();
            query.initWith(List.of(parent));
            var access = service.openPortalRead(ORG_ID, ENV_ID, PORTAL_ID);

            assertThat(access.requireParent(parent.getId())).isSameAs(parent);
            access.requireItems(List.of(parent));

            verify(query).findByIdAndEnvironmentId(ENV_ID, parent.getId());
            verify(portals).findByIdAndEnvironmentId(PORTAL_ID, ENV_ID);
        }

        @Test
        void should_load_ancestors_of_a_parent_already_captured_by_the_reader() {
            var root = aFolder("Root").toBuilder().reference(PORTAL).build();
            var parent = aFolder("Parent", root.getId()).toBuilder().reference(PORTAL).build();
            var page = aPage("Page", parent.getId()).toBuilder().reference(PORTAL).build();
            query.initWith(List.of(root));
            var access = service.openPortalRead(ORG_ID, ENV_ID, PORTAL_ID);
            access.remember(List.of(parent, page));

            access.requireItems(List.of(page));

            verify(query).search(PortalNavigationItemQueryCriteria.builder().environmentId(ENV_ID).ids(Set.of(root.getId())).build());
        }

        @Test
        void should_validate_all_missing_ancestors_across_batch_boundaries() {
            var parents = IntStream.range(0, 501)
                .mapToObj(index -> (PortalNavigationItem) aFolder("Parent " + index).toBuilder().reference(PORTAL).build())
                .toList();
            var pages = parents
                .stream()
                .map(parent -> (PortalNavigationItem) aPage("Page", parent.getId()).toBuilder().reference(PORTAL).build())
                .toList();
            query.initWith(parents);
            var access = service.openPortalRead(ORG_ID, ENV_ID, PORTAL_ID);
            access.remember(pages);

            access.requireItems(pages);

            var criteria = ArgumentCaptor.forClass(PortalNavigationItemQueryCriteria.class);
            verify(query, times(2)).search(criteria.capture());
            assertThat(criteria.getAllValues()).allSatisfy(value -> assertThat(value.getIds()).hasSizeLessThanOrEqualTo(500));
            assertThat(criteria.getAllValues())
                .flatExtracting(value -> value.getIds())
                .containsExactlyInAnyOrderElementsOf(parents.stream().map(PortalNavigationItem::getId).toList());
        }

        @Test
        void should_not_authorize_shared_documentation_through_another_portals_listing() {
            var listing = apiListing(null, OTHER_PORTAL);
            var document = aPage("Guide", null).toBuilder().reference(API).build();
            query.initWith(List.of(listing));
            when(apiQueries.findExistingIds(Set.of(API.apiId()), ENV_ID)).thenReturn(Set.of(API.apiId()));
            var access = service.openPortalRead(ORG_ID, ENV_ID, PORTAL_ID);
            access.remember(List.of(document));

            assertThatThrownBy(() -> access.requireItems(List.of(document))).isInstanceOf(PortalNavigationItemNotFoundException.class);
        }

        @Test
        void should_batch_api_access_and_find_standalone_listings_outside_the_returned_area() {
            var section = aFolder("Homepage APIs").toBuilder().reference(PORTAL).area(PortalArea.HOMEPAGE).build();
            var firstListing = apiListing(section.getId(), PORTAL);
            firstListing.setArea(PortalArea.HOMEPAGE);
            var secondListing = anApi(PortalNavigationItemId.random().json(), "Second API", section.getId(), "api-b");
            secondListing.changeOwner(PORTAL);
            secondListing.setArea(PortalArea.HOMEPAGE);
            var firstDocument = aPage("First guide", null).toBuilder().reference(API).build();
            var secondDocument = aPage("Second guide", null)
                .toBuilder()
                .reference(new NavigationItemReference.ApiReference("api-b"))
                .build();
            query.initWith(List.of(section, firstListing, secondListing));
            when(apiQueries.findExistingIds(Set.of(API.apiId(), "api-b"), ENV_ID)).thenReturn(Set.of(API.apiId(), "api-b"));
            var access = service.openPortalRead(ORG_ID, ENV_ID, PORTAL_ID);
            access.remember(List.of(firstDocument, secondDocument));

            access.requireItems(List.of(firstDocument, secondDocument));
            access.requireItems(List.of(firstDocument, secondDocument));

            verify(apiQueries).findExistingIds(Set.of(API.apiId(), "api-b"), ENV_ID);
            var criteria = ArgumentCaptor.forClass(PortalNavigationItemQueryCriteria.class);
            verify(query, times(2)).search(criteria.capture());
            assertThat(criteria.getAllValues())
                .filteredOn(value -> value.getApiIds() != null)
                .singleElement()
                .satisfies(value -> {
                    assertThat(value.getApiIds()).containsExactlyInAnyOrder(API.apiId(), "api-b");
                    assertThat(value.getReference()).isEqualTo(PORTAL);
                    assertThat(value.getType()).isEqualTo(PortalNavigationItemType.API);
                    assertThat(value.getArea()).isNull();
                    assertThat(value.getParentId()).isNull();
                });
            verify(apis, never()).findById(any());
        }

        @Test
        void should_validate_shared_documentation_access_across_api_listing_batch_boundaries() {
            var apiIds = IntStream.range(0, 501)
                .mapToObj(index -> "api-" + index)
                .toList();
            var documents = apiIds
                .stream()
                .map(apiId ->
                    (PortalNavigationItem) aPage("Guide " + apiId, null)
                        .toBuilder()
                        .reference(new NavigationItemReference.ApiReference(apiId))
                        .build()
                )
                .toList();
            var listings = apiIds
                .stream()
                .map(apiId -> {
                    var listing = anApi(PortalNavigationItemId.random().json(), "API " + apiId, null, apiId);
                    listing.changeOwner(PORTAL);
                    return (PortalNavigationItem) listing;
                })
                .toList();
            query.initWith(listings);
            var existingApiIds = Set.copyOf(apiIds);
            when(apiQueries.findExistingIds(existingApiIds, ENV_ID)).thenReturn(existingApiIds);
            var access = service.openPortalRead(ORG_ID, ENV_ID, PORTAL_ID);
            access.remember(documents);

            access.requireItems(documents);

            var criteria = ArgumentCaptor.forClass(PortalNavigationItemQueryCriteria.class);
            verify(query, times(2)).search(criteria.capture());
            assertThat(criteria.getAllValues()).allSatisfy(value -> {
                assertThat(value.getEnvironmentId()).isEqualTo(ENV_ID);
                assertThat(value.getReference()).isEqualTo(PORTAL);
                assertThat(value.getType()).isEqualTo(PortalNavigationItemType.API);
                assertThat(value.getApiIds()).isNotEmpty().hasSizeLessThanOrEqualTo(500);
            });
            assertThat(criteria.getAllValues())
                .flatExtracting(value -> value.getApiIds())
                .containsExactlyInAnyOrderElementsOf(existingApiIds);
            verify(portals).findByIdAndEnvironmentId(PORTAL_ID, ENV_ID);
            verify(apiQueries).findExistingIds(existingApiIds, ENV_ID);
            verify(query, never()).findByIdAndEnvironmentId(any(), any());
            verify(apis, never()).findById(any());
        }

        @Test
        void should_ignore_a_corrupt_supporting_listing_when_another_valid_listing_exists() {
            var section = aFolder("APIs").toBuilder().reference(PORTAL).build();
            var validListing = apiListing(section.getId(), PORTAL);
            var broken = aFolder("Loop").toBuilder().reference(PORTAL).build();
            broken.updateParent(broken);
            var corruptListing = apiListing(broken.getId(), PORTAL);
            var document = aPage("Guide", null).toBuilder().reference(API).build();
            query.initWith(List.of(section, validListing, broken, corruptListing));
            when(apiQueries.findExistingIds(Set.of(API.apiId()), ENV_ID)).thenReturn(Set.of(API.apiId()));
            var access = service.openPortalRead(ORG_ID, ENV_ID, PORTAL_ID);
            access.remember(List.of(document));

            access.requireItems(List.of(document));

            access.remember(List.of(corruptListing));
            assertThatThrownBy(() -> access.requireItems(List.of(corruptListing))).isInstanceOf(
                PortalNavigationItemNotFoundException.class
            );
        }

        @Test
        void should_not_authorize_shared_documentation_through_product_only_listings() {
            var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-a")
                .toBuilder()
                .reference(PORTAL)
                .build();
            var listing = apiListing(product.getId(), PORTAL);
            var document = aPage("Guide", null).toBuilder().reference(API).build();
            query.initWith(List.of(product, listing));
            when(apiQueries.findExistingIds(Set.of(API.apiId()), ENV_ID)).thenReturn(Set.of(API.apiId()));
            var access = service.openPortalRead(ORG_ID, ENV_ID, PORTAL_ID);
            access.remember(List.of(document));

            assertThatThrownBy(() -> access.requireItems(List.of(document))).isInstanceOf(PortalNavigationItemNotFoundException.class);
        }

        @Test
        void should_reject_documentation_of_an_api_missing_from_the_environment() {
            var document = aPage("Guide", null).toBuilder().reference(API).build();
            var access = service.openPortalRead(ORG_ID, ENV_ID, PORTAL_ID);
            access.remember(List.of(document));

            assertThatThrownBy(() -> access.requireItems(List.of(document))).isInstanceOf(PortalNavigationItemNotFoundException.class);
        }
    }

    private static PortalNavigationApi apiListing(PortalNavigationItemId parentId, NavigationItemReference reference) {
        var listing = anApi(PortalNavigationItemId.random().json(), "API", parentId, API.apiId());
        listing.changeOwner(reference);
        return listing;
    }
}
