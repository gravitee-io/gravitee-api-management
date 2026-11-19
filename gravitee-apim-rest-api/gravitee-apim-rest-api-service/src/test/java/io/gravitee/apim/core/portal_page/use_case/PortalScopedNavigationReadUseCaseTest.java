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

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PORTAL_REFERENCE;
import static fixtures.core.model.PortalNavigationItemFixtures.aFolder;
import static fixtures.core.model.PortalNavigationItemFixtures.aPage;
import static fixtures.core.model.PortalNavigationItemFixtures.anApi;
import static fixtures.core.model.PortalNavigationItemFixtures.anApiProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import fixtures.core.model.PortalPageContentFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiPortalSearchQueryServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.NavigationPortalDomainServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.domain_service.ApiProductAccessibleIdsDomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.portal.exception.DefaultPortalNotFoundException;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.CheckTypoToleranceDomainService;
import io.gravitee.apim.core.portal_page.domain_service.DefaultContentRenderer;
import io.gravitee.apim.core.portal_page.domain_service.PortalCatalogNavigationVisibilityDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiProductVisibilityDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemReadDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemRenderingDomainService;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The Portal API reads of one portal's navigation: every read sees only that portal's items, and an unknown
 * portal answers 404 whoever asks.
 */
class PortalScopedNavigationReadUseCaseTest {

    private static final String API_ID = "api-a";
    private static final String OTHER_API_ID = "api-b";

    private final ArrayList<PortalNavigationItem> storage = new ArrayList<>();
    private final PortalNavigationItemsQueryServiceInMemory navigation = new PortalNavigationItemsQueryServiceInMemory(storage);
    private final PortalNavigationItemsCrudServiceInMemory writes = new PortalNavigationItemsCrudServiceInMemory(storage);
    private final PortalPageContentQueryServiceInMemory contents = new PortalPageContentQueryServiceInMemory();
    private final ApiCrudServiceInMemory apis = new ApiCrudServiceInMemory();
    private final ApiPortalSearchQueryServiceInMemory apiSearch = new ApiPortalSearchQueryServiceInMemory();
    private final ApiQueryServiceInMemory apiQuery = new ApiQueryServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProducts = new ApiProductQueryServiceInMemory();
    private final NavigationPortalDomainServiceInMemory portals = new NavigationPortalDomainServiceInMemory();
    private final MembershipQueryServiceInMemory memberships = new MembershipQueryServiceInMemory();
    private final ApiOwnedNavigationDomainService apiNavigation = new ApiOwnedNavigationDomainService(navigation, writes);
    private final PortalNavigationAccessDomainService access = new PortalNavigationAccessDomainService(
        portals.portalCrudService(),
        apis,
        navigation,
        contents,
        apiNavigation,
        apiQuery
    );
    private final PortalNavigationApiVisibilityDomainService apiVisibility = new PortalNavigationApiVisibilityDomainService(
        navigation,
        new ApiPortalMembershipDomainService(memberships, new SubscriptionQueryServiceInMemory(), apiQuery)
    );
    private final PortalNavigationApiProductVisibilityDomainService apiProductVisibility =
        new PortalNavigationApiProductVisibilityDomainService(
            navigation,
            new ApiProductAccessibleIdsDomainService(apiProducts, memberships)
        );
    private final CheckTypoToleranceDomainService typoTolerance = new CheckTypoToleranceDomainService(new ParametersQueryServiceInMemory());

