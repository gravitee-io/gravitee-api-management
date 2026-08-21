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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.apim.core.log.crud_service.AuthzDecisionLogsCrudService;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.apim.core.log.model.AuthzDecisionLog;
import io.gravitee.apim.core.log.model.AuthzDecisionLogFilters;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.RecordType;
import io.gravitee.gamma.rest.core.observability.logs.model.ApiReference;
import io.gravitee.gamma.rest.core.observability.logs.model.FailureOrigin;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsSearchQuery;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import java.time.Instant;
import java.util.Arrays;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityLogsDataPortAdapterTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";

    @Mock
    private ConnectionLogsCrudService connectionLogsCrudService;

    @Mock
    private AuthzDecisionLogsCrudService authzDecisionLogsCrudService;

    @Mock
    private AnalyticsQueryService analyticsQueryService;

    @Mock
    private UserContextLoader userContextLoader;

    @Mock
    private PlanCrudService planCrudService;

    @Mock
    private ApplicationCrudService applicationCrudService;

    @Mock
    private InstanceQueryService instanceQueryService;

    @Mock
    private ApiProductQueryService apiProductQueryService;

    private ObservabilityLogsDataPortAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ObservabilityLogsDataPortAdapter(
            connectionLogsCrudService,
            authzDecisionLogsCrudService,
            analyticsQueryService,
            userContextLoader,
            planCrudService,
            applicationCrudService,
            instanceQueryService,
            apiProductQueryService
        );
    }

    @Test
    void should_reject_filter_not_translatable_to_log_search() {
        var query = queryWith(new FilterCondition("API_TYPE", FilterOperator.EQ, List.of("HTTP_PROXY")));

        assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("API_TYPE");

        verifyNoInteractions(connectionLogsCrudService);
    }

    @Test
    void should_propagate_api_type_onto_log_rows() {
        when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(SearchLogsFilters.class), any(), any())).thenReturn(
            new SearchLogsResponse<>(1, List.of(BaseConnectionLog.builder().apiId("api-1").build()))
        );

        var page = adapter.searchLogs(ORG, ENV, queryWith());

        assertThat(page.data()).hasSize(1);
        assertThat(page.data().getFirst().apiType()).isEqualTo("HTTP_PROXY");
        assertThat(page.data().getFirst().apiName()).isEqualTo("API 1");
    }

    @Nested
    class HttpStatusFilter {

        @Test
        void should_translate_eq_to_single_status() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("200")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().statuses()).containsExactly(200);
        }

        @Test
        void should_translate_gte_to_status_range_with_open_upper_bound() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("500")));

            adapter.searchLogs(ORG, ENV, query);

            var ranges = captureSearchFilters().statusRanges();
            assertThat(ranges).hasSize(1);
            assertThat(ranges.getFirst().gte()).isEqualTo(500);
            assertThat(ranges.getFirst().lte()).isNull();
        }

        @Test
        void should_translate_lte_to_status_range_with_open_lower_bound() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_STATUS", FilterOperator.LTE, List.of("299")));

            adapter.searchLogs(ORG, ENV, query);

            var ranges = captureSearchFilters().statusRanges();
            assertThat(ranges).hasSize(1);
            assertThat(ranges.getFirst().gte()).isNull();
            assertThat(ranges.getFirst().lte()).isEqualTo(299);
        }

        @Test
        void should_merge_gte_and_lte_into_single_closed_range() {
            stubEmptySearchResult();
            var query = queryWith(
                new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("400")),
                new FilterCondition("HTTP_STATUS", FilterOperator.LTE, List.of("500"))
            );

            adapter.searchLogs(ORG, ENV, query);

            var ranges = captureSearchFilters().statusRanges();
            assertThat(ranges).hasSize(1);
            assertThat(ranges.getFirst().gte()).isEqualTo(400);
            assertThat(ranges.getFirst().lte()).isEqualTo(500);
        }

        @Test
        void should_reject_inverted_gte_lte_range() {
            var query = queryWith(
                new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("500")),
                new FilterCondition("HTTP_STATUS", FilterOperator.LTE, List.of("200"))
            );

            assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("gte")
                .hasMessageContaining("lte");
        }

        @Test
        void should_reject_out_of_range_status_code() {
            var query = queryWith(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("1000")));

            assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("1000");
        }

        @Test
        void should_reject_status_code_below_minimum() {
            var query = queryWith(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("99")));

            assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("99");
        }

        @Test
        void should_reject_non_numeric_status_code() {
            var query = queryWith(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("abc")));

            assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("abc");
        }
    }

    @Nested
    class HttpStatusCodeGroupFilter {

        @Test
        void should_pass_single_group_key() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_STATUS_CODE_GROUP", FilterOperator.EQ, List.of("2XX")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().statusCodeGroups()).containsExactly("2XX");
        }

        @Test
        void should_pass_multiple_group_keys() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_STATUS_CODE_GROUP", FilterOperator.IN, List.of("2XX", "4XX")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().statusCodeGroups()).containsExactlyInAnyOrder("2XX", "4XX");
        }

        @Test
        void should_normalize_group_key_to_uppercase() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_STATUS_CODE_GROUP", FilterOperator.EQ, List.of("5xx")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().statusCodeGroups()).containsExactly("5XX");
        }

        @Test
        void should_not_mix_groups_into_status_ranges() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_STATUS_CODE_GROUP", FilterOperator.EQ, List.of("2XX")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().statusRanges()).isEmpty();
        }

        @Test
        void should_reject_unknown_group() {
            var query = queryWith(new FilterCondition("HTTP_STATUS_CODE_GROUP", FilterOperator.EQ, List.of("9XX")));

            assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("9XX");
        }
    }

    @Nested
    class LlmMcpFilters {

        @Test
        void should_translate_llm_proxy_model() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("LLM_PROXY_MODEL", FilterOperator.IN, List.of("gpt-4", "claude-3")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().llmProxyModels()).containsExactlyInAnyOrder("gpt-4", "claude-3");
        }

        @Test
        void should_translate_llm_proxy_provider() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("LLM_PROXY_PROVIDER", FilterOperator.IN, List.of("openai")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().llmProxyProviders()).containsExactly("openai");
        }

        @Test
        void should_translate_mcp_proxy_tool() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("MCP_PROXY_TOOL", FilterOperator.IN, List.of("tool-1")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().mcpProxyTools()).containsExactly("tool-1");
        }

        @Test
        void should_translate_mcp_proxy_resource() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("MCP_PROXY_RESOURCE", FilterOperator.IN, List.of("res-1")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().mcpProxyResources()).containsExactly("res-1");
        }

        @Test
        void should_translate_mcp_proxy_prompt() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("MCP_PROXY_PROMPT", FilterOperator.IN, List.of("prompt-1")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().mcpProxyPrompts()).containsExactly("prompt-1");
        }
    }

    @Nested
    class TenantFilter {

        @Test
        void should_translate_tenant() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("TENANT", FilterOperator.IN, List.of("tenant-a", "tenant-b")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().tenants()).containsExactlyInAnyOrder("tenant-a", "tenant-b");
        }

        @Test
        void should_leave_tenants_empty_when_not_requested() {
            stubEmptySearchResult();

            adapter.searchLogs(ORG, ENV, queryWith());

            assertThat(captureSearchFilters().tenants()).isEmpty();
        }
    }

    @Nested
    class ResponseTimeFilter {

        @Test
        void should_translate_gte_to_response_time_from() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_GATEWAY_RESPONSE_TIME", FilterOperator.GTE, List.of("100")));

            adapter.searchLogs(ORG, ENV, query);

            var ranges = captureSearchFilters().responseTimeRanges();
            assertThat(ranges).hasSize(1);
            assertThat(ranges.getFirst().from()).isEqualTo(100L);
            assertThat(ranges.getFirst().to()).isNull();
        }

        @Test
        void should_translate_lte_to_response_time_to() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_GATEWAY_RESPONSE_TIME", FilterOperator.LTE, List.of("500")));

            adapter.searchLogs(ORG, ENV, query);

            var ranges = captureSearchFilters().responseTimeRanges();
            assertThat(ranges).hasSize(1);
            assertThat(ranges.getFirst().from()).isNull();
            assertThat(ranges.getFirst().to()).isEqualTo(500L);
        }

        @Test
        void should_translate_eq_to_exact_range() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_GATEWAY_RESPONSE_TIME", FilterOperator.EQ, List.of("250")));

            adapter.searchLogs(ORG, ENV, query);

            var ranges = captureSearchFilters().responseTimeRanges();
            assertThat(ranges).hasSize(1);
            assertThat(ranges.getFirst().from()).isEqualTo(250L);
            assertThat(ranges.getFirst().to()).isEqualTo(250L);
        }
    }

    @Nested
    class OtherFilters {

        @Test
        void should_translate_http_method() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_METHOD", FilterOperator.EQ, List.of("GET")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().methods()).containsExactly(HttpMethod.GET);
        }

        @Test
        void should_fallback_to_other_for_unknown_method() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_METHOD", FilterOperator.EQ, List.of("UNKNOWN")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().methods()).containsExactly(HttpMethod.OTHER);
        }

        @Test
        void should_translate_application_filter() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("APPLICATION", FilterOperator.IN, List.of("app-1", "app-2")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().applicationIds()).containsExactlyInAnyOrder("app-1", "app-2");
        }

        @Test
        void should_translate_uri_filter() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("URI", FilterOperator.EQ, List.of("/api/v1/users")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().uri()).isEqualTo("/api/v1/users");
        }
    }

    @Nested
    class NativeKafkaFields {

        @Test
        void should_translate_native_connection_status_filter() {
            stubEmptySearchResult();
            var query = queryWith(
                new FilterCondition("NATIVE_CONNECTION_STATUS", FilterOperator.IN, List.of("CONNECTED", "SESSION_ERROR"))
            );

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().nativeConnectionStatuses()).containsExactlyInAnyOrder("CONNECTED", "SESSION_ERROR");
        }

        @Test
        void should_translate_failure_origin_filter() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("FAILURE_ORIGIN", FilterOperator.IN, List.of("GATEWAY_TO_BROKER", "NONE")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().failureOrigins()).containsExactlyInAnyOrder("GATEWAY_TO_BROKER", "NONE");
        }

        @Test
        void should_hoist_native_fields_and_derive_failure_origin() {
            when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(SearchLogsFilters.class), any(), any())).thenReturn(
                new SearchLogsResponse<>(
                    1,
                    List.of(
                        BaseConnectionLog.builder()
                            .apiId("api-1")
                            .errorKey("BROKER_NOT_AVAILABLE")
                            .additionalMetrics(
                                Map.of(
                                    "keyword_native-kafka_connection-status",
                                    "SESSION_ERROR",
                                    "keyword_native-kafka_client-id",
                                    "orders-consumer",
                                    "keyword_native-kafka_broker-id",
                                    "1",
                                    "long_native-kafka_connection-duration-ms",
                                    1250L
                                )
                            )
                            .build()
                    )
                )
            );

            var page = adapter.searchLogs(ORG, ENV, queryWith());

            var entry = page.data().getFirst();
            assertThat(entry.connectionStatus()).isEqualTo("SESSION_ERROR");
            assertThat(entry.failureOrigin()).isEqualTo(FailureOrigin.GATEWAY_TO_BROKER);
            assertThat(entry.clientId()).isEqualTo("orders-consumer");
            assertThat(entry.brokerId()).isEqualTo("1");
            assertThat(entry.connectionDurationMs()).isEqualTo(1250L);
        }

        @Test
        void should_read_duration_from_legacy_keyword_key_as_fallback() {
            when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(SearchLogsFilters.class), any(), any())).thenReturn(
                new SearchLogsResponse<>(
                    1,
                    List.of(
                        BaseConnectionLog.builder()
                            .apiId("api-1")
                            .additionalMetrics(
                                Map.of(
                                    "keyword_native-kafka_connection-status",
                                    "CONNECTION_ERROR",
                                    "keyword_native-kafka_connection-duration-ms",
                                    "740"
                                )
                            )
                            .build()
                    )
                )
            );

            var page = adapter.searchLogs(ORG, ENV, queryWith());

            assertThat(page.data().getFirst().connectionDurationMs()).isEqualTo(740L);
        }

        @Test
        void should_propagate_host_and_subscription_id_onto_log_rows() {
            when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(SearchLogsFilters.class), any(), any())).thenReturn(
                new SearchLogsResponse<>(
                    1,
                    List.of(BaseConnectionLog.builder().apiId("api-1").host("kafka.orders.gravitee.dev").subscriptionId("sub-1").build())
                )
            );

            var page = adapter.searchLogs(ORG, ENV, queryWith());

            assertThat(page.data().getFirst().host()).isEqualTo("kafka.orders.gravitee.dev");
            assertThat(page.data().getFirst().subscriptionId()).isEqualTo("sub-1");
        }

        @Test
        void should_not_derive_failure_origin_for_rows_without_connection_status() {
            when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(SearchLogsFilters.class), any(), any())).thenReturn(
                new SearchLogsResponse<>(1, List.of(BaseConnectionLog.builder().apiId("api-1").errorKey("GATEWAY_TIMEOUT").build()))
            );

            var page = adapter.searchLogs(ORG, ENV, queryWith());

            var entry = page.data().getFirst();
            assertThat(entry.connectionStatus()).isNull();
            assertThat(entry.failureOrigin()).isNull();
            assertThat(entry.clientId()).isNull();
            assertThat(entry.brokerId()).isNull();
            assertThat(entry.connectionDurationMs()).isNull();
        }
    }

    @Nested
    class PayloadFilter {

        @Test
        void should_translate_payload_contains_to_body_text() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("PAYLOAD", FilterOperator.CONTAINS, List.of("error 500")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().bodyText()).isEqualTo("error 500");
        }

        @Test
        void should_reject_empty_payload_values() {
            var query = queryWith(new FilterCondition("PAYLOAD", FilterOperator.CONTAINS, List.of()));

            assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining("non-blank");
        }

        @Test
        void should_use_first_value_when_multiple_payload_values_provided() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("PAYLOAD", FilterOperator.CONTAINS, List.of("first", "second")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().bodyText()).isEqualTo("first");
        }

        @Test
        void should_combine_payload_with_other_filters() {
            stubEmptySearchResult();
            var query = queryWith(
                new FilterCondition("PAYLOAD", FilterOperator.CONTAINS, List.of("quantum")),
                new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("200")),
                new FilterCondition("HTTP_METHOD", FilterOperator.EQ, List.of("POST"))
            );

            adapter.searchLogs(ORG, ENV, query);

            var filters = captureSearchFilters();
            assertThat(filters.bodyText()).isEqualTo("quantum");
            assertThat(filters.statuses()).containsExactly(200);
            assertThat(filters.methods()).containsExactly(HttpMethod.POST);
        }

        @Test
        void should_translate_payload_with_special_characters() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("PAYLOAD", FilterOperator.CONTAINS, List.of("{\"key\":\"value\"}")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().bodyText()).isEqualTo("{\"key\":\"value\"}");
        }
    }

    private LogsSearchQuery queryWith(FilterCondition... conditions) {
        return LogsSearchQuery.builder()
            .apiIds(Set.of("api-1"))
            .apisById(Map.of("api-1", new ApiReference("API 1", "HTTP_PROXY")))
            .conditions(List.of(conditions))
            .page(1)
            .perPage(20)
            .build();
    }

    private void stubEmptySearchResult() {
        when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(SearchLogsFilters.class), any(), any())).thenReturn(
            new SearchLogsResponse<>(0, List.of())
        );
    }

    private SearchLogsFilters captureSearchFilters() {
        var captor = ArgumentCaptor.forClass(SearchLogsFilters.class);
        verify(connectionLogsCrudService).searchApiConnectionLogs(any(), captor.capture(), any(), any());
        return captor.getValue();
    }

    @Nested
    class AuthzDecisions {

        @Test
        void should_read_decisions_instead_of_connection_logs() {
            when(authzDecisionLogsCrudService.searchDecisionLogs(any(), any(), any())).thenReturn(
                new SearchLogsResponse<>(
                    3,
                    List.of(
                        AuthzDecisionLog.builder()
                            .eventId("evt-1")
                            .apiId("api-1")
                            .timestamp(1_000L)
                            .requestId("req-1")
                            .gatewayId("gateway-1")
                            .operation("evaluate")
                            .status("success")
                            .caller("pep")
                            .decision("PERMIT")
                            .matchedPolicyNames(List.of("allow-readers"))
                            .subjectId("alice")
                            .durationNanos(4_200L)
                            .build()
                    )
                )
            );

            var page = adapter.searchLogs(ORG, ENV, decisionQuery());

            assertThat(page.totalCount()).isEqualTo(3);
            var entry = page.data().getFirst();
            assertThat(entry.apiName()).isEqualTo("API 1");
            assertThat(entry.timestamp()).isEqualTo(Instant.ofEpochMilli(1_000L));
            assertThat(entry.authz())
                .isNotNull()
                .satisfies(authz -> {
                    assertThat(authz.decision()).isEqualTo("PERMIT");
                    assertThat(authz.caller()).isEqualTo("pep");
                    assertThat(authz.operation()).isEqualTo("evaluate");
                    assertThat(authz.eventId()).isEqualTo("evt-1");
                    assertThat(authz.status()).isEqualTo("success");
                    assertThat(authz.subjectId()).isEqualTo("alice");
                    assertThat(authz.durationNanos()).isEqualTo(4_200L);
                    assertThat(authz.matchedPolicyNames()).containsExactly("allow-readers");
                });
            assertThat(entry.additionalMetrics()).isNull();
            verifyNoInteractions(connectionLogsCrudService);
        }

        @Test
        void should_leave_absent_decision_details_null() {
            when(authzDecisionLogsCrudService.searchDecisionLogs(any(), any(), any())).thenReturn(
                new SearchLogsResponse<>(1, List.of(AuthzDecisionLog.builder().eventId("evt-1").apiId("api-1").build()))
            );

            var entry = adapter.searchLogs(ORG, ENV, decisionQuery()).data().getFirst();

            assertThat(entry.timestamp()).isNull();
            assertThat(entry.authz().eventId()).isEqualTo("evt-1");
            assertThat(entry.authz().decision()).isNull();
            assertThat(entry.authz().subjectId()).isNull();
            assertThat(entry.authz().batchIndex()).isNull();
        }

        @Test
        void should_translate_the_decision_condition_into_a_repository_filter() {
            when(authzDecisionLogsCrudService.searchDecisionLogs(any(), any(), any())).thenReturn(new SearchLogsResponse<>(0, List.of()));

            adapter.searchLogs(
                ORG,
                ENV,
                decisionQueryWith(new FilterCondition("AUTHZ_DECISION", FilterOperator.IN, List.of("PERMIT", "FORBID")))
            );

            var captor = ArgumentCaptor.forClass(AuthzDecisionLogFilters.class);
            verify(authzDecisionLogsCrudService).searchDecisionLogs(any(), captor.capture(), any());
            assertThat(captor.getValue().decisions()).containsExactly("PERMIT", "FORBID");
            assertThat(captor.getValue().apiIds()).containsExactly("api-1");
        }

        @Test
        void should_route_every_decision_condition_to_its_own_repository_field() {
            when(authzDecisionLogsCrudService.searchDecisionLogs(any(), any(), any())).thenReturn(new SearchLogsResponse<>(0, List.of()));

            adapter.searchLogs(
                ORG,
                ENV,
                decisionQueryWith(
                    new FilterCondition("AUTHZ_STATUS", FilterOperator.EQ, List.of("error")),
                    new FilterCondition("AUTHZ_OPERATION", FilterOperator.EQ, List.of("search")),
                    new FilterCondition("PDP", FilterOperator.EQ, List.of("pdp-a")),
                    new FilterCondition("MATCHED_POLICY", FilterOperator.EQ, List.of("forbid-delete")),
                    new FilterCondition("POLICY_VERSION", FilterOperator.EQ, List.of("9")),
                    new FilterCondition("REQUEST_ID", FilterOperator.EQ, List.of("req-1")),
                    new FilterCondition("REASON", FilterOperator.CONTAINS, List.of("forbid"))
                )
            );

            var captor = ArgumentCaptor.forClass(AuthzDecisionLogFilters.class);
            verify(authzDecisionLogsCrudService).searchDecisionLogs(any(), captor.capture(), any());
            var filters = captor.getValue();
            // Distinct values per field: swapping two mappings would otherwise still satisfy the assertions.
            assertThat(filters.statuses()).containsExactly("error");
            assertThat(filters.operations()).containsExactly("search");
            assertThat(filters.targetPdpIds()).containsExactly("pdp-a");
            assertThat(filters.matchedPolicyNames()).containsExactly("forbid-delete");
            assertThat(filters.policyGenerations()).containsExactly("9");
            assertThat(filters.requestIds()).containsExactly("req-1");
            assertThat(filters.reasonContains()).isEqualTo("forbid");
        }

        @Test
        void should_refuse_a_policy_version_that_is_not_a_number_instead_of_failing_the_shard() {
            assertThatThrownBy(() ->
                adapter.searchLogs(ORG, ENV, decisionQueryWith(new FilterCondition("POLICY_VERSION", FilterOperator.EQ, List.of("v9"))))
            )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("v9");
        }

        @Test
        void should_keep_a_decimal_policy_version_that_elasticsearch_already_coerces() {
            when(authzDecisionLogsCrudService.searchDecisionLogs(any(), any(), any())).thenReturn(new SearchLogsResponse<>(0, List.of()));

            adapter.searchLogs(ORG, ENV, decisionQueryWith(new FilterCondition("POLICY_VERSION", FilterOperator.EQ, List.of("3.0"))));

            var captor = ArgumentCaptor.forClass(AuthzDecisionLogFilters.class);
            verify(authzDecisionLogsCrudService).searchDecisionLogs(any(), captor.capture(), any());
            assertThat(captor.getValue().policyGenerations()).containsExactly("3.0");
        }

        @Test
        void should_skip_a_blank_reason_value_rather_than_dropping_the_whole_clause() {
            when(authzDecisionLogsCrudService.searchDecisionLogs(any(), any(), any())).thenReturn(new SearchLogsResponse<>(0, List.of()));

            // The validator only refuses a condition whose values are all blank, so a leading blank would
            // otherwise silently return the unfiltered set under an active filter chip.
            adapter.searchLogs(
                ORG,
                ENV,
                decisionQueryWith(new FilterCondition("REASON", FilterOperator.CONTAINS, Arrays.asList("", "forbid")))
            );

            var captor = ArgumentCaptor.forClass(AuthzDecisionLogFilters.class);
            verify(authzDecisionLogsCrudService).searchDecisionLogs(any(), captor.capture(), any());
            assertThat(captor.getValue().reasonContains()).isEqualTo("forbid");
        }

        @Test
        void should_leave_the_decision_filter_empty_when_none_is_requested() {
            when(authzDecisionLogsCrudService.searchDecisionLogs(any(), any(), any())).thenReturn(new SearchLogsResponse<>(0, List.of()));

            adapter.searchLogs(ORG, ENV, decisionQuery());

            var captor = ArgumentCaptor.forClass(AuthzDecisionLogFilters.class);
            verify(authzDecisionLogsCrudService).searchDecisionLogs(any(), captor.capture(), any());
            assertThat(captor.getValue().decisions()).isEmpty();
        }

        @Test
        void should_not_read_a_decision_for_an_api_the_caller_cannot_access() {
            when(userContextLoader.loadApi(any(), eq("api-1"))).thenAnswer(invocation -> invocation.getArgument(0));

            var decision = adapter.getDecision(ORG, ENV, "api-1", "evt-1");

            assertThat(decision).isEmpty();
            // The scoping has to happen before the store is touched, otherwise the guard is decorative.
            verifyNoInteractions(authzDecisionLogsCrudService);
        }

        @Test
        void should_resolve_the_api_by_id_rather_than_loading_the_whole_environment() {
            grantAccessToApi1();
            when(authzDecisionLogsCrudService.findDecisionLog(any(), eq("api-1"), eq("evt-1"))).thenReturn(
                Optional.of(AuthzDecisionLog.builder().eventId("evt-1").apiId("api-1").build())
            );

            adapter.getDecision(ORG, ENV, "api-1", "evt-1");

            // Opening one decision must not pay for every api in the environment.
            verify(userContextLoader).loadApi(any(), eq("api-1"));
            verify(userContextLoader, never()).loadApis(any());
        }

        @Test
        void should_read_the_decision_of_an_accessible_api_and_enrich_it_with_the_api_identity() {
            grantAccessToApi1();
            when(authzDecisionLogsCrudService.findDecisionLog(any(), eq("api-1"), eq("evt-1"))).thenReturn(
                Optional.of(AuthzDecisionLog.builder().eventId("evt-1").apiId("api-1").requestId("req-1").decision("PERMIT").build())
            );

            var decision = adapter.getDecision(ORG, ENV, "api-1", "evt-1");

            assertThat(decision).isPresent();
            assertThat(decision.get().requestId()).isEqualTo("req-1");
            assertThat(decision.get().apiName()).isEqualTo("API 1");
            assertThat(decision.get().apiType()).isEqualTo("AUTHZ");
        }

        @Test
        void should_report_no_decision_when_the_accessible_api_has_no_such_event() {
            grantAccessToApi1();
            when(authzDecisionLogsCrudService.findDecisionLog(any(), eq("api-1"), eq("gone"))).thenReturn(Optional.empty());

            assertThat(adapter.getDecision(ORG, ENV, "api-1", "gone")).isEmpty();
        }

        private void grantAccessToApi1() {
            when(userContextLoader.loadApi(any(), eq("api-1"))).thenAnswer(invocation ->
                ((UserContext) invocation.getArgument(0)).withApis(
                    List.of(Api.builder().id("api-1").name("API 1").type(ApiType.AUTHZ).build())
                )
            );
        }

        private LogsSearchQuery decisionQuery() {
            return decisionQueryWith();
        }

        private LogsSearchQuery decisionQueryWith(FilterCondition... conditions) {
            return LogsSearchQuery.builder()
                .apiIds(Set.of("api-1"))
                .apisById(Map.of("api-1", new ApiReference("API 1", "HTTP_PROXY")))
                .conditions(List.of(conditions))
                .page(1)
                .perPage(20)
                .recordType(RecordType.AUTHZ_DECISION)
                .build();
        }
    }
}
