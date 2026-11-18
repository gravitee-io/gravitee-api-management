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
package io.gravitee.rest.api.portal.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.rest.api.portal.rest.fixture.PortalNavigationFixtures;
import io.gravitee.rest.api.portal.rest.model.BasePortalNavigationItem;
import io.gravitee.rest.api.portal.rest.model.PortalNavigationItem;
import io.gravitee.rest.api.portal.rest.model.PortalNavigationItemsSearchResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

public class PortalScopedNavigationItemsResourceTest extends AbstractResourceTest {

    static final String ORG_ID = "DEFAULT";
    static final String ENV_ID = "DEFAULT";
    private static final GenericType<List<PortalNavigationItem>> ITEMS = new GenericType<>() {};

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

    @Autowired
    private PortalPageContentQueryServiceInMemory portalPageContentQueryService;

    @Autowired
    private PortalCrudServiceInMemory portalCrudService;

    private PortalId portal;
    private PortalId otherPortal;

    @Override
    protected String contextPath() {
        return "portal/";
    }

    @BeforeEach
    public void init() {
        GraviteeContext.setCurrentOrganization(ORG_ID);
        GraviteeContext.setCurrentEnvironment(ENV_ID);
        portal = givenPortal(portalCrudService, ENV_ID);
        otherPortal = givenPortal(portalCrudService, ENV_ID);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        portalNavigationItemsQueryService.reset();
        portalPageContentQueryService.reset();
        portalCrudService.reset();
    }

    @ParameterizedTest
    @ValueSource(strings = { "TOP_NAVBAR", "HOMEPAGE" })
    void should_list_only_the_portals_items(String area) {
        var own = folder("Own", portal, PortalArea.valueOf(area));
        var other = folder("Other", otherPortal, PortalArea.valueOf(area));
        portalNavigationItemsQueryService.initWith(List.of(own, other));

        Response response = target(portal + "/portal-navigation-items").queryParam("area", area).request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(ids(response.readEntity(ITEMS))).containsExactly(own.getId().json());
    }

    @Test
    void should_keep_listing_every_portals_items_on_the_unscoped_path() {
        var own = folder("Own", portal, PortalArea.TOP_NAVBAR);
        var other = folder("Other", otherPortal, PortalArea.TOP_NAVBAR);
        portalNavigationItemsQueryService.initWith(List.of(own, other));

        Response response = root().path("environments/DEFAULT/portal-navigation-items").queryParam("area", "TOP_NAVBAR").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(ids(response.readEntity(ITEMS))).containsExactlyInAnyOrder(own.getId().json(), other.getId().json());
    }

    @Test
    void should_search_only_the_portals_apis() {
        var own = apiListing("api-own", portal);
        var other = apiListing("api-other", otherPortal);
        portalNavigationItemsQueryService.initWith(List.of(own, other));

        Response response = target(portal + "/portal-navigation-items/_search").queryParam("type", "api").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(ids(response.readEntity(PortalNavigationItemsSearchResponse.class).getData())).containsExactly(own.getId().json());
    }

    @Test
    void should_return_an_item_of_the_portal_and_its_content() {
        var contentId = PortalPageContentId.random();
        portalPageContentQueryService.initWith(
            List.of(PortalPageContentFixtures.anAsyncApiPageContent(contentId, ORG_ID, ENV_ID, "asyncapi: '3.0.0'"))
        );
        var page = page(portal, contentId);
        portalNavigationItemsQueryService.initWith(List.of(page));
        var itemPath = portal + "/portal-navigation-items/" + page.getId().json();

        assertThat(target(itemPath).request().get().getStatus()).isEqualTo(200);
        assertThat(target(itemPath + "/content").request().get().getStatus()).isEqualTo(200);
    }

    @Test
    void should_not_return_an_item_of_another_portal() {
        var page = page(otherPortal, PortalPageContentId.random());
        portalNavigationItemsQueryService.initWith(List.of(page));
        var itemPath = portal + "/portal-navigation-items/" + page.getId().json();

        assertThat(target(itemPath).request().get().getStatus()).isEqualTo(404);
        assertThat(target(itemPath + "/content").request().get().getStatus()).isEqualTo(404);
    }

    @Test
    void should_answer_not_found_for_a_portal_of_another_environment() {
        var elsewhere = givenPortal(portalCrudService, "another-env");
        var page = page(portal, PortalPageContentId.random());
        portalNavigationItemsQueryService.initWith(List.of(page));

        assertThat(statusesForEveryPath(this::target, elsewhere.toString(), page.getId().json())).containsOnly(404);
    }

    @ParameterizedTest
    @ValueSource(strings = { "6c2b1e9e-0000-4000-8000-000000000000", "not-a-uuid" })
    void should_answer_not_found_on_every_path_for_an_unknown_portal(String unknownPortal) {
        var page = page(portal, PortalPageContentId.random());
        portalNavigationItemsQueryService.initWith(List.of(page));

        assertThat(statusesForEveryPath(this::target, unknownPortal, page.getId().json())).containsOnly(404);
    }

    @Test
    void should_answer_not_found_for_a_malformed_item_id() {
        var unknownPortal = "6c2b1e9e-0000-4000-8000-000000000000";

        assertThat(
            Stream.of(unknownPortal, portal.toString())
                .flatMap(portalId -> Stream.of("", "/content").map(suffix -> portalId + "/portal-navigation-items/not-a-uuid" + suffix))
                .map(path -> target(path).request().get().getStatus())
        ).containsOnly(404);
    }

    static List<Integer> statusesForEveryPath(Function<String, WebTarget> target, String portalId, String itemId) {
        var base = portalId + "/portal-navigation-items";
        return Stream.of(
            target.apply(base).queryParam("area", "TOP_NAVBAR"),
            target.apply(base + "/_search").queryParam("type", "api"),
            target.apply(base + "/_search").queryParam("type", "catalog"),
            target.apply(base + "/" + itemId),
            target.apply(base + "/" + itemId + "/content")
        )
            .map(request -> request.request().get().getStatus())
            .toList();
    }

    @SneakyThrows
    static PortalId givenPortal(PortalCrudServiceInMemory portals, String environmentId) {
        return portals
            .create(Portal.create(environmentId, ORG_ID, "Portal", URI.create("https://portal.example.com/next").toURL()))
            .getId();
    }

    static PortalNavigationPage page(PortalId portalId, PortalPageContentId contentId) {
        var page = PortalNavigationFixtures.page(PortalNavigationFixtures.randomNavigationId(), "Page", PortalArea.TOP_NAVBAR, contentId)
            .toBuilder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .reference(new NavigationItemReference.PortalReference(portalId))
            .build();
        page.markAsRoot();
        return page;
    }

    private static PortalNavigationFolder folder(String title, PortalId portalId, PortalArea area) {
        var folder = PortalNavigationFixtures.folder(PortalNavigationFixtures.randomNavigationId(), title, area)
            .toBuilder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .reference(new NavigationItemReference.PortalReference(portalId))
            .build();
        folder.markAsRoot();
        return folder;
    }

    private static PortalNavigationApi apiListing(String apiId, PortalId portalId) {
        var listing = PortalNavigationApi.builder()
            .id(PortalNavigationFixtures.randomNavigationId())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(apiId)
            .segment(apiId)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId(apiId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .reference(new NavigationItemReference.PortalReference(portalId))
            .build();
        listing.markAsRoot();
        return listing;
    }

    private static List<String> ids(List<PortalNavigationItem> items) {
        return items
            .stream()
            .map(item -> ((BasePortalNavigationItem) item.getActualInstance()).getId())
            .toList();
    }
}
