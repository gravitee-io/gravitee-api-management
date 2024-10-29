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
package io.gravitee.apim.core.plan.domain_service;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.tuple;

import fixtures.core.model.PlanFixtures;
import inmemory.InMemoryAlternative;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.plan.model.Plan;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ReorderPlanDomainServiceTest {

    private static final String API_ID = "my-api";

    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory(planCrudService);

    ReorderPlanDomainService service;

    @BeforeEach
    void setUp() {
        service = new ReorderPlanDomainService(planQueryService, planCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(planCrudService).forEach(InMemoryAlternative::reset);
    }

    @ParameterizedTest
    @MethodSource("reorderAfterUpdateTestData")
    void should_reorder_all_plans_when_order_is_updated(
        Map<String, Integer> existingOrder,
        Map.Entry<String, Integer> toUpdate,
        List<Tuple> expectedOrder
    ) {
        // Given
        var plans = givenExistingPlans(
            existingOrder
                .entrySet()
                .stream()
                .map(entry -> (Plan) PlanFixtures.HttpV4.aKeyless().toBuilder().id(entry.getKey()).order(entry.getValue()).build())
                .toList()
        );

        // When

        service.reorderAfterUpdate(
            plans
                .stream()
                .filter(p -> p.getId().equals(toUpdate.getKey()))
                .findFirst()
                .map(p -> p.toBuilder().order(toUpdate.getValue()).build())
                .orElseThrow()
        );
        // Then
        Assertions.assertThat(planCrudService.storage()).extracting(Plan::getId, Plan::getOrder).containsAll(expectedOrder);
    }

    @ParameterizedTest
    @MethodSource("reorderAfterDeleteTestData")
    void should_update_order_all_plan_when_plans_have_been_deleted(Map<String, Integer> existingOrder, List<Tuple> expectedOrder) {
        // Given
        givenExistingPlans(
            existingOrder
                .entrySet()
                .stream()
                .map(entry ->
                    (Plan) PlanFixtures.HttpV4.aKeyless().toBuilder().id(entry.getKey()).apiId(API_ID).order(entry.getValue()).build()
                )
                .toList()
        );

        // When
        service.refreshOrderAfterDelete(API_ID);

        // Then
        Assertions.assertThat(planCrudService.storage()).extracting(Plan::getId, Plan::getOrder).containsAll(expectedOrder);
    }

    static Stream<Arguments> reorderAfterUpdateTestData() {
        return Stream.of(
            Arguments.of(
                Map.ofEntries(entry("plan1", 1), entry("plan2", 2), entry("plan3", 3)),
                entry("plan3", 0),
                List.of(tuple("plan3", 1), tuple("plan1", 2), tuple("plan2", 3))
            ),
            Arguments.of(
                Map.ofEntries(entry("plan1", 1), entry("plan2", 2), entry("plan3", 3)),
                entry("plan1", 5),
                List.of(tuple("plan2", 1), tuple("plan3", 2), tuple("plan1", 3))
            ),
            Arguments.of(
                Map.ofEntries(entry("plan1", 1), entry("plan2", 2), entry("plan3", 3)),
                entry("plan1", 2),
                List.of(tuple("plan2", 1), tuple("plan1", 2), tuple("plan3", 3))
            )
        );
    }

    static Stream<Arguments> reorderAfterDeleteTestData() {
        return Stream.of(
            Arguments.of(
                Map.ofEntries(entry("plan1", 1), entry("plan2", 2), entry("plan3", 5)),
                List.of(tuple("plan1", 1), tuple("plan2", 2), tuple("plan3", 3))
            ),
            Arguments.of(
                Map.ofEntries(entry("plan1", 2), entry("plan2", 3), entry("plan3", 4)),
                List.of(tuple("plan1", 1), tuple("plan2", 2), tuple("plan3", 3))
            ),
            Arguments.of(
                Map.ofEntries(entry("plan1", 1), entry("plan2", 2), entry("plan3", 3)),
                List.of(tuple("plan1", 1), tuple("plan2", 2), tuple("plan3", 3))
            )
        );
    }

    List<Plan> givenExistingPlans(List<Plan> plans) {
        planQueryService.initWith(plans);
        return plans;
    }
}
