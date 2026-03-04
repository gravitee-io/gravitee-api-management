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
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsAverageConnectionDurationResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsAverageMessagesPerRequestResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsOverPeriodResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsRequestsCountResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponseStatusOvertimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponseStatusRangesResponse;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.analytics.AverageConnectionDuration;
import io.gravitee.rest.api.model.v4.analytics.AverageMessagesPerRequest;
import io.gravitee.rest.api.model.v4.analytics.RequestsCount;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.model.v4.analytics.V4AnalyticsCount;
import io.gravitee.rest.api.model.v4.analytics.V4AnalyticsDateHisto;
import io.gravitee.rest.api.model.v4.analytics.V4AnalyticsGroupBy;
import io.gravitee.rest.api.model.v4.analytics.V4AnalyticsStats;
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

    /**
     * Tests for the unified V4 analytics endpoint GET .../analytics?type=COUNT&from=&to=
     * (Story 1 – unified endpoint and COUNT).
     */
    @Nested
    class UnifiedV4Analytics {

        WebTarget unifiedTarget;

        @BeforeEach
        void prepareTarget() {
            unifiedTarget = rootTarget();
        }

        @Test
        void should_return_403_when_no_API_ANALYTICS_READ() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_ANALYTICS,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = unifiedTarget
                .queryParam("type", "COUNT")
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_COUNT_when_type_is_COUNT() {
            apiCrudServiceInMemory.initWith(
                List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build())
            );
            fakeAnalyticsQueryService.v4AnalyticsCount = V4AnalyticsCount.builder().count(42L).build();

            final Response response = unifiedTarget
                .queryParam("type", "COUNT")
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(V4AnalyticsCountResponse.class)
                .satisfies(r -> {
                    assertThat(r.type).isEqualTo("COUNT");
                    assertThat(r.count).isEqualTo(42L);
                });
        }

        @Test
        void should_return_COUNT_zero_when_no_data() {
            apiCrudServiceInMemory.initWith(
                List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build())
            );
            fakeAnalyticsQueryService.v4AnalyticsCount = null;

            final Response response = unifiedTarget
                .queryParam("type", "COUNT")
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(V4AnalyticsCountResponse.class)
                .satisfies(r -> {
                    assertThat(r.type).isEqualTo("COUNT");
                    assertThat(r.count).isEqualTo(0L);
                });
        }

        @Test
        void should_return_400_when_type_missing() {
            final Response response = unifiedTarget
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_from_to_missing() {
            final Response response = unifiedTarget.queryParam("type", "COUNT").request().get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_from_gte_to() {
            final Response response = unifiedTarget
                .queryParam("type", "COUNT")
                .queryParam("from", 2000L)
                .queryParam("to", 1000L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_type_invalid() {
            final Response response = unifiedTarget
                .queryParam("type", "INVALID")
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_STATS_when_type_is_STATS() {
            apiCrudServiceInMemory.initWith(
                List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build())
            );
            fakeAnalyticsQueryService.v4AnalyticsStats =
                V4AnalyticsStats.builder().count(10L).min(1.0).max(100.0).avg(50.5).sum(505.0).build();

            final Response response = unifiedTarget
                .queryParam("type", "STATS")
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .queryParam("field", "gateway-response-time-ms")
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(V4AnalyticsStatsResponse.class)
                .satisfies(r -> {
                    assertThat(r.type).isEqualTo("STATS");
                    assertThat(r.count).isEqualTo(10L);
                    assertThat(r.min).isEqualTo(1.0);
                    assertThat(r.max).isEqualTo(100.0);
                    assertThat(r.avg).isEqualTo(50.5);
                    assertThat(r.sum).isEqualTo(505.0);
                });
        }

        @Test
        void should_return_400_when_STATS_without_field() {
            final Response response = unifiedTarget
                .queryParam("type", "STATS")
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_GROUP_BY_when_type_is_GROUP_BY() {
            apiCrudServiceInMemory.initWith(
                List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build())
            );
            fakeAnalyticsQueryService.v4AnalyticsGroupBy =
                V4AnalyticsGroupBy.builder().values(Map.of("200", 80L, "404", 10L)).metadata(Map.of()).build();

            final Response response = unifiedTarget
                .queryParam("type", "GROUP_BY")
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .queryParam("field", "status")
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(V4AnalyticsGroupByResponse.class)
                .satisfies(r -> {
                    assertThat(r.type).isEqualTo("GROUP_BY");
                    assertThat(r.values).containsAllEntriesOf(Map.of("200", 80L, "404", 10L));
                });
        }

        @Test
        void should_return_400_when_GROUP_BY_without_field() {
            final Response response = unifiedTarget
                .queryParam("type", "GROUP_BY")
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_DATE_HISTO_when_type_is_DATE_HISTO() {
            apiCrudServiceInMemory.initWith(
                List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build())
            );
            fakeAnalyticsQueryService.v4AnalyticsDateHisto =
                V4AnalyticsDateHisto
                    .builder()
                    .timestamp(List.of(1000L, 2000L))
                    .values(
                        List.of(
                            V4AnalyticsDateHisto.DateHistoValue
                                .builder()
                                .field("status")
                                .buckets(List.of(5L, 10L))
                                .metadata(Map.of())
                                .build()
                        )
                    )
                    .build();

            final Response response = unifiedTarget
                .queryParam("type", "DATE_HISTO")
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .queryParam("field", "status")
                .queryParam("interval", 3600000L)
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(V4AnalyticsDateHistoResponse.class)
                .satisfies(r -> {
                    assertThat(r.type).isEqualTo("DATE_HISTO");
                    assertThat(r.timestamp).containsExactly(1000L, 2000L);
                    assertThat(r.values).hasSize(1);
                    assertThat(r.values.get(0).field).isEqualTo("status");
                    assertThat(r.values.get(0).buckets).containsExactly(5L, 10L);
                });
        }

        @Test
        void should_return_400_when_DATE_HISTO_without_field() {
            final Response response = unifiedTarget
                .queryParam("type", "DATE_HISTO")
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .queryParam("interval", 3600000L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_DATE_HISTO_without_interval() {
            final Response response = unifiedTarget
                .queryParam("type", "DATE_HISTO")
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .queryParam("field", "status")
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_DATE_HISTO_interval_out_of_range() {
            final Response response = unifiedTarget
                .queryParam("type", "DATE_HISTO")
                .queryParam("from", 1000L)
                .queryParam("to", 2000L)
                .queryParam("field", "status")
                .queryParam("interval", 500L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        /** DTO for unified analytics COUNT response JSON. */
        @SuppressWarnings("unused")
        public static class V4AnalyticsCountResponse {

            public String type;
            public Long count;
        }

        /** DTO for unified analytics STATS response JSON. */
        @SuppressWarnings("unused")
        public static class V4AnalyticsStatsResponse {

            public String type;
            public Long count;
            public Double min;
            public Double max;
            public Double avg;
            public Double sum;
        }

        /** DTO for unified analytics GROUP_BY response JSON. */
        @SuppressWarnings("unused")
        public static class V4AnalyticsGroupByResponse {

            public String type;
            public Map<String, Long> values;
            public Map<String, Map<String, Object>> metadata;
        }

        /** DTO for unified analytics DATE_HISTO response JSON. */
        @SuppressWarnings("unused")
        public static class V4AnalyticsDateHistoResponse {

            public String type;
            public List<Long> timestamp;
            public List<DateHistoValueResponse> values;
        }

        @SuppressWarnings("unused")
        public static class DateHistoValueResponse {

            public String field;
            public List<Long> buckets;
            public Map<String, Object> metadata;
        }
    }
}