    private final ListPortalNavigationItemsUseCase list = new ListPortalNavigationItemsUseCase(
        access,
        new PortalNavigationItemReadDomainService(
            navigation,
            List.of(apiVisibility, apiProductVisibility),
            new PortalNavigationItemSourceDomainServiceInMemory(),
            apiSearch,
            apiNavigation,
            new PortalNavigationItemRenderingDomainService(navigation),
            access
        )
    );
    private final GetPortalNavigationItemUseCase get = new GetPortalNavigationItemUseCase(
        access,
        navigation,
        List.of(apiVisibility, apiProductVisibility),
        new PortalNavigationItemSourceDomainServiceInMemory()
    );
    private final GetPortalPageContentByNavigationIdUseCase content = new GetPortalPageContentByNavigationIdUseCase(
        access,
        navigation,
        contents,
        List.of(apiVisibility, apiProductVisibility),
        List.of(new DefaultContentRenderer())
    );
    private final GetVisiblePortalNavigationApisUseCase searchApis = new GetVisiblePortalNavigationApisUseCase(
        access,
        apiVisibility,
        apiSearch,
        typoTolerance
    );
    private final GetVisiblePortalCatalogItemsUseCase searchCatalog = new GetVisiblePortalCatalogItemsUseCase(
        access,
        navigation,
        apiVisibility,
        apiProductVisibility,
        new PortalCatalogNavigationVisibilityDomainService(apiProductVisibility),
        apiSearch,
        apiQuery,
        apiProducts,
        typoTolerance
    );

    private PortalId selectedPortal;
    private NavigationItemReference.PortalReference selected;

    @BeforeEach
    void set_up() {
        portals.givenDefaultPortalForNavigationFixtures();
        selectedPortal = portals.givenAutomationPortal(ORG_ID, ENV_ID);
        selected = new NavigationItemReference.PortalReference(selectedPortal);
        var a = Api.builder().id(API_ID).name("Api A").environmentId(ENV_ID).build();
        var b = Api.builder().id(OTHER_API_ID).name("Api B").environmentId(ENV_ID).build();
        apis.initWith(List.of(a, b));
        apiSearch.initWith(List.of(a, b));
        apiQuery.initWith(List.of(a, b));
    }

    @Nested
    class List_items {

        @ParameterizedTest
        @ValueSource(strings = { "TOP_NAVBAR", "HOMEPAGE" })
        void should_return_only_the_portals_items_in_every_area(String area) {
            var defaultFolder = rootFolder("Default", PORTAL_REFERENCE, PortalArea.valueOf(area));
            var selectedFolder = rootFolder("Selected", selected, PortalArea.valueOf(area));
            var child = aPage("Selected page", selectedFolder.getId())
                .toBuilder()
                .area(PortalArea.valueOf(area))
                .reference(selected)
                .build();
            navigation.initWith(List.of(defaultFolder, selectedFolder, child));

            assertThat(list(Optional.of(selectedPortal), PortalArea.valueOf(area), Optional.empty()).items())
                .extracting(PortalNavigationItem::getId)
                .containsExactlyInAnyOrder(selectedFolder.getId(), child.getId());
        }

        @Test
        void should_render_api_documentation_under_the_portals_own_listing() {
            var selectedListing = apiListing(null, selected, API_ID);
            var defaultListing = apiListing(null, PORTAL_REFERENCE, API_ID);
            var document = apiDocument(API_ID);
            navigation.initWith(List.of(selectedListing, defaultListing, document));

            assertThat(list(Optional.of(selectedPortal), PortalArea.TOP_NAVBAR, Optional.empty()).items())
                .extracting(PortalNavigationItem::getId, PortalNavigationItem::getParentId)
                .containsExactlyInAnyOrder(tuple(selectedListing.getId(), null), tuple(document.getId(), selectedListing.getId()));
        }

        @Test
        void should_reject_a_parent_of_another_portal() {
            var defaultFolder = rootFolder("Default", PORTAL_REFERENCE, PortalArea.TOP_NAVBAR);
            navigation.initWith(List.of(defaultFolder));

            assertThatThrownBy(() ->
                list(Optional.of(selectedPortal), PortalArea.TOP_NAVBAR, Optional.of(defaultFolder.getId()))
            ).isInstanceOf(PortalNavigationItemNotFoundException.class);
        }

