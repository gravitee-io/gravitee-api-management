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
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.use_case.DeleteApiProductUseCase;
import io.gravitee.apim.core.api_product.use_case.GetApiProductsUseCase;
import io.gravitee.apim.core.api_product.use_case.UpdateApiProductUseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApiProductResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";

    @Inject
    private GetApiProductsUseCase getApiProductByIdUseCase;

    @Inject
    private DeleteApiProductUseCase deleteApiProductUseCase;

    @Inject
    private UpdateApiProductUseCase updateApiProductUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/api-products/" + API_PRODUCT_ID;
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
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(getApiProductByIdUseCase, deleteApiProductUseCase, updateApiProductUseCase);
    }

    @Nested
    class GetApiProductByIdTest {

        @Test
        void should_get_api_product_by_id() {
            ApiProduct apiProduct = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .environmentId(ENV_ID)
                .name("My API Product")
                .description("Product description")
                .version("1.0.0")
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .apiIds(new HashSet<>())
                .build();

            when(getApiProductByIdUseCase.execute(any())).thenReturn(GetApiProductsUseCase.Output.single(Optional.of(apiProduct)));

            final Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);

            var result = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.ApiProduct.class);

            assertAll(
                () -> assertThat(result.getId()).isEqualTo(API_PRODUCT_ID),
                () -> assertThat(result.getName()).isEqualTo("My API Product"),
                () -> assertThat(result.getDescription()).isEqualTo("Product description"),
                () -> assertThat(result.getVersion()).isEqualTo("1.0.0"),
                () -> assertThat(result.getEnvironmentId()).isEqualTo(ENV_ID)
            );
        }

        @Test
        void should_return_404_when_api_product_not_found() {
            when(getApiProductByIdUseCase.execute(any())).thenReturn(GetApiProductsUseCase.Output.single(Optional.empty()));

            final Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_DEFINITION, API_PRODUCT_ID, RolePermissionAction.READ, () ->
                rootTarget().request().get()
            );
        }
    }

    @Nested
    class DeleteApiProductTest {

        @Test
        void should_delete_api_product() {
            final Response response = rootTarget().request().delete();

            MAPIAssertions.assertThat(response).hasStatus(NO_CONTENT_204);

            var captor = ArgumentCaptor.forClass(DeleteApiProductUseCase.Input.class);
            verify(deleteApiProductUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                var input = captor.getValue();
                soft.assertThat(input.apiProductId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(input.auditInfo()).isInstanceOf(AuditInfo.class);
            });
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_DEFINITION, API_PRODUCT_ID, RolePermissionAction.DELETE, () ->
                rootTarget().request().delete()
            );
        }
    }

    @Nested
    class UpdateApiProductTest {

        @Test
        void should_update_api_product() {
            ApiProduct updatedApiProduct = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .environmentId(ENV_ID)
                .name("Updated Product")
                .description("Updated description")
                .version("2.0.0")
                .createdAt(ZonedDateTime.now().minusDays(1))
                .updatedAt(ZonedDateTime.now())
                .apiIds(Set.of("api-1", "api-2"))
                .build();

            when(updateApiProductUseCase.execute(any())).thenReturn(new UpdateApiProductUseCase.Output(updatedApiProduct));

            var updatePayload = new io.gravitee.rest.api.management.v2.rest.model.UpdateApiProduct();
            updatePayload.setName("Updated Product");
            updatePayload.setDescription("Updated description");
            updatePayload.setVersion("2.0.0");

            io.gravitee.rest.api.management.v2.rest.model.ApiProduct updatedResponse;
            try (Response response = rootTarget().request().put(json(updatePayload))) {
                assertThat(response.getStatus()).isEqualTo(OK_200);
                updatedResponse = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.ApiProduct.class);
            }

            assertAll(
                () -> assertThat(updatedResponse.getId()).isEqualTo(API_PRODUCT_ID),
                () -> assertThat(updatedResponse.getName()).isEqualTo("Updated Product"),
                () -> assertThat(updatedResponse.getDescription()).isEqualTo("Updated description"),
                () -> assertThat(updatedResponse.getVersion()).isEqualTo("2.0.0"),
                () -> assertThat(updatedResponse.getEnvironmentId()).isEqualTo(ENV_ID),
                () -> assertThat(updatedResponse.getCreatedAt()).isNotNull(),
                () -> assertThat(updatedResponse.getUpdatedAt()).isNotNull()
            );

            var captor = ArgumentCaptor.forClass(UpdateApiProductUseCase.Input.class);
            verify(updateApiProductUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                var input = captor.getValue();
                soft.assertThat(input.apiProductId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(input.updateApiProduct().getName()).isEqualTo("Updated Product");
                soft.assertThat(input.auditInfo()).isInstanceOf(AuditInfo.class);
            });
        }

        @Test
        void should_return_400_if_execute_fails_with_invalid_data_exception() {
            when(updateApiProductUseCase.execute(any())).thenThrow(new InvalidDataException("Name is required."));

            try (
                Response response = rootTarget().request().put(json(new io.gravitee.rest.api.management.v2.rest.model.UpdateApiProduct()))
            ) {
                assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            }
        }

        @Test
        void should_return_400_if_missing_body() {
            try (Response response = rootTarget().request().put(json(""))) {
                assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            }
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_DEFINITION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().request().put(json(""))
            );
        }
    }
}
