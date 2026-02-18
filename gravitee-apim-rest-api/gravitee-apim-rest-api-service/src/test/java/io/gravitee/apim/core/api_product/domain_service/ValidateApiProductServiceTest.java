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
package io.gravitee.apim.core.api_product.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ValidateApiProductServiceTest {

    private static final String ENV_ID = "env-id";

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory(apiCrudService);
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();

    private ValidateApiProductService validateApiProductService;

    @BeforeEach
    void setUp() {
        Stream.of(apiCrudService, apiQueryService, planQueryService, apiProductQueryService).forEach(InMemoryAlternative::reset);
        validateApiProductService = new ValidateApiProductService(
            apiQueryService,
            apiCrudService,
            planQueryService,
            apiProductQueryService
        );
    }

    @Nested
    class Validate {

        @Test
        void should_throw_when_name_is_empty() {
            ApiProduct product = ApiProduct.builder().id("p1").name("").version("1.0").environmentId(ENV_ID).build();

            assertThatThrownBy(() -> validateApiProductService.validate(product))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("API Product name is required.");
        }

        @Test
        void should_throw_when_name_is_null() {
            ApiProduct product = ApiProduct.builder().id("p1").version("1.0").environmentId(ENV_ID).build();

            assertThatThrownBy(() -> validateApiProductService.validate(product))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("API Product name is required.");
        }

        @Test
        void should_throw_when_version_is_empty() {
            ApiProduct product = ApiProduct.builder().id("p1").name("Product").version("").environmentId(ENV_ID).build();

            assertThatThrownBy(() -> validateApiProductService.validate(product))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("API Product version is required.");
        }

        @Test
        void should_not_throw_when_name_and_version_are_set() {
            ApiProduct product = ApiProduct.builder().id("p1").name("Product").version("1.0").environmentId(ENV_ID).build();

            assertThatCode(() -> validateApiProductService.validate(product)).doesNotThrowAnyException();
        }
    }

    @Nested
    class ValidateApiIdsForProduct {

        @Test
        void should_do_nothing_when_apiIds_empty() {
            assertThatCode(() -> validateApiProductService.validateApiIdsForProduct(ENV_ID, List.of())).doesNotThrowAnyException();
        }

        @Test
        void should_throw_when_api_does_not_exist() {
            ApiProduct product = ApiProduct.builder().id("p1").name("P").version("1.0").environmentId(ENV_ID).build();
            apiProductQueryService.initWith(List.of(product));

            assertThatThrownBy(() -> validateApiProductService.validateApiIdsForProduct(ENV_ID, List.of("non-existent")))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("do not exist")
                .hasMessageContaining("non-existent");
        }

        @Test
        void should_throw_when_api_is_not_v4() {
            Api v2Api = ApiFixtures.aProxyApiV2().toBuilder().id("api-v2").environmentId(ENV_ID).build();
            apiCrudService.initWith(List.of(v2Api));

            assertThatThrownBy(() -> validateApiProductService.validateApiIdsForProduct(ENV_ID, List.of("api-v2")))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("not V4")
                .hasMessageContaining("api-v2");
        }

        @Test
        void should_throw_when_api_not_allowed_in_api_products() {
            Api api = createV4ProxyApi("api-1", false);
            apiCrudService.initWith(List.of(api));

            assertThatThrownBy(() -> validateApiProductService.validateApiIdsForProduct(ENV_ID, List.of("api-1")))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("not allowed in API Products")
                .hasMessageContaining("api-1");
        }

        @Test
        void should_not_throw_when_apis_valid_and_allowed() {
            Api api = createV4ProxyApi("api-1", true);
            apiCrudService.initWith(List.of(api));

            assertThatCode(() -> validateApiProductService.validateApiIdsForProduct(ENV_ID, List.of("api-1"))).doesNotThrowAnyException();
        }
    }

    @Nested
    class GetApisToUndeployOnRemoval {

        @Test
        void should_return_empty_when_apiIds_null() {
            assertThat(validateApiProductService.getApisToUndeployOnRemoval(null, "product-1")).isEmpty();
        }

        @Test
        void should_return_empty_when_apiIds_empty() {
            assertThat(validateApiProductService.getApisToUndeployOnRemoval(Set.of(), "product-1")).isEmpty();
        }

        @Test
        void should_return_api_when_removing_deployed_api_without_plan_and_not_in_other_product() {
            Api deployedNoPlan = createV4ProxyApi("api-1", true).toBuilder().deployedAt(ZonedDateTime.now()).build();
            apiCrudService.initWith(List.of(deployedNoPlan));

            ApiProduct currentProduct = ApiProduct.builder()
                .id("product-1")
                .name("P")
                .version("1.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1"))
                .build();
            apiProductQueryService.initWith(List.of(currentProduct));

            List<Api> toUndeploy = validateApiProductService.getApisToUndeployOnRemoval(Set.of("api-1"), "product-1");
            assertThat(toUndeploy).hasSize(1).extracting(Api::getId).containsExactly("api-1");
        }

        @Test
        void should_return_empty_when_removing_deployed_api_that_has_published_plan() {
            Api deployedApi = createV4ProxyApi("api-1", true).toBuilder().deployedAt(ZonedDateTime.now()).build();
            apiCrudService.initWith(List.of(deployedApi));

            Plan plan = PlanFixtures.HttpV4.aKeyless().toBuilder().referenceId("api-1").id("plan-1").build();
            planQueryService.initWith(List.of(plan));

            ApiProduct currentProduct = ApiProduct.builder()
                .id("product-1")
                .name("P")
                .version("1.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1"))
                .build();
            apiProductQueryService.initWith(List.of(currentProduct));

            assertThat(validateApiProductService.getApisToUndeployOnRemoval(Set.of("api-1"), "product-1")).isEmpty();
        }

        @Test
        void should_return_empty_when_removing_undeployed_api() {
            Api undeployedApi = createV4ProxyApi("api-1", true).toBuilder().deployedAt(null).build();
            apiCrudService.initWith(List.of(undeployedApi));

            ApiProduct currentProduct = ApiProduct.builder()
                .id("product-1")
                .name("P")
                .version("1.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1"))
                .build();
            apiProductQueryService.initWith(List.of(currentProduct));

            assertThat(validateApiProductService.getApisToUndeployOnRemoval(Set.of("api-1"), "product-1")).isEmpty();
        }

        @Test
        void should_return_empty_when_removed_api_is_in_another_product_with_plan() {
            Api deployedNoPlan = createV4ProxyApi("api-1", true).toBuilder().deployedAt(ZonedDateTime.now()).build();
            apiCrudService.initWith(List.of(deployedNoPlan));

            ApiProduct currentProduct = ApiProduct.builder()
                .id("product-1")
                .name("P1")
                .version("1.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1"))
                .build();
            ApiProduct otherProduct = ApiProduct.builder()
                .id("product-2")
                .name("P2")
                .version("1.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1"))
                .build();
            apiProductQueryService.initWith(List.of(currentProduct, otherProduct));

            Plan product2Plan = PlanFixtures.HttpV4.aKeyless()
                .toBuilder()
                .id("plan-product-2")
                .referenceId("product-2")
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .environmentId(ENV_ID)
                .build();
            product2Plan.setPlanStatus(PlanStatus.PUBLISHED);
            planQueryService.initWith(List.of(product2Plan));

            assertThat(validateApiProductService.getApisToUndeployOnRemoval(Set.of("api-1"), "product-1")).isEmpty();
        }

        @Test
        void should_return_api_when_only_other_product_containing_api_has_no_plans() {
            Api deployedNoPlan = createV4ProxyApi("api-1", true).toBuilder().deployedAt(ZonedDateTime.now()).build();
            apiCrudService.initWith(List.of(deployedNoPlan));

            ApiProduct currentProduct = ApiProduct.builder()
                .id("product-1")
                .name("P1")
                .version("1.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1"))
                .build();
            ApiProduct otherProductNoPlans = ApiProduct.builder()
                .id("product-2")
                .name("P2")
                .version("1.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1"))
                .build();
            apiProductQueryService.initWith(List.of(currentProduct, otherProductNoPlans));

            List<Api> toUndeploy = validateApiProductService.getApisToUndeployOnRemoval(Set.of("api-1"), "product-1");
            assertThat(toUndeploy).hasSize(1).extracting(Api::getId).containsExactly("api-1");
        }

        @Test
        void should_return_api_when_only_other_product_containing_api_is_in_different_environment() {
            Api deployedNoPlan = createV4ProxyApi("api-1", true).toBuilder().deployedAt(ZonedDateTime.now()).build();
            apiCrudService.initWith(List.of(deployedNoPlan));

            ApiProduct currentProduct = ApiProduct.builder()
                .id("product-1")
                .name("P1")
                .version("1.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1"))
                .build();
            ApiProduct productInOtherEnv = ApiProduct.builder()
                .id("product-2")
                .name("P2")
                .version("1.0")
                .environmentId("other-env")
                .apiIds(Set.of("api-1"))
                .build();
            apiProductQueryService.initWith(List.of(currentProduct, productInOtherEnv));

            List<Api> toUndeploy = validateApiProductService.getApisToUndeployOnRemoval(Set.of("api-1"), "product-1");
            assertThat(toUndeploy).hasSize(1).extracting(Api::getId).containsExactly("api-1");
        }

        @Test
        void should_return_empty_when_api_not_found_in_crud() {
            ApiProduct currentProduct = ApiProduct.builder()
                .id("product-1")
                .name("P")
                .version("1.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("missing-api"))
                .build();
            apiProductQueryService.initWith(List.of(currentProduct));

            assertThat(validateApiProductService.getApisToUndeployOnRemoval(Set.of("missing-api"), "product-1")).isEmpty();
        }

        @Test
        void should_return_empty_for_multiple_apis_when_other_product_has_plan() {
            Api deployedNoPlan1 = createV4ProxyApi("api-1", true).toBuilder().deployedAt(ZonedDateTime.now()).build();
            Api deployedNoPlan2 = createV4ProxyApi("api-2", true).toBuilder().deployedAt(ZonedDateTime.now()).build();
            apiCrudService.initWith(List.of(deployedNoPlan1, deployedNoPlan2));

            ApiProduct currentProduct = ApiProduct.builder()
                .id("product-1")
                .name("P1")
                .version("1.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1", "api-2"))
                .build();
            ApiProduct otherProduct = ApiProduct.builder()
                .id("product-2")
                .name("P2")
                .version("1.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1", "api-2"))
                .build();
            apiProductQueryService.initWith(List.of(currentProduct, otherProduct));

            Plan product2Plan = PlanFixtures.HttpV4.aKeyless()
                .toBuilder()
                .id("plan-product-2")
                .referenceId("product-2")
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .environmentId(ENV_ID)
                .build();
            product2Plan.setPlanStatus(PlanStatus.PUBLISHED);
            planQueryService.initWith(List.of(product2Plan));

            assertThat(validateApiProductService.getApisToUndeployOnRemoval(Set.of("api-1", "api-2"), "product-1")).isEmpty();
        }
    }

    private static Api createV4ProxyApi(String id, boolean allowedInApiProducts) {
        var apiDefinition = ApiFixtures.aProxyApiV4().getApiDefinitionValue();
        if (apiDefinition instanceof io.gravitee.definition.model.v4.Api v4Api) {
            var updatedDef = v4Api.toBuilder().id(id).allowedInApiProducts(allowedInApiProducts).build();
            return ApiFixtures.aProxyApiV4().toBuilder().id(id).environmentId(ENV_ID).apiDefinitionValue(updatedDef).build();
        }
        throw new IllegalStateException("Expected V4 API definition");
    }
}