        @Test
        void should_answer_an_empty_list_for_a_parent_of_another_portal_when_no_portal_is_given() {
            var selectedFolder = rootFolder("Selected", selected, PortalArea.TOP_NAVBAR);
            var child = aPage("Selected page", selectedFolder.getId()).toBuilder().area(PortalArea.TOP_NAVBAR).reference(selected).build();
            navigation.initWith(List.of(selectedFolder, child));

            assertThat(list(Optional.empty(), PortalArea.TOP_NAVBAR, Optional.of(selectedFolder.getId())).items()).isEmpty();
        }

        @Test
        void should_list_the_children_of_a_default_portal_parent_when_no_portal_is_given() {
            var defaultFolder = rootFolder("Default", PORTAL_REFERENCE, PortalArea.TOP_NAVBAR);
            var child = aPage("Default page", defaultFolder.getId())
                .toBuilder()
                .area(PortalArea.TOP_NAVBAR)
                .reference(PORTAL_REFERENCE)
                .build();
            navigation.initWith(List.of(defaultFolder, child));

            assertThat(list(Optional.empty(), PortalArea.TOP_NAVBAR, Optional.of(defaultFolder.getId())).items())
                .extracting(PortalNavigationItem::getId)
                .containsExactly(child.getId());
        }

        @Test
        void should_return_only_the_default_portals_items_when_no_portal_is_given() {
            var defaultFolder = rootFolder("Default", PORTAL_REFERENCE, PortalArea.TOP_NAVBAR);
            var selectedFolder = rootFolder("Selected", selected, PortalArea.TOP_NAVBAR);
            navigation.initWith(List.of(defaultFolder, selectedFolder));

            assertThat(list(Optional.empty(), PortalArea.TOP_NAVBAR, Optional.empty()).items())
                .extracting(PortalNavigationItem::getId)
                .containsExactly(defaultFolder.getId());
        }
    }

    @Nested
    class Get_item {

        @Test
        void should_return_an_item_of_the_portal() {
            var page = pageWithContent(selected);
            navigation.initWith(List.of(page));

            assertThat(get(page.getId(), selectedPortal, anonymous()).portalNavigationItem().getId()).isEqualTo(page.getId());
        }

        @Test
        void should_not_return_an_item_of_another_portal() {
            var page = pageWithContent(PORTAL_REFERENCE);
            navigation.initWith(List.of(page));

            assertThatThrownBy(() -> get(page.getId(), selectedPortal, anonymous())).isInstanceOf(
                PortalNavigationItemNotFoundException.class
            );
            assertThatThrownBy(() -> content(page.getId(), selectedPortal, anonymous())).isInstanceOf(
                PortalNavigationItemNotFoundException.class
            );
        }

        @Test
        void should_read_the_items_of_the_default_portal_when_no_portal_is_given() {
            var selectedPage = pageWithContent(selected);
            var defaultPage = pageWithContent(PORTAL_REFERENCE);
            navigation.initWith(List.of(defaultPage, selectedPage));

            assertThat(get(defaultPage.getId(), Optional.empty(), anonymous()).portalNavigationItem().getId()).isEqualTo(
                defaultPage.getId()
            );
            assertThat(content(defaultPage.getId(), Optional.empty(), anonymous()).portalNavigationItem().getId()).isEqualTo(
                defaultPage.getId()
            );
            assertThatThrownBy(() -> get(selectedPage.getId(), Optional.empty(), anonymous())).isInstanceOf(
                PortalNavigationItemNotFoundException.class
            );
            assertThatThrownBy(() -> content(selectedPage.getId(), Optional.empty(), anonymous())).isInstanceOf(
                PortalNavigationItemNotFoundException.class
            );
        }

        @Test
        void should_return_the_content_of_an_item_of_the_portal() {
            var page = pageWithContent(selected);
            navigation.initWith(List.of(page));

            assertThat(content(page.getId(), selectedPortal, anonymous()).portalNavigationItem().getId()).isEqualTo(page.getId());
        }

