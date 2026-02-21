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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanUpdates;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateApiProductPlanUseCaseTest {

    private static final String API_PRODUCT_ID = "api-product-id";
    private static final String PLAN_ID = "plan-id";

    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final UpdatePlanDomainService updatePlanDomainService = mock(UpdatePlanDomainService.class);

    private final UpdateApiProductPlanUseCase updateApiProductPlanUseCase = new UpdateApiProductPlanUseCase(
        updatePlanDomainService,
        planCrudService,
        apiProductCrudService
    );

    @BeforeEach
    void setUp() {
        Plan existingPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(PLAN_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .build();
        planCrudService.initWith(List.of(existingPlan));
        apiProductCrudService.initWith(List.of(ApiProduct.builder().id(API_PRODUCT_ID).environmentId("env").name("API Product").build()));
    }

    @Test
    void should_update_plan_for_api_product() {
        Plan updatedPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(PLAN_ID)
            .name("Updated name")
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .build();
        when(
            updatePlanDomainService.updatePlanForApiProduct(any(Plan.class), any(), any(ApiProduct.class), any(AuditInfo.class))
        ).thenReturn(updatedPlan);

        PlanUpdates planUpdates = PlanUpdates.builder().id(PLAN_ID).name("Updated name").build();
        AuditInfo auditInfo = AuditInfoFixtures.anAuditInfo("org", "env", "user");
        var output = updateApiProductPlanUseCase.execute(
            UpdateApiProductPlanUseCase.Input.builder().planToUpdate(planUpdates).apiProductId(API_PRODUCT_ID).auditInfo(auditInfo).build()
        );

        assertThat(output.updated()).isEqualTo(updatedPlan);
    }
}
