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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.exception.ApiProductPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.search.model.IndexableApiProduct;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.PaginationInvalidException;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiProductSearchServiceImplTest {

    private static final String ENV_ID = "env-1";
    private static final String ORG_ID = "org-1";

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private ApiProductQueryService apiProductQueryService;

    @Mock
    private ApiProductPrimaryOwnerDomainService apiProductPrimaryOwnerDomainService;

    private ApiProductSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApiProductSearchServiceImpl(searchEngineService, apiProductQueryService, apiProductPrimaryOwnerDomainService);
    }

    @Test
    void should_return_empty_page_short_circuit_when_search_has_no_results() {
        ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);
        when(searchEngineService.search(any(), any())).thenReturn(new SearchResult(Set.of(), 0));
        var queryBuilder = QueryBuilder.create(IndexableApiProduct.class);
        var pageable = new PageableImpl(1, 10);

        Page<ApiProduct> result = service.search(executionContext, queryBuilder, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPageNumber()).isZero();
        assertThat(result.getTotalElements()).isZero();
        verify(searchEngineService).search(eq(executionContext), any());
        verify(apiProductQueryService, org.mockito.Mockito.never()).findByEnvironmentIdAndIdIn(any(), any());
    }

    @Test
    void should_apply_pagination_get_page_subset() {
        ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);
        when(searchEngineService.search(any(), any())).thenReturn(new SearchResult(List.of("id-1", "id-2", "id-3", "id-4", "id-5"), 5));
        ApiProduct p3 = ApiProduct.builder().id("id-3").name("P3").environmentId(ENV_ID).build();
        when(apiProductQueryService.findByEnvironmentIdAndIdIn(eq(ENV_ID), eq(Set.of("id-3", "id-4")))).thenReturn(
            Set.of(p3, ApiProduct.builder().id("id-4").name("P4").environmentId(ENV_ID).build())
        );
        when(apiProductPrimaryOwnerDomainService.getApiProductPrimaryOwner(eq(ORG_ID), any())).thenReturn(null);
        var queryBuilder = QueryBuilder.create(IndexableApiProduct.class);
        var pageable = new PageableImpl(2, 2); // page 2, size 2 -> ids at index 2,3 = id-3, id-4

        Page<ApiProduct> result = service.search(executionContext, queryBuilder, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo("id-3");
        assertThat(result.getContent().get(1).getId()).isEqualTo("id-4");
        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
    }

    @Test
    void should_throw_pagination_invalid_exception_when_page_number_less_than_one() {
        ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);
        when(searchEngineService.search(any(), any())).thenReturn(new SearchResult(List.of("id-1"), 1));
        var queryBuilder = QueryBuilder.create(IndexableApiProduct.class);
        // Use a pageable that returns 0 (PageableImpl normalizes 0 to 1, so we use a custom one to test the guard)
        Pageable pageable = new Pageable() {
            @Override
            public int getPageNumber() {
                return 0;
            }

            @Override
            public int getPageSize() {
                return 10;
            }
        };

        assertThatThrownBy(() -> service.search(executionContext, queryBuilder, pageable)).isInstanceOf(PaginationInvalidException.class);
    }

    @Test
    void should_throw_pagination_invalid_exception_when_start_index_exceeds_total() {
        ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);
        when(searchEngineService.search(any(), any())).thenReturn(new SearchResult(List.of("id-1", "id-2"), 2));
        var queryBuilder = QueryBuilder.create(IndexableApiProduct.class);
        var pageable = new PageableImpl(5, 10); // page 5, only 2 total

        assertThatThrownBy(() -> service.search(executionContext, queryBuilder, pageable)).isInstanceOf(PaginationInvalidException.class);
    }

    @Test
    void should_swallow_primary_owner_not_found_and_return_product_without_owner() {
        ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);
        when(searchEngineService.search(any(), any())).thenReturn(new SearchResult(List.of("id-1"), 1));
        ApiProduct apiProduct = ApiProduct.builder().id("id-1").name("P1").environmentId(ENV_ID).build();
        when(apiProductQueryService.findByEnvironmentIdAndIdIn(eq(ENV_ID), eq(Set.of("id-1")))).thenReturn(Set.of(apiProduct));
        when(apiProductPrimaryOwnerDomainService.getApiProductPrimaryOwner(ORG_ID, "id-1")).thenThrow(
            new ApiProductPrimaryOwnerNotFoundException("id-1")
        );

        Page<ApiProduct> result = service.search(executionContext, QueryBuilder.create(IndexableApiProduct.class), new PageableImpl(1, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPrimaryOwner()).isNull();
        assertThat(result.getContent().get(0).getId()).isEqualTo("id-1");
    }

    @Test
    void should_add_primary_owner_when_found() {
        ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);
        when(searchEngineService.search(any(), any())).thenReturn(new SearchResult(List.of("id-1"), 1));
        ApiProduct apiProduct = ApiProduct.builder().id("id-1").name("P1").environmentId(ENV_ID).build();
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity("u1", "o@e.io", "Owner", PrimaryOwnerEntity.Type.USER);
        when(apiProductQueryService.findByEnvironmentIdAndIdIn(eq(ENV_ID), eq(Set.of("id-1")))).thenReturn(Set.of(apiProduct));
        when(apiProductPrimaryOwnerDomainService.getApiProductPrimaryOwner(ORG_ID, "id-1")).thenReturn(owner);

        Page<ApiProduct> result = service.search(executionContext, QueryBuilder.create(IndexableApiProduct.class), new PageableImpl(1, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPrimaryOwner()).isEqualTo(owner);
    }
}
