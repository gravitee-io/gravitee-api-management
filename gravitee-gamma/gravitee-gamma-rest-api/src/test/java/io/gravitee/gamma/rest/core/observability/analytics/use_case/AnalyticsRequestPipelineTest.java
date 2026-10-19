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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.rest.core.observability.analytics.model.DataKind;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import io.gravitee.gamma.rest.core.observability.logs.model.TrafficScope;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import io.gravitee.gamma.rest.infra.adapter.EntrypointScopeProviderAdapter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsRequestPipelineTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final Instant FROM = Instant.parse("2026-06-10T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-06-11T00:00:00Z");

    @Mock
    private ObservabilityAnalyticsDataPort analyticsDataPort;

    @Mock
    private FilterRegistry filterRegistry;

    private AnalyticsRequestPipeline pipeline;

    @BeforeEach
    void setUp() {
        var accessibleApiScope = new AccessibleApiScopeDomainService();
        var filterValidator = new ObservabilityFilterValidator(filterRegistry);
        pipeline = new AnalyticsRequestPipeline(filterValidator, accessibleApiScope, new EntrypointScopeProviderAdapter());

        lenient()
            .when(filterRegistry.getFilters(any(), any()))
            .thenReturn(
                List.of(
                    new FilterSpec(
                        "API",
                        "API",
                        FilterType.KEYWORD,
                        List.of(FilterOperator.EQ, FilterOperator.IN),
                        null,
                        null,
                        Set.of(Signal.LOGS, Signal.ANALYTICS),
                        ApiType.ALL
                    ),
                    new FilterSpec(
                        "API_PRODUCT",
                        "API Product",
                        FilterType.KEYWORD,
                        List.of(FilterOperator.EQ, FilterOperator.IN),
                        null,
                        null,
                        Set.of(Signal.LOGS, Signal.ANALYTICS),
                        ApiType.ALL
                    ),
                    new FilterSpec(
                        "HTTP_STATUS",
                        "Status Code",
                        FilterType.NUMBER,
                        List.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE),
                        null,
                        new FilterSpec.Range(100, 599),
                        Set.of(Signal.LOGS, Signal.ANALYTICS),
                        Set.of(ApiType.HTTP_PROXY)
                    ),
                    new FilterSpec(
                        "ENTRYPOINT",
                        "Entrypoint",
                        FilterType.KEYWORD,
                        List.of(FilterOperator.IN),
                        null,
                        null,
                        Set.of(Signal.LOGS, Signal.ANALYTICS),
                        ApiType.ALL
                    ),
                    new FilterSpec(
                        "API_TYPE",
                        "API Type",
                        FilterType.ENUM,
                        List.of(FilterOperator.EQ, FilterOperator.IN),
                        List.of(
                            new FilterSpec.EnumValue("HTTP_PROXY", "HTTP Proxy"),
                            new FilterSpec.EnumValue("LLM", "LLM"),
                            new FilterSpec.EnumValue("MCP", "MCP")
                        ),
                        null,
                        Set.of(Signal.LOGS, Signal.ANALYTICS),
                        ApiType.API_KINDS
                    ),
                    new FilterSpec(
                        "RECORD_TYPE",
                        "Record Type",
                        FilterType.ENUM,
                        List.of(FilterOperator.EQ, FilterOperator.IN),
                        List.of(
                            new FilterSpec.EnumValue("REQUEST", "Request"),
                            new FilterSpec.EnumValue("AUTHZ_DECISION", "Authz decision")
                        ),
                        null,
                        Set.of(Signal.LOGS, Signal.ANALYTICS),
                        ApiType.API_KINDS
                    )
                )
            );
    }

    @Nested
    class DataKindOf {

        private static final FilterCondition BY_METHOD = new FilterCondition("HTTP_METHOD", FilterOperator.EQ, List.of("GET"));

        @Test
        void should_read_the_aggregates_when_no_field_identifies_a_caller() {
            assertThat(pipeline.dataKindOf(List.of(BY_METHOD), List.of(List.of(BY_METHOD)), List.of("HTTP_STATUS"))).isEqualTo(
                DataKind.AGGREGATES
            );
        }

        @Test
        void should_read_the_requests_when_a_condition_identifies_a_caller() {
            var byConsumerIp = new FilterCondition("CONSUMER_IP", FilterOperator.EQ, List.of("10.0.0.1"));

            assertThat(pipeline.dataKindOf(List.of(byConsumerIp), List.of(), List.of())).isEqualTo(DataKind.PER_REQUEST);
        }

        @Test
        void should_read_the_requests_when_a_metric_condition_identifies_a_caller() {
            var bySubject = new FilterCondition("AUTHZ_SUBJECT_ID", FilterOperator.IN, List.of("alice"));

            assertThat(pipeline.dataKindOf(List.of(), List.of(List.of(), List.of(bySubject)), List.of())).isEqualTo(DataKind.PER_REQUEST);
        }

        @Test
        void should_read_the_requests_when_a_facet_identifies_a_caller() {
            assertThat(pipeline.dataKindOf(List.of(), List.of(), List.of("NATIVE_CLIENT_ID"))).isEqualTo(DataKind.PER_REQUEST);
        }

        @Test
        void should_read_the_requests_when_the_request_asks_for_decision_records() {
            var decisions = new FilterCondition("RECORD_TYPE", FilterOperator.EQ, List.of("AUTHZ_DECISION"));
            var notRequests = new FilterCondition("RECORD_TYPE", FilterOperator.NEQ, List.of("REQUEST"));

            assertThat(pipeline.dataKindOf(List.of(decisions), List.of(), List.of())).isEqualTo(DataKind.PER_REQUEST);
            assertThat(pipeline.dataKindOf(List.of(notRequests), List.of(), List.of())).isEqualTo(DataKind.PER_REQUEST);
        }

        @Test
        void should_read_the_aggregates_when_the_request_asks_for_request_records() {
            var requests = new FilterCondition("RECORD_TYPE", FilterOperator.EQ, List.of("REQUEST"));

            assertThat(pipeline.dataKindOf(List.of(requests), List.of(), List.of())).isEqualTo(DataKind.AGGREGATES);
        }

        @Test
        void should_load_the_apis_of_the_kind_the_request_reads() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.PER_REQUEST)).thenReturn(
                List.of(new AccessibleApi("logged-api", "Logged API", ApiType.HTTP_PROXY))
            );

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, List.of(), FROM, TO, DataKind.PER_REQUEST, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactly("logged-api");
        }
    }

    /** AI_WORKSPACE_USER[READ] on a workspace opens the aggregates of the calls made through it, nothing else. */
    @Nested
    class WorkspaceReader {

        private static final FilterCondition ON_WORKSPACE = new FilterCondition("API_PRODUCT", FilterOperator.EQ, List.of("workspace"));
        private static final List<String> REQUEST_COUNT = List.of("HTTP_REQUESTS");

        /** The workspace alone, as an equality or as the one value of a list: gamma-lib-observability sends the list. */
        static Stream<FilterCondition> onTheWorkspace() {
            return Stream.of(ON_WORKSPACE, new FilterCondition("API_PRODUCT", FilterOperator.IN, List.of("workspace")));
        }

        static Stream<FilterCondition> onAnotherWorkspace() {
            return Stream.of(
                new FilterCondition("API_PRODUCT", FilterOperator.EQ, List.of("other")),
                new FilterCondition("API_PRODUCT", FilterOperator.IN, List.of("other"))
            );
        }

        @BeforeEach
        void theEngineNarrowsTheRequestCountByTheWorkspace() {
            lenient().when(analyticsDataPort.narrowsByApiProduct(REQUEST_COUNT)).thenReturn(true);
        }

        /** A search path that ignores the workspace condition would count every call of the workspace's APIs. */
        @Test
        void should_not_open_the_workspace_to_a_metric_the_engine_does_not_narrow_by_it() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(List.of());
            when(analyticsDataPort.narrowsByApiProduct(List.of("AUTHZ_DECISIONS"))).thenReturn(false);

            var scope = pipeline
                .prepare(
                    ORG_ID,
                    ENV_ID,
                    List.of(ON_WORKSPACE),
                    FROM,
                    TO,
                    DataKind.AGGREGATES,
                    List.of("AUTHZ_DECISIONS"),
                    analyticsDataPort
                )
                .scopeWith(List.of());

            assertThat(scope.isEmpty()).isTrue();
            verify(analyticsDataPort, never()).loadWorkspaceApis(any(), any(), any());
        }

        @ParameterizedTest
        @MethodSource("onTheWorkspace")
        void should_count_the_workspace_apis_its_reader_is_no_member_of_within_the_workspace_calls_only(FilterCondition onWorkspace) {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(List.of());
            when(analyticsDataPort.loadWorkspaceApis(ORG_ID, ENV_ID, "workspace")).thenReturn(
                List.of(new AccessibleApi("shared-llm", "Shared LLM", ApiType.LLM))
            );

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, List.of(onWorkspace), FROM, TO, DataKind.AGGREGATES, REQUEST_COUNT, analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactly("shared-llm");
            // A proxy shared with another workspace: only the calls made through this one are counted.
            assertThat(scope.filters()).contains(onWorkspace);
        }

        /** The port finds no API of a workspace its caller does not read. */
        @ParameterizedTest
        @MethodSource("onAnotherWorkspace")
        void should_count_nothing_for_a_reader_of_another_workspace(FilterCondition onAnotherWorkspace) {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(List.of());
            when(analyticsDataPort.loadWorkspaceApis(ORG_ID, ENV_ID, "other")).thenReturn(List.of());

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, List.of(onAnotherWorkspace), FROM, TO, DataKind.AGGREGATES, REQUEST_COUNT, analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.isEmpty()).isTrue();
            verify(analyticsDataPort, never()).loadWorkspaceApis(ORG_ID, ENV_ID, "workspace");
        }

        @Test
        void should_keep_the_apis_the_caller_reads_alongside_the_workspace_ones() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(
                List.of(new AccessibleApi("owned", "Owned", ApiType.HTTP_PROXY), new AccessibleApi("shared-llm", "Shared LLM", ApiType.LLM))
            );
            when(analyticsDataPort.loadWorkspaceApis(ORG_ID, ENV_ID, "workspace")).thenReturn(
                List.of(new AccessibleApi("shared-llm", "Shared LLM", ApiType.LLM))
            );

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, List.of(ON_WORKSPACE), FROM, TO, DataKind.AGGREGATES, REQUEST_COUNT, analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactlyInAnyOrder("owned", "shared-llm");
        }

        static Stream<Arguments> queriesNotNarrowedToOneWorkspace() {
            return Stream.of(
                Arguments.of("no workspace", List.of()),
                Arguments.of(
                    "several workspaces",
                    List.of(new FilterCondition("API_PRODUCT", FilterOperator.IN, List.of("workspace", "other")))
                ),
                Arguments.of(
                    "the same workspace twice",
                    List.of(new FilterCondition("API_PRODUCT", FilterOperator.IN, List.of("workspace", "workspace")))
                ),
                Arguments.of(
                    "a null workspace",
                    List.of(new FilterCondition("API_PRODUCT", FilterOperator.IN, Arrays.asList((String) null)))
                ),
                Arguments.of(
                    "two workspace conditions",
                    List.of(ON_WORKSPACE, new FilterCondition("API_PRODUCT", FilterOperator.EQ, List.of("other")))
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("queriesNotNarrowedToOneWorkspace")
        void should_not_open_any_workspace_to_a_query_not_narrowed_to_one(String description, List<FilterCondition> filters) {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(List.of());

            pipeline.prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, REQUEST_COUNT, analyticsDataPort);

            verify(analyticsDataPort, never()).loadWorkspaceApis(any(), any(), any());
        }

        /** Logs, traces and fields identifying callers are per-request data: a workspace never opens them. */
        @ParameterizedTest
        @MethodSource("onTheWorkspace")
        void should_not_open_the_workspace_to_per_request_data(FilterCondition onWorkspace) {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.PER_REQUEST)).thenReturn(List.of());

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, List.of(onWorkspace), FROM, TO, DataKind.PER_REQUEST, REQUEST_COUNT, analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.isEmpty()).isTrue();
            verify(analyticsDataPort, never()).loadWorkspaceApis(any(), any(), any());
        }
    }

    @Nested
    class Scoping {

        @Test
        void should_compute_scope_from_accessible_apis() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(
                List.of(
                    new AccessibleApi("api-proxy", "Proxy API", ApiType.HTTP_PROXY),
                    new AccessibleApi("api-llm", "LLM API", ApiType.LLM)
                )
            );

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, List.of(), FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactlyInAnyOrder("api-proxy", "api-llm");
        }

        @Test
        void should_intersect_user_api_filter_with_accessible_apis() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY), new AccessibleApi("api-2", "API 2", ApiType.HTTP_PROXY))
            );

            var filters = List.of(new FilterCondition("API", FilterOperator.EQ, List.of("api-1")));
            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactly("api-1");
        }

        @Test
        void should_return_empty_scope_when_no_accessible_apis() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(List.of());

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, List.of(), FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).isEmpty();
        }

        @Test
        void should_return_the_empty_scope_rather_than_an_environment_wide_query_when_no_api_is_accessible() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(List.of());

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, List.of(), FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.isEmpty()).isTrue();
            assertThat(scope.filters()).isEmpty();
        }

        @Test
        void should_return_empty_scope_when_user_filters_unknown_api() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );

            var filters = List.of(new FilterCondition("API", FilterOperator.IN, List.of("non-existent-api")));
            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.isEmpty()).isTrue();
            assertThat(scope.apiIds()).isEmpty();
        }
    }

    /** AIM: Studio is an MCP API, Orders a PROXY API exposing mcp, Petstore a plain PROXY API. */
    @Nested
    class TrafficScopes {

        @BeforeEach
        void readable_apis() {
            lenient()
                .when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES))
                .thenReturn(
                    List.of(
                        new AccessibleApi("studio", "Studio", ApiType.MCP),
                        new AccessibleApi("orders", "Orders", ApiType.HTTP_PROXY),
                        new AccessibleApi("petstore", "Petstore", ApiType.HTTP_PROXY)
                    )
                );
        }

        @Test
        void should_keep_every_readable_api_and_narrow_their_calls_when_an_entrypoint_adds_a_wanted_kind() {
            var filters = List.of(new FilterCondition("API_TYPE", FilterOperator.IN, List.of("MCP", "LLM")));

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactlyInAnyOrder("studio", "orders", "petstore");
            assertThat(scope.trafficScope()).isEqualTo(new TrafficScope(Set.of("studio"), Set.of("mcp")));
        }

        @Test
        void should_scope_by_type_alone_when_no_entrypoint_adds_the_wanted_kinds() {
            var filters = List.of(new FilterCondition("API_TYPE", FilterOperator.EQ, List.of("HTTP_PROXY")));

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactlyInAnyOrder("orders", "petstore");
            assertThat(scope.trafficScope()).isNull();
        }

        @Test
        void should_carry_no_traffic_scope_without_a_kind_condition() {
            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, List.of(), FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.trafficScope()).isNull();
        }

        @Test
        void should_narrow_an_explicitly_selected_api_of_another_type_to_its_calls_through_the_entrypoint() {
            var filters = List.of(
                new FilterCondition("API", FilterOperator.EQ, List.of("orders")),
                new FilterCondition("API_TYPE", FilterOperator.IN, List.of("MCP", "LLM"))
            );

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactly("orders");
            assertThat(scope.trafficScope()).isEqualTo(new TrafficScope(Set.of(), Set.of("mcp")));
        }
    }

    @Nested
    class ConditionsAnded {

        private static final List<AccessibleApi> READABLE = List.of(
            new AccessibleApi("proxy-1", "Proxy 1", ApiType.HTTP_PROXY),
            new AccessibleApi("proxy-2", "Proxy 2", ApiType.HTTP_PROXY),
            new AccessibleApi("llm-1", "LLM 1", ApiType.LLM)
        );

        @BeforeEach
        void readable_apis() {
            lenient().when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(READABLE);
        }

        @Test
        void should_scope_to_the_intersection_of_every_api_condition() {
            var filters = List.of(
                new FilterCondition("API", FilterOperator.IN, List.of("proxy-1", "proxy-2")),
                new FilterCondition("API", FilterOperator.IN, List.of("proxy-2", "llm-1"))
            );

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactly("proxy-2");
            assertThat(scope.filters()).containsExactly(new FilterCondition("API", FilterOperator.IN, List.of("proxy-2")));
        }

        @Test
        void should_answer_the_empty_scope_for_disjoint_api_conditions_rather_than_every_readable_api() {
            var filters = List.of(
                new FilterCondition("API", FilterOperator.EQ, List.of("proxy-1")),
                new FilterCondition("API", FilterOperator.EQ, List.of("proxy-2"))
            );

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.isEmpty()).isTrue();
        }

        @Test
        void should_resolve_an_api_type_condition_to_the_readable_apis_of_that_type() {
            var filters = List.of(new FilterCondition("API_TYPE", FilterOperator.EQ, List.of("HTTP_PROXY")));

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactlyInAnyOrder("proxy-1", "proxy-2");
            assertThat(scope.filters()).noneMatch(condition -> "API_TYPE".equals(condition.name()));
        }

        @Test
        void should_scope_to_the_intersection_of_every_api_type_condition() {
            var filters = List.of(
                new FilterCondition("API_TYPE", FilterOperator.IN, List.of("HTTP_PROXY", "LLM")),
                new FilterCondition("API_TYPE", FilterOperator.EQ, List.of("LLM"))
            );

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactly("llm-1");
        }

        @Test
        void should_answer_the_empty_scope_for_disjoint_api_type_conditions() {
            var filters = List.of(
                new FilterCondition("API_TYPE", FilterOperator.EQ, List.of("HTTP_PROXY")),
                new FilterCondition("API_TYPE", FilterOperator.EQ, List.of("LLM"))
            );

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.isEmpty()).isTrue();
        }

        @Test
        void should_intersect_api_and_api_type_conditions() {
            var filters = List.of(
                new FilterCondition("API", FilterOperator.IN, List.of("proxy-1", "llm-1")),
                new FilterCondition("API_TYPE", FilterOperator.EQ, List.of("LLM"))
            );

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactly("llm-1");
        }

        @Test
        void should_never_widen_the_readable_apis() {
            var filters = List.of(new FilterCondition("API", FilterOperator.IN, List.of("proxy-1", "not-readable")));

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.apiIds()).containsExactly("proxy-1");
        }

        @Test
        void should_hand_every_condition_on_one_field_to_the_engine() {
            var filters = List.of(
                new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("200")),
                new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("500"))
            );

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.filters()).containsSubsequence(filters);
        }

        @Test
        void should_append_a_group_condition_to_the_request_conditions() {
            var status200 = new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("200"));
            var status500 = new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("500"));
            var prepared = pipeline.prepare(
                ORG_ID,
                ENV_ID,
                List.of(status200),
                FROM,
                TO,
                DataKind.AGGREGATES,
                List.of(),
                analyticsDataPort
            );

            var scope = prepared.scopeWith(List.of(status500));

            assertThat(scope.filters()).containsSubsequence(status200, status500);
        }

        @Test
        void should_intersect_a_group_api_condition_with_the_request_ones() {
            var prepared = pipeline.prepare(
                ORG_ID,
                ENV_ID,
                List.of(new FilterCondition("API_TYPE", FilterOperator.EQ, List.of("HTTP_PROXY"))),
                FROM,
                TO,
                DataKind.AGGREGATES,
                List.of(),
                analyticsDataPort
            );

            var narrowed = prepared.scopeWith(List.of(new FilterCondition("API", FilterOperator.EQ, List.of("proxy-2"))));
            var emptied = prepared.scopeWith(List.of(new FilterCondition("API", FilterOperator.EQ, List.of("llm-1"))));

            assertThat(narrowed.apiIds()).containsExactly("proxy-2");
            assertThat(emptied.isEmpty()).isTrue();
            assertThat(prepared.scopeWith(List.of()).apiIds()).containsExactlyInAnyOrder("proxy-1", "proxy-2");
        }

        @Test
        void should_load_the_accessible_apis_once_whatever_the_number_of_groups() {
            var prepared = pipeline.prepare(ORG_ID, ENV_ID, List.of(), FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort);

            prepared.scopeWith(List.of(new FilterCondition("API", FilterOperator.EQ, List.of("proxy-1"))));
            prepared.scopeWith(List.of(new FilterCondition("API", FilterOperator.EQ, List.of("proxy-2"))));

            verify(analyticsDataPort, times(1)).loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES);
        }
    }

    @Nested
    class MetricLevelConditions {

        @Test
        void should_refuse_a_record_type_at_metric_level() {
            var decisions = new FilterCondition("RECORD_TYPE", FilterOperator.EQ, List.of("AUTHZ_DECISION"));

            assertThatThrownBy(() -> pipeline.validateMetricConditions(List.of(List.of(), List.of(decisions))))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.filter.record_type_on_metric");
        }

        @Test
        void should_accept_a_field_repeated_in_one_metric() {
            var conditions = List.of(
                new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("500")),
                new FilterCondition("HTTP_STATUS", FilterOperator.LTE, List.of("100"))
            );

            assertThatCode(() -> pipeline.validateMetricConditions(List.of(conditions))).doesNotThrowAnyException();
        }

        @Test
        void should_share_the_set_every_metric_carries() {
            var status = new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("500"));
            var entrypoint = new FilterCondition("ENTRYPOINT", FilterOperator.IN, List.of("http-proxy"));

            var shared = pipeline.sharedMetricConditions(List.of(List.of(status, entrypoint), List.of(entrypoint, status)));

            assertThat(shared).containsExactly(status, entrypoint);
        }

        @Test
        void should_share_a_set_whose_in_values_come_in_another_order() {
            var methods = new FilterCondition("ENTRYPOINT", FilterOperator.IN, List.of("http-proxy", "sse"));
            var reordered = new FilterCondition("ENTRYPOINT", FilterOperator.IN, List.of("sse", "http-proxy"));

            assertThat(pipeline.sharedMetricConditions(List.of(List.of(methods), List.of(reordered)))).containsExactly(methods);
        }

        @Test
        void should_refuse_metrics_carrying_different_sets() {
            var status = new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("500"));

            assertThatThrownBy(() -> pipeline.sharedMetricConditions(List.of(List.of(status), List.of())))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.differing_metric_conditions");
        }
    }

    @Nested
    class FilterValidation {

        @Test
        void should_reject_unknown_filter_name() {
            var filters = List.of(new FilterCondition("UNKNOWN", FilterOperator.EQ, List.of("val")));

            assertThatThrownBy(() ->
                pipeline.prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
            ).isInstanceOf(UnsupportedObservabilityFilterException.class);
        }

        @Test
        void should_reject_unsupported_operator() {
            var filters = List.of(new FilterCondition("HTTP_STATUS", FilterOperator.CONTAINS, List.of("200")));

            assertThatThrownBy(() ->
                pipeline.prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
            ).isInstanceOf(UnsupportedObservabilityFilterException.class);
        }
    }

    @Nested
    class TimeRange {

        @Test
        void should_reject_inverted_time_range() {
            var from = Instant.parse("2026-06-11T12:00:00Z");
            var to = Instant.parse("2026-06-10T12:00:00Z");

            assertThatThrownBy(() ->
                pipeline.prepare(ORG_ID, ENV_ID, List.of(), from, to, DataKind.AGGREGATES, List.of(), analyticsDataPort)
            )
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .hasMessageContaining("from")
                .extracting("technicalCode")
                .isEqualTo("observability.query.invalid_time_range");
        }

        @ParameterizedTest
        @CsvSource(value = { "null, 2026-06-11T00:00:00Z, from", "2026-06-10T00:00:00Z, null, to" }, nullValues = "null")
        void should_reject_a_missing_bound_before_loading_the_accessible_apis(Instant from, Instant to, String missingBound) {
            assertThatThrownBy(() ->
                pipeline.prepare(ORG_ID, ENV_ID, List.of(), from, to, DataKind.AGGREGATES, List.of(), analyticsDataPort)
            )
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .hasMessageContaining("'" + missingBound + "'")
                .extracting("technicalCode")
                .isEqualTo("observability.query.time_range_required");
            verifyNoInteractions(analyticsDataPort);
        }

        @Test
        void should_preserve_valid_time_range() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );

            var from = Instant.parse("2026-06-10T00:00:00Z");
            var to = Instant.parse("2026-06-11T00:00:00Z");
            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, List.of(), from, to, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.from()).isEqualTo(from);
            assertThat(scope.to()).isEqualTo(to);
        }
    }

    @Nested
    class EntrypointScope {

        @Test
        void should_add_no_entrypoint_condition_and_leave_the_default_scope_to_the_engine() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, List.of(), FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.filters()).noneMatch(condition -> "ENTRYPOINT".equals(condition.name()));
        }

        @Test
        void should_preserve_user_entrypoint_filter_when_provided() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );

            var filters = List.of(new FilterCondition("ENTRYPOINT", FilterOperator.IN, List.of("http-proxy")));
            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, filters, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            var entrypoints = scope
                .filters()
                .stream()
                .filter(c -> "ENTRYPOINT".equals(c.name()))
                .toList();
            assertThat(entrypoints).hasSize(1);
            assertThat(entrypoints.getFirst().values()).containsExactly("http-proxy");
        }
    }

    @Nested
    class AuthzDecisionScoping {

        @Test
        void should_strip_the_record_type_condition_before_it_reaches_the_analytics_engine() {
            var conditions = List.of(new FilterCondition("RECORD_TYPE", FilterOperator.EQ, List.of("AUTHZ_DECISION")));

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, conditions, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.filters()).noneMatch(condition -> "RECORD_TYPE".equals(condition.name()));
        }

        @Test
        void should_scope_decision_records_to_the_accessible_apis() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );
            var conditions = List.of(new FilterCondition("RECORD_TYPE", FilterOperator.EQ, List.of("AUTHZ_DECISION")));

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, conditions, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.filters()).anyMatch(condition -> "API".equals(condition.name()));
        }

        @Test
        void should_strip_a_record_type_request_condition_before_it_reaches_the_analytics_engine() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );
            var conditions = List.of(new FilterCondition("RECORD_TYPE", FilterOperator.EQ, List.of("REQUEST")));

            var scope = pipeline
                .prepare(ORG_ID, ENV_ID, conditions, FROM, TO, DataKind.AGGREGATES, List.of(), analyticsDataPort)
                .scopeWith(List.of());

            assertThat(scope.filters()).noneMatch(condition -> "RECORD_TYPE".equals(condition.name()));
            assertThat(scope.filters()).anyMatch(condition -> "API".equals(condition.name()));
        }
    }
}
