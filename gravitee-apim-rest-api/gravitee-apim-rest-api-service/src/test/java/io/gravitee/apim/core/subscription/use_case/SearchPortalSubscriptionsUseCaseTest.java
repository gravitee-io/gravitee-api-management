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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionSearchQueryService;
import io.gravitee.apim.core.subscription.query_service.SubscriptionTargetSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchPortalSubscriptionsUseCaseTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");
    private static final PageableImpl PAGEABLE = new PageableImpl(2, 10);

    @Mock
    private SubscriptionSearchQueryService subscriptionSearchQueryService;

    @Mock
    private SubscriptionTargetSearchQueryService subscriptionTargetSearchQueryService;

    @Captor
    private ArgumentCaptor<SubscriptionSearchQueryService.Criteria> criteriaCaptor;

    private SearchPortalSubscriptionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchPortalSubscriptionsUseCase(subscriptionSearchQueryService, subscriptionTargetSearchQueryService);
    }

    @Test
    void should_default_to_api_subscriptions() {
        var page = new Page<SubscriptionEntity>(List.of(), 2, 0, 0);
        when(
            subscriptionSearchQueryService.search(
                EXECUTION_CONTEXT,
                new SubscriptionSearchQueryService.Criteria(
                    Set.of(SubscriptionReferenceType.API),
                    null,
                    null,
                    Set.of("app-id"),
                    null,
                    Set.of(SubscriptionStatus.ACCEPTED),
                    null
                ),
                PAGEABLE
            )
        ).thenReturn(page);

        var output = useCase.execute(
            new SearchPortalSubscriptionsUseCase.Input(
                EXECUTION_CONTEXT,
                Set.of("app-id"),
                Set.of(SubscriptionStatus.ACCEPTED),
                null,
                null,
                null,
                null,
                PAGEABLE
            )
        );

        assertThat(output.page().getContent()).isEmpty();
        assertThat(output.page().getPageNumber()).isEqualTo(page.getPageNumber());
        assertThat(output.page().getTotalElements()).isEqualTo(page.getTotalElements());
        verify(subscriptionTargetSearchQueryService, never()).search(EXECUTION_CONTEXT, null, Set.of(SubscriptionReferenceType.API));
    }

    @Test
    void should_resolve_matching_target_ids_before_searching_subscriptions() {
        Set<SubscriptionReferenceType> referenceTypes = Set.of(SubscriptionReferenceType.API, SubscriptionReferenceType.API_PRODUCT);
        when(subscriptionTargetSearchQueryService.search(EXECUTION_CONTEXT, "payment", referenceTypes)).thenReturn(
            new SubscriptionTargetSearchQueryService.MatchingTargetIds(Set.of("api-1", "api-2"), Set.of("product-1"))
        );
        when(
            subscriptionSearchQueryService.search(
                org.mockito.ArgumentMatchers.eq(EXECUTION_CONTEXT),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(PAGEABLE)
            )
        ).thenReturn(new Page<>(List.of(), 2, 0, 3));

        useCase.execute(
            new SearchPortalSubscriptionsUseCase.Input(
                EXECUTION_CONTEXT,
                Set.of("app-id"),
                Set.of(SubscriptionStatus.ACCEPTED),
                referenceTypes,
                null,
                null,
                " payment ",
                PAGEABLE
            )
        );

        verify(subscriptionSearchQueryService).search(
            org.mockito.ArgumentMatchers.eq(EXECUTION_CONTEXT),
            criteriaCaptor.capture(),
            org.mockito.ArgumentMatchers.eq(PAGEABLE)
        );
        assertThat(criteriaCaptor.getValue().apiIds()).containsExactlyInAnyOrder("api-1", "api-2");
        assertThat(criteriaCaptor.getValue().apiProductIds()).containsExactly("product-1");
        assertThat(criteriaCaptor.getValue().referenceTypes()).isEqualTo(referenceTypes);
    }

    @Test
    void should_intersect_name_matches_with_explicit_target_ids() {
        when(subscriptionTargetSearchQueryService.search(EXECUTION_CONTEXT, "payment", Set.of(SubscriptionReferenceType.API))).thenReturn(
            new SubscriptionTargetSearchQueryService.MatchingTargetIds(Set.of("api-1", "api-2"), Set.of())
        );
        when(
            subscriptionSearchQueryService.search(
                org.mockito.ArgumentMatchers.eq(EXECUTION_CONTEXT),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(PAGEABLE)
            )
        ).thenReturn(new Page<>(List.of(), 2, 0, 1));

        useCase.execute(
            new SearchPortalSubscriptionsUseCase.Input(
                EXECUTION_CONTEXT,
                Set.of("app-id"),
                null,
                Set.of(SubscriptionReferenceType.API),
                Set.of("api-2", "api-3"),
                null,
                "payment",
                PAGEABLE
            )
        );

        verify(subscriptionSearchQueryService).search(
            org.mockito.ArgumentMatchers.eq(EXECUTION_CONTEXT),
            criteriaCaptor.capture(),
            org.mockito.ArgumentMatchers.eq(PAGEABLE)
        );
        assertThat(criteriaCaptor.getValue().apiIds()).containsExactly("api-2");
    }

    @Test
    void should_return_an_empty_page_without_searching_subscriptions_when_no_target_matches() {
        when(
            subscriptionTargetSearchQueryService.search(EXECUTION_CONTEXT, "missing", Set.of(SubscriptionReferenceType.API_PRODUCT))
        ).thenReturn(new SubscriptionTargetSearchQueryService.MatchingTargetIds(Set.of(), Set.of()));

        var output = useCase.execute(
            new SearchPortalSubscriptionsUseCase.Input(
                EXECUTION_CONTEXT,
                Set.of("app-id"),
                null,
                Set.of(SubscriptionReferenceType.API_PRODUCT),
                null,
                null,
                "missing",
                PAGEABLE
            )
        );

        assertThat(output.page().getContent()).isEmpty();
        assertThat(output.page().getPageNumber()).isEqualTo(2);
        assertThat(output.page().getTotalElements()).isZero();
        verify(subscriptionSearchQueryService, never()).search(
            org.mockito.ArgumentMatchers.eq(EXECUTION_CONTEXT),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(PAGEABLE)
        );
    }

    @Test
    void should_normalize_legacy_api_subscription_reference() {
        SubscriptionEntity legacySubscription = SubscriptionEntity.builder().id("subscription-id").api("api-id").build();
        when(
            subscriptionSearchQueryService.search(
                org.mockito.ArgumentMatchers.eq(EXECUTION_CONTEXT),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(PAGEABLE)
            )
        ).thenReturn(new Page<>(List.of(legacySubscription), 2, 1, 1));

        var output = useCase.execute(
            new SearchPortalSubscriptionsUseCase.Input(
                EXECUTION_CONTEXT,
                Set.of("app-id"),
                null,
                Set.of(SubscriptionReferenceType.API),
                null,
                null,
                null,
                PAGEABLE
            )
        );

        SubscriptionEntity normalized = output.page().getContent().getFirst();
        assertThat(normalized.getApi()).isEqualTo("api-id");
        assertThat(normalized.getReferenceId()).isEqualTo("api-id");
        assertThat(normalized.getReferenceType()).isEqualTo("API");
    }
}