        @Test
        void should_return_api_documentation_when_the_portal_lists_the_api() {
            var document = pageWithContent(new NavigationItemReference.ApiReference(API_ID));
            navigation.initWith(List.of(apiListing(null, selected, API_ID), document));

            assertThat(get(document.getId(), selectedPortal, anonymous()).portalNavigationItem().getId()).isEqualTo(document.getId());
            assertThat(content(document.getId(), selectedPortal, anonymous()).portalNavigationItem().getId()).isEqualTo(document.getId());
        }

        @Test
        void should_not_return_api_documentation_when_only_another_portal_lists_the_api() {
            var document = pageWithContent(new NavigationItemReference.ApiReference(API_ID));
            navigation.initWith(List.of(apiListing(null, PORTAL_REFERENCE, API_ID), document));

            assertThatThrownBy(() -> get(document.getId(), selectedPortal, anonymous())).isInstanceOf(
                PortalNavigationItemNotFoundException.class
            );
            assertThatThrownBy(() -> content(document.getId(), selectedPortal, anonymous())).isInstanceOf(
                PortalNavigationItemNotFoundException.class
            );
        }
    }

    @Nested
    class Search {

        @Test
        void should_resolve_api_visibility_within_the_portal() {
            var privateListing = apiListing(null, selected, API_ID);
            privateListing.setVisibility(PortalVisibility.PRIVATE);
            var publicListingElsewhere = apiListing(null, PORTAL_REFERENCE, API_ID);
            navigation.initWith(List.of(privateListing, publicListingElsewhere));

            assertThat(searchApis(Optional.of(selectedPortal), Optional.empty()).apis().getContent()).isEmpty();
            assertThat(searchApis(Optional.of(selectedPortal), Optional.of("non-member")).apis().getContent()).isEmpty();
            assertThat(searchCatalog(Optional.of(selectedPortal), Optional.of("non-member")).items().getContent()).isEmpty();

            memberships.initWith(List.of(apiMember("member", API_ID)));

            assertThat(searchApis(Optional.of(selectedPortal), Optional.of("member")).apis().getContent())
                .extracting(PortalNavigationItem::getId)
                .containsExactly(privateListing.getId());
            assertThat(searchCatalog(Optional.of(selectedPortal), Optional.of("member")).items().getContent())
                .extracting(PortalNavigationItem::getId)
                .containsExactly(privateListing.getId());
        }

        @Test
        void should_return_only_the_portals_api_listings() {
            var selectedListing = apiListing(null, selected, API_ID);
            var defaultListing = apiListing(null, PORTAL_REFERENCE, OTHER_API_ID);
            navigation.initWith(List.of(selectedListing, defaultListing));

            assertThat(searchApis(Optional.of(selectedPortal)).apis().getContent())
                .extracting(PortalNavigationItem::getId)
                .containsExactly(selectedListing.getId());
            assertThat(searchApis(Optional.empty()).apis().getContent())
                .extracting(PortalNavigationItem::getId)
                .containsExactly(defaultListing.getId());
        }

        @Test
        void should_return_only_the_portals_catalog_entries() {
            var selectedListing = apiListing(null, selected, API_ID);
            var defaultListing = apiListing(null, PORTAL_REFERENCE, OTHER_API_ID);
            var defaultProduct = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-a");
            var selectedProduct = anApiProduct(PortalNavigationItemId.random().json(), "Selected product", null, "product-b");
            selectedProduct.changeOwner(selected);
            apiProducts.initWith(
                List.of(
                    ApiProduct.builder().id("product-a").name("Product").environmentId(ENV_ID).build(),
                    ApiProduct.builder().id("product-b").name("Selected product").environmentId(ENV_ID).build()
                )
            );
            navigation.initWith(List.of(selectedListing, defaultListing, defaultProduct, selectedProduct));

            assertThat(searchCatalog(Optional.of(selectedPortal), Optional.empty()).items().getContent())
                .extracting(PortalNavigationItem::getId)
                .containsExactlyInAnyOrder(selectedListing.getId(), selectedProduct.getId());
            assertThat(searchCatalog(Optional.empty(), Optional.empty()).items().getContent())
                .extracting(PortalNavigationItem::getId)
                .containsExactlyInAnyOrder(defaultListing.getId(), defaultProduct.getId());
        }
    }

