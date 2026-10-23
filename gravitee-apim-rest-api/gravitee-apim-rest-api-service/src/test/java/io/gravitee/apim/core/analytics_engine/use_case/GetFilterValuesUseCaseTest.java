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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.FilterSpecFixtures;
import io.gravitee.apim.core.analytics.domain_service.AiWorkspaceAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterValue;
import io.gravitee.apim.core.analytics_engine.model.FilterValuesPage;
import io.gravitee.apim.core.analytics_engine.model.TrafficScope;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.analytics_engine.query_service.FilterValuesQueryService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.application.query_service.ApplicationQueryService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.FilterType;
import io.gravitee.apim.core.observability.model.NumberRange;
import io.gravitee.apim.core.observability.model.Signal;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetFilterValuesUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId("org-id").environmentId("env-id").build();
    private static final AuditInfo AUDIT_INFO_WITH_ACTOR = AuditInfo.builder()
        .organizationId("org-id")
        .environmentId("env-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private static final AnalyticsQueryContext ANALYTICS_CONTEXT = new AnalyticsQueryContext(
        AUDIT_INFO,
        new ExecutionContext("org-id", "env-id"),
        Set.of(),
        Map.of(),
        Map.of(),
        Map.of(),
        Optional.empty()
    );

    @Mock
    private AnalyticsDefinitionQueryService definitionQueryService;

    @Mock
    private FilterValuesQueryService filterValuesQueryService;

    @Mock
    private FilterValueNameResolver filterValueNameResolver;

    @Mock
    private AnalyticsQueryContextLoaderResolver contextLoader;

    @Mock
    private ApplicationQueryService applicationQueryService;

    @Mock
    private PlanQueryService planQueryService;

    @Mock
    private io.gravitee.apim.core.api_product.query_service.ApiProductQueryService apiProductQueryService;

    @Mock
    private AiWorkspaceAnalyticsApisDomainService workspaceApis;

    private GetFilterValuesUseCase useCase;

    @BeforeEach
    void setUp() {
        when(contextLoader.load(any(), any(), any())).thenReturn(ANALYTICS_CONTEXT);
        useCase = new GetFilterValuesUseCase(
            definitionQueryService,
            filterValuesQueryService,
            filterValueNameResolver,
            contextLoader,
            applicationQueryService,
            planQueryService,
            apiProductQueryService,
            workspaceApis
        );
    }

    @Test
    void should_reject_invalid_filter_name() {
        assertThatThrownBy(() -> useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "INVALID_FILTER", null, null, 1, 10, null)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("Invalid filter name");
        verifyNoInteractions(contextLoader);
    }

    @Test
    void should_neither_accept_nor_advertise_a_name_only_the_back_end_builds() {
        assertThatThrownBy(() -> useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "TRAFFIC_SCOPE", null, null, 1, 10, null)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("Invalid filter name")
            .satisfies(e -> assertThat(((ValidationDomainException) e).getParameters().get("validNames")).doesNotContain("TRAFFIC_SCOPE"));
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
                    FilterSpecFixtures.enumeration(
                        FilterSpec.Name.HTTP_METHOD,
                        "HTTP Method",
                        List.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
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
                List.of(FilterSpecFixtures.keyword(FilterSpec.Name.GATEWAY, "Gateway"))
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
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of("allowed-api-1", "allowed-api-2"),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Optional.empty()
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

            verify(contextLoader).load(AUDIT_INFO, AnalyticsScope.MANAGEMENT, ApiDataKind.AGGREGATES);
            verifyNoMoreInteractions(contextLoader);
        }

        /** A field of the logs only (request ids, transaction ids) is per-request data: its values come from the logs rows. */
        @Test
        void should_list_the_values_of_a_logs_only_field_from_the_apis_whose_requests_the_user_reads() {
            when(definitionQueryService.getAllFilters()).thenReturn(
                List.of(FilterSpecFixtures.withSignals(FilterSpecFixtures.keyword(FilterSpec.Name.REQUEST_ID, "Request ID"), Signal.LOGS))
            );
            when(
                filterValuesQueryService.searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(10), any(), any(), any())
            ).thenReturn(new FilterValuesPage(Collections.emptyList(), null));

            useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "REQUEST_ID", null, null, 1, 10, null));

            verify(contextLoader).load(AUDIT_INFO, AnalyticsScope.MANAGEMENT, ApiDataKind.PER_REQUEST);
        }

        /** The values of a field identifying a caller come from the requests the user reads, never the aggregates alone. */
        @Test
        void should_list_the_values_of_an_identifying_field_from_the_apis_whose_requests_the_user_reads() {
            when(definitionQueryService.getAllFilters()).thenReturn(
                List.of(FilterSpecFixtures.keyword(FilterSpec.Name.CONSUMER_IP, "Consumer IP"))
            );
            when(
                filterValuesQueryService.searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(10), any(), any(), any())
            ).thenReturn(new FilterValuesPage(Collections.emptyList(), null));

            useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "CONSUMER_IP", null, null, 1, 10, null));

            verify(contextLoader).load(AUDIT_INFO, AnalyticsScope.MANAGEMENT, ApiDataKind.PER_REQUEST);
        }
    }

    @Nested
    class IdBasedKeywordFilters {

        @BeforeEach
        void setUp() {
            when(definitionQueryService.getAllFilters()).thenReturn(List.of(FilterSpecFixtures.keyword(FilterSpec.Name.API, "API")));
        }

        @Test
        void should_resolve_names_for_id_based_filter() {
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("api-id-1", "My API 1", "api-id-2", "My API 2"),
                    Map.of(),
                    Map.of(),
                    Optional.empty()
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
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("api-id-1", "My API 1", "other-id", "Other"),
                    Map.of(),
                    Map.of(),
                    Optional.empty()
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
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("api-id-1", "My API 1"),
                    Map.of(),
                    Map.of(),
                    Optional.empty()
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
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("api-id-1", "My API 1", "api-id-2", "My API 2"),
                    Map.of(),
                    Map.of(),
                    Optional.empty()
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
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("api-id-1", "My API 1"),
                    Map.of(),
                    Map.of(),
                    Optional.empty()
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
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("match-id", "Matching API"),
                    Map.of(),
                    Map.of(),
                    Optional.empty()
                )
            );

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", null, null, 1, 10, "Match"));

            assertThat(output.valuesPage().data()).extracting(FilterValue::id).containsExactly("match-id");
            verifyNoInteractions(filterValuesQueryService);
            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_pass_page_number_for_id_based_without_query() {
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("api-id-3", "My API 3"),
                    Map.of(),
                    Map.of(),
                    Optional.empty()
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
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of(),
                    Map.of("id-a", "My API A", "id-b", "My API B", "id-c", "My API C", "id-d", "My API D", "id-e", "My API E"),
                    Map.of(),
                    Map.of(),
                    Optional.empty()
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
                List.of(FilterSpecFixtures.keyword(FilterSpec.Name.APPLICATION, "Application"))
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
        void should_search_by_name_the_applications_found_in_the_calls_of_the_readable_apis() {
            givenReadableApis("api-1");
            var app1 = application("app-id-1", "My App 1");
            var app2 = application("app-id-2", "My App 2");
            var other = application("app-id-3", "Other");
            when(applicationQueryService.findByEnvironment("env-id")).thenReturn(Set.of(app1, app2, other));
            var from = Instant.parse("2026-01-01T00:00:00Z");
            var to = Instant.parse("2026-01-02T00:00:00Z");
            when(
                filterValuesQueryService.findValuesInCalls(
                    "org-id",
                    "env-id",
                    FilterSpec.Name.APPLICATION,
                    from,
                    to,
                    Set.of("app-id-1", "app-id-2"),
                    Set.of("api-1")
                )
            ).thenReturn(Set.of("app-id-1"));

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "APPLICATION", from, to, 1, 10, "My App"));

            assertThat(output.valuesPage().data()).containsExactly(new FilterValue("My App 1", "app-id-1"));
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(1L);
            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_find_no_application_by_name_when_no_api_is_readable() {
            when(applicationQueryService.findByEnvironment("env-id")).thenReturn(Set.of(application("app-id-1", "My App 1")));

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "APPLICATION", null, null, 1, 10, "My App"));

            assertThat(output.valuesPage().data()).isEmpty();
            assertThat(output.valuesPage().totalFilteredCount()).isZero();
        }

        @Test
        void should_paginate_application_search_by_name() {
            givenReadableApis("api-1");
            var app1 = application("a-1", "My App A");
            var app2 = application("a-2", "My App B");
            var app3 = application("a-3", "My App C");
            when(applicationQueryService.findByEnvironment("env-id")).thenReturn(Set.of(app1, app2, app3));
            when(
                filterValuesQueryService.findValuesInCalls(any(), any(), eq(FilterSpec.Name.APPLICATION), any(), any(), any(), any())
            ).thenReturn(Set.of("a-1", "a-2", "a-3"));

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
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of("scoped-api-1", "scoped-api-2"),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Optional.empty()
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
            when(definitionQueryService.getAllFilters()).thenReturn(List.of(FilterSpecFixtures.keyword(FilterSpec.Name.PLAN, "Plan")));
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

        /** A plan without calls in the period is not offered: its label could not be resolved from the rows either. */
        @Test
        void should_search_by_name_the_plans_of_the_readable_apis_found_in_their_calls() {
            givenReadableApis("api-1");
            var gold = Plan.builder().id("plan-id-1").name("Gold Plan").build();
            var goldPlus = Plan.builder().id("plan-id-2").name("Gold Plus").build();
            when(planQueryService.findAllByApiIds(eq(Set.of("api-1")), eq(Set.of("env-id")))).thenReturn(List.of(gold, goldPlus));
            when(
                filterValuesQueryService.findValuesInCalls(
                    "org-id",
                    "env-id",
                    FilterSpec.Name.PLAN,
                    null,
                    null,
                    Set.of("plan-id-1", "plan-id-2"),
                    Set.of("api-1")
                )
            ).thenReturn(Set.of("plan-id-1"));
            when(filterValueNameResolver.planLabels("env-id", List.of(gold))).thenReturn(Map.of("plan-id-1", "Gold Plan · Payments API"));

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "PLAN", null, null, 1, 10, "Gold"));

            assertThat(output.valuesPage().data()).containsExactly(new FilterValue("Gold Plan · Payments API", "plan-id-1"));
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(1L);
        }

        @Test
        void should_paginate_plan_search_by_name() {
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of("api-1"),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Optional.empty()
                )
            );
            when(planQueryService.findAllByApiIds(eq(Set.of("api-1")), eq(Set.of("env-id")))).thenReturn(
                List.of(
                    Plan.builder().id("p-1").name("Gold A").build(),
                    Plan.builder().id("p-2").name("Gold B").build(),
                    Plan.builder().id("p-3").name("Gold C").build()
                )
            );
            when(filterValuesQueryService.findValuesInCalls(any(), any(), eq(FilterSpec.Name.PLAN), any(), any(), any(), any())).thenReturn(
                Set.of("p-1", "p-2", "p-3")
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
            when(contextLoader.load(any(), any(), any())).thenReturn(ANALYTICS_CONTEXT);

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "PLAN", null, null, 1, 10, "Gold"));

            assertThat(output.valuesPage().data()).isEmpty();
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(0L);
            verifyNoInteractions(filterValuesQueryService);
            verifyNoInteractions(planQueryService);
        }

        @Test
        void should_pass_authorized_api_ids_for_plan_id_based_without_query() {
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of("plan-scope-a", "plan-scope-b"),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Optional.empty()
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
    class ApiProductIdBasedFilters {

        @BeforeEach
        void setUp() {
            when(definitionQueryService.getAllFilters()).thenReturn(
                List.of(FilterSpecFixtures.keyword(FilterSpec.Name.API_PRODUCT, "API Product"))
            );
        }

        @Test
        void should_resolve_names_via_es_for_api_product_filter_without_query() {
            var esPage = new FilterValuesPage(List.of(new FilterValue("prod-id-1")), null);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.API_PRODUCT),
                    any(),
                    any(),
                    anyInt(),
                    eq(10),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(esPage);
            when(filterValueNameResolver.resolveNames(any(), eq(FilterSpec.Name.API_PRODUCT), any())).thenReturn(
                Map.of("prod-id-1", "My API Product")
            );

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API_PRODUCT", null, null, 1, 10, null));

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("My API Product");
                    assertThat(v.id()).isEqualTo("prod-id-1");
                });
            verifyNoInteractions(apiProductQueryService);
        }

        @Test
        void should_pass_time_range_to_es_for_api_product_without_query() {
            var from = Instant.parse("2025-01-01T00:00:00Z");
            var to = Instant.parse("2025-12-31T23:59:59Z");
            when(
                filterValuesQueryService.searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(10), any(), any(), any())
            ).thenReturn(new FilterValuesPage(List.of(new FilterValue("prod-id-1")), null));
            when(filterValueNameResolver.resolveNames(any(), eq(FilterSpec.Name.API_PRODUCT), any())).thenReturn(
                Map.of("prod-id-1", "My API Product")
            );

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API_PRODUCT", from, to, 1, 10, null));

            verify(filterValuesQueryService).searchFilterValues(
                eq("org-id"),
                eq("env-id"),
                eq(FilterSpec.Name.API_PRODUCT),
                eq(from),
                eq(to),
                eq(1),
                eq(10),
                eq(null),
                eq(null),
                eq(Set.of())
            );
            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("My API Product");
                    assertThat(v.id()).isEqualTo("prod-id-1");
                });
            verifyNoInteractions(apiProductQueryService);
        }

        @Test
        void should_pass_page_number_for_api_product_without_query() {
            var esPage = new FilterValuesPage(List.of(new FilterValue("prod-id-3")), null);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.API_PRODUCT),
                    any(),
                    any(),
                    anyInt(),
                    eq(5),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(esPage);
            when(filterValueNameResolver.resolveNames(any(), eq(FilterSpec.Name.API_PRODUCT), any())).thenReturn(
                Map.of("prod-id-3", "My API Product 3")
            );

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API_PRODUCT", null, null, 2, 5, null));

            verify(filterValuesQueryService).searchFilterValues(
                eq("org-id"),
                eq("env-id"),
                eq(FilterSpec.Name.API_PRODUCT),
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
                    assertThat(v.value()).isEqualTo("My API Product 3");
                    assertThat(v.id()).isEqualTo("prod-id-3");
                });
            verifyNoInteractions(apiProductQueryService);
        }

        @Test
        void should_search_by_name_the_api_products_found_in_the_calls_of_the_readable_apis() {
            givenReadableApis("api-1");
            var prod1 = ApiProduct.builder().id("prod-id-1").name("My Product 1").build();
            var prod2 = ApiProduct.builder().id("prod-id-2").name("My Product 2").build();
            when(apiProductQueryService.findByEnvironmentId("env-id")).thenReturn(Set.of(prod1, prod2));
            when(
                filterValuesQueryService.findValuesInCalls(
                    "org-id",
                    "env-id",
                    FilterSpec.Name.API_PRODUCT,
                    null,
                    null,
                    Set.of("prod-id-1", "prod-id-2"),
                    Set.of("api-1")
                )
            ).thenReturn(Set.of("prod-id-1"));

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API_PRODUCT", null, null, 1, 10, "My Prod"));

            assertThat(output.valuesPage().data()).containsExactly(new FilterValue("My Product 1", "prod-id-1"));
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(1L);
            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_find_no_api_product_by_name_when_no_api_is_readable() {
            when(apiProductQueryService.findByEnvironmentId("env-id")).thenReturn(
                Set.of(ApiProduct.builder().id("prod-id-1").name("My Product 1").build())
            );

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API_PRODUCT", null, null, 1, 10, "My Prod"));

            assertThat(output.valuesPage().data()).isEmpty();
        }

        @Test
        void should_paginate_api_product_search_by_name() {
            givenReadableApis("api-1");
            var prod1 = ApiProduct.builder().id("p-1").name("My Product A").build();
            var prod2 = ApiProduct.builder().id("p-2").name("My Product B").build();
            var prod3 = ApiProduct.builder().id("p-3").name("My Product C").build();
            when(apiProductQueryService.findByEnvironmentId("env-id")).thenReturn(Set.of(prod1, prod2, prod3));
            when(
                filterValuesQueryService.findValuesInCalls(any(), any(), eq(FilterSpec.Name.API_PRODUCT), any(), any(), any(), any())
            ).thenReturn(Set.of("p-1", "p-2", "p-3"));

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API_PRODUCT", null, null, 2, 2, "My Prod"));

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("My Product C");
                    assertThat(v.id()).isEqualTo("p-3");
                });
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(3L);
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "   " })
        void should_treat_empty_or_blank_query_as_no_query_for_api_product(String query) {
            var esPage = new FilterValuesPage(List.of(new FilterValue("prod-id-1")), null);
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    eq(FilterSpec.Name.API_PRODUCT),
                    any(),
                    any(),
                    anyInt(),
                    eq(10),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(esPage);
            when(filterValueNameResolver.resolveNames(any(), eq(FilterSpec.Name.API_PRODUCT), any())).thenReturn(
                Map.of("prod-id-1", "My API Product")
            );

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API_PRODUCT", null, null, 1, 10, query));

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("My API Product");
                    assertThat(v.id()).isEqualTo("prod-id-1");
                });
            verifyNoInteractions(apiProductQueryService);
        }

        @Test
        void should_exclude_api_products_with_null_name_when_searching_by_name() {
            givenReadableApis("api-1");
            var prod1 = ApiProduct.builder().id("prod-id-1").name("My API Product").build();
            var prodNoName = ApiProduct.builder().id("prod-id-2").name(null).build();
            when(apiProductQueryService.findByEnvironmentId("env-id")).thenReturn(Set.of(prod1, prodNoName));
            when(
                filterValuesQueryService.findValuesInCalls(any(), any(), eq(FilterSpec.Name.API_PRODUCT), any(), any(), any(), any())
            ).thenReturn(Set.of("prod-id-1", "prod-id-2"));

            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API_PRODUCT", null, null, 1, 10, "My"));

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> assertThat(v.value()).isEqualTo("My API Product"));
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(1L);
        }
    }

    @Nested
    class ApiTypeConstraints {

        @BeforeEach
        void setUp() {
            when(definitionQueryService.getAllFilters()).thenReturn(
                List.of(
                    FilterSpecFixtures.enumeration(
                        FilterSpec.Name.API_TYPE,
                        "API Type",
                        List.of("HTTP_PROXY", "LLM", "MESSAGE", "MCP", "A2A", "NATIVE", "EDGE")
                    ),
                    FilterSpecFixtures.keyword(FilterSpec.Name.API, "API")
                )
            );
        }

        @Test
        void should_restrict_api_type_enum_values_when_apiTypes_provided() {
            var output = useCase.execute(
                new GetFilterValuesUseCase.Input(
                    AUDIT_INFO,
                    "API_TYPE",
                    null,
                    null,
                    1,
                    10,
                    null,
                    Set.of(ApiType.MCP_PROXY, ApiType.LLM_PROXY)
                )
            );

            assertThat(output.valuesPage().data()).extracting(FilterValue::value).containsExactlyInAnyOrder("MCP", "LLM");
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(2L);
        }

        @Test
        void should_return_all_api_type_enum_values_when_apiTypes_empty() {
            var output = useCase.execute(new GetFilterValuesUseCase.Input(AUDIT_INFO, "API_TYPE", null, null, 1, 10, null, Set.of()));

            assertThat(output.valuesPage().data())
                .extracting(FilterValue::value)
                .containsExactly("HTTP_PROXY", "LLM", "MESSAGE", "MCP", "A2A", "NATIVE", "EDGE");
        }

        @Test
        void should_combine_apiTypes_and_query_on_api_type_enum() {
            var output = useCase.execute(
                new GetFilterValuesUseCase.Input(
                    AUDIT_INFO,
                    "API_TYPE",
                    null,
                    null,
                    1,
                    10,
                    "m",
                    Set.of(ApiType.MCP_PROXY, ApiType.LLM_PROXY, ApiType.MESSAGE)
                )
            );

            assertThat(output.valuesPage().data()).extracting(FilterValue::value).containsExactlyInAnyOrder("LLM", "MESSAGE", "MCP");
        }

        @Test
        void should_narrow_keyword_filter_authorized_ids_by_apiTypes() {
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of("api-proxy-1", "api-mcp-1", "api-native-1"),
                    Map.of("api-proxy-1", "Proxy API", "api-mcp-1", "MCP API", "api-native-1", "Native API"),
                    Map.of(),
                    Map.of(
                        ApiType.PROXY,
                        Set.of("api-proxy-1"),
                        ApiType.MCP_PROXY,
                        Set.of("api-mcp-1"),
                        ApiType.NATIVE,
                        Set.of("api-native-1")
                    ),
                    Optional.empty()
                )
            );

            var output = useCase.execute(
                new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", null, null, 1, 10, "API", Set.of(ApiType.MCP_PROXY))
            );

            assertThat(output.valuesPage().data())
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.value()).isEqualTo("MCP API");
                    assertThat(v.id()).isEqualTo("api-mcp-1");
                });
        }

        @Test
        void should_return_empty_when_apiTypes_matches_no_authorized_ids() {
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    Set.of("api-proxy-1"),
                    Map.of("api-proxy-1", "Proxy API"),
                    Map.of(),
                    Map.of(ApiType.PROXY, Set.of("api-proxy-1")),
                    Optional.empty()
                )
            );

            var output = useCase.execute(
                new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", null, null, 1, 10, "API", Set.of(ApiType.MCP_PROXY))
            );

            assertThat(output.valuesPage().data()).isEmpty();
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(0L);
        }
    }

    /**
     * A module scoped by API kind whose kinds an entrypoint can add to an API of another type: AIM sees Studio by
     * type, and Orders, a PROXY API exposing {@code mcp}, through its MCP calls only.
     */
    @Nested
    class TrafficScopeConstraints {

        private static final Set<String> READABLE = Set.of("orders", "studio", "native");
        private static final TrafficScope AIM = new TrafficScope(Set.of("studio"), Set.of("mcp"));

        @BeforeEach
        void setUp() {
            when(definitionQueryService.getAllFilters()).thenReturn(
                List.of(
                    FilterSpecFixtures.keyword(FilterSpec.Name.API, "API"),
                    FilterSpecFixtures.keyword(FilterSpec.Name.APPLICATION, "Application"),
                    FilterSpecFixtures.keyword(FilterSpec.Name.HTTP_METHOD, "HTTP Method")
                )
            );
            when(contextLoader.load(any(), any(), any())).thenReturn(
                new AnalyticsQueryContext(
                    AUDIT_INFO,
                    new ExecutionContext("org-id", "env-id"),
                    READABLE,
                    Map.of("orders", "Orders API", "studio", "Studio API", "native", "Native API"),
                    Map.of(),
                    Map.of(ApiType.PROXY, Set.of("orders"), ApiType.MCP_PROXY, Set.of("studio"), ApiType.NATIVE, Set.of("native")),
                    Optional.empty()
                )
            );
        }

        private GetFilterValuesUseCase.Input aim(String filterName, String query) {
            return new GetFilterValuesUseCase.Input(
                AUDIT_INFO,
                filterName,
                null,
                null,
                1,
                10,
                query,
                Set.of(ApiType.MCP_PROXY),
                AnalyticsScope.MANAGEMENT,
                Set.of("mcp")
            );
        }

        @Test
        void should_offer_only_the_values_of_the_calls_the_module_sees() {
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    anyInt(),
                    anyInt(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(new FilterValuesPage(List.of(new FilterValue("GET")), null));

            useCase.execute(aim("HTTP_METHOD", null));

            verify(filterValuesQueryService).searchFilterValues(
                "org-id",
                "env-id",
                FilterSpec.Name.HTTP_METHOD,
                null,
                null,
                1,
                10,
                null,
                null,
                READABLE,
                AIM
            );
        }

        /** A member's request scoped to an API product they hold no workspace grant on: their rows, through its calls. */
        @Test
        void should_keep_both_the_traffic_scope_and_the_api_product_of_the_request() {
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    anyInt(),
                    anyInt(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(new FilterValuesPage(List.of(new FilterValue("GET")), null));

            useCase.execute(
                new GetFilterValuesUseCase.Input(
                    AUDIT_INFO_WITH_ACTOR,
                    "HTTP_METHOD",
                    null,
                    null,
                    1,
                    10,
                    null,
                    Set.of(ApiType.MCP_PROXY),
                    AnalyticsScope.MANAGEMENT,
                    Set.of("mcp"),
                    "workspace-1"
                )
            );

            verify(filterValuesQueryService).searchFilterValues(
                "org-id",
                "env-id",
                FilterSpec.Name.HTTP_METHOD,
                null,
                null,
                1,
                10,
                null,
                null,
                READABLE,
                AIM,
                "workspace-1"
            );
        }

        @Test
        void should_list_the_apis_the_module_sees_from_their_calls() {
            when(
                filterValuesQueryService.searchFilterValues(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    anyInt(),
                    anyInt(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(new FilterValuesPage(List.of(new FilterValue("orders"), new FilterValue("studio")), null));

            var output = useCase.execute(aim("API", null));

            verify(filterValuesQueryService).searchFilterValues(
                "org-id",
                "env-id",
                FilterSpec.Name.API,
                null,
                null,
                1,
                10,
                null,
                null,
                READABLE,
                AIM
            );
            assertThat(output.valuesPage().data()).extracting(FilterValue::value).containsExactly("Orders API", "Studio API");
        }

        @Test
        void should_find_by_name_an_api_in_scope_by_type_and_one_with_calls_through_an_entrypoint_adding_the_kind() {
            when(
                filterValuesQueryService.searchFilterValues(
                    eq("org-id"),
                    eq("env-id"),
                    eq(FilterSpec.Name.API),
                    any(),
                    any(),
                    eq(1),
                    anyInt(),
                    any(),
                    any(),
                    eq(Set.of("orders", "native")),
                    eq(new TrafficScope(Set.of(), Set.of("mcp")))
                )
            ).thenReturn(new FilterValuesPage(List.of(new FilterValue("orders")), null));

            var output = useCase.execute(aim("API", "api"));

            assertThat(output.valuesPage().data()).extracting(FilterValue::value).containsExactly("Orders API", "Studio API");
            assertThat(output.valuesPage().totalFilteredCount()).isEqualTo(2L);
        }

        @Test
        void should_keep_today_s_type_narrowing_when_no_entrypoint_adds_the_kind() {
            var output = useCase.execute(
                new GetFilterValuesUseCase.Input(AUDIT_INFO, "API", null, null, 1, 10, "api", Set.of(ApiType.MCP_PROXY))
            );

            assertThat(output.valuesPage().data()).extracting(FilterValue::value).containsExactly("Studio API");
            verifyNoInteractions(filterValuesQueryService);
        }

        /** Applications are searched in the calls of every readable API: the APIs in the module's scope would be computed for nothing. */
        @Test
        void should_search_applications_by_name_in_the_calls_of_every_readable_api() {
            when(applicationQueryService.findByEnvironment("env-id")).thenReturn(Set.of(application("app-id-1", "Agent client")));
            when(
                filterValuesQueryService.findValuesInCalls(
                    "org-id",
                    "env-id",
                    FilterSpec.Name.APPLICATION,
                    null,
                    null,
                    Set.of("app-id-1"),
                    READABLE
                )
            ).thenReturn(Set.of("app-id-1"));

            var output = useCase.execute(aim("APPLICATION", "agent"));

            assertThat(output.valuesPage().data()).extracting(FilterValue::id).containsExactly("app-id-1");
            verify(filterValuesQueryService, never()).searchFilterValues(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyInt(),
                anyInt(),
                any(),
                any(),
                any(),
                any()
            );
        }
    }

    @Nested
    class UnsupportedFilterTypes {

        @BeforeEach
        void setUp() {
            when(definitionQueryService.getAllFilters()).thenReturn(
                List.of(
                    FilterSpecFixtures.number(FilterSpec.Name.HTTP_STATUS, "Status Code", new NumberRange(100, 599)),
                    FilterSpecFixtures.string(FilterSpec.Name.HTTP_PATH, "Path")
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

    private void givenReadableApis(String... apiIds) {
        when(contextLoader.load(any(), any(), any())).thenReturn(
            new AnalyticsQueryContext(
                AUDIT_INFO,
                new ExecutionContext("org-id", "env-id"),
                Set.of(apiIds),
                Map.of(),
                Map.of(),
                Map.of(),
                Optional.empty()
            )
        );
    }

    private static BaseApplicationEntity application(String id, String name) {
        var application = new BaseApplicationEntity();
        application.setId(id);
        application.setName(name);
        return application;
    }
}
