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
package io.gravitee.rest.api.service.impl.filtering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.search.Order;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.search.query.SearchSortStrategy;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FilteringServiceImplTest {

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    ApiAuthorizationService apiAuthorizationService;

    @Mock
    ApiSearchService apiSearchService;

    @InjectMocks
    FilteringServiceImpl filteringService;

    @Test
    void getApplicationsOrderByNumberOfSubscriptions_emptyIds_doesNotRunUnfilteredQuery() {
        // An empty application set must not trigger an unfiltered ranking query.
        Collection<String> result = filteringService.getApplicationsOrderByNumberOfSubscriptions(new HashSet<>(), Order.DESC);

        assertThat(result).isEmpty();
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void getApplicationsOrderByNumberOfSubscriptions_nullIds_doesNotRunUnfilteredQuery() {
        Collection<String> result = filteringService.getApplicationsOrderByNumberOfSubscriptions(null, Order.DESC);

        assertThat(result).isEmpty();
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void getApplicationsOrderByNumberOfSubscriptions_withIds_delegatesToRanking() {
        Set<String> ranking = new LinkedHashSet<>(List.of("app-2"));
        when(subscriptionService.findReferenceIdsOrderByNumberOfSubscriptions(any(), eq(Order.DESC))).thenReturn(ranking);

        Collection<String> result = filteringService.getApplicationsOrderByNumberOfSubscriptions(
            new HashSet<>(List.of("app-1", "app-2")),
            Order.DESC
        );

        assertThat(result).contains("app-1", "app-2");
        verify(subscriptionService).findReferenceIdsOrderByNumberOfSubscriptions(any(), eq(Order.DESC));
    }

    @Test
    void searchApis_appliesStableSortStrategy() throws Exception {
        ExecutionContext executionContext = new ExecutionContext("org", "env");
        when(apiAuthorizationService.findAccessibleApiIdsForUser(eq(executionContext), eq("user"), any(ApiQuery.class))).thenReturn(
            Set.of("api-1")
        );

        filteringService.searchApis(executionContext, "user", "term");

        verify(apiSearchService).searchIds(
            eq(executionContext),
            eq("term"),
            any(),
            eq(null),
            any(),
            eq(false),
            eq(SearchSortStrategy.SCORE_WITH_NAME_AND_ID_TIE_BREAKERS)
        );
    }
}
