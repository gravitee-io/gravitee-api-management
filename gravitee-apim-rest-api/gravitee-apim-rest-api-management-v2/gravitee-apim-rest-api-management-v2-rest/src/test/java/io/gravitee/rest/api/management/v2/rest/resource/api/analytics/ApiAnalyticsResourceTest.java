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
import io.gravitee.apim.core.analytics.model.DateHistoResult;
import io.gravitee.apim.core.analytics.model.GroupByResult;
import io.gravitee.apim.core.analytics.model.StatsResult;
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
    WebTarget analyticsTarget;

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

    @Nested
    class UnifiedAnalytics {

        private static final long FROM = 1609459200000L;
        private static final long TO = 1609545600000L;

        @BeforeEach
        void prepareTarget() {
            analyticsTarget = rootTarget();
        }

        @Nested
        class Validation {

            @BeforeEach
            void initWithMessageApi() {
                apiCrudServiceInMemory.initWith(
                    List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build())
                );
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

                var response = analyticsTarget
                    .queryParam("type", "COUNT")
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
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
            void should_return_400_if_type_missing() {
                var response = analyticsTarget.queryParam("from", FROM).queryParam("to", TO).request().get();
                MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
            }

            @Test
            void should_return_400_if_from_missing() {
                var response = analyticsTarget.queryParam("type", "COUNT").queryParam("to", TO).request().get();
                MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
            }

            @Test
            void should_return_400_if_to_missing() {
                var response = analyticsTarget.queryParam("type", "COUNT").queryParam("from", FROM).request().get();
                MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
            }

            @Test
            void should_return_400_if_from_gte_to() {
                var response = analyticsTarget
                    .queryParam("type", "COUNT")
                    .queryParam("from", TO)
                    .queryParam("to", FROM)
                    .request()
                    .get();
                MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
            }

            @Test
            void should_return_400_if_field_missing_for_stats() {
                var response = analyticsTarget
                    .queryParam("type", "STATS")
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
                    .request()
                    .get();
                MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
            }

            @Test
            void should_return_400_if_field_missing_for_group_by() {
                var response = analyticsTarget
                    .queryParam("type", "GROUP_BY")
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
                    .request()
                    .get();
                MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
            }

            @Test
            void should_return_400_if_interval_missing_for_date_histo() {
                var response = analyticsTarget
                    .queryParam("type", "DATE_HISTO")
                    .queryParam("field", "status")
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
                    .request()
                    .get();
                MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
            }

            @Test
            void should_return_400_for_unsupported_field() {
                var response = analyticsTarget
                    .queryParam("type", "GROUP_BY")
                    .queryParam("field", "invalid-field")
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
                    .request()
                    .get();
                MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
            }
        }

        @Test
        void should_return_4xx_for_tcp_api() {
            apiCrudServiceInMemory.initWith(
                List.of(ApiFixtures.aTcpApiV4().toBuilder().id(API).environmentId(ENVIRONMENT).build())
            );

            var response = analyticsTarget
                .queryParam("type", "COUNT")
                .queryParam("from", FROM)
                .queryParam("to", TO)
                .request()
                .get();

            assertThat(response.getStatus()).isGreaterThanOrEqualTo(400).isLessThan(500);
        }

        @Nested
        class CountQuery {

            @BeforeEach
            void initWithMessageApi() {
                apiCrudServiceInMemory.initWith(
                    List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build())
                );
            }

            @Test
            void should_return_count() {
                fakeAnalyticsQueryService.countResult = 12345L;

                var response = analyticsTarget
                    .queryParam("type", "COUNT")
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(OK_200)
                    .asEntity(Map.class)
                    .satisfies(body -> {
                        assertThat(body.get("type")).isEqualTo("COUNT");
                        assertThat(body.get("count")).isEqualTo(12345);
                    });
            }

            @Test
            void should_return_zero_count_when_no_data() {
                fakeAnalyticsQueryService.countResult = null;

                var response = analyticsTarget
                    .queryParam("type", "COUNT")
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(OK_200)
                    .asEntity(Map.class)
                    .satisfies(body -> {
                        assertThat(body.get("type")).isEqualTo("COUNT");
                        assertThat(body.get("count")).isEqualTo(0);
                    });
            }
        }

        @Nested
        class StatsQuery {

            @BeforeEach
            void initWithMessageApi() {
                apiCrudServiceInMemory.initWith(
                    List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build())
                );
            }

            @Test
            void should_return_stats() {
                fakeAnalyticsQueryService.statsResult =
                    StatsResult.builder().count(50).min(10).max(200).avg(42.5).sum(2125).build();

                var response = analyticsTarget
                    .queryParam("type", "STATS")
                    .queryParam("field", "gateway-response-time-ms")
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(OK_200)
                    .asEntity(Map.class)
                    .satisfies(body -> {
                        assertThat(body.get("type")).isEqualTo("STATS");
                        assertThat(body.get("count")).isEqualTo(50);
                        assertThat(body.get("min")).isEqualTo(10);
                        assertThat(body.get("max")).isEqualTo(200);
                        assertThat(body.get("avg")).isEqualTo(42.5);
                        assertThat(body.get("sum")).isEqualTo(2125);
                    });
            }

            @Test
            void should_return_stats_with_zero_values_for_empty_range() {
                fakeAnalyticsQueryService.statsResult = null;

                var response = analyticsTarget
                    .queryParam("type", "STATS")
                    .queryParam("field", "gateway-response-time-ms")
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(OK_200)
                    .asEntity(Map.class)
                    .satisfies(body -> {
                        assertThat(body.get("type")).isEqualTo("STATS");
                        assertThat(body.get("count")).isEqualTo(0);
                        assertThat(body.get("min")).isEqualTo(0);
                        assertThat(body.get("max")).isEqualTo(0);
                        assertThat(body.get("avg")).isEqualTo(0);
                        assertThat(body.get("sum")).isEqualTo(0);
                    });
            }
        }

        @Nested
        class GroupByQuery {

            @BeforeEach
            void initWithMessageApi() {
                apiCrudServiceInMemory.initWith(
                    List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build())
                );
            }

            @Test
            void should_return_group_by() {
                fakeAnalyticsQueryService.groupByResult =
                    GroupByResult
                        .builder()
                        .values(Map.of("200", 100L, "404", 5L, "500", 2L))
                        .metadata(
                            Map.of(
                                "200",
                                Map.of("name", "200"),
                                "404",
                                Map.of("name", "404"),
                                "500",
                                Map.of("name", "500")
                            )
                        )
                        .build();

                var response = analyticsTarget
                    .queryParam("type", "GROUP_BY")
                    .queryParam("field", "status")
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(OK_200)
                    .asEntity(Map.class)
                    .satisfies(body -> {
                        assertThat(body.get("type")).isEqualTo("GROUP_BY");
                        assertThat(body.get("values")).isEqualTo(Map.of("200", 100, "404", 5, "500", 2));
                    });
            }

            @Test
            void should_return_empty_group_by_when_no_data() {
                fakeAnalyticsQueryService.groupByResult = null;

                var response = analyticsTarget
                    .queryParam("type", "GROUP_BY")
                    .queryParam("field", "status")
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(OK_200)
                    .asEntity(Map.class)
                    .satisfies(body -> {
                        assertThat(body.get("type")).isEqualTo("GROUP_BY");
                        assertThat(body.get("values")).isEqualTo(Map.of());
                    });
            }
        }

        @Nested
        class DateHistoQuery {

            @BeforeEach
            void initWithMessageApi() {
                apiCrudServiceInMemory.initWith(
                    List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build())
                );
            }

            @Test
            void should_return_date_histo() {
                fakeAnalyticsQueryService.dateHistoResult =
                    DateHistoResult
                        .builder()
                        .timestamps(List.of(1609459200000L, 1609462800000L))
                        .values(
                            List.of(
                                new DateHistoResult.DateHistoBucket(
                                    "200",
                                    List.of(100L, 150L),
                                    Map.of("name", "200")
                                ),
                                new DateHistoResult.DateHistoBucket(
                                    "404",
                                    List.of(5L, 2L),
                                    Map.of("name", "404")
                                )
                            )
                        )
                        .build();

                var response = analyticsTarget
                    .queryParam("type", "DATE_HISTO")
                    .queryParam("field", "status")
                    .queryParam("interval", 3600000)
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(OK_200)
                    .asEntity(Map.class)
                    .satisfies(body -> {
                        assertThat(body.get("type")).isEqualTo("DATE_HISTO");
                        assertThat(body.get("timestamps"))
                            .isEqualTo(List.of(1609459200000L, 1609462800000L));
                        @SuppressWarnings("unchecked")
                        var values = (List<Map<String, Object>>) body.get("values");
                        assertThat(values).hasSize(2);
                        assertThat(values.get(0).get("field")).isEqualTo("200");
                        assertThat(values.get(0).get("buckets")).isEqualTo(List.of(100, 150));
                        assertThat(values.get(1).get("field")).isEqualTo("404");
                        assertThat(values.get(1).get("buckets")).isEqualTo(List.of(5, 2));
                    });
            }

            @Test
            void should_return_empty_date_histo_when_no_data() {
                fakeAnalyticsQueryService.dateHistoResult = null;

                var response = analyticsTarget
                    .queryParam("type", "DATE_HISTO")
                    .queryParam("field", "status")
                    .queryParam("interval", 3600000)
                    .queryParam("from", FROM)
                    .queryParam("to", TO)
                    .request()
                    .get();

                MAPIAssertions
                    .assertThat(response)
                    .hasStatus(OK_200)
                    .asEntity(Map.class)
                    .satisfies(body -> {
                        assertThat(body.get("type")).isEqualTo("DATE_HISTO");
                        assertThat(body.get("timestamps")).isEqualTo(List.of());
                        assertThat(body.get("values")).isEqualTo(List.of());
                    });
            }
        }
    }
}
