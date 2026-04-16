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
package io.gravitee.apim.infra.domain_service.analytics_engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterValue;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FilterValueNameResolverImplTest {

    private static final String ENVIRONMENT_ID = "env-id";

    @Mock
    private ApiCrudService apiCrudService;

    @Mock
    private ApiQueryService apiQueryService;

    @Mock
    private ApplicationCrudService applicationCrudService;

    @Mock
    private PlanCrudService planCrudService;

    private FilterValueNameResolverImpl resolver;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        resolver = new FilterValueNameResolverImpl(apiCrudService, apiQueryService, applicationCrudService, planCrudService);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    class ResolveNames {

        @Test
        void should_return_empty_map_for_null_ids() {
            var result = resolver.resolveNames(ENVIRONMENT_ID, FilterSpec.Name.API, null);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_map_for_empty_ids() {
            var result = resolver.resolveNames(ENVIRONMENT_ID, FilterSpec.Name.API, Collections.emptyList());

            assertThat(result).isEmpty();
        }

        @Test
        void should_resolve_api_names() {
            var ids = List.of("api-1", "api-2");
            when(apiCrudService.findByIds(ids)).thenReturn(
                List.of(Api.builder().id("api-1").name("My API 1").build(), Api.builder().id("api-2").name("My API 2").build())
            );

            var result = resolver.resolveNames(ENVIRONMENT_ID, FilterSpec.Name.API, ids);

            assertThat(result).containsEntry("api-1", "My API 1").containsEntry("api-2", "My API 2").hasSize(2);
        }

        @Test
        void should_resolve_application_names_and_include_unknown() {
            var ids = List.of("app-1", "app-2");
            var app1 = new BaseApplicationEntity();
            app1.setId("app-1");
            app1.setName("My App 1");
            var app2 = new BaseApplicationEntity();
            app2.setId("app-2");
            app2.setName("My App 2");

            when(applicationCrudService.findByIds(ids, "env-id")).thenReturn(List.of(app1, app2));

            var result = resolver.resolveNames(ENVIRONMENT_ID, FilterSpec.Name.APPLICATION, ids);

            assertThat(result)
                .containsEntry("app-1", "My App 1")
                .containsEntry("app-2", "My App 2")
                .containsEntry("1", "Unknown")
                .hasSize(3);
        }

        @Test
        void should_resolve_plan_names() {
            var ids = List.of("plan-1", "plan-2");
            when(planCrudService.findByIds(ids)).thenReturn(
                List.of(Plan.builder().id("plan-1").name("Gold Plan").build(), Plan.builder().id("plan-2").name("Silver Plan").build())
            );

            var result = resolver.resolveNames(ENVIRONMENT_ID, FilterSpec.Name.PLAN, ids);

            assertThat(result).containsEntry("plan-1", "Gold Plan").containsEntry("plan-2", "Silver Plan").hasSize(2);
        }

        @Test
        void should_return_empty_map_for_unsupported_filter() {
            var result = resolver.resolveNames(ENVIRONMENT_ID, FilterSpec.Name.GATEWAY, List.of("gw-1"));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SearchByName {

        @Test
        void should_return_empty_list_for_null_query() {
            var result = resolver.searchByName(ENVIRONMENT_ID, FilterSpec.Name.API, null, 1, 10);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_list_for_blank_query() {
            var result = resolver.searchByName(ENVIRONMENT_ID, FilterSpec.Name.API, "   ", 1, 10);

            assertThat(result).isEmpty();
        }

        @Test
        void should_search_apis_by_name() {
            var api1 = Api.builder().id("api-1").name("My API Alpha").build();
            var api2 = Api.builder().id("api-2").name("My API Beta").build();

            when(apiQueryService.search(any(ApiSearchCriteria.class), eq(null), any(ApiFieldFilter.class))).thenReturn(
                Stream.of(api1, api2)
            );

            var result = resolver.searchByName(ENVIRONMENT_ID, FilterSpec.Name.API, "My API", 1, 10);

            assertThat(result)
                .hasSize(2)
                .satisfiesExactly(
                    v -> {
                        assertThat(v.value()).isEqualTo("My API Alpha");
                        assertThat(v.id()).isEqualTo("api-1");
                    },
                    v -> {
                        assertThat(v.value()).isEqualTo("My API Beta");
                        assertThat(v.id()).isEqualTo("api-2");
                    }
                );
        }

        @Test
        void should_limit_api_search_results_to_per_page() {
            var apis = Stream.of(
                Api.builder().id("api-1").name("API 1").build(),
                Api.builder().id("api-2").name("API 2").build(),
                Api.builder().id("api-3").name("API 3").build()
            );
            when(apiQueryService.search(any(ApiSearchCriteria.class), eq(null), any(ApiFieldFilter.class))).thenReturn(apis);

            var result = resolver.searchByName(ENVIRONMENT_ID, FilterSpec.Name.API, "API", 1, 2);

            assertThat(result).hasSize(2);
        }

        @Test
        void should_skip_results_for_second_page_of_api_search() {
            var apis = Stream.of(
                Api.builder().id("api-1").name("API 1").build(),
                Api.builder().id("api-2").name("API 2").build(),
                Api.builder().id("api-3").name("API 3").build()
            );
            when(apiQueryService.search(any(ApiSearchCriteria.class), eq(null), any(ApiFieldFilter.class))).thenReturn(apis);

            var result = resolver.searchByName(ENVIRONMENT_ID, FilterSpec.Name.API, "API", 2, 2);

            assertThat(result)
                .hasSize(1)
                .satisfiesExactly(v -> {
                    assertThat(v.value()).isEqualTo("API 3");
                    assertThat(v.id()).isEqualTo("api-3");
                });
        }

        @Test
        void should_return_empty_list_for_unsupported_filter_search() {
            var result = resolver.searchByName(ENVIRONMENT_ID, FilterSpec.Name.APPLICATION, "My App", 1, 10);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_list_when_no_apis_match() {
            when(apiQueryService.search(any(ApiSearchCriteria.class), eq(null), any(ApiFieldFilter.class))).thenReturn(Stream.empty());

            var result = resolver.searchByName(ENVIRONMENT_ID, FilterSpec.Name.API, "nonexistent", 1, 10);

            assertThat(result).isEmpty();
        }
    }
}
