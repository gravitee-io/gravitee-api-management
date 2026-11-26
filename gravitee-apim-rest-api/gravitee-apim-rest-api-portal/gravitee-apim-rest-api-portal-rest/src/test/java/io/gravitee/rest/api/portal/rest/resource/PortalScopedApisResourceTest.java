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

import static io.gravitee.rest.api.portal.rest.fixture.PortalNavigationFixtures.givenDefaultPortal;
import static io.gravitee.rest.api.portal.rest.resource.PortalScopedNavigationItemsResourceTest.ENV_ID;
import static io.gravitee.rest.api.portal.rest.resource.PortalScopedNavigationItemsResourceTest.ORG_ID;
import static io.gravitee.rest.api.portal.rest.resource.PortalScopedNavigationItemsResourceTest.givenPortal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import inmemory.ApiPortalSearchQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.portal.rest.fixture.PortalNavigationFixtures;
import io.gravitee.rest.api.portal.rest.model.ApisResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

public class PortalScopedApisResourceTest extends AbstractResourceTest {

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

    @Autowired
    private ApiPortalSearchQueryServiceInMemory apiPortalSearchQueryService;

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
        resetAllMocks();
        GraviteeContext.setCurrentOrganization(ORG_ID);
        GraviteeContext.setCurrentEnvironment(ENV_ID);
        portal = givenPortal(portalCrudService, ENV_ID);
        otherPortal = givenPortal(portalCrudService, ENV_ID);
        portalNavigationItemsQueryService.initWith(List.of(apiListing("api-own", portal), apiListing("api-other", otherPortal)));
        apiPortalSearchQueryService.initWith(List.of(anApi("api-own"), anApi("api-other")));
        echoApisById();
    }

    private static Api anApi(String id) {
        return Api.builder().id(id).name(id).environmentId(ENV_ID).build();
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        portalNavigationItemsQueryService.reset();
        apiPortalSearchQueryService.reset();
        portalCrudService.reset();
    }

    @ParameterizedTest
    @ValueSource(strings = { "documentation", "" })
    void should_search_only_the_portals_apis_whatever_the_view(String view) {
        Response response = search(target(portal + "/apis/_search").queryParam("view", view));

        assertThat(response.getStatus()).isEqualTo(200);
        var body = response.readEntity(ApisResponse.class);
        assertThat(body.getData()).extracting("id").containsExactly("api-own");
    }

    @Test
    void should_search_only_the_default_portals_apis_on_the_unscoped_path() {
        var defaultPortal = givenDefaultPortal(portalCrudService, ENV_ID);
        portalNavigationItemsQueryService.initWith(
            List.of(apiListing("api-default", defaultPortal), apiListing("api-own", portal), apiListing("api-other", otherPortal))
        );
        apiPortalSearchQueryService.initWith(List.of(anApi("api-default"), anApi("api-own"), anApi("api-other")));

        Response response = search(unscoped().queryParam("view", "documentation"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.readEntity(ApisResponse.class).getData()).extracting("id").containsExactly("api-default");
    }

    @Test
    void should_answer_not_found_on_the_unscoped_path_without_a_default_portal() {
        assertThat(search(unscoped().queryParam("view", "documentation")).getStatus()).isEqualTo(404);
    }

    private WebTarget unscoped() {
        return root().path("environments/" + ENV_ID + "/apis/_search");
    }

    @Test
    void should_answer_not_found_for_an_unknown_portal() {
        assertThat(search(target("6c2b1e9e-0000-4000-8000-000000000000/apis/_search")).getStatus()).isEqualTo(404);
    }

    @Test
    void should_answer_bad_request_for_a_malformed_portal_id() {
        assertThat(search(target("not-a-uuid/apis/_search")).getStatus()).isEqualTo(400);
    }

    @Test
    void should_answer_not_found_for_a_portal_of_another_environment() {
        var elsewhere = givenPortal(portalCrudService, "another-env");

        assertThat(search(target(elsewhere + "/apis/_search")).getStatus()).isEqualTo(404);
    }

    @Test
    void should_not_expose_the_other_api_endpoints_under_a_portal() {
        assertThat(target(portal + "/apis").request().get().getStatus()).isEqualTo(404);
        assertThat(target(portal + "/apis/categories").request().get().getStatus()).isEqualTo(404);
        assertThat(target(portal + "/apis/api-own/ratings").request().get().getStatus()).isEqualTo(404);
        assertThat(target(portal + "/apis/api-own/subscribers").request().get().getStatus()).isEqualTo(404);
    }

    static Response search(WebTarget target) {
        return target.request().post(Entity.json(null));
    }

    private void echoApisById() {
        doAnswer(invocation ->
            invocation
                .<ApiQuery>getArgument(1)
                .getIds()
                .stream()
                .map(id -> {
                    var entity = new ApiEntity();
                    entity.setId(id);
                    entity.setName(id);
                    return entity;
                })
                .toList()
        )
            .when(apiSearchService)
            .search(eq(GraviteeContext.getExecutionContext()), any(ApiQuery.class));
        when(apiMapper.convert(eq(GraviteeContext.getExecutionContext()), any())).thenCallRealMethod();
    }

    static PortalNavigationApi apiListing(String apiId, PortalId portalId) {
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
}
