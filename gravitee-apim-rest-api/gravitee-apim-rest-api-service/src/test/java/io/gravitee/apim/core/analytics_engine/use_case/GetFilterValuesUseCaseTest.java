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
package io.gravitee.apim.core.analytics_engine.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoader;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterValue;
import io.gravitee.apim.core.analytics_engine.model.FilterValuesPage;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.analytics_engine.query_service.FilterValuesQueryService;
import io.gravitee.apim.core.application.query_service.ApplicationQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.FilterType;
import io.gravitee.apim.core.observability.model.NumberRange;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetFilterValuesUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId("org-id").environmentId("env-id").build();

    private static final AnalyticsQueryContext ANALYTICS_CONTEXT = new AnalyticsQueryContext(
        AUDIT_INFO,
        new ExecutionContext("org-id", "env-id"),
        Set.of(),
        Map.of(),
        Map.of(),
        Map.of()
    );

    @Mock
    private AnalyticsDefinitionQueryService definitionQueryService;

    @Mock
    private FilterValuesQueryService filterValuesQueryService;

    @Mock
    private FilterValueNameResolver filterValueNameResolver;

    @Mock
    private AnalyticsQueryContextLoader contextLoader;

    @Mock
    private ApplicationQueryService applicationQueryService;

    @Mock
    private PlanQueryService planQueryService;

    private GetFilterValuesUseCase useCase;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(contextLoader.load(any())).thenReturn(ANALYTICS_CONTEXT);
        useCase = new GetFilterValuesUseCase(
            definitionQueryService,
            filterValuesQueryService,
            filterValueNameResolver,
            contextLoader,
            applicationQueryService,
            planQueryService
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void should_reject_invalid_filter_name() {
        assertThatThrownBy(() -> useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "INVALID_FILTER", null, null, 1, 10, null)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("Invalid filter name");
        verifyNoInteractions(contextLoader);
    }

    @Test
    void should_reject_page_below_1() {
        assertThatThrownBy(() -> useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 0, 10, null)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("page must be between 1 and 10000");
        verifyNoInteractions(contextLoader);
    }

    @Test
    void should_reject_page_above_10000() {
        assertThatThrownBy(() -> useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 10001, 10, null)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("page must be between 1 and 10000");
        verifyNoInteractions(contextLoader);
    }

    @Test
    void should_reject_per_page_below_1() {
        assertThatThrownBy(() -> useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 1, 0, null)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("perPage must be between 1 and 100");
        verifyNoInteractions(contextLoader);
    }

    @Test
    void should_reject_per_page_above_100() {
        assertThatThrownBy(() -> useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 1, 101, null)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("perPage must be between 1 and 100");
        verifyNoInteractions(contextLoader);
    }

    @Test
    void should_reject_negative_per_page() {
        assertThatThrownBy(() -> useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 1, -5, null)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("perPage must be between 1 and 100");
        verifyNoInteractions(contextLoader);
    }

    @Nested
    class EnumFilters {

        @BeforeEach
        void setUp() {
            when(definitionQueryService.getAllFilters()).thenReturn(
                List.of(
                    new FilterSpec(
                        FilterSpec.Name.HTTP_METHOD,
                        "HTTP Method",
                        FilterType.ENUM,
                        List.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"),
                        null,
                        List.of(FilterOperator.EQ, FilterOperator.IN),
                        null
                    )
                )
            );
        }

        @Test
        void should_return_static_enum_values() {
            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 1, 10, null));

            assertThat(output.valuesPage().data())
                .extracting(FilterValue::value)
                .containsExactly("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(7L);
            verifyNoInteractions(filterValuesQueryService);
        }

        @Test
        void should_paginate_enum_values() {
            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 1, 3, null));

            assertThat(output.valuesPage().data()).extracting(FilterValue::value).containsExactly("GET", "POST", "PUT");
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(7L);
        }

        @Test
        void should_paginate_enum_values_second_page() {
            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 2, 3, null));

            assertThat(output.valuesPage().data()).extracting(FilterValue::value).containsExactly("DELETE", "PATCH", "HEAD");
        }

        @Test
        void should_return_empty_page_when_page_exceeds_total() {
            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 5, 10, null));

            assertThat(output.valuesPage().data()).isEmpty();
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(7L);
        }

        @Test
        void should_filter_enum_values_by_query() {
            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 1, 10, "et"));

            assertThat(output.valuesPage().data()).extracting(FilterValue::value).containsExactly("GET", "DELETE");
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(2L);
        }

        @Test
        void should_filter_enum_values_by_query_case_insensitive() {
            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 1, 10, "PO"));

            assertThat(output.valuesPage().data()).extracting(FilterValue::value).containsExactly("POST");
        }

        @Test
        void should_return_all_enum_values_when_query_is_blank() {
            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 1, 10, "   "));

            assertThat(output.valuesPage().data())
                .extracting(FilterValue::value)
                .containsExactly("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
        }

        @Test
        void should_paginate_enum_values_after_query_filtering() {
            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 1, 1, "et"));

            assertThat(output.valuesPage().data()).extracting(FilterValue::value).containsExactly("GET");

            var output2 = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 2, 1, "et"));

            assertThat(output2.valuesPage().data()).extracting(FilterValue::value).containsExactly("DELETE");
        }

        @Test
        void should_return_no_id_for_enum_values() {
            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_METHOD", null, null, 1, 10, null));

            assertThat(output.valuesPage().data()).allSatisfy(v -> assertThat(v.id()).isNull());
        }
    }

    @Nested
    class DirectValueKeywordFilters {

        @BeforeEach
        void setUp() {
            when(definitionQueryService.getAllFilters()).thenReturn(
                List.of(
                    new FilterSpec(
                        FilterSpec.Name.GATEWAY,
                        "Gateway",
                        FilterType.KEYWORD,
                        null,
                        null,
                        List.of(FilterOperator.EQ, FilterOperator.IN),
                        null
                    )
                )
            );
        }

        @Test
        void should_delegate_to_filter_values_query_service() {
            var expectedPage = new FilterValuesPage(List.of(new FilterValue("gw-1"), new FilterValue("gw-2")), null);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.GATEWAY),
                    any(),
                    any(),
                    anyInt(),
                    eq(10),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(expectedPage);

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "GATEWAY", null, null, 1, 10, null));

            assertThat(output.valuesPage().data()).hasSize(2);
            assertThat(output.valuesPage().totalFilteredCount()).isNull();
            verify(filterValuesQueryService).searchFilterValues(
                eq("org-id"),
                eq("env-id"),
                eq(FilterSpec.Name.GATEWAY),
                eq(null),
                eq(null),
                eq(1),
                eq(10),
                eq(null),
                eq(null),
                eq(Set.of())
            );
        }

        @Test
        void should_pass_search_pattern_to_query_service_for_direct_value() {
            when(
                filterValuesQueryService.searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(10), any(), any(), any())
            ).thenReturn(new FilterValuesPage(Collections.emptyList(), null));

            useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "GATEWAY", null, null, 1, 10, "gw-"));

            verify(filterValuesQueryService).searchFilterValues(
                eq("org-id"),
                eq("env-id"),
                eq(FilterSpec.Name.GATEWAY),
                eq(null),
                eq(null),
                eq(1),
                eq(10),
                eq(null),
                eq("gw-"),
                eq(Set.of())
            );
        }

        @Test
        void should_pass_both_time_range_and_search_pattern() {
            var from = Instant.parse("2025-01-01T00:00:00Z");
            var to = Instant.parse("2025-12-31T23:59:59Z");

            when(
                filterValuesQueryService.searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(10), any(), any(), any())
            ).thenReturn(new FilterValuesPage(List.of(new FilterValue("gw-prod-1")), null));

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "GATEWAY", from, to, 1, 10, "prod"));

            verify(filterValuesQueryService).searchFilterValues(
                eq("org-id"),
                eq("env-id"),
                eq(FilterSpec.Name.GATEWAY),
                eq(from),
                eq(to),
                eq(1),
                eq(10),
                eq(null),
                eq("prod"),
                eq(Set.of())
            );
            assertThat(output.valuesPage().data()).extracting(FilterValue::value).containsExactly("gw-prod-1");
        }

        @Test
        void should_not_resolve_names_for_direct_value_keyword() {
            when(
                filterValuesQueryService.searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(10), any(), any(), any())
            ).thenReturn(new FilterValuesPage(List.of(new FilterValue("gw-1")), null));

            useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "GATEWAY", null, null, 1, 10, null));

            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_pass_page_number_to_query_service() {
            when(
                filterValuesQueryService.searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(5), any(), any(), any())
            ).thenReturn(new FilterValuesPage(List.of(new FilterValue("gw-6")), null));

            useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "GATEWAY", null, null, 2, 5, null));

            verify(filterValuesQueryService).searchFilterValues(
                eq("org-id"),
                eq("env-id"),
                eq(FilterSpec.Name.GATEWAY),
                eq(null),
                eq(null),
                eq(2),
                eq(5),
                eq(null),
                eq(null),
                eq(Set.of())
            );
        }

        @Test
        void should_pass_page_number_with_search_pattern() {
            when(
                filterValuesQueryService.searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(5), any(), any(), any())
            ).thenReturn(new FilterValuesPage(List.of(new FilterValue("gw-prod-6")), null));

            useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "GATEWAY", null, null, 3, 5, "prod"));

            verify(filterValuesQueryService).searchFilterValues(
                eq("org-id"),
                eq("env-id"),
                eq(FilterSpec.Name.GATEWAY),
                eq(null),
                eq(null),
                eq(3),
                eq(5),
                eq(null),
                eq("prod"),
                eq(Set.of())
            );
        }

        @Test
        void should_pass_authorized_api_ids_from_analytics_context_to_query_service() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of("allowed-api-1", "allowed-api-2"),
                    Map.of(),
                    Map.of(),
                    Map.of()
                )
            );
            when(
                filterValuesQueryService.searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(10), any(), any(), any())
            ).thenReturn(new FilterValuesPage(List.of(new FilterValue("gw-1")), null));

            useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "GATEWAY", null, null, 1, 10, null));

            verify(filterValuesQueryService).searchFilterValues(
                eq("org-id"),
                eq("env-id"),
                eq(FilterSpec.Name.GATEWAY),
                eq(null),
                eq(null),
                eq(1),
                eq(10),
                eq(null),
                eq(null),
                eq(Set.of("allowed-api-1", "allowed-api-2"))
            );
        }

        @Test
        void should_load_analytics_context_once_per_request_for_keyword_filter() {
            when(
                filterValuesQueryService.searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(10), any(), any(), any())
            ).thenReturn(new FilterValuesPage(Collections.emptyList(), null));

            useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "GATEWAY", null, null, 1, 10, null));

            verify(contextLoader).load(eq(AUDIT_INFO));
            verifyNoMoreInteractions(contextLoader);
        }
    }

    @Nested
    class IdBasedKeywordFilters {

        @BeforeEach
        void setUp() {
            when(definitionQueryService.getAllFilters()).thenReturn(
                List.of(
                    new FilterSpec(
                        FilterSpec.Name.API,
                        "API",
                        FilterType.KEYWORD,
                        null,
                        null,
                        List.of(FilterOperator.EQ, FilterOperator.IN),
                        null
                    )
                )
            );
        }

        @Test
        void should_resolve_names_for_id_based_filter() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("api-id-1", "My API 1", "api-id-2", "My API 2"),
                    Map.of(),
                    Map.of()
                )
            );
            var esPage = new FilterValuesPage(List.of(new FilterValue("api-id-1"), new FilterValue("api-id-2")), null);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.API),
                    any(),
                    any(),
                    anyInt(),
                    eq(10),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(esPage);

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", null, null, 1, 10, null));

            assertThat(output.valuesPage().data()).satisfiesExactly(
                v -> {
                    assertThat(v.value()).isEqualTo("My API 1");
                    assertThat(v.id()).isEqualTo("api-id-1");
                },
                v -> {
                    assertThat(v.value()).isEqualTo("My API 2");
                    assertThat(v.id()).isEqualTo("api-id-2");
                }
            );
            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_search_by_name_when_query_provided() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("api-id-1", "My API 1", "other-id", "Other"),
                    Map.of(),
                    Map.of()
                )
            );

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", null, null, 1, 10, "My"));

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("My API 1");
                    assertThat(v.id()).isEqualTo("api-id-1");
                });
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(1L);
            verifyNoInteractions(filterValuesQueryService);
            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_pass_time_range_for_id_based_without_query() {
            var from = Instant.parse("2025-01-01T00:00:00Z");
            var to = Instant.parse("2025-12-31T23:59:59Z");

            when(
                filterValuesQueryService.searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(10), any(), any(), any())
            ).thenReturn(new FilterValuesPage(Collections.emptyList(), null));

            useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", from, to, 1, 10, null));

            verify(filterValuesQueryService).searchFilterValues(
                eq("org-id"),
                eq("env-id"),
                eq(FilterSpec.Name.API),
                eq(from),
                eq(to),
                eq(1),
                eq(10),
                eq(null),
                eq(null),
                eq(Set.of())
            );
            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_fallback_to_raw_id_when_name_not_resolved() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("api-id-1", "My API 1"),
                    Map.of(),
                    Map.of()
                )
            );
            var esPage = new FilterValuesPage(List.of(new FilterValue("api-id-1"), new FilterValue("api-id-unknown")), null);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.API),
                    any(),
                    any(),
                    anyInt(),
                    eq(10),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(esPage);

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", null, null, 1, 10, null));

            assertThat(output.valuesPage().data()).satisfiesExactly(
                v -> {
                    assertThat(v.value()).isEqualTo("My API 1");
                    assertThat(v.id()).isEqualTo("api-id-1");
                },
                v -> {
                    assertThat(v.value()).isEqualTo("api-id-unknown");
                    assertThat(v.id()).isEqualTo("api-id-unknown");
                }
            );
            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_preserve_after_key_from_es_for_id_based_without_query() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("api-id-1", "My API 1", "api-id-2", "My API 2"),
                    Map.of(),
                    Map.of()
                )
            );
            var afterKey = Map.<String, Object>of("value", "api-id-2");
            var esPage = new FilterValuesPage(List.of(new FilterValue("api-id-1"), new FilterValue("api-id-2")), afterKey);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.API),
                    any(),
                    any(),
                    anyInt(),
                    eq(10),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(esPage);

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", null, null, 1, 10, null));

            assertThat(output.valuesPage().afterKey()).containsEntry("value", "api-id-2");
            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_treat_blank_query_as_no_query_for_id_based() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("api-id-1", "My API 1"),
                    Map.of(),
                    Map.of()
                )
            );
            var esPage = new FilterValuesPage(List.of(new FilterValue("api-id-1")), null);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.API),
                    any(),
                    any(),
                    anyInt(),
                    eq(10),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(esPage);

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", null, null, 1, 10, "   "));

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("My API 1");
                    assertThat(v.id()).isEqualTo("api-id-1");
                });
            verify(filterValuesQueryService).searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(10), any(), any(), any());
            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_not_call_es_when_query_provided_for_id_based() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("match-id", "Matching API"),
                    Map.of(),
                    Map.of()
                )
            );

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", null, null, 1, 10, "Match"));

            assertThat(output.valuesPage().data()).extracting(FilterValue::id).containsExactly("match-id");
            verifyNoInteractions(filterValuesQueryService);
            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_pass_page_number_for_id_based_without_query() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("api-id-3", "My API 3"),
                    Map.of(),
                    Map.of()
                )
            );
            var esPage = new FilterValuesPage(List.of(new FilterValue("api-id-3")), null);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.API),
                    any(),
                    any(),
                    anyInt(),
                    eq(5),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(esPage);

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", null, null, 2, 5, null));

            verify(filterValuesQueryService).searchFilterValues(
                eq("org-id"),
                eq("env-id"),
                eq(FilterSpec.Name.API),
                eq(null),
                eq(null),
                eq(2),
                eq(5),
                eq(null),
                eq(null),
                eq(Set.of())
            );
            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("My API 3");
                    assertThat(v.id()).isEqualTo("api-id-3");
                });
            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_pass_page_number_for_id_based_search_by_name() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("id-a", "My API A", "id-b", "My API B", "id-c", "My API C", "id-d", "My API D", "id-e", "My API E"),
                    Map.of(),
                    Map.of()
                )
            );

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", null, null, 2, 2, "My"));

            assertThat(output.valuesPage().data())
                .hasSize(2)
                .satisfiesExactly(
                    v -> {
                        assertThat(v.value()).isEqualTo("My API C");
                        assertThat(v.id()).isEqualTo("id-c");
                    },
                    v -> {
                        assertThat(v.value()).isEqualTo("My API D");
                        assertThat(v.id()).isEqualTo("id-d");
                    }
                );
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(5L);
            verifyNoInteractions(filterValuesQueryService);
            verifyNoInteractions(filterValueNameResolver);
        }
    }

    @Nested
    class ApplicationIdBasedFilters {

        @BeforeEach
        void setUp() {
            when(definitionQueryService.getAllFilters()).thenReturn(
                List.of(
                    new FilterSpec(
                        FilterSpec.Name.APPLICATION,
                        "Application",
                        FilterType.KEYWORD,
                        null,
                        null,
                        List.of(FilterOperator.EQ, FilterOperator.IN),
                        null
                    )
                )
            );
        }

        @Test
        void should_resolve_names_for_application_filter() {
            var esPage = new FilterValuesPage(List.of(new FilterValue("app-id-1")), null);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.APPLICATION),
                    any(),
                    any(),
                    anyInt(),
                    eq(10),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(esPage);
            when(filterValueNameResolver.resolveNames(any(), eq(FilterSpec.Name.APPLICATION), any())).thenReturn(
                Map.of("app-id-1", "My Application")
            );

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "APPLICATION", null, null, 1, 10, null));

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("My Application");
                    assertThat(v.id()).isEqualTo("app-id-1");
                });
        }

        @Test
        void should_search_application_by_name_when_query_provided() {
            var app = new BaseApplicationEntity();
            app.setId("app-id-1");
            app.setName("My App 1");
            when(applicationQueryService.findByEnvironment("env-id")).thenReturn(Set.of(app));

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "APPLICATION", null, null, 1, 10, "My App"));

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("My App 1");
                    assertThat(v.id()).isEqualTo("app-id-1");
                });
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(1L);
            verifyNoInteractions(filterValuesQueryService);
            verifyNoInteractions(filterValueNameResolver);
            verify(applicationQueryService).findByEnvironment("env-id");
        }

        @Test
        void should_paginate_application_search_by_name() {
            var app1 = new BaseApplicationEntity();
            app1.setId("a-1");
            app1.setName("My App A");
            var app2 = new BaseApplicationEntity();
            app2.setId("a-2");
            app2.setName("My App B");
            var app3 = new BaseApplicationEntity();
            app3.setId("a-3");
            app3.setName("My App C");
            when(applicationQueryService.findByEnvironment("env-id")).thenReturn(Set.of(app1, app2, app3));

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "APPLICATION", null, null, 2, 2, "My App"));

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("My App C");
                    assertThat(v.id()).isEqualTo("a-3");
                });
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(3L);
        }

        @Test
        void should_pass_authorized_api_ids_for_application_id_based_without_query() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of("scoped-api-1", "scoped-api-2"),
                    Map.of(),
                    Map.of(),
                    Map.of()
                )
            );
            var esPage = new FilterValuesPage(List.of(new FilterValue("app-id-1")), null);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.APPLICATION),
                    any(),
                    any(),
                    anyInt(),
                    eq(10),
                    any(),
                    any(),
                    eq(Set.of("scoped-api-1", "scoped-api-2"))
                )
            ).thenReturn(esPage);
            when(filterValueNameResolver.resolveNames(any(), eq(FilterSpec.Name.APPLICATION), any())).thenReturn(
                Map.of("app-id-1", "My Application")
            );

            useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "APPLICATION", null, null, 1, 10, null));

            verify(filterValuesQueryService).searchFilterValues(
                eq("org-id"),
                eq("env-id"),
                eq(FilterSpec.Name.APPLICATION),
                eq(null),
                eq(null),
                eq(1),
                eq(10),
                eq(null),
                eq(null),
                eq(Set.of("scoped-api-1", "scoped-api-2"))
            );
        }
    }

    @Nested
    class PlanIdBasedFilters {

        @BeforeEach
        void setUp() {
            when(definitionQueryService.getAllFilters()).thenReturn(
                List.of(
                    new FilterSpec(
                        FilterSpec.Name.PLAN,
                        "Plan",
                        FilterType.KEYWORD,
                        null,
                        null,
                        List.of(FilterOperator.EQ, FilterOperator.IN),
                        null
                    )
                )
            );
        }

        @Test
        void should_resolve_names_for_plan_filter() {
            var esPage = new FilterValuesPage(List.of(new FilterValue("plan-id-1")), null);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.PLAN),
                    any(),
                    any(),
                    anyInt(),
                    eq(10),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(esPage);
            when(filterValueNameResolver.resolveNames(any(), eq(FilterSpec.Name.PLAN), any())).thenReturn(Map.of("plan-id-1", "Gold Plan"));

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "PLAN", null, null, 1, 10, null));

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("Gold Plan");
                    assertThat(v.id()).isEqualTo("plan-id-1");
                });
        }

        @Test
        void should_search_plan_by_name_when_query_provided() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of("api-1"),
                    Map.of(),
                    Map.of(),
                    Map.of()
                )
            );
            when(planQueryService.findAllByApiIds(eq(Set.of("api-1")), eq(Set.of("env-id")))).thenReturn(
                List.of(Plan.builder().id("plan-id-1").name("Gold Plan").build())
            );

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "PLAN", null, null, 1, 10, "Gold"));

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("Gold Plan");
                    assertThat(v.id()).isEqualTo("plan-id-1");
                });
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(1L);
            verifyNoInteractions(filterValuesQueryService);
            verifyNoInteractions(filterValueNameResolver);
            verify(planQueryService).findAllByApiIds(eq(Set.of("api-1")), eq(Set.of("env-id")));
        }

        @Test
        void should_paginate_plan_search_by_name() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of("api-1"),
                    Map.of(),
                    Map.of(),
                    Map.of()
                )
            );
            when(planQueryService.findAllByApiIds(eq(Set.of("api-1")), eq(Set.of("env-id")))).thenReturn(
                List.of(
                    Plan.builder().id("p-1").name("Gold A").build(),
                    Plan.builder().id("p-2").name("Gold B").build(),
                    Plan.builder().id("p-3").name("Gold C").build()
                )
            );

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "PLAN", null, null, 2, 2, "Gold"));

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("Gold C");
                    assertThat(v.id()).isEqualTo("p-3");
                });
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(3L);
        }

        @Test
        void should_return_empty_when_plan_search_by_name_and_no_authorized_apis() {
            when(contextLoader.load(any())).thenReturn(ANALYTICS_CONTEXT);

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "PLAN", null, null, 1, 10, "Gold"));

            assertThat(output.valuesPage().data()).isEmpty();
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(0L);
            verifyNoInteractions(filterValuesQueryService);
            verifyNoInteractions(planQueryService);
        }

        @Test
        void should_pass_authorized_api_ids_for_plan_id_based_without_query() {
            when(contextLoader.load(any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of("plan-scope-a", "plan-scope-b"),
                    Map.of(),
                    Map.of(),
                    Map.of()
                )
            );
            var esPage = new FilterValuesPage(List.of(new FilterValue("plan-id-1")), null);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.PLAN),
                    any(),
                    any(),
                    anyInt(),
                    eq(10),
                    any(),
                    any(),
                    eq(Set.of("plan-scope-a", "plan-scope-b"))
                )
            ).thenReturn(esPage);
            when(filterValueNameResolver.resolveNames(any(), eq(FilterSpec.Name.PLAN), any())).thenReturn(Map.of("plan-id-1", "Gold Plan"));

            useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "PLAN", null, null, 1, 10, null));

            verify(filterValuesQueryService).searchFilterValues(
                eq("org-id"),
                eq("env-id"),
                eq(FilterSpec.Name.PLAN),
                eq(null),
                eq(null),
                eq(1),
                eq(10),
                eq(null),
                eq(null),
                eq(Set.of("plan-scope-a", "plan-scope-b"))
            );
        }
    }

    @Nested
    class UnsupportedFilterTypes {

        @BeforeEach
        void setUp() {
            when(definitionQueryService.getAllFilters()).thenReturn(
                List.of(
                    new FilterSpec(
                        FilterSpec.Name.HTTP_STATUS,
                        "Status Code",
                        FilterType.NUMBER,
                        null,
                        new NumberRange(100, 599),
                        List.of(FilterOperator.EQ, FilterOperator.LTE, FilterOperator.GTE),
                        null
                    ),
                    new FilterSpec(FilterSpec.Name.HTTP_PATH, "Path", FilterType.STRING, null, null, List.of(FilterOperator.EQ), null)
                )
            );
        }

        @Test
        void should_reject_number_filter() {
            assertThatThrownBy(() -> useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_STATUS", null, null, 1, 10, null)))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("does not support value listing");
        }

        @Test
        void should_reject_string_filter() {
            assertThatThrownBy(() -> useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "HTTP_PATH", null, null, 1, 10, null)))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("does not support value listing");
        }
    }
}
