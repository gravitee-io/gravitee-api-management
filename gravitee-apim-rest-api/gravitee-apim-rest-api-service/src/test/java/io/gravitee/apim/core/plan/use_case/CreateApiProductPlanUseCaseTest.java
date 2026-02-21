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
package io.gravitee.apim.core.plan.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiProductCrudServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateApiProductPlanUseCaseTest {

    private static final String API_PRODUCT_ID = "api-product-id";
    private static final String GENERATED_ID = "generated-plan-id";

    private final CreatePlanDomainService createPlanDomainService = mock(CreatePlanDomainService.class);
    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();

    private final CreateApiProductPlanUseCase createApiProductPlanUseCase = new CreateApiProductPlanUseCase(
        createPlanDomainService,
        apiProductCrudService
    );

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> GENERATED_ID);
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @BeforeEach
    void setUp() {
        apiProductCrudService.initWith(
            List.of(ApiProduct.builder().id(API_PRODUCT_ID).environmentId("env-id").name("API Product").build())
        );
    }

    @AfterEach
    void tearDown() {
        apiProductCrudService.reset();
    }

    @Test
    void should_create_plan_for_api_product() {
        ApiProduct apiProduct = apiProductCrudService.get(API_PRODUCT_ID);
        Plan planToCreate = Plan.builder()
            .name("Plan")
            .planDefinitionHttpV4(
                io.gravitee.definition.model.v4.plan.Plan.builder().name("Plan").mode(PlanMode.STANDARD).status(PlanStatus.STAGING).build()
            )
            .build();
        Plan createdPlan = Plan.builder().id(GENERATED_ID).name("Plan").referenceId(API_PRODUCT_ID).build();
        when(
            createPlanDomainService.createApiProductPlan(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(apiProduct),
                org.mockito.ArgumentMatchers.any()
            )
        ).thenReturn(createdPlan);

        AuditInfo auditInfo = AuditInfoFixtures.anAuditInfo("org", "env", "user");
        var output = createApiProductPlanUseCase.execute(
            new CreateApiProductPlanUseCase.Input(API_PRODUCT_ID, ap -> planToCreate, auditInfo)
        );

        assertThat(output.id()).isEqualTo(GENERATED_ID);
        assertThat(output.plan()).isEqualTo(createdPlan);
    }
}
