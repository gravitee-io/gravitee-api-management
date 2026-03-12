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
package io.gravitee.apim.infra.query_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApiPortalSearchQueryServiceImplTest {

    ApiSearchService apiSearchService;
    ApiQueryService apiQueryService;
    ApiPortalSearchQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        apiSearchService = mock(ApiSearchService.class);
        apiQueryService = mock(ApiQueryService.class);
        service = new ApiPortalSearchQueryServiceImpl(apiSearchService, apiQueryService);
    }

    @Nested
    class WithoutQuery {

        @Test
        void should_return_first_page_of_allowed_apis() {
            var api1 = anApi("api-1");
            var api2 = anApi("api-2");
            var api3 = anApi("api-3");
            givenApis(List.of(api1, api2, api3));

            var result = service.search("env", "org", null, Set.of("api-1", "api-2", "api-3"), new PageableImpl(1, 2), null);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        void should_return_second_page_of_allowed_apis() {
            var api1 = anApi("api-1");
            var api2 = anApi("api-2");
            var api3 = anApi("api-3");
            givenApis(List.of(api1, api2, api3));

            var result = service.search("env", "org", null, Set.of("api-1", "api-2", "api-3"), new PageableImpl(2, 2), null);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        void should_return_empty_page_when_page_exceeds_total() {
            givenApis(List.of(anApi("api-1")));

            var result = service.search("env", "org", null, Set.of("api-1"), new PageableImpl(5, 10), null);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void should_not_call_lucene_search_when_no_query() {
            givenApis(List.of(anApi("api-1")));

            service.search("env", "org", null, Set.of("api-1"), new PageableImpl(1, 10), null);

            verify(apiSearchService, never()).searchIds(any(), any(), any(), any());
        }

        @Test
        void should_not_call_lucene_search_for_blank_query() {
            givenApis(List.of(anApi("api-1")));

            service.search("env", "org", "  ", Set.of("api-1"), new PageableImpl(1, 10), null);

            verify(apiSearchService, never()).searchIds(any(), any(), any(), any());
        }

        @Test
        void should_return_empty_page_when_allowed_ids_is_empty() {
            var result = service.search("env", "org", null, Set.of(), new PageableImpl(1, 10), null);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            verify(apiQueryService, never()).search(any(), any(), any(), any());
        }

        @Test
        void should_pass_allowed_ids_to_api_query_service() {
            var api1 = anApi("api-1");
            var api2 = anApi("api-2");
            givenApis(List.of(api1, api2));

            service.search("env", "org", null, Set.of("api-1", "api-2"), new PageableImpl(1, 10), null);

            var captor = ArgumentCaptor.forClass(ApiSearchCriteria.class);
            verify(apiQueryService).search(captor.capture(), any(), any(), any());
            assertThat(captor.getValue().getIds()).containsExactlyInAnyOrder("api-1", "api-2");
            assertThat(captor.getValue().getEnvironmentId()).isEqualTo("env");
        }
    }

    @Nested
    class WithQuery {

        @Test
        void should_intersect_lucene_results_with_allowed_ids() {
            // lucene returns api-1 and api-3; only api-1 and api-2 are allowed
            when(apiSearchService.searchIds(any(), eq("search-term"), any(), any())).thenReturn(List.of("api-1", "api-3"));
            givenApis(List.of(anApi("api-1")));

            var result = service.search("env", "org", "search-term", Set.of("api-1", "api-2"), new PageableImpl(1, 10), null);

            assertThat(result.getContent()).extracting(Api::getId).containsExactly("api-1");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void should_return_empty_page_when_no_lucene_results_match_allowed_ids() {
            when(apiSearchService.searchIds(any(), any(), any(), any())).thenReturn(List.of("api-3", "api-4"));

            var result = service.search("env", "org", "term", Set.of("api-1", "api-2"), new PageableImpl(1, 10), null);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            verify(apiQueryService, never()).search(any(), any(), any(), any());
        }

        @Test
        void should_use_correct_execution_context_for_lucene_search() {
            when(apiSearchService.searchIds(any(), any(), any(), any())).thenReturn(List.of());

            service.search("my-env", "my-org", "term", Set.of("api-1"), new PageableImpl(1, 10), null);

            var captor = ArgumentCaptor.forClass(ExecutionContext.class);
            verify(apiSearchService).searchIds(captor.capture(), any(), any(), any());
            assertThat(captor.getValue().getEnvironmentId()).isEqualTo("my-env");
            assertThat(captor.getValue().getOrganizationId()).isEqualTo("my-org");
        }

        @Test
        void should_trim_query_before_passing_to_lucene() {
            when(apiSearchService.searchIds(any(), any(), any(), any())).thenReturn(List.of());

            service.search("env", "org", "  trimmed  ", Set.of("api-1"), new PageableImpl(1, 10), null);

            verify(apiSearchService).searchIds(any(), eq("trimmed"), any(), any());
        }

        @Test
        void should_paginate_lucene_results() {
            when(apiSearchService.searchIds(any(), any(), any(), any())).thenReturn(List.of("api-1", "api-2", "api-3"));
            givenApis(List.of(anApi("api-1"), anApi("api-2")));

            var result = service.search("env", "org", "term", Set.of("api-1", "api-2", "api-3"), new PageableImpl(1, 2), null);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
        }
    }

    @Nested
    class WithNullPageable {

        @Test
        void should_default_page_number_to_1_and_page_size_to_10() {
            var apis = List.of(anApi("api-1"), anApi("api-2"));
            givenApis(apis);

            var result = service.search("env", "org", null, Set.of("api-1", "api-2"), null, null);

            assertThat(result.getPageNumber()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(2);
        }
    }

    // --- helpers ---

    private Api anApi(String id) {
        return ApiFixtures.aProxyApiV4().toBuilder().id(id).environmentId("env").build();
    }

    private void givenApis(List<Api> apis) {
        when(apiQueryService.search(any(), any(), any(), any())).thenAnswer(invocation -> {
            ApiSearchCriteria criteria = invocation.getArgument(0);
            io.gravitee.rest.api.model.common.Pageable pageable = invocation.getArgument(2);
            List<String> ids = criteria.getIds();
            List<Api> filtered = (ids == null || ids.isEmpty())
                ? apis
                : apis
                    .stream()
                    .filter(a -> ids.contains(a.getId()))
                    .toList();

            int pageNumber = pageable != null ? pageable.getPageNumber() : 1;
            int pageSize = pageable != null ? pageable.getPageSize() : 10;
            int total = filtered.size();

            if (total == 0 || pageSize <= 0) {
                return new Page<>(List.of(), pageNumber, 0, total);
            }
            int start = (pageNumber - 1) * pageSize;
            if (start >= total) {
                return new Page<>(List.of(), pageNumber, 0, total);
            }
            List<Api> pageContent = filtered.subList(start, Math.min(start + pageSize, total));
            return new Page<>(pageContent, pageNumber, pageContent.size(), total);
        });
    }
}
