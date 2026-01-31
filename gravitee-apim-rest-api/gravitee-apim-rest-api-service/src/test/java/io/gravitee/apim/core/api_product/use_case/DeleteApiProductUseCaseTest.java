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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import inmemory.AbstractUseCaseTest;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api_product.domain_service.ValidateApiProductService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
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

        // Verify UNDEPLOY event was published
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
}
