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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import fixtures.core.model.ApiFixtures;
import inmemory.AbstractUseCaseTest;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api_product.domain_service.ValidateApiProductService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteApiProductUseCaseTest extends AbstractUseCaseTest {

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory(apiCrudService);
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final EventCrudService eventCrudService = mock(EventCrudService.class);
    private final EventLatestCrudService eventLatestCrudService = mock(EventLatestCrudService.class);
    private final ApiStateDomainService apiStateDomainService = mock(ApiStateDomainService.class);
    private DeleteApiProductUseCase deleteApiProductUseCase;

    @BeforeEach
    void setUp() {
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var validateApiProductService = new ValidateApiProductService(
            apiQueryService,
            apiCrudService,
            planQueryService,
            apiProductQueryService
        );
        deleteApiProductUseCase = new DeleteApiProductUseCase(
            apiProductCrudService,
            auditService,
            apiProductQueryService,
            validateApiProductService,
            apiStateDomainService,
            eventCrudService,
            eventLatestCrudService
        );
    }

    @Test
    void should_delete_api_product() {
        var productId = "api-product-id";
        apiProductCrudService.create(
            io.gravitee.apim.core.api_product.model.ApiProduct.builder().id(productId).name("Product").environmentId(ENV_ID).build()
        );
        apiProductQueryService.initWith(
            java.util.List.of(
                io.gravitee.apim.core.api_product.model.ApiProduct.builder().id(productId).name("Product").environmentId(ENV_ID).build()
            )
        );
        var input = new DeleteApiProductUseCase.Input(productId, AUDIT_INFO);
        deleteApiProductUseCase.execute(input);
        assertThat(apiProductCrudService.findById(productId)).isEmpty();

        // No APIs in product → stop must not be called
        verify(apiStateDomainService, never()).stop(any(), any());
        verify(eventCrudService).createEvent(eq(ORG_ID), eq(ENV_ID), any(), any(), any(), any());
        verify(eventLatestCrudService).createOrPatchLatestEvent(eq(ORG_ID), eq(productId), any());
    }

    @Test
    void should_throw_exception_if_product_does_not_exist() {
        var input = new DeleteApiProductUseCase.Input("missing-id", AUDIT_INFO);

        assertThatThrownBy(() -> deleteApiProductUseCase.execute(input))
            .isInstanceOf(ApiProductNotFoundException.class)
            .hasMessage("API Product not found.");
    }

    @Test
    void should_auto_undeploy_deployed_api_without_plans_then_delete_product() {
        var productId = "api-product-id";
        var deployedApiNoPlans = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("api-deployed-no-plans")
            .environmentId(ENV_ID)
            .deployedAt(ZonedDateTime.now())
            .build();
        ApiProduct product = ApiProduct.builder()
            .id(productId)
            .name("Product")
            .environmentId(ENV_ID)
            .apiIds(Set.of("api-deployed-no-plans"))
            .build();

        apiCrudService.initWith(List.of(deployedApiNoPlans));
        apiProductCrudService.initWith(List.of(product));
        apiProductQueryService.initWith(List.of(product));

        var input = new DeleteApiProductUseCase.Input(productId, AUDIT_INFO);

        deleteApiProductUseCase.execute(input);

        assertThat(apiProductCrudService.findById(productId)).isEmpty();
        verify(apiStateDomainService).stop(argThat(api -> "api-deployed-no-plans".equals(api.getId())), eq(AUDIT_INFO));
        verify(eventCrudService).createEvent(eq(ORG_ID), eq(ENV_ID), any(), any(), any(), any());
    }

    @Test
    void should_not_call_stop_when_product_has_only_undeployed_apis() {
        var productId = "api-product-id";
        var undeployedApi = ApiFixtures.aProxyApiV4().toBuilder().id("api-undeployed").environmentId(ENV_ID).deployedAt(null).build();
        ApiProduct product = ApiProduct.builder()
            .id(productId)
            .name("Product")
            .environmentId(ENV_ID)
            .apiIds(Set.of("api-undeployed"))
            .build();

        apiCrudService.initWith(List.of(undeployedApi));
        apiProductCrudService.initWith(List.of(product));
        apiProductQueryService.initWith(List.of(product));

        deleteApiProductUseCase.execute(new DeleteApiProductUseCase.Input(productId, AUDIT_INFO));

        assertThat(apiProductCrudService.findById(productId)).isEmpty();
        verify(apiStateDomainService, never()).stop(any(), any());
    }

    @Test
    void should_stop_each_deployed_api_without_plan_when_deleting_product_with_multiple_apis() {
        var productId = "api-product-id";
        var api1 = ApiFixtures.aProxyApiV4().toBuilder().id("api-1").environmentId(ENV_ID).deployedAt(ZonedDateTime.now()).build();
        var api2 = ApiFixtures.aProxyApiV4().toBuilder().id("api-2").environmentId(ENV_ID).deployedAt(ZonedDateTime.now()).build();
        ApiProduct product = ApiProduct.builder()
            .id(productId)
            .name("Product")
            .environmentId(ENV_ID)
            .apiIds(Set.of("api-1", "api-2"))
            .build();

        apiCrudService.initWith(List.of(api1, api2));
        apiProductCrudService.initWith(List.of(product));
        apiProductQueryService.initWith(List.of(product));

        deleteApiProductUseCase.execute(new DeleteApiProductUseCase.Input(productId, AUDIT_INFO));

        assertThat(apiProductCrudService.findById(productId)).isEmpty();
        verify(apiStateDomainService, times(2)).stop(any(), eq(AUDIT_INFO));
        verify(apiStateDomainService).stop(argThat(api -> "api-1".equals(api.getId())), eq(AUDIT_INFO));
        verify(apiStateDomainService).stop(argThat(api -> "api-2".equals(api.getId())), eq(AUDIT_INFO));
    }
}
