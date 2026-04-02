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
package io.gravitee.rest.api.management.v2.rest.resource.api.analytics;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics.model.AnalyticsDateHisto;
import io.gravitee.apim.core.analytics.model.AnalyticsGroupBy;
import io.gravitee.apim.core.analytics.model.AnalyticsStats;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsAverageConnectionDurationResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsAverageMessagesPerRequestResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsCountResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsDateHistoResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsGroupByResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsOverPeriodResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsRequestsCountResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponseStatusOvertimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponseStatusRangesResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsStatsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.analytics.AverageConnectionDuration;
import io.gravitee.rest.api.model.v4.analytics.AverageMessagesPerRequest;
import io.gravitee.rest.api.model.v4.analytics.RequestsCount;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiAnalyticsResourceTest extends ApiResourceTest {

    WebTarget requestsCountTarget;
    WebTarget statusCodesByEntrypointTarget;
    WebTarget statusCodesOvertimeTarget;
    WebTarget averageMessagesPerRequestTarget;
    WebTarget averageConnectionDurationTarget;
    WebTarget responseTimeOverTimeTarget;
    WebTarget analyticsTarget; // unified endpoint — no sub-path

    @Inject
    FakeAnalyticsQueryService fakeAnalyticsQueryService;

    @Inject
    ApiCrudServiceInMemory apiCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/analytics";
    }

    @BeforeEach
    public void setup() {
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        fakeAnalyticsQueryService.reset();
        apiCrudServiceInMemory.reset();
    }

    @Nested
    class RequestsCountAnalytics {

        @BeforeEach
        public void prepareTarget() {
            requestsCountTarget = rootTarget().path("requests-count");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_ANALYTICS,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = requestsCountTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_requests_count() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            fakeAnalyticsQueryService.requestsCount =
                RequestsCount.builder().total(11L).countsByEntrypoint(Map.of("http-get", 10L, "sse", 1L)).build();

            final Response response = requestsCountTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsRequestsCountResponse.class)
                .satisfies(r -> {
                    assertThat(r.getTotal()).isEqualTo(11L);
                    assertThat(r.getCountsByEntrypoint()).containsAllEntriesOf(Map.of("http-get", 10, "sse", 1));
                });
        }
    }

    @Nested
    class AverageMessagesPerRequestAnalytics {

        @BeforeEach
        public void prepareTarget() {
            averageMessagesPerRequestTarget = rootTarget().path("average-messages-per-request");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_ANALYTICS,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = averageMessagesPerRequestTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_average_messages_per_request() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            fakeAnalyticsQueryService.averageMessagesPerRequest =
                AverageMessagesPerRequest
                    .builder()
                    .globalAverage(55.0)
                    .averagesByEntrypoint(Map.of("http-get", 10.0, "sse", 100.0))
                    .build();

            final Response response = averageMessagesPerRequestTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsAverageMessagesPerRequestResponse.class)
                .satisfies(r -> {
                    assertThat(r.getAverage()).isEqualTo(55.0);
                    assertThat(r.getAveragesByEntrypoint()).containsAllEntriesOf(Map.of("http-get", 10.0, "sse", 100.0));
                });
        }
    }

    @Nested
    class AverageConnectionDurationAnalytics {

        @BeforeEach
        public void prepareTarget() {
            averageConnectionDurationTarget = rootTarget().path("average-connection-duration");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_ANALYTICS,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = averageConnectionDurationTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_average_messages_per_request() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            fakeAnalyticsQueryService.averageConnectionDuration =
                AverageConnectionDuration
                    .builder()
                    .globalAverage(55.0)
                    .averagesByEntrypoint(Map.of("http-get", 10.0, "sse", 100.0))
                    .build();

            final Response response = averageConnectionDurationTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsAverageConnectionDurationResponse.class)
                .satisfies(r -> {
                    assertThat(r.getAverage()).isEqualTo(55.0);
                    assertThat(r.getAveragesByEntrypoint()).containsAllEntriesOf(Map.of("http-get", 10.0, "sse", 100.0));
                });
        }
    }

    @Nested
    class ResponseStatusRangesAnalytics {

        @BeforeEach
        public void prepareTarget() {
            statusCodesByEntrypointTarget = rootTarget().path("response-status-ranges");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_ANALYTICS,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = statusCodesByEntrypointTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_status_codes_by_entrypoint() {
            var FROM = 1728981738L;
            var TO = 1729068138L;
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            fakeAnalyticsQueryService.responseStatusRanges =
                ResponseStatusRanges
                    .builder()
                    .statusRangesCountByEntrypoint(Map.of("http-get", Map.of("100.0-200.0", 1L), "http-post", Map.of("100.0-200.0", 1L)))
                    .build();

            final Response response = statusCodesByEntrypointTarget.queryParam("from", FROM).queryParam("to", TO).request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsResponseStatusRangesResponse.class)
                .extracting(ApiAnalyticsResponseStatusRangesResponse::getRangesByEntrypoint)
                .isNotNull()
                .satisfies(rangesByEntrypoint -> {
                    assertThat(rangesByEntrypoint).hasSize(2);
                    assertThat(rangesByEntrypoint.keySet()).containsExactlyInAnyOrder("http-get", "http-post");
                });
        }
    }

    @Nested
    class GetResponseTimeOverTime {

        @BeforeEach
        public void prepareTarget() {
            responseTimeOverTimeTarget = rootTarget().path("response-time-over-time");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_ANALYTICS,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = responseTimeOverTimeTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_status_codes_by_entrypoint() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            // the order of keys is important
            fakeAnalyticsQueryService.averageAggregate = new LinkedHashMap<>();
            fakeAnalyticsQueryService.averageAggregate.put("1970-01-01T00:00:00", 1.2D);
            fakeAnalyticsQueryService.averageAggregate.put("1970-01-01T00:30:00", 1.6D);

            final Response response = responseTimeOverTimeTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsOverPeriodResponse.class)
                .extracting(ApiAnalyticsOverPeriodResponse::getData)
                .isNotNull()
                .satisfies(output -> {
                    assertThat(output).hasSize(2).containsExactly(1L, 2L);
                });
        }
    }

    /**
     * Tests for GET /v2/apis/{apiId}/analytics (unified endpoint — Story 1).
     *
     * <p>This class covers parameter validation (400) and permission enforcement (403).
     * Successful 200 paths with real data are added per-type in Stories 2–5.</p>
     */
    @Nested
    class GetApiAnalytics {

        @BeforeEach
        public void prepareTarget() {
            // The unified endpoint is at the analytics root — no sub-path
            analyticsTarget = rootTarget();
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_ANALYTICS,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = analyticsTarget.queryParam("type", "COUNT").request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_400_when_type_is_missing() {
            final Response response = analyticsTarget.request().get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_type_is_invalid() {
            final Response response = analyticsTarget.queryParam("type", "UNKNOWN").request().get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_field_is_missing_for_stats_type() {
            final Response response = analyticsTarget.queryParam("type", "STATS").request().get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_field_is_missing_for_group_by_type() {
            final Response response = analyticsTarget.queryParam("type", "GROUP_BY").request().get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_interval_is_missing_for_date_histo_type() {
            final Response response = analyticsTarget.queryParam("type", "DATE_HISTO").queryParam("field", "status").request().get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_200_with_count_response_for_count_type() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            fakeAnalyticsQueryService.requestsCount = RequestsCount.builder().total(1234L).build();

            final Response response = analyticsTarget.queryParam("type", "COUNT").request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsCountResponse.class)
                .satisfies(r -> {
                    assertThat(r.getType()).isEqualTo("COUNT");
                    assertThat(r.getCount()).isEqualTo(1234L);
                });
        }

        @Test
        void should_return_zero_count_when_no_analytics_data_exists() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            fakeAnalyticsQueryService.requestsCount = null; // simulates no docs in Elasticsearch

            final Response response = analyticsTarget.queryParam("type", "COUNT").request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsCountResponse.class)
                .satisfies(r -> assertThat(r.getCount()).isZero());
        }

        @Test
        void should_return_200_with_stats_response_for_stats_type() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            fakeAnalyticsQueryService.analyticsStats = new AnalyticsStats(50L, 1.0, 300.0, 75.5, 3775.0);

            final Response response = analyticsTarget
                .queryParam("type", "STATS")
                .queryParam("field", "gateway-response-time-ms")
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsStatsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getType()).isEqualTo("STATS");
                    assertThat(r.getCount()).isEqualTo(50L);
                    assertThat(r.getMin()).isEqualTo(1.0);
                    assertThat(r.getMax()).isEqualTo(300.0);
                    assertThat(r.getAvg()).isEqualTo(75.5);
                    assertThat(r.getSum()).isEqualTo(3775.0);
                });
        }

        @Test
        void should_return_200_with_empty_stats_when_service_returns_no_data() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            // fakeAnalyticsQueryService.analyticsStats is null by default → empty

            final Response response = analyticsTarget
                .queryParam("type", "STATS")
                .queryParam("field", "gateway-response-time-ms")
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsStatsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getType()).isEqualTo("STATS");
                    assertThat(r.getCount()).isZero();
                    assertThat(r.getMin()).isNull();
                    assertThat(r.getMax()).isNull();
                    assertThat(r.getAvg()).isNull();
                    assertThat(r.getSum()).isNull();
                });
        }

        @Test
        void should_return_200_with_group_by_response_for_group_by_type() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            fakeAnalyticsQueryService.analyticsGroupBy =
                new AnalyticsGroupBy(
                    java.util.Map.of("200", 1000L, "404", 50L),
                    java.util.Map.of("200", java.util.Map.of("name", "200"), "404", java.util.Map.of("name", "404"))
                );

            final Response response = analyticsTarget.queryParam("type", "GROUP_BY").queryParam("field", "status").request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsGroupByResponse.class)
                .satisfies(r -> {
                    assertThat(r.getType()).isEqualTo("GROUP_BY");
                    assertThat(r.getValues()).containsEntry("200", 1000L).containsEntry("404", 50L);
                    assertThat(r.getMetadata()).containsKey("200").containsKey("404");
                });
        }

        @Test
        void should_return_200_with_empty_group_by_when_service_returns_no_data() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            // fakeAnalyticsQueryService.analyticsGroupBy is null → empty

            final Response response = analyticsTarget.queryParam("type", "GROUP_BY").queryParam("field", "status").request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsGroupByResponse.class)
                .satisfies(r -> {
                    assertThat(r.getType()).isEqualTo("GROUP_BY");
                    assertThat(r.getValues()).isEmpty();
                    assertThat(r.getMetadata()).isEmpty();
                });
        }

        @Test
        void should_accept_lowercase_type_parameter() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENVIRONMENT).build()));

            final Response response = analyticsTarget.queryParam("type", "count").request().get();

            MAPIAssertions.assertThat(response).hasStatus(OK_200);
        }

        @Test
        void should_return_200_with_date_histo_response_for_date_histo_type() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            fakeAnalyticsQueryService.analyticsDateHisto =
                new AnalyticsDateHisto(
                    List.of(1697000000000L, 1697003600000L),
                    List.of(new AnalyticsDateHisto.Bucket("200", List.of(120L, 130L), Map.of("name", "200")))
                );

            final Response response = analyticsTarget
                .queryParam("type", "DATE_HISTO")
                .queryParam("field", "status")
                .queryParam("interval", "3600000")
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsDateHistoResponse.class)
                .satisfies(r -> {
                    assertThat(r.getType()).isEqualTo("DATE_HISTO");
                    assertThat(r.getTimestamp()).containsExactly(1697000000000L, 1697003600000L);
                    assertThat(r.getValues()).hasSize(1);
                    assertThat(r.getValues().get(0).getField()).isEqualTo("200");
                    assertThat(r.getValues().get(0).getBuckets()).containsExactly(120L, 130L);
                });
        }

        @Test
        void should_return_200_with_empty_date_histo_when_service_returns_no_data() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            // fakeAnalyticsQueryService.analyticsDateHisto is null → empty

            final Response response = analyticsTarget
                .queryParam("type", "DATE_HISTO")
                .queryParam("field", "status")
                .queryParam("interval", "3600000")
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsDateHistoResponse.class)
                .satisfies(r -> {
                    assertThat(r.getType()).isEqualTo("DATE_HISTO");
                    assertThat(r.getTimestamp()).isEmpty();
                    assertThat(r.getValues()).isEmpty();
                });
        }
    }

    @Nested
    class ResponseStatusOvertimeAnalytics {

        @BeforeEach
        public void prepareTarget() {
            statusCodesOvertimeTarget = rootTarget().path("response-status-overtime");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_ANALYTICS,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = statusCodesOvertimeTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_status_codes_overtime() {
            var expectedTimeRange = new ResponseStatusOvertime.TimeRange(
                Instant.now().minusSeconds(60),
                Instant.now(),
                Duration.ofMinutes(10)
            );
            var expectedData = Map.of("200", List.of(0L, 0L, 0L, 2L, 2L, 0L, 1L, 0L, 0L));
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            fakeAnalyticsQueryService.responseStatusOvertime =
                ResponseStatusOvertime.builder().timeRange(expectedTimeRange).data(expectedData).build();

            final Response response = statusCodesOvertimeTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiAnalyticsResponseStatusOvertimeResponse.class)
                .isNotNull()
                .satisfies(result -> {
                    SoftAssertions.assertSoftly(softly -> {
                        softly.assertThat(result.getData()).isEqualTo(expectedData);
                        softly
                            .assertThat(result.getTimeRange())
                            .extracting(
                                io.gravitee.rest.api.management.v2.rest.model.AnalyticTimeRange::getFrom,
                                io.gravitee.rest.api.management.v2.rest.model.AnalyticTimeRange::getTo,
                                io.gravitee.rest.api.management.v2.rest.model.AnalyticTimeRange::getInterval
                            )
                            .contains(
                                expectedTimeRange.from().toEpochMilli(),
                                expectedTimeRange.to().toEpochMilli(),
                                expectedTimeRange.interval().toMillis()
                            );
                    });
                });
        }
    }
}
