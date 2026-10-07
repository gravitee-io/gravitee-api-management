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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.flow.exception.InvalidFlowException;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class UpdateApiProductPlanFlowsUseCaseTest {

    private static final String API_PRODUCT_ID = "api-product-id";
    private static final String OTHER_API_PRODUCT_ID = "other-api-product-id";
    private static final AuditInfo AUDIT_INFO = new AuditInfo("org-id", "env-id", AuditActor.builder().userId("user-id").build());
    private static final ApiProduct API_PRODUCT = ApiProduct.builder().id(API_PRODUCT_ID).environmentId("env-id").build();

    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final FlowValidationDomainService flowValidationDomainService = new FlowValidationDomainService(
        mock(PolicyValidationDomainService.class),
        new EntrypointPluginQueryServiceInMemory()
    );

    @Mock
    private UpdatePlanDomainService updatePlanDomainService;

    private UpdateApiProductPlanFlowsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateApiProductPlanFlowsUseCase(
            updatePlanDomainService,
            flowValidationDomainService,
            planQueryService,
            apiProductCrudService
        );
        apiProductCrudService.initWith(List.of(API_PRODUCT));
    }

    @AfterEach
    void tearDown() {
        planQueryService.reset();
        apiProductCrudService.reset();
    }

    @Test
    void should_write_the_given_name_and_flows_of_each_plan() {
        planQueryService.initWith(List.of(productPlan("plan-1", PlanStatus.PUBLISHED), productPlan("plan-2", PlanStatus.STAGING)));
        when(updatePlanDomainService.updatePlanForApiProduct(any(), any(), eq(API_PRODUCT), eq(AUDIT_INFO))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );
        var flows1 = List.of(Flow.builder().name("budget-1").build());
        var flows2 = List.of(Flow.builder().name("budget-2").build());

        var output = useCase.execute(
            new UpdateApiProductPlanFlowsUseCase.Input(
                API_PRODUCT_ID,
                List.of(
                    new UpdateApiProductPlanFlowsUseCase.PlanFlows("plan-1", "Renamed", flows1),
                    new UpdateApiProductPlanFlowsUseCase.PlanFlows("plan-2", null, flows2)
                ),
                AUDIT_INFO
            )
        );

        assertThat(output.updated()).extracting(Plan::getId).containsExactly("plan-1", "plan-2");
        assertThat(output.updated()).extracting(Plan::getName).containsExactly("Renamed", "stored name of plan-2");
        assertThat(output.updated())
            .extracting(plan -> plan.getPlanDefinitionHttpV4().getFlows())
            .containsExactly(flows1, flows2);
    }

    @Test
    void should_keep_every_other_field_of_the_stored_plan() {
        var stored = productPlan("plan-1", PlanStatus.PUBLISHED);
        var expected = stored.copy();
        planQueryService.initWith(List.of(stored));
        var captor = ArgumentCaptor.forClass(Plan.class);
        when(updatePlanDomainService.updatePlanForApiProduct(captor.capture(), any(), any(), any())).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        useCase.execute(
            new UpdateApiProductPlanFlowsUseCase.Input(
                API_PRODUCT_ID,
                List.of(new UpdateApiProductPlanFlowsUseCase.PlanFlows("plan-1", null, List.of())),
                AUDIT_INFO
            )
        );

        var written = captor.getValue();
        assertThat(written).usingRecursiveComparison().ignoringFields("planDefinitionHttpV4.flows").isEqualTo(expected);
    }

    @Test
    void should_check_closed_plans_against_the_statuses_of_the_plans_being_written() {
        planQueryService.initWith(List.of(productPlan("plan-1", PlanStatus.PUBLISHED), productPlan("plan-2", PlanStatus.CLOSED)));
        when(updatePlanDomainService.updatePlanForApiProduct(any(), any(), any(), any())).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        useCase.execute(
            new UpdateApiProductPlanFlowsUseCase.Input(
                API_PRODUCT_ID,
                List.of(
                    new UpdateApiProductPlanFlowsUseCase.PlanFlows("plan-1", null, List.of()),
                    new UpdateApiProductPlanFlowsUseCase.PlanFlows("plan-2", null, List.of())
                ),
                AUDIT_INFO
            )
        );

        verify(updatePlanDomainService, times(2)).updatePlanForApiProduct(
            any(),
            eq(Map.of("plan-1", PlanStatus.PUBLISHED, "plan-2", PlanStatus.CLOSED)),
            eq(API_PRODUCT),
            eq(AUDIT_INFO)
        );
    }

    @Test
    void should_write_nothing_when_one_plan_is_not_a_plan_of_the_product() {
        planQueryService.initWith(
            List.of(
                productPlan("plan-1", PlanStatus.PUBLISHED),
                productPlan("foreign-plan", PlanStatus.PUBLISHED).toBuilder().referenceId(OTHER_API_PRODUCT_ID).build()
            )
        );

        assertThatThrownBy(() ->
            useCase.execute(
                new UpdateApiProductPlanFlowsUseCase.Input(
                    API_PRODUCT_ID,
                    List.of(
                        new UpdateApiProductPlanFlowsUseCase.PlanFlows("plan-1", null, List.of()),
                        new UpdateApiProductPlanFlowsUseCase.PlanFlows("foreign-plan", null, List.of())
                    ),
                    AUDIT_INFO
                )
            )
        )
            .isInstanceOf(PlanNotFoundException.class)
            .hasMessageContaining("foreign-plan");
        verify(updatePlanDomainService, never()).updatePlanForApiProduct(any(), any(), any(), any());
    }

    @Test
    void should_write_nothing_when_one_plan_is_listed_twice() {
        planQueryService.initWith(List.of(productPlan("plan-1", PlanStatus.PUBLISHED)));

        assertThatThrownBy(() ->
            useCase.execute(
                new UpdateApiProductPlanFlowsUseCase.Input(
                    API_PRODUCT_ID,
                    List.of(
                        new UpdateApiProductPlanFlowsUseCase.PlanFlows("plan-1", null, List.of()),
                        new UpdateApiProductPlanFlowsUseCase.PlanFlows("plan-1", "Renamed", List.of())
                    ),
                    AUDIT_INFO
                )
            )
        )
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("plan-1");
        verify(updatePlanDomainService, never()).updatePlanForApiProduct(any(), any(), any(), any());
    }

    @Test
    void should_write_nothing_when_the_flows_of_a_later_plan_are_invalid() {
        planQueryService.initWith(List.of(productPlan("plan-1", PlanStatus.PUBLISHED), productPlan("plan-2", PlanStatus.PUBLISHED)));
        var withoutPathOperator = Flow.builder()
            .name("budget-2")
            .selectors(List.of(HttpSelector.builder().path("/").pathOperator(null).build()))
            .build();

        assertThatThrownBy(() ->
            useCase.execute(
                new UpdateApiProductPlanFlowsUseCase.Input(
                    API_PRODUCT_ID,
                    List.of(
                        new UpdateApiProductPlanFlowsUseCase.PlanFlows("plan-1", null, List.of()),
                        new UpdateApiProductPlanFlowsUseCase.PlanFlows("plan-2", null, List.of(withoutPathOperator))
                    ),
                    AUDIT_INFO
                )
            )
        ).isInstanceOf(InvalidFlowException.class);
        verify(updatePlanDomainService, never()).updatePlanForApiProduct(any(), any(), any(), any());
    }

    @Test
    void should_refuse_a_plan_without_an_http_v4_definition() {
        planQueryService.initWith(List.of(productPlan("plan-1", PlanStatus.PUBLISHED).toBuilder().planDefinitionHttpV4(null).build()));

        assertThatThrownBy(() ->
            useCase.execute(
                new UpdateApiProductPlanFlowsUseCase.Input(
                    API_PRODUCT_ID,
                    List.of(new UpdateApiProductPlanFlowsUseCase.PlanFlows("plan-1", null, List.of())),
                    AUDIT_INFO
                )
            )
        )
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("plan-1");
        verify(updatePlanDomainService, never()).updatePlanForApiProduct(any(), any(), any(), any());
    }

    @Test
    void should_require_the_flows_of_a_plan() {
        assertThatThrownBy(() -> new UpdateApiProductPlanFlowsUseCase.PlanFlows("plan-1", null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("plan-1");
    }

    private static Plan productPlan(String id, PlanStatus status) {
        var plan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(id)
            .name("stored name of " + id)
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .build();
        plan.setPlanStatus(status);
        return plan;
    }
}
