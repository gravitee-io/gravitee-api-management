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
package io.gravitee.apim.core.subscription.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SearchSubscriptionsUseCaseTest {

    private static final String REFERENCE_ID = "ref-id";
    private static final ExecutionContext EXECUTION_CONTEXT = GraviteeContext.getExecutionContext();
    private static final PageableImpl PAGEABLE = new PageableImpl(1, 10);

    @Mock
    private SubscriptionSearchQueryService subscriptionSearchQueryService;

    @Captor
    private ArgumentCaptor<SubscriptionReferenceType> referenceTypeCaptor;

    private AutoCloseable mocks;
    private SearchSubscriptionsUseCase useCase;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        useCase = new SearchSubscriptionsUseCase(subscriptionSearchQueryService);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @ParameterizedTest
    @EnumSource(SubscriptionReferenceType.class)
    void should_call_query_service_with_reference_type_and_id(SubscriptionReferenceType referenceType) {
        Page<SubscriptionEntity> expectedPage = new Page<>(List.of(), 1, 10, 0);
        when(
            subscriptionSearchQueryService.search(
                eq(EXECUTION_CONTEXT),
                eq(REFERENCE_ID),
                eq(referenceType),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(PAGEABLE)
            )
        ).thenReturn(expectedPage);

        var input = new SearchSubscriptionsUseCase.Input(EXECUTION_CONTEXT, REFERENCE_ID, referenceType, null, null, null, null, PAGEABLE);

        SearchSubscriptionsUseCase.Output output = useCase.execute(input);

        assertThat(output.page()).isSameAs(expectedPage);
        verify(subscriptionSearchQueryService).search(
            eq(EXECUTION_CONTEXT),
            eq(REFERENCE_ID),
            referenceTypeCaptor.capture(),
            eq(null),
            eq(null),
            eq(null),
            eq(null),
            eq(PAGEABLE)
        );
        assertThat(referenceTypeCaptor.getValue()).isEqualTo(referenceType);
    }

    @Test
    void should_pass_filters_to_query_service() {
        Set<String> applicationIds = Set.of("app-1");
        Set<String> planIds = Set.of("plan-1");
        Set<SubscriptionStatus> statuses = Set.of(SubscriptionStatus.ACCEPTED);
        String apiKey = "api-key-1";
        Page<SubscriptionEntity> expectedPage = new Page<>(List.of(), 1, 10, 0);
        when(
            subscriptionSearchQueryService.search(
                eq(EXECUTION_CONTEXT),
                eq(REFERENCE_ID),
                eq(SubscriptionReferenceType.API_PRODUCT),
                eq(applicationIds),
                eq(planIds),
                eq(statuses),
                eq(apiKey),
                eq(PAGEABLE)
            )
        ).thenReturn(expectedPage);

        var input = new SearchSubscriptionsUseCase.Input(
            EXECUTION_CONTEXT,
            REFERENCE_ID,
            SubscriptionReferenceType.API_PRODUCT,
            applicationIds,
            planIds,
            statuses,
            apiKey,
            PAGEABLE
        );

        useCase.execute(input);

        verify(subscriptionSearchQueryService).search(
            eq(EXECUTION_CONTEXT),
            eq(REFERENCE_ID),
            eq(SubscriptionReferenceType.API_PRODUCT),
            eq(applicationIds),
            eq(planIds),
            eq(statuses),
            eq(apiKey),
            eq(PAGEABLE)
        );
    }
}
