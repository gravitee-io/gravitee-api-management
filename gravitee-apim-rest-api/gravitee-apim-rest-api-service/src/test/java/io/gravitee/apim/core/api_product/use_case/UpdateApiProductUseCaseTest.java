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
package io.gravitee.apim.core.api_product.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import fixtures.core.model.ApiFixtures;
import inmemory.AbstractUseCaseTest;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.domain_service.ValidateApiProductService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.UpdateApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateApiProductUseCaseTest extends AbstractUseCaseTest {

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory(apiCrudService);
    private final EventCrudService eventCrudService = mock(EventCrudService.class);
    private final EventLatestCrudService eventLatestCrudService = mock(EventLatestCrudService.class);
    private UpdateApiProductUseCase updateApiProductUseCase;

    @BeforeEach
    void setUp() {
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var validateApiProductService = new ValidateApiProductService(apiQueryService);
        updateApiProductUseCase = new UpdateApiProductUseCase(
            apiProductCrudService,
            auditService,
            apiProductQueryService,
            validateApiProductService,
            eventCrudService,
            eventLatestCrudService
        );
    }

    @Test
    void should_update_api_product() {
        ApiProduct existing = ApiProduct.builder()
            .id("api-product-id")
            .name("Old Name")
            .version("1.0.0")
            .description("desc")
            .apiIds(Set.of("api-1"))
            .environmentId(ENV_ID)
            .build();
        apiProductCrudService.initWith(List.of(existing));
        apiProductQueryService.initWith(List.of(existing));

        Api api2 = createV4ProxyApi("api-2", true);
        apiCrudService.initWith(List.of(api2));

        var toUpdate = UpdateApiProduct.builder().name("New Name").version("2.0.0").description("new desc").apiIds(Set.of("api-2")).build();

        var input = new UpdateApiProductUseCase.Input("api-product-id", toUpdate, AUDIT_INFO);
        var output = updateApiProductUseCase.execute(input);

        assertAll(
            () -> assertThat(output.apiProduct().getId()).isEqualTo("api-product-id"),
            () -> assertThat(output.apiProduct().getName()).isEqualTo("New Name"),
            () -> assertThat(output.apiProduct().getVersion()).isEqualTo("2.0.0"),
            () -> assertThat(output.apiProduct().getDescription()).isEqualTo("new desc"),
            () -> assertThat(output.apiProduct().getApiIds()).containsExactly("api-2") // Replace, not merge
        );

        // Verify DEPLOY event was published
        verify(eventCrudService).createEvent(eq(ORG_ID), eq(ENV_ID), any(), any(), any(), any());
        verify(eventLatestCrudService).createOrPatchLatestEvent(eq(ORG_ID), eq("api-product-id"), any());
    }

    @Test
    void should_throw_exception_if_api_product_not_found() {
        var toUpdate = UpdateApiProduct.builder().name("Name").version("1.0.0").build();
        var input = new UpdateApiProductUseCase.Input("missing-id", toUpdate, AUDIT_INFO);
        Assertions.assertThatThrownBy(() -> updateApiProductUseCase.execute(input))
            .isInstanceOf(ApiProductNotFoundException.class)
            .hasMessageContaining("API Product not found");
    }

    @Test
    void should_filter_out_apis_not_allowed_in_product() {
        ApiProduct existing = ApiProduct.builder()
            .id("api-product-id")
            .name("API Product")
            .version("1.0.0")
            .apiIds(Set.of("api-1"))
            .environmentId(ENV_ID)
            .build();
        apiProductCrudService.initWith(List.of(existing));
        apiProductQueryService.initWith(List.of(existing));

        Api allowedApi = createV4ProxyApi("api-allowed", true);
        Api notAllowedApi = createV4ProxyApi("api-not-allowed", false);
        Api nullAllowedApi = createV4ProxyApi("api-null-allowed", null);

        apiCrudService.initWith(List.of(allowedApi, notAllowedApi, nullAllowedApi));

        var toUpdate = UpdateApiProduct.builder().apiIds(Set.of("api-allowed", "api-not-allowed", "api-null-allowed")).build();

        var input = new UpdateApiProductUseCase.Input("api-product-id", toUpdate, AUDIT_INFO);
        var output = updateApiProductUseCase.execute(input);
        assertThat(output.apiProduct().getApiIds()).containsExactly("api-allowed");
    }

    @Test
    void should_filter_out_non_existent_apis() {
        ApiProduct existing = ApiProduct.builder()
            .id("api-product-id")
            .name("API Product")
            .version("1.0.0")
            .apiIds(Set.of("api-1"))
            .environmentId(ENV_ID)
            .build();
        apiProductCrudService.initWith(List.of(existing));
        apiProductQueryService.initWith(List.of(existing));

        Api allowedApi = createV4ProxyApi("api-allowed", true);
        apiCrudService.initWith(List.of(allowedApi));

        var toUpdate = UpdateApiProduct.builder().apiIds(Set.of("api-allowed", "non-existent-1", "non-existent-2")).build();

        var input = new UpdateApiProductUseCase.Input("api-product-id", toUpdate, AUDIT_INFO);
        var output = updateApiProductUseCase.execute(input);

        assertThat(output.apiProduct().getApiIds()).containsExactly("api-allowed");
    }

    @Test
    void should_include_all_allowed_apis() {
        ApiProduct existing = ApiProduct.builder()
            .id("api-product-id")
            .name("API Product")
            .version("1.0.0")
            .apiIds(Set.of("api-1"))
            .environmentId(ENV_ID)
            .build();
        apiProductCrudService.initWith(List.of(existing));
        apiProductQueryService.initWith(List.of(existing));

        Api api1 = createV4ProxyApi("api-1", true);
        Api api2 = createV4ProxyApi("api-2", true);
        Api api3 = createV4ProxyApi("api-3", true);

        apiCrudService.initWith(List.of(api1, api2, api3));

        var toUpdate = UpdateApiProduct.builder().apiIds(Set.of("api-1", "api-2", "api-3")).build();

        var input = new UpdateApiProductUseCase.Input("api-product-id", toUpdate, AUDIT_INFO);
        var output = updateApiProductUseCase.execute(input);
        assertThat(output.apiProduct().getApiIds()).containsExactlyInAnyOrder("api-1", "api-2", "api-3");
    }

    @Test
    void should_clear_all_apis_when_empty_apiIds_provided() {
        ApiProduct existing = ApiProduct.builder()
            .id("api-product-id")
            .name("API Product")
            .version("1.0.0")
            .apiIds(Set.of("api-1", "api-2", "api-3"))
            .environmentId(ENV_ID)
            .build();
        apiProductCrudService.initWith(List.of(existing));
        apiProductQueryService.initWith(List.of(existing));

        var toUpdate = UpdateApiProduct.builder().apiIds(Set.of()).build();

        var input = new UpdateApiProductUseCase.Input("api-product-id", toUpdate, AUDIT_INFO);
        var output = updateApiProductUseCase.execute(input);

        // Empty apiIds should clear the entire list (replace, not merge)
        assertThat(output.apiProduct().getApiIds()).isEmpty();
    }

    @Test
    void should_clear_apis_when_all_provided_apis_are_not_allowed() {
        ApiProduct existing = ApiProduct.builder()
            .id("api-product-id")
            .name("API Product")
            .version("1.0.0")
            .apiIds(Set.of("api-existing"))
            .environmentId(ENV_ID)
            .build();
        apiProductCrudService.initWith(List.of(existing));
        apiProductQueryService.initWith(List.of(existing));

        Api api1 = createV4ProxyApi("api-1", false);
        Api api2 = createV4ProxyApi("api-2", null);

        apiCrudService.initWith(List.of(api1, api2));

        var toUpdate = UpdateApiProduct.builder().apiIds(Set.of("api-1", "api-2")).build();

        var input = new UpdateApiProductUseCase.Input("api-product-id", toUpdate, AUDIT_INFO);
        var output = updateApiProductUseCase.execute(input);
        assertThat(output.apiProduct().getApiIds()).isEmpty();
    }

    private Api createV4ProxyApi(String id, Boolean allowedInApiProducts) {
        var apiDefinition = ApiFixtures.aProxyApiV4().getApiDefinitionValue();
        if (apiDefinition instanceof io.gravitee.definition.model.v4.Api v4Api) {
            var updatedApiDefinition = v4Api.toBuilder().id(id).allowedInApiProducts(allowedInApiProducts).build();
            return ApiFixtures.aProxyApiV4().toBuilder().id(id).environmentId(ENV_ID).apiDefinitionValue(updatedApiDefinition).build();
        }
        throw new IllegalStateException("Expected V4 API definition");
    }
}
