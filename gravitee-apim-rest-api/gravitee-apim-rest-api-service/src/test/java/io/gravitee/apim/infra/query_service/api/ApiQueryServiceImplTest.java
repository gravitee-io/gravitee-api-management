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
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.EnvironmentCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.LifecycleState;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiQueryServiceImplTest {

    ApiRepository apiRepository;
    EnvironmentCrudServiceInMemory environmentCrudService;
    ApiQueryService service;

    @BeforeEach
    void setUp() {
        apiRepository = mock(ApiRepository.class);
        environmentCrudService = new EnvironmentCrudServiceInMemory();
        service = new ApiQueryServiceImpl(apiRepository, environmentCrudService);
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

    @Test
    void should_find_all_started_apis_for_an_organization() {
        var api = anApi();
        givenEnvironments(
            List.of(
                Environment.builder().organizationId("org-id").id("env-1").build(),
                Environment.builder().organizationId("org-id").id("env-2").build()
            )
        );
        when(
            apiRepository.search(
                eq(new ApiCriteria.Builder().environments(List.of("env-1", "env-2")).state(LifecycleState.STARTED).build()),
                any(),
                any()
            )
        )
            .thenReturn(ApiAdapter.INSTANCE.toRepositoryStream(Stream.of(api)));

        var res = service.findAllStartedApisByOrganization("org-id");
        assertThat(res).hasSize(1).containsExactly(api);
    }

    private void givenMatchingApis(Stream<Api> apis) {
        when(apiRepository.search(any(), any(), any())).thenReturn(ApiAdapter.INSTANCE.toRepositoryStream(apis));
    }

    private void givenEnvironments(List<Environment> environments) {
        environmentCrudService.initWith(environments);
    }

    private Api anApi() {
        return ApiFixtures.aProxyApiV4();
    }
}