    @Nested
    class No_default_portal {

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_answer_not_found_on_every_read_when_no_portal_is_given(boolean authenticated) {
            portals.portalCrudService().reset();
            portals.givenAutomationPortal(ORG_ID, ENV_ID);
            portals.givenAutomationPortal(ORG_ID, ENV_ID);
            var userId = authenticated ? Optional.of("user-1") : Optional.<String>empty();
            var viewer = PortalNavigationItemViewerContext.forPortal(userId.orElse(null));
            var page = pageWithContent(PORTAL_REFERENCE);
            navigation.initWith(List.of(page));

            assertThatThrownBy(() -> list(Optional.empty(), PortalArea.TOP_NAVBAR, Optional.empty(), viewer)).isInstanceOf(
                DefaultPortalNotFoundException.class
            );
            assertThatThrownBy(() -> get(page.getId(), Optional.empty(), viewer)).isInstanceOf(DefaultPortalNotFoundException.class);
            assertThatThrownBy(() -> content(page.getId(), Optional.empty(), viewer)).isInstanceOf(DefaultPortalNotFoundException.class);
            assertThatThrownBy(() -> searchApis(Optional.empty(), userId)).isInstanceOf(DefaultPortalNotFoundException.class);
            assertThatThrownBy(() -> searchCatalog(Optional.empty(), userId)).isInstanceOf(DefaultPortalNotFoundException.class);
        }
    }

    @Nested
    class Unknown_portal {

        private final PortalId unknown = PortalId.random();

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_answer_not_found_on_every_read(boolean authenticated) {
            assertEveryReadIsNotFound(unknown, authenticated ? Optional.of("user-1") : Optional.empty());
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_answer_not_found_on_every_read_for_a_portal_of_another_environment(boolean authenticated) {
            var elsewhere = portals.givenAutomationPortal(ORG_ID, "another-env");

            assertEveryReadIsNotFound(elsewhere, authenticated ? Optional.of("user-1") : Optional.empty());
        }

        private void assertEveryReadIsNotFound(PortalId portalId, Optional<String> userId) {
            var viewer = PortalNavigationItemViewerContext.forPortal(userId.orElse(null));
            var page = pageWithContent(PORTAL_REFERENCE);
            navigation.initWith(List.of(page));

            assertThatThrownBy(() -> list(Optional.of(portalId), PortalArea.TOP_NAVBAR, Optional.empty(), viewer)).isInstanceOf(
                PortalNotFoundException.class
            );
            assertThatThrownBy(() -> get(page.getId(), portalId, viewer)).isInstanceOf(PortalNotFoundException.class);
            assertThatThrownBy(() -> content(page.getId(), portalId, viewer)).isInstanceOf(PortalNotFoundException.class);
            assertThatThrownBy(() -> searchApis(Optional.of(portalId), userId)).isInstanceOf(PortalNotFoundException.class);
            assertThatThrownBy(() -> searchCatalog(Optional.of(portalId), userId)).isInstanceOf(PortalNotFoundException.class);
        }
    }

    private ListPortalNavigationItemsUseCase.Output list(
        Optional<PortalId> portalId,
        PortalArea area,
        Optional<PortalNavigationItemId> parentId
    ) {
        return list(portalId, area, parentId, anonymous());
    }

    private ListPortalNavigationItemsUseCase.Output list(
        Optional<PortalId> portalId,
        PortalArea area,
        Optional<PortalNavigationItemId> parentId,
        PortalNavigationItemViewerContext viewer
    ) {
        return list.execute(new ListPortalNavigationItemsUseCase.Input(ENV_ID, ORG_ID, area, parentId, true, viewer, false, portalId));
    }

