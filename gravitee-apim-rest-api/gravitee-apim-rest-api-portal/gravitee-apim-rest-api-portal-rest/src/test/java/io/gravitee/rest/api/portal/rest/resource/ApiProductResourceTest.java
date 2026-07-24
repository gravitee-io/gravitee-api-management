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

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.rest.api.portal.rest.model.PortalApiProductDetails;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final UUID API_PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final PortalNavigationItemId NAVIGATION_ITEM_ID = PortalNavigationItemId.of("00000000-0000-0000-0000-000000000102");

    @Autowired
    private ApiProductQueryServiceInMemory apiProductQueryService;

    @Autowired
    private ApiQueryServiceInMemory apiQueryService;

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;

    @Override
    protected String contextPath() {
        return "api-products/";
    }

    @BeforeEach
    void setUp() {
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
        apiProductQueryService.reset();
        apiQueryService.reset();
        navigationItemsQueryService.reset();
    }

    @Test
    void should_return_api_product_details_and_accessible_apis() {
        var api = Api.builder().id("api-id").environmentId(ENVIRONMENT_ID).name("Payments API").version("2.0.0").build();
        apiProductQueryService.initWith(List.of(apiProduct(Set.of(api.getId()))));
        apiQueryService.initWith(List.of(api));
        navigationItemsQueryService.initWith(List.of(apiProductNavigationItem(), apiNavigationItem(api)));

        Response response = target(API_PRODUCT_ID.toString()).request().get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        var result = response.readEntity(PortalApiProductDetails.class);
        assertThat(result.getId()).isEqualTo(API_PRODUCT_ID);
        assertThat(result.getName()).isEqualTo("AI Workspace");
        assertThat(result.getDescription()).isEqualTo("Consumer description");
        assertThat(result.getVersion()).isEqualTo("1.0.0");
        assertThat(result.getKind()).isEqualTo(PortalApiProductDetails.KindEnum.AI_WORKSPACE);
        assertThat(result.getNavigationItemId()).isEqualTo(NAVIGATION_ITEM_ID.id());
        assertThat(result.getTags()).containsExactly("ai", "public");
        assertThat(result.getApis())
            .singleElement()
            .satisfies(includedApi -> {
                assertThat(includedApi.getId()).isEqualTo("api-id");
                assertThat(includedApi.getName()).isEqualTo("Payments API");
                assertThat(includedApi.getVersion()).isEqualTo("2.0.0");
            });
    }

    @Test
    void should_return_not_found_when_api_product_is_not_exposed_in_navigation() {
        apiProductQueryService.initWith(List.of(apiProduct(Set.of())));

        Response response = target(API_PRODUCT_ID.toString()).request().get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    private static ApiProduct apiProduct(Set<String> apiIds) {
        return ApiProduct.builder()
            .id(API_PRODUCT_ID.toString())
            .environmentId(ENVIRONMENT_ID)
            .name("AI Workspace")
            .description("Consumer description")
            .version("1.0.0")
            .kind(ApiProductKind.AI_WORKSPACE)
            .tags(Set.of("public", "ai"))
            .apiIds(apiIds)
            .build();
    }

    private static PortalNavigationApiProduct apiProductNavigationItem() {
        return PortalNavigationApiProduct.builder()
            .id(NAVIGATION_ITEM_ID)
            .organizationId("organization-id")
            .environmentId(ENVIRONMENT_ID)
            .title("AI Workspace")
            .segment("ai-workspace")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiProductId(API_PRODUCT_ID.toString())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private static PortalNavigationItem apiNavigationItem(Api api) {
        return PortalNavigationApi.builder()
            .id(PortalNavigationItemId.random())
            .organizationId("organization-id")
            .environmentId(ENVIRONMENT_ID)
            .title(api.getName())
            .segment("payments-api")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(NAVIGATION_ITEM_ID)
            .apiId(api.getId())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }
}
