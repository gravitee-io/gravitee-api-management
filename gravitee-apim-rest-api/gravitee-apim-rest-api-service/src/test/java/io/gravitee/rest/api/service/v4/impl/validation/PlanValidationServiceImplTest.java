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
package io.gravitee.rest.api.service.v4.impl.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.v4.validation.FlowValidationService;
import io.gravitee.rest.api.service.v4.validation.PlanValidationService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class PlanValidationServiceImplTest {

    @Mock
    private FlowValidationService flowValidationService;

    private PlanValidationService cut;

    @BeforeEach
    void setUp() {
        cut = new PlanValidationServiceImpl(flowValidationService);
    }

    @Test
    void shouldReturnNullWhenNoPlan() {
        assertThat(cut.validateAndSanitize(ApiType.PROXY, null)).isNull();
    }

    @Test
    void shouldReturnEmptyWhenEmptySetOfPlans() {
        assertThat(cut.validateAndSanitize(ApiType.PROXY, Set.of())).isNotNull().isEmpty();
    }

    @Test
    void shouldValidateAndSanitizePlans() {
        PlanEntity plan1 = new PlanEntity();
        plan1.setId("plan1");
        PlanEntity plan2 = new PlanEntity();
        plan2.setId("plan2");
        Flow flowPlan2 = new Flow();
        plan2.setFlows(List.of(flowPlan2));
        PlanEntity plan3 = new PlanEntity();
        plan3.setId("plan3");
        Flow flow1Plan3 = new Flow();
        Flow flow2Plan3 = new Flow();
        plan3.setFlows(List.of(flow1Plan3, flow2Plan3));
        when(flowValidationService.validateAndSanitize(any(), anyList())).thenAnswer(invocation -> invocation.getArguments()[1]);
        final Set<PlanEntity> result = cut.validateAndSanitize(ApiType.PROXY, Set.of(plan1, plan2, plan3));
        assertThat(result).isNotNull().hasSize(3);
        assertThat(
            result
                .stream()
                .filter(p -> p.getId().equals("plan1"))
                .findFirst()
                .get()
                .getFlows()
        ).isEmpty();
        assertThat(
            result
                .stream()
                .filter(p -> p.getId().equals("plan2"))
                .findFirst()
                .get()
                .getFlows()
        ).hasSize(1);
        assertThat(
            result
                .stream()
                .filter(p -> p.getId().equals("plan3"))
                .findFirst()
                .get()
                .getFlows()
        ).hasSize(2);
    }
}