    private GetPortalNavigationItemUseCase.Output get(
        PortalNavigationItemId id,
        PortalId portalId,
        PortalNavigationItemViewerContext viewer
    ) {
        return get(id, Optional.of(portalId), viewer);
    }

    private GetPortalNavigationItemUseCase.Output get(
        PortalNavigationItemId id,
        Optional<PortalId> portalId,
        PortalNavigationItemViewerContext viewer
    ) {
        return get.execute(new GetPortalNavigationItemUseCase.Input(id, ORG_ID, ENV_ID, viewer, portalId));
    }

    private GetPortalPageContentByNavigationIdUseCase.Output content(
        PortalNavigationItemId id,
        PortalId portalId,
        PortalNavigationItemViewerContext viewer
    ) {
        return content(id, Optional.of(portalId), viewer);
    }

    private GetPortalPageContentByNavigationIdUseCase.Output content(
        PortalNavigationItemId id,
        Optional<PortalId> portalId,
        PortalNavigationItemViewerContext viewer
    ) {
        return content.execute(new GetPortalPageContentByNavigationIdUseCase.Input(id, ORG_ID, ENV_ID, viewer, portalId));
    }

    private GetVisiblePortalNavigationApisUseCase.Output searchApis(Optional<PortalId> portalId) {
        return searchApis(portalId, Optional.empty());
    }

    private GetVisiblePortalNavigationApisUseCase.Output searchApis(Optional<PortalId> portalId, Optional<String> userId) {
        return searchApis.execute(
            new GetVisiblePortalNavigationApisUseCase.Input(
                ENV_ID,
                ORG_ID,
                userId,
                new PageableImpl(1, 10),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                portalId
            )
        );
    }

    private GetVisiblePortalCatalogItemsUseCase.Output searchCatalog(Optional<PortalId> portalId, Optional<String> userId) {
        return searchCatalog.execute(
            new GetVisiblePortalCatalogItemsUseCase.Input(
                ENV_ID,
                ORG_ID,
                PortalNavigationItemViewerContext.forPortal(userId.orElse(null)),
                new PageableImpl(1, 10),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                portalId
            )
        );
    }

    private static Membership apiMember(String userId, String apiId) {
        return Membership.builder()
            .id("membership-" + userId + "-" + apiId)
            .memberId(userId)
            .memberType(Membership.Type.USER)
            .referenceType(Membership.ReferenceType.API)
            .referenceId(apiId)
            .build();
    }

    private static PortalNavigationItemViewerContext anonymous() {
        return PortalNavigationItemViewerContext.forPortal((String) null);
    }

    private static PortalNavigationFolder rootFolder(String title, NavigationItemReference reference, PortalArea area) {
        var folder = aFolder(title).toBuilder().area(area).reference(reference).build();
        folder.markAsRoot();
        return folder;
    }

    private PortalNavigationPage pageWithContent(NavigationItemReference reference) {
        var contentId = PortalPageContentId.random();
        contents.initWith(List.of(PortalPageContentFixtures.anAsyncApiPageContent(contentId, ORG_ID, ENV_ID, "asyncapi: '3.0.0'")));
        var page = aPage(PortalNavigationItemId.random().json(), "Page", null, contentId).toBuilder().reference(reference).build();
        page.markAsRoot();
        return page;
    }

    private static PortalNavigationApi apiListing(PortalNavigationItemId parentId, NavigationItemReference reference, String apiId) {
        var listing = anApi(PortalNavigationItemId.random().json(), "API " + apiId, parentId, apiId);
        listing.changeOwner(reference);
        if (parentId == null) {
            listing.markAsRoot();
        }
        return listing;
    }

    private static PortalNavigationFolder apiDocument(String apiId) {
        var document = aFolder("Guide").toBuilder().reference(new NavigationItemReference.ApiReference(apiId)).build();
        document.markAsRoot();
        return document;
    }
}
