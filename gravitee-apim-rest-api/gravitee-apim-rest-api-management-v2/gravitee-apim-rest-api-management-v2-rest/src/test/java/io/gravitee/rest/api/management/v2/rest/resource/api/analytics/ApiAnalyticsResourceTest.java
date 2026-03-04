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
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
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
import io.gravitee.rest.api.model.v4.analytics.Stats;
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
    class UnifiedApiAnalytics {

        @BeforeEach
        void prepareTarget() {
            analyticsTarget = rootTarget();
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
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

            final Response response = analyticsTarget
                .queryParam("type", "COUNT")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
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
        void should_return_empty_count_contract() {
            final Response response = analyticsTarget
                .queryParam("type", "COUNT")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(Map.class)
                .satisfies(body -> {
                    assertThat(body).containsEntry("type", "COUNT");
                    assertThat(body.get("count")).isEqualTo(0);
                });
        }

        @Test
        void should_return_count_contract_with_data() {
            fakeAnalyticsQueryService.requestsCount = RequestsCount.builder().total(42L).build();

            final Response response = analyticsTarget
                .queryParam("type", "COUNT")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(Map.class)
                .satisfies(body -> {
                    assertThat(body).containsEntry("type", "COUNT");
                    assertThat(body.get("count")).isEqualTo(42);
                });
        }

        @Test
        void should_return_empty_stats_contract() {
            final Response response = analyticsTarget
                .queryParam("type", "STATS")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .queryParam("field", "status")
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(Map.class)
                .satisfies(body -> {
                    assertThat(body).containsEntry("type", "STATS");
                    assertThat(body).containsKeys("count", "min", "max", "avg", "sum");
                    assertThat(body.get("count")).isEqualTo(0);
                });
        }

        @Test
        void should_return_stats_contract_with_data() {
            fakeAnalyticsQueryService.stats = Stats.builder().count(12345L).min(2D).max(1500D).avg(125.5D).sum(1549567D).build();

            final Response response = analyticsTarget
                .queryParam("type", "STATS")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .queryParam("field", "status")
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(Map.class)
                .satisfies(body -> {
                    assertThat(body).containsEntry("type", "STATS");
                    assertThat(body.get("count")).isEqualTo(12345);
                    assertThat(body.get("min")).isEqualTo(2.0);
                    assertThat(body.get("max")).isEqualTo(1500.0);
                    assertThat(body.get("avg")).isEqualTo(125.5);
                    assertThat(body.get("sum")).isEqualTo(1549567.0);
                });
        }

        @Test
        void should_return_empty_group_by_contract() {
            final Response response = analyticsTarget
                .queryParam("type", "GROUP_BY")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .queryParam("field", "status")
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(Map.class)
                .satisfies(body -> {
                    assertThat(body).containsEntry("type", "GROUP_BY");
                    assertThat((Map<?, ?>) body.get("values")).isEmpty();
                    assertThat((Map<?, ?>) body.get("metadata")).isEmpty();
                });
        }

        @Test
        void should_return_empty_date_histo_contract() {
            final Response response = analyticsTarget
                .queryParam("type", "DATE_HISTO")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .queryParam("interval", 1000L)
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(Map.class)
                .satisfies(body -> {
                    assertThat(body).containsEntry("type", "DATE_HISTO");
                    assertThat((List<?>) body.get("timestamp")).isEmpty();
                    assertThat((List<?>) body.get("values")).isEmpty();
                });
        }

        @Test
        void should_return_400_if_type_is_missing() {
            final Response response = analyticsTarget.queryParam("from", 0L).queryParam("to", 1L).request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Validation error");
        }

        @Test
        void should_return_400_if_from_is_after_to() {
            final Response response = analyticsTarget
                .queryParam("type", "COUNT")
                .queryParam("from", 2L)
                .queryParam("to", 1L)
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Validation error");
        }

        @Test
        void should_return_400_if_stats_field_is_missing() {
            final Response response = analyticsTarget
                .queryParam("type", "STATS")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_if_stats_field_is_not_supported() {
            final Response response = analyticsTarget
                .queryParam("type", "STATS")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .queryParam("field", "unknown-field")
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessage("Unsupported stats field");
        }

        @Test
        void should_return_400_if_group_by_field_is_missing() {
            final Response response = analyticsTarget
                .queryParam("type", "GROUP_BY")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_if_date_histo_interval_is_missing() {
            final Response response = analyticsTarget
                .queryParam("type", "DATE_HISTO")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_if_period_is_too_large() {
            final Response response = analyticsTarget
                .queryParam("type", "COUNT")
                .queryParam("from", 0L)
                .queryParam("to", 31_536_000_001L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_404_if_api_is_not_found() {
            apiCrudServiceInMemory.reset();

            final Response response = analyticsTarget
                .queryParam("type", "COUNT")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Api not found.");
        }

        @Test
        void should_return_400_if_api_definition_is_not_v4() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2().toBuilder().environmentId(ENVIRONMENT).build()));

            final Response response = analyticsTarget
                .queryParam("type", "COUNT")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_if_api_is_tcp_proxy() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aTcpApiV4().toBuilder().environmentId(ENVIRONMENT).build()));

            final Response response = analyticsTarget
                .queryParam("type", "COUNT")
                .queryParam("from", 0L)
                .queryParam("to", 1L)
                .request()
                .get();

            MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }
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
}
