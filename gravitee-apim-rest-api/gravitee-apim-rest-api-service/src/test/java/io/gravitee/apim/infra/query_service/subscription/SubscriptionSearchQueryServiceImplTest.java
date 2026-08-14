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
package io.gravitee.apim.infra.query_service.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionSearchQueryService;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionSearchQueryServiceImplTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");

    @Mock
    private SubscriptionService subscriptionService;

    private SubscriptionSearchQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        queryService = new SubscriptionSearchQueryServiceImpl(subscriptionService);
    }

    @Test
    void should_search_multiple_target_types_with_stable_sorting() {
        var criteria = new SubscriptionSearchQueryService.Criteria(
            Set.of(SubscriptionReferenceType.API, SubscriptionReferenceType.API_PRODUCT),
            Set.of("api-id"),
            Set.of("api-product-id"),
            Set.of("application-id"),
            Set.of("plan-id"),
            Set.of(SubscriptionStatus.ACCEPTED),
            null
        );
        when(subscriptionService.search(eq(EXECUTION_CONTEXT), org.mockito.ArgumentMatchers.any(SubscriptionQuery.class))).thenReturn(
            List.of()
        );

        queryService.search(EXECUTION_CONTEXT, criteria, null);

        var queryCaptor = ArgumentCaptor.forClass(SubscriptionQuery.class);
        verify(subscriptionService).search(eq(EXECUTION_CONTEXT), queryCaptor.capture());
        assertThat(queryCaptor.getValue()).satisfies(query -> {
            assertThat(query.getReferenceTypes()).containsExactlyInAnyOrder(
                GenericPlanEntity.ReferenceType.API,
                GenericPlanEntity.ReferenceType.API_PRODUCT
            );
            assertThat(query.getApis()).containsExactly("api-id");
            assertThat(query.getApiProducts()).containsExactly("api-product-id");
            assertThat(query.getSortable().getField()).isEqualTo("id");
            assertThat(query.getSortable().isAscOrder()).isTrue();
        });
    }

    @Test
    void should_preserve_existing_sorting_for_a_single_target_type() {
        var criteria = new SubscriptionSearchQueryService.Criteria(
            Set.of(SubscriptionReferenceType.API),
            null,
            null,
            Set.of("application-id"),
            null,
            null,
            null
        );
        when(subscriptionService.search(eq(EXECUTION_CONTEXT), org.mockito.ArgumentMatchers.any(SubscriptionQuery.class))).thenReturn(
            List.of()
        );

        queryService.search(EXECUTION_CONTEXT, criteria, null);

        var queryCaptor = ArgumentCaptor.forClass(SubscriptionQuery.class);
        verify(subscriptionService).search(eq(EXECUTION_CONTEXT), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getSortable()).isNull();
    }
}
