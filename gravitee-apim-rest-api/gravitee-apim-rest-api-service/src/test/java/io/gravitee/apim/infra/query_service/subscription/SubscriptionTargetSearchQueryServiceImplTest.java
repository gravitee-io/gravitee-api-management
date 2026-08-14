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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionTargetSearchQueryServiceImplTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private SearchEngineService searchEngineService;

    private SubscriptionTargetSearchQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        queryService = new SubscriptionTargetSearchQueryServiceImpl(apiSearchService, searchEngineService);
    }

    @Test
    void should_search_api_names_only_when_api_type_is_selected() {
        when(apiSearchService.searchIds(EXECUTION_CONTEXT, "payment", Map.of(), null)).thenReturn(List.of("api-1"));

        var result = queryService.search(EXECUTION_CONTEXT, "payment", Set.of(SubscriptionReferenceType.API));

        assertThat(result.apiIds()).containsExactly("api-1");
        assertThat(result.apiProductIds()).isEmpty();
        verifyNoInteractions(searchEngineService);
    }

    @Test
    void should_search_api_product_names_only_when_api_product_type_is_selected() {
        when(searchEngineService.search(eq(EXECUTION_CONTEXT), any())).thenReturn(new SearchResult(List.of("product-1")));

        var result = queryService.search(EXECUTION_CONTEXT, "payment", Set.of(SubscriptionReferenceType.API_PRODUCT));

        assertThat(result.apiIds()).isEmpty();
        assertThat(result.apiProductIds()).containsExactly("product-1");
        verify(apiSearchService, never()).searchIds(any(), any(), any(), any());
    }

    @Test
    void should_search_both_target_types() {
        when(apiSearchService.searchIds(EXECUTION_CONTEXT, "payment", Map.of(), null)).thenReturn(List.of("api-1"));
        when(searchEngineService.search(eq(EXECUTION_CONTEXT), any())).thenReturn(new SearchResult(List.of("product-1")));

        var result = queryService.search(
            EXECUTION_CONTEXT,
            "payment",
            Set.of(SubscriptionReferenceType.API, SubscriptionReferenceType.API_PRODUCT)
        );

        assertThat(result.apiIds()).containsExactly("api-1");
        assertThat(result.apiProductIds()).containsExactly("product-1");
    }
}
