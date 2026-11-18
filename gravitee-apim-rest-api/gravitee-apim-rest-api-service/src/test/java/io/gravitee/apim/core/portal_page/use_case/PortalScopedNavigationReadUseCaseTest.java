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
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
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
        void should_return_every_portals_items_when_no_portal_is_given() {
            var defaultFolder = rootFolder("Default", PORTAL_REFERENCE, PortalArea.TOP_NAVBAR);
            var selectedFolder = rootFolder("Selected", selected, PortalArea.TOP_NAVBAR);
            navigation.initWith(List.of(defaultFolder, selectedFolder));

            assertThat(list(Optional.empty(), PortalArea.TOP_NAVBAR, Optional.empty()).items())
                .extracting(PortalNavigationItem::getId)
                .containsExactlyInAnyOrder(defaultFolder.getId(), selectedFolder.getId());
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
        void should_return_the_content_of_an_item_of_the_portal() {
            var page = pageWithContent(selected);
            navigation.initWith(List.of(page));

            assertThat(content(page.getId(), selectedPortal, anonymous()).portalNavigationItem().getId()).isEqualTo(page.getId());
        }
    }

    @Nested
    class Search {

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
                .containsExactlyInAnyOrder(selectedListing.getId(), defaultListing.getId());
        }

        @Test
        void should_return_only_the_portals_catalog_entries() {
            var selectedListing = apiListing(null, selected, API_ID);
            var defaultListing = apiListing(null, PORTAL_REFERENCE, OTHER_API_ID);
            var defaultProduct = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-a");
            apiProducts.initWith(List.of(ApiProduct.builder().id("product-a").name("Product").environmentId(ENV_ID).build()));
            navigation.initWith(List.of(selectedListing, defaultListing, defaultProduct));

            assertThat(searchCatalog(Optional.of(selectedPortal)).items().getContent())
                .extracting(PortalNavigationItem::getId)
                .containsExactly(selectedListing.getId());
            assertThat(searchCatalog(Optional.empty()).items().getContent())
                .extracting(PortalNavigationItem::getId)
                .containsExactlyInAnyOrder(selectedListing.getId(), defaultListing.getId(), defaultProduct.getId());
        }
    }

    @Nested
    class Unknown_portal {

        private final PortalId unknown = PortalId.random();

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_answer_not_found_on_every_read(boolean authenticated) {
            var viewer = authenticated ? PortalNavigationItemViewerContext.forPortal("user-1") : anonymous();
            var page = pageWithContent(PORTAL_REFERENCE);
            navigation.initWith(List.of(page));

            assertThatThrownBy(() -> list(Optional.of(unknown), PortalArea.TOP_NAVBAR, Optional.empty())).isInstanceOf(
                PortalNotFoundException.class
            );
            assertThatThrownBy(() -> get(page.getId(), unknown, viewer)).isInstanceOf(PortalNotFoundException.class);
            assertThatThrownBy(() -> content(page.getId(), unknown, viewer)).isInstanceOf(PortalNotFoundException.class);
            assertThatThrownBy(() -> searchApis(Optional.of(unknown))).isInstanceOf(PortalNotFoundException.class);
            assertThatThrownBy(() -> searchCatalog(Optional.of(unknown))).isInstanceOf(PortalNotFoundException.class);
        }

        @Test
        void should_answer_not_found_for_a_portal_of_another_environment() {
            var elsewhere = portals.givenAutomationPortal(ORG_ID, "another-env");

            assertThatThrownBy(() -> list(Optional.of(elsewhere), PortalArea.TOP_NAVBAR, Optional.empty())).isInstanceOf(
                PortalNotFoundException.class
            );
        }
    }

    private ListPortalNavigationItemsUseCase.Output list(
        Optional<PortalId> portalId,
        PortalArea area,
        Optional<PortalNavigationItemId> parentId
    ) {
        return list.execute(new ListPortalNavigationItemsUseCase.Input(ENV_ID, ORG_ID, area, parentId, true, anonymous(), false, portalId));
    }

    private GetPortalNavigationItemUseCase.Output get(
        PortalNavigationItemId id,
        PortalId portalId,
        PortalNavigationItemViewerContext viewer
    ) {
        return get.execute(new GetPortalNavigationItemUseCase.Input(id, ORG_ID, ENV_ID, viewer, Optional.of(portalId)));
    }

    private GetPortalPageContentByNavigationIdUseCase.Output content(
        PortalNavigationItemId id,
        PortalId portalId,
        PortalNavigationItemViewerContext viewer
    ) {
        return content.execute(
            new GetPortalPageContentByNavigationIdUseCase.Input(id.json(), ORG_ID, ENV_ID, viewer, Optional.of(portalId))
        );
    }

    private GetVisiblePortalNavigationApisUseCase.Output searchApis(Optional<PortalId> portalId) {
        return searchApis.execute(
            new GetVisiblePortalNavigationApisUseCase.Input(
                ENV_ID,
                ORG_ID,
                Optional.empty(),
                new PageableImpl(1, 10),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                portalId
            )
        );
    }

    private GetVisiblePortalCatalogItemsUseCase.Output searchCatalog(Optional<PortalId> portalId) {
        return searchCatalog.execute(
            new GetVisiblePortalCatalogItemsUseCase.Input(
                ENV_ID,
                ORG_ID,
                anonymous(),
                new PageableImpl(1, 10),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                portalId
            )
        );
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
