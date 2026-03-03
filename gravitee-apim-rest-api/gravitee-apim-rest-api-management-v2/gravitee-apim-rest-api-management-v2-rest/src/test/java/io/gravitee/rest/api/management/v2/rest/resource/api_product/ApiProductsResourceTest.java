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
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.core.model.LicenseFixtures;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.use_case.CreateApiProductUseCase;
import io.gravitee.apim.core.api_product.use_case.GetApiProductsUseCase;
import io.gravitee.apim.core.api_product.use_case.SearchApiProductsUseCase;
import io.gravitee.apim.core.api_product.use_case.VerifyApiProductNameUseCase;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.common.data.domain.Page;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.management.v2.rest.model.ApiProductSearchQuery;
import io.gravitee.rest.api.management.v2.rest.model.CreateApiProduct;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiProduct;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiProductResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiProductsResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    private CreateApiProductUseCase createApiProductUseCase;

    @Inject
    private GetApiProductsUseCase getApiProductsUseCase;

    @Inject
    private VerifyApiProductNameUseCase verifyApiProductNameUseCase;

    @Inject
    private SearchApiProductsUseCase searchApiProductsUseCase;

    @Inject
    private LicenseManager licenseManager;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/api-products";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(createApiProductUseCase, getApiProductsUseCase, verifyApiProductNameUseCase, searchApiProductsUseCase);
    }

    @Nested
    class VerifyApiProductNameTest {

        @Test
        void should_verify_api_product_name_is_available() {
            when(permissionService.hasPermission(any(), any(), any(), any(RolePermissionAction[].class))).thenReturn(true);
            VerifyApiProduct verifyApiProduct = new VerifyApiProduct();
            verifyApiProduct.setName("My API Product");

            when(verifyApiProductNameUseCase.execute(any())).thenReturn(new VerifyApiProductNameUseCase.Output("My API Product"));

            final Response response = rootTarget().path("_verify").request().post(json(verifyApiProduct));

            assertThat(response.getStatus()).isEqualTo(OK_200);

            var result = response.readEntity(VerifyApiProductResponse.class);
            assertAll(() -> assertThat(result.getOk()).isTrue(), () -> assertThat(result.getReason()).isNull());
        }

        @Test
        void should_verify_api_product_name_is_available_for_update() {
            when(permissionService.hasPermission(any(), any(), any(), any(RolePermissionAction[].class))).thenReturn(true);
            VerifyApiProduct verifyApiProduct = new VerifyApiProduct();
            verifyApiProduct.setName("My API Product");
            verifyApiProduct.setApiProductId("existing-id");
            when(verifyApiProductNameUseCase.execute(any())).thenReturn(new VerifyApiProductNameUseCase.Output("My API Product"));
            final Response response = rootTarget().path("_verify").request().post(json(verifyApiProduct));
            assertThat(response.getStatus()).isEqualTo(OK_200);
            var result = response.readEntity(VerifyApiProductResponse.class);
            assertAll(() -> assertThat(result.getOk()).isTrue(), () -> assertThat(result.getReason()).isNull());
        }

        @Test
        void should_return_400_if_missing_body() {
            when(permissionService.hasPermission(any(), any(), any(), any(RolePermissionAction[].class))).thenReturn(true);
            final Response response = rootTarget().path("_verify").request().post(json(null));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            reset(permissionService);
            when(
                permissionService.hasPermission(
                    any(),
                    eq(RolePermission.ENVIRONMENT_API_PRODUCT),
                    eq(ENV_ID),
                    eq(RolePermissionAction.CREATE),
                    eq(RolePermissionAction.UPDATE)
                )
            ).thenReturn(false);
            VerifyApiProduct verifyApiProduct = new VerifyApiProduct();
            verifyApiProduct.setName("My API Product");
            Response response = rootTarget().path("_verify").request().post(json(verifyApiProduct));
            MAPIAssertions.assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }
    }

    @Nested
    class CreateApiProductTest {

        @Test
        void should_create_api_product() {
            CreateApiProduct createApiProduct = new CreateApiProduct();
            createApiProduct.setName("My API Product");
            createApiProduct.setDescription("Product description");
            createApiProduct.setVersion("1.0.0");

            ApiProduct output = ApiProduct.builder()
                .id("api-product-id")
                .environmentId(ENV_ID)
                .name(createApiProduct.getName())
                .description(createApiProduct.getDescription())
                .version(createApiProduct.getVersion())
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .apiIds(new HashSet<>())
                .primaryOwner(PrimaryOwnerEntity.builder().id("user-id").displayName("User").type(PrimaryOwnerEntity.Type.USER).build())
                .build();

            when(createApiProductUseCase.execute(any())).thenReturn(new CreateApiProductUseCase.Output(output));

            final Response response = rootTarget().request().post(json(createApiProduct));

            assertThat(response.getStatus()).isEqualTo(CREATED_201);

            var createdApiProduct = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.ApiProduct.class);

            assertAll(
                () -> assertThat(createdApiProduct.getId()).isEqualTo(output.getId()),
                () -> assertThat(createdApiProduct.getName()).isEqualTo(createApiProduct.getName()),
                () -> assertThat(createdApiProduct.getDescription()).isEqualTo(createApiProduct.getDescription()),
                () -> assertThat(createdApiProduct.getVersion()).isEqualTo(createApiProduct.getVersion()),
                () -> assertThat(createdApiProduct.getCreatedAt()).isNotNull(),
                () -> assertThat(createdApiProduct.getUpdatedAt()).isNotNull()
            );
        }

        @Test
        void should_return_400_if_missing_body() {
            final Response response = rootTarget().request().post(json(null));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_when_license_does_not_allow_api_product() {
            when(createApiProductUseCase.execute(any())).thenThrow(
                new io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException("api-product")
            );

            CreateApiProduct createApiProduct = new CreateApiProduct();
            createApiProduct.setName("My API Product");
            createApiProduct.setDescription("Product description");
            createApiProduct.setVersion("1.0.0");

            final Response response = rootTarget().request().post(json(createApiProduct));

            assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.ENVIRONMENT_API_PRODUCT, ENV_ID, RolePermissionAction.CREATE, () ->
                rootTarget().request().post(json(new CreateApiProduct()))
            );
        }
    }

    @Nested
    class GetApiProductsTest {

        @Test
        void should_get_api_products() {
            Set<ApiProduct> apiProducts = Set.of(
                ApiProduct.builder()
                    .id("api-product-1")
                    .environmentId(ENV_ID)
                    .name("Product 1")
                    .description("Description 1")
                    .version("1.0.0")
                    .createdAt(ZonedDateTime.now())
                    .updatedAt(ZonedDateTime.now())
                    .apiIds(new HashSet<>())
                    .build(),
                ApiProduct.builder()
                    .id("api-product-2")
                    .environmentId(ENV_ID)
                    .name("Product 2")
                    .description("Description 2")
                    .version("1.0.0")
                    .createdAt(ZonedDateTime.now())
                    .updatedAt(ZonedDateTime.now())
                    .apiIds(new HashSet<>())
                    .build()
            );

            when(getApiProductsUseCase.execute(any())).thenReturn(GetApiProductsUseCase.Output.multiple(apiProducts));

            final Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);

            var responseMap = response.readEntity(java.util.Map.class);

            assertAll(
                () -> assertThat(responseMap).containsKey("data"),
                () -> assertThat(responseMap).containsKey("pagination"),
                () -> assertThat(responseMap).containsKey("links")
            );
        }

        @Test
        void should_return_empty_list_when_no_api_products() {
            when(getApiProductsUseCase.execute(any())).thenReturn(GetApiProductsUseCase.Output.multiple(Set.of()));

            final Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);

            var responseMap = response.readEntity(java.util.Map.class);
            assertThat(responseMap).containsKey("data");
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.ENVIRONMENT_API_PRODUCT, ENV_ID, RolePermissionAction.READ, () -> rootTarget().request().get());
        }
    }

    @Nested
    class SearchApiProductsTest {

        @Test
        void should_return_full_list_when_no_query_and_no_ids() {
            when(permissionService.hasPermission(any(), any(), any(), any(RolePermissionAction[].class))).thenReturn(true);
            ApiProductSearchQuery searchQuery = new ApiProductSearchQuery();
            List<ApiProduct> apiProductsList = List.of(
                ApiProduct.builder()
                    .id("api-product-1")
                    .environmentId(ENV_ID)
                    .name("Product One")
                    .version("1.0.0")
                    .createdAt(ZonedDateTime.now())
                    .updatedAt(ZonedDateTime.now())
                    .apiIds(new HashSet<>())
                    .build()
            );
            when(searchApiProductsUseCase.execute(any())).thenReturn(
                new SearchApiProductsUseCase.Output(new Page<>(apiProductsList, 1, apiProductsList.size(), apiProductsList.size()))
            );

            final Response response = rootTarget().path("_search").request().post(json(searchQuery));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var responseMap = response.readEntity(java.util.Map.class);
            assertThat(responseMap).containsKeys("data", "pagination", "links");
            assertThat((java.util.List<?>) responseMap.get("data")).hasSize(1);
        }

        @Test
        void should_search_by_query() {
            when(permissionService.hasPermission(any(), any(), any(), any(RolePermissionAction[].class))).thenReturn(true);
            ApiProductSearchQuery searchQuery = new ApiProductSearchQuery();
            searchQuery.setQuery("My Product");

            List<ApiProduct> apiProductsList = List.of(
                ApiProduct.builder()
                    .id("api-product-1")
                    .environmentId(ENV_ID)
                    .name("My Product One")
                    .version("1.0.0")
                    .createdAt(ZonedDateTime.now())
                    .updatedAt(ZonedDateTime.now())
                    .apiIds(new HashSet<>())
                    .build()
            );
            when(searchApiProductsUseCase.execute(any())).thenReturn(
                new SearchApiProductsUseCase.Output(new Page<>(apiProductsList, 1, apiProductsList.size(), apiProductsList.size()))
            );

            final Response response = rootTarget().path("_search").request().post(json(searchQuery));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var responseMap = response.readEntity(java.util.Map.class);
            assertThat(responseMap).containsKeys("data", "pagination", "links");
        }

        @Test
        void should_search_by_ids() {
            when(permissionService.hasPermission(any(), any(), any(), any(RolePermissionAction[].class))).thenReturn(true);
            ApiProductSearchQuery searchQuery = new ApiProductSearchQuery();
            searchQuery.setIds(java.util.List.of("api-product-1", "api-product-2"));

            List<ApiProduct> apiProductsList = List.of(
                ApiProduct.builder()
                    .id("api-product-1")
                    .environmentId(ENV_ID)
                    .name("Product 1")
                    .version("1.0.0")
                    .createdAt(ZonedDateTime.now())
                    .updatedAt(ZonedDateTime.now())
                    .apiIds(new HashSet<>())
                    .build()
            );
            when(searchApiProductsUseCase.execute(any())).thenReturn(
                new SearchApiProductsUseCase.Output(new Page<>(apiProductsList, 1, apiProductsList.size(), apiProductsList.size()))
            );

            final Response response = rootTarget().path("_search").request().post(json(searchQuery));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var responseMap = response.readEntity(java.util.Map.class);
            assertThat(responseMap).containsKeys("data", "pagination", "links");
        }

        @Test
        void should_return_400_when_invalid_sortBy() {
            when(permissionService.hasPermission(any(), any(), any(), any(RolePermissionAction[].class))).thenReturn(true);
            ApiProductSearchQuery searchQuery = new ApiProductSearchQuery();
            searchQuery.setQuery("test");

            final Response response = rootTarget().path("_search").queryParam("sortBy", "invalid_sort").request().post(json(searchQuery));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            ApiProductSearchQuery searchQuery = new ApiProductSearchQuery();
            searchQuery.setQuery("test");
            shouldReturn403(RolePermission.ENVIRONMENT_API_PRODUCT, ENV_ID, RolePermissionAction.READ, () ->
                rootTarget().path("_search").request().post(json(searchQuery))
            );
        }
    }
}
