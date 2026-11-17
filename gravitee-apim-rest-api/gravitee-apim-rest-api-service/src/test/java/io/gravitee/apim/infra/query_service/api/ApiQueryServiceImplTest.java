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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiQueryServiceImplTest {

    ApiRepository apiRepository;
    ApiQueryService service;

    @BeforeEach
    void setUp() {
        apiRepository = mock(ApiRepository.class);
        service = new ApiQueryServiceImpl(apiRepository);
    }

    @Test
    void search_should_return_matching_api_entities() {
        Api api = anApi();
        givenMatchingApis(Stream.of(api));

        var res = service
            .search(ApiSearchCriteria.builder().build(), Sortable.builder().build(), ApiFieldFilter.builder().build())
            .toList();
        assertThat(res).hasSize(1).containsExactly(api);
    }

    private void givenMatchingApis(Stream<Api> apis) {
        when(apiRepository.search(any(), any(), any())).thenReturn(ApiAdapter.INSTANCE.toRepositoryStream(apis));
    }

    private Api anApi() {
        return ApiFixtures.aProxyApiV4();
    }

    @Test
    void should_list_apis_matching_integration_id() {
        //Given
        var integrationId = "integration-id";
        var pageable = new PageableImpl(1, 5);

        var expectedApis = List.of(fixtures.repository.ApiFixtures.aFederatedApi());
        var page = new Page<>(expectedApis, pageable.getPageNumber(), expectedApis.size(), expectedApis.size());
        when(apiRepository.search(any(), any(), any(), any())).thenReturn(page);

        //When
        Page<Api> responsePage = service.findByIntegrationId(integrationId, pageable);

        //Then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(responsePage).isNotNull();
            softly.assertThat(responsePage.getPageNumber()).isEqualTo(1);
            softly.assertThat(responsePage.getPageElements()).isEqualTo(1);
            softly.assertThat(responsePage.getTotalElements()).isEqualTo(1);
            softly.assertThat(responsePage.getContent().get(0).getId()).isEqualTo("api-id");
        });
    }

    @Test
    void should_find_existing_api_ids_in_the_environment_without_loading_api_models() {
        var requestedIds = Set.of("existing-api", "missing-api", "other-environment-api");
        when(apiRepository.searchIds(anyList(), any(), isNull())).thenReturn(new Page<>(List.of("existing-api"), 0, 1, 1));

        var existingIds = service.findExistingIds(requestedIds, "environment-id");

        assertThat(existingIds).containsExactly("existing-api");
        verify(apiRepository).searchIds(
            argThat(
                criteria ->
                    criteria.size() == 1 &&
                    "environment-id".equals(criteria.getFirst().getEnvironmentId()) &&
                    requestedIds.equals(Set.copyOf(criteria.getFirst().getIds()))
            ),
            argThat(pageable -> pageable.pageNumber() == 0 && pageable.pageSize() == requestedIds.size()),
            isNull()
        );
        verifyNoMoreInteractions(apiRepository);
    }

    @Test
    void should_not_query_the_repository_for_an_empty_set_of_api_ids() {
        assertThat(service.findExistingIds(Set.of(), "environment-id")).isEmpty();

        verifyNoInteractions(apiRepository);
    }

    @Test
    void should_require_an_environment_before_looking_up_api_ids() {
        assertThatNullPointerException().isThrownBy(() -> service.findExistingIds(Set.of("api-id"), null));

        verifyNoInteractions(apiRepository);
    }

    @Test
    void should_find_all_existing_api_ids_in_bounded_queries_without_loading_api_models() {
        var requestedIds = IntStream.range(0, 1001)
            .mapToObj(id -> "api-" + id)
            .collect(Collectors.toSet());
        var queriedBatches = new ArrayList<Set<String>>();
        when(apiRepository.searchIds(anyList(), any(), isNull())).thenAnswer(invocation -> {
            List<ApiCriteria> criteria = invocation.getArgument(0);
            Pageable pageable = invocation.getArgument(1);
            assertThat(criteria)
                .singleElement()
                .satisfies(batch -> {
                    assertThat(batch.getEnvironmentId()).isEqualTo("environment-id");
                    assertThat(batch.getIds()).hasSizeLessThanOrEqualTo(500).isNotEmpty();
                    assertThat(pageable.pageNumber()).isZero();
                    assertThat(pageable.pageSize()).isEqualTo(batch.getIds().size());
                    queriedBatches.add(Set.copyOf(batch.getIds()));
                });
            var batchIds = List.copyOf(criteria.getFirst().getIds());
            return new Page<>(batchIds, 0, batchIds.size(), batchIds.size());
        });

        var existingIds = service.findExistingIds(requestedIds, "environment-id");

        assertThat(existingIds).isEqualTo(requestedIds);
        assertThat(queriedBatches).hasSize(3);
        assertThat(queriedBatches.stream().mapToInt(Set::size).sum()).isEqualTo(requestedIds.size());
        verify(apiRepository, times(3)).searchIds(anyList(), any(), isNull());
        verifyNoMoreInteractions(apiRepository);
    }
}
