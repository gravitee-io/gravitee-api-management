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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics.model.Bucket;
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
                .satisfies(output -> assertThat(output).hasSize(2).containsExactly(1L, 2L));
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
                .satisfies(result ->
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
                    })
                );
        }
    }

    @Nested
    class PerformApiAnalytics {

        @Nested
        class HistogramAnalytics {

            @Test
            void should_return_histogram_analytics_response() {
                // Arrange
                apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
                // Simulate a histogram analytics result
                var expectedTimestamp = new io.gravitee.apim.core.analytics.model.Timestamp(
                    Instant.now().minusSeconds(60),
                    Instant.now(),
                    Duration.ofMinutes(10)
                );
                var expectedBuckets = List.of(
                    io.gravitee.apim.core.analytics.model.Bucket
                        .builder()
                        .name("by_status")
                        .field("status")
                        .buckets(
                            List.of(
                                new Bucket(null, "status", "200", List.of(0L, 0L, 1L), null),
                                new Bucket(null, "status", "404", List.of(0L, 2L, 0L), null)
                            )
                        )
                        .build()
                );
                fakeAnalyticsQueryService.histogramAnalytics =
                    io.gravitee.apim.core.analytics.model.HistogramAnalytics
                        .builder()
                        .timestamp(expectedTimestamp)
                        .values(expectedBuckets)
                        .build();

                var response = rootTarget()
                    .queryParam("type", "HISTOGRAM")
                    .queryParam("from", expectedTimestamp.getFrom().toEpochMilli())
                    .queryParam("to", expectedTimestamp.getTo().toEpochMilli())
                    .queryParam("interval", expectedTimestamp.getInterval().toMillis())
                    .queryParam("aggregations", "FIELD:status")
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(OK_200)
                    .asEntity(io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponse.class)
                    .satisfies(result -> {
                        var histogram = result.getHistogramAnalytics();
                        assertThat(histogram).isNotNull();
                        assertThat(histogram.getTimestamp()).isNotNull();
                        assertThat(histogram.getTimestamp().getFrom()).isEqualTo(expectedTimestamp.getFrom().toEpochMilli());
                        assertThat(histogram.getTimestamp().getTo()).isEqualTo(expectedTimestamp.getTo().toEpochMilli());
                        assertThat(histogram.getTimestamp().getInterval()).isEqualTo(expectedTimestamp.getInterval().toMillis());
                        assertThat(histogram.getValues()).hasSize(1);
                        var bucket = histogram.getValues().getFirst();
                        assertThat(bucket.getName()).isEqualTo("by_status");
                        assertThat(bucket.getField()).isEqualTo("status");
                        assertThat(bucket.getBuckets()).size().isEqualTo(2);
                    });
            }

            @Test
            void should_return_histogram_analytics_response_for_avg_gateway_response_time_ms() {
                apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
                var expectedTimestamp = new io.gravitee.apim.core.analytics.model.Timestamp(
                    Instant.now().minusSeconds(60),
                    Instant.now(),
                    Duration.ofMinutes(10)
                );
                var expectedBuckets = List.of(
                    io.gravitee.apim.core.analytics.model.Bucket
                        .builder()
                        .name("avg_gateway-response-time-ms")
                        .field("gateway-response-time-ms")
                        .buckets(
                            List.of(
                                new Bucket(null, "gateway-response-time-ms", "avg_gateway-response-time-ms", List.of(120.5, 110.0), null)
                            )
                        )
                        .build()
                );
                fakeAnalyticsQueryService.histogramAnalytics =
                    io.gravitee.apim.core.analytics.model.HistogramAnalytics
                        .builder()
                        .timestamp(expectedTimestamp)
                        .values(expectedBuckets)
                        .build();

                var response = rootTarget()
                    .queryParam("type", "HISTOGRAM")
                    .queryParam("from", expectedTimestamp.getFrom().toEpochMilli())
                    .queryParam("to", expectedTimestamp.getTo().toEpochMilli())
                    .queryParam("interval", expectedTimestamp.getInterval().toMillis())
                    .queryParam("aggregations", "AVG:gateway-response-time-ms")
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(OK_200)
                    .asEntity(io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponse.class)
                    .satisfies(result -> {
                        var histogram = result.getHistogramAnalytics();
                        assertThat(histogram).isNotNull();
                        assertThat(histogram.getTimestamp()).isNotNull();
                        assertThat(histogram.getTimestamp().getFrom()).isEqualTo(expectedTimestamp.getFrom().toEpochMilli());
                        assertThat(histogram.getTimestamp().getTo()).isEqualTo(expectedTimestamp.getTo().toEpochMilli());
                        assertThat(histogram.getTimestamp().getInterval()).isEqualTo(expectedTimestamp.getInterval().toMillis());
                        assertThat(histogram.getValues()).hasSize(1);
                        var bucket = histogram.getValues().getFirst();
                        assertThat(bucket.getName()).isEqualTo("avg_gateway-response-time-ms");
                        assertThat(bucket.getField()).isEqualTo("gateway-response-time-ms");
                        assertThat(bucket.getBuckets()).hasSize(1);
                        var avgBucket = bucket.getBuckets().getFirst();
                        assertThat(avgBucket.getName()).isEqualTo("avg_gateway-response-time-ms");
                        assertThat(avgBucket.getData()).containsExactly(120.5, 110.0);
                    });
            }

            @Test
            void should_return_bad_request_when_aggregation_type_is_invalid() {
                apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
                var expectedTimestamp = new io.gravitee.apim.core.analytics.model.Timestamp(
                    Instant.now().minusSeconds(60),
                    Instant.now(),
                    Duration.ofMinutes(10)
                );
                var response = rootTarget()
                    .queryParam("type", "HISTOGRAM")
                    .queryParam("from", expectedTimestamp.getFrom().toEpochMilli())
                    .queryParam("to", expectedTimestamp.getTo().toEpochMilli())
                    .queryParam("interval", expectedTimestamp.getInterval().toMillis())
                    .queryParam("aggregations", "INVALID:status")
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(400)
                    .asError()
                    .hasHttpStatus(400)
                    .hasMessage("Invalid aggregation type: INVALID");
            }
        }

        @Nested
        class GroupByAnalytics {

            @Test
            void should_return_group_by_analytics_response() {
                apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));

                var expectedTimestamp = new io.gravitee.apim.core.analytics.model.Timestamp(
                    Instant.now().minusSeconds(60),
                    Instant.now(),
                    Duration.ofMinutes(10)
                );
                String ranges = "100:199;200:299;300:399;400:499;500:599";

                var expectedAnalytics = io.gravitee.apim.core.analytics.model.GroupByAnalytics
                    .builder()
                    .values(Map.of("100:199", 0L, "200:299", 5L, "300:399", 0L, "400:499", 1L, "500:599", 0L))
                    .build();

                var expectedMetadata = Map.of(
                    "100:199",
                    Map.of("name", "100:199"),
                    "200:299",
                    Map.of("name", "200:299"),
                    "300:399",
                    Map.of("name", "300:399"),
                    "400:499",
                    Map.of("name", "400:499"),
                    "500:599",
                    Map.of("name", "500:599")
                );

                fakeAnalyticsQueryService.groupByAnalytics = expectedAnalytics;

                var response = rootTarget()
                    .queryParam("type", "GROUP_BY")
                    .queryParam("field", "status")
                    .queryParam("ranges", ranges)
                    .queryParam("interval", expectedTimestamp.getInterval().toMillis())
                    .queryParam("from", expectedTimestamp.getFrom().toEpochMilli())
                    .queryParam("to", expectedTimestamp.getTo().toEpochMilli())
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(OK_200)
                    .asEntity(io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponse.class)
                    .satisfies(result -> {
                        var groupBy = result.getGroupByAnalytics();
                        assertThat(groupBy).isNotNull();
                        assertThat(groupBy.getAnalyticsType())
                            .isEqualTo(io.gravitee.rest.api.management.v2.rest.model.AnalyticsType.GROUP_BY);
                        assertThat(groupBy.getValues()).hasSize(5);
                        assertThat(groupBy.getValues()).containsEntry("200:299", 5L);
                        assertThat(groupBy.getValues()).containsEntry("400:499", 1L);
                        assertThat(groupBy.getMetadata()).isEqualTo(expectedMetadata);
                    });
            }
        }

        @Nested
        class StatsAnalytics {

            @Test
            void should_return_stats_analytics_response() {
                apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
                fakeAnalyticsQueryService.statsAnalytics = new io.gravitee.apim.core.analytics.model.StatsAnalytics(1f, 2f, 3f, 4f, 5f);

                var response = rootTarget()
                    .queryParam("type", "STATS")
                    .queryParam("field", "response-time")
                    .queryParam("from", 1000L)
                    .queryParam("to", 2000L)
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(OK_200)
                    .asEntity(io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponse.class)
                    .satisfies(result -> {
                        var stats = result.getStatsAnalytics();
                        assertThat(stats).isNotNull();
                        assertThat(stats.getAvg()).isEqualTo(1f);
                        assertThat(stats.getMin()).isEqualTo(2f);
                        assertThat(stats.getMax()).isEqualTo(3f);
                        assertThat(stats.getSum()).isEqualTo(4f);
                        assertThat(stats.getCount()).isEqualTo(5f);
                    });
            }

            @Test
            void should_return_not_found_when_no_data() {
                apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
                fakeAnalyticsQueryService.statsAnalytics = null;

                var response = rootTarget()
                    .queryParam("type", "STATS")
                    .queryParam("field", "response-time")
                    .queryParam("from", 1000L)
                    .queryParam("to", 2000L)
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(404)
                    .asError()
                    .hasHttpStatus(404)
                    .hasMessage("No stats analytics found for api: " + API);
            }
        }
    }
}
