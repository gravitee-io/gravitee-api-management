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
package io.gravitee.rest.api.management.v2.rest.resource.api.health;

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fakes.FakeApiHealthQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.api_health.model.AvailabilityHealthCheck;
import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTime;
import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTimeOvertime;
import io.gravitee.apim.core.api_health.model.HealthCheckLog;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.AnalyticTimeRange;
import io.gravitee.rest.api.management.v2.rest.model.ApiHealthAvailabilityResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiHealthAverageResponseTimeOvertimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiHealthAverageResponseTimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiHealthLogsResponse;
import io.gravitee.rest.api.management.v2.rest.model.HealthCheckLogRequest;
import io.gravitee.rest.api.management.v2.rest.model.HealthCheckLogStep;
import io.gravitee.rest.api.management.v2.rest.model.IntegrationsResponse;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiHealthResourceTest extends ApiResourceTest {

    private static final Instant INSTANT = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant FROM = INSTANT.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = INSTANT;
    private static final Duration INTERVAL = Duration.ofMinutes(10);

    WebTarget averageResponseTimeTarget;
    WebTarget averageResponseTimeOvertimeTarget;
    WebTarget healthCheckLogsTarget;

    WebTarget availabilityTarget;

    @Inject
    FakeApiHealthQueryService apiHealthQueryService;

    @Inject
    ApiCrudServiceInMemory apiCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/health";
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
        apiHealthQueryService.reset();
        apiCrudServiceInMemory.reset();
    }

    @Nested
    class AverageResponseTime {

        @BeforeEach
        public void setUp() {
            averageResponseTimeTarget = rootTarget().path("average-response-time");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_HEALTH,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = averageResponseTimeTarget.request().get();

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
            apiHealthQueryService.averageHealthCheckResponseTime = new AverageHealthCheckResponseTime(3L, Map.of("default", 3L));

            final Response response = averageResponseTimeTarget
                .queryParam("field", "endpoint")
                .queryParam("from", FROM.toEpochMilli())
                .queryParam("to", TO.toEpochMilli())
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiHealthAverageResponseTimeResponse.class)
                .satisfies(r -> {
                    assertThat(r.getGlobal()).isEqualTo(3L);
                    assertThat(r.getGroup()).containsAllEntriesOf(Map.of("default", 3L));
                });
        }
    }

    @Nested
    class AverageResponseTimeOvertime {

        @BeforeEach
        public void setUp() {
            averageResponseTimeOvertimeTarget = rootTarget().path("average-response-time-overtime");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_HEALTH,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = averageResponseTimeOvertimeTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_buckets() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            apiHealthQueryService.averageHealthCheckResponseTimeOvertime =
                new AverageHealthCheckResponseTimeOvertime(
                    new AverageHealthCheckResponseTimeOvertime.TimeRange(FROM, TO, INTERVAL),
                    List.of(3L)
                );

            final Response response = averageResponseTimeOvertimeTarget
                .queryParam("from", FROM.toEpochMilli())
                .queryParam("to", TO.toEpochMilli())
                .queryParam("interval", INTERVAL.toMillis())
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiHealthAverageResponseTimeOvertimeResponse.class)
                .satisfies(r -> {
                    assertThat(r.getTimeRange())
                        .isEqualTo(
                            AnalyticTimeRange
                                .builder()
                                .to(TO.toEpochMilli())
                                .from(FROM.toEpochMilli())
                                .interval(INTERVAL.toMillis())
                                .build()
                        );
                    assertThat(r.getData()).isEqualTo(List.of(3L));
                });
        }
    }

    @Nested
    class Availability {

        @BeforeEach
        public void setUp() {
            availabilityTarget = rootTarget().path("availability");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_HEALTH,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = availabilityTarget.request().get();

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
            apiHealthQueryService.availabilityHealthCheck = new AvailabilityHealthCheck(.75f, Map.of("default", .75f));

            final Response response = availabilityTarget
                .queryParam("field", "endpoint")
                .queryParam("from", FROM.toEpochMilli())
                .queryParam("to", TO.toEpochMilli())
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiHealthAvailabilityResponse.class)
                .satisfies(r -> {
                    assertThat(r.getGlobal()).isEqualTo(.75f);
                    assertThat(r.getGroup()).containsAllEntriesOf(Map.of("default", .75f));
                });
        }
    }

    @Nested
    class GetHealthCheckLogs {

        @BeforeEach
        public void setUp() {
            healthCheckLogsTarget = rootTarget().path("logs");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_HEALTH,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = healthCheckLogsTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_logs() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            apiHealthQueryService.healthCheckLogs =
                new Page<>(
                    List.of(
                        new HealthCheckLog(
                            "id",
                            INSTANT,
                            API,
                            "endpoint",
                            "gateway",
                            100L,
                            true,
                            List.of(
                                new HealthCheckLog.Step(
                                    "step1",
                                    true,
                                    "message",
                                    new HealthCheckLog.Request("uri", "GET", Map.of("key", "value")),
                                    new HealthCheckLog.Response(200, "body", Map.of("key", "value"))
                                ),
                                new HealthCheckLog.Step(
                                    "step2",
                                    true,
                                    "message",
                                    new HealthCheckLog.Request("uri", "GET", null),
                                    new HealthCheckLog.Response(200, "body", null)
                                )
                            )
                        )
                    ),
                    1,
                    5,
                    1
                );

            final Response response = healthCheckLogsTarget
                .queryParam("from", FROM.toEpochMilli())
                .queryParam("to", TO.toEpochMilli())
                .queryParam("success", false)
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiHealthLogsResponse.class)
                .extracting(ApiHealthLogsResponse::getData)
                .isEqualTo(
                    List.of(
                        new io.gravitee.rest.api.management.v2.rest.model.HealthCheckLog()
                            .id("id")
                            .timestamp(INSTANT.atOffset(ZoneOffset.UTC))
                            .endpointName("endpoint")
                            .gatewayId("gateway")
                            .responseTime(100L)
                            .success(true)
                            .steps(
                                List.of(
                                    new HealthCheckLogStep()
                                        .name("step1")
                                        .success(true)
                                        .message("message")
                                        .request(
                                            new io.gravitee.rest.api.management.v2.rest.model.HealthCheckLogRequest()
                                                .uri("uri")
                                                .method("GET")
                                                .headers(Map.of("key", "value"))
                                        )
                                        .response(
                                            new io.gravitee.rest.api.management.v2.rest.model.HealthCheckLogResponse()
                                                .status(200)
                                                .body("body")
                                                .headers(Map.of("key", "value"))
                                        ),
                                    new HealthCheckLogStep()
                                        .name("step2")
                                        .success(true)
                                        .message("message")
                                        .request(
                                            new io.gravitee.rest.api.management.v2.rest.model.HealthCheckLogRequest()
                                                .uri("uri")
                                                .method("GET")
                                                .headers(Map.of())
                                        )
                                        .response(
                                            new io.gravitee.rest.api.management.v2.rest.model.HealthCheckLogResponse()
                                                .status(200)
                                                .body("body")
                                                .headers(Map.of())
                                        )
                                )
                            )
                    )
                );
        }

        @Test
        public void should_use_default_pagination_when_no_pagination_param() {
            // Given
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            apiHealthQueryService.healthCheckLogs =
                new Page<>(
                    List.of(
                        new HealthCheckLog(
                            "id",
                            INSTANT,
                            API,
                            "endpoint",
                            "gateway",
                            100L,
                            true,
                            List.of(
                                new HealthCheckLog.Step(
                                    "step1",
                                    true,
                                    "message",
                                    new HealthCheckLog.Request("uri", "GET", Map.of("key", "value")),
                                    new HealthCheckLog.Response(200, "body", Map.of("key", "value"))
                                ),
                                new HealthCheckLog.Step(
                                    "step2",
                                    true,
                                    "message",
                                    new HealthCheckLog.Request("uri", "GET", null),
                                    new HealthCheckLog.Response(200, "body", null)
                                )
                            )
                        )
                    ),
                    1,
                    5,
                    1
                );

            // When
            Response response = healthCheckLogsTarget
                .queryParam("from", FROM.toEpochMilli())
                .queryParam("to", TO.toEpochMilli())
                .request()
                .get();

            // Then
            MAPIAssertions
                .assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(ApiHealthLogsResponse.class)
                .extracting(ApiHealthLogsResponse::getPagination)
                .isEqualTo(Pagination.builder().page(1).perPage(10).pageItemsCount(1).pageCount(1).totalCount(1L).build());
        }

        @Test
        public void should_compute_links() {
            // Given
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));
            apiHealthQueryService.healthCheckLogs =
                new Page<>(
                    List.of(
                        new HealthCheckLog(
                            "id",
                            INSTANT,
                            API,
                            "endpoint",
                            "gateway",
                            100L,
                            true,
                            List.of(
                                new HealthCheckLog.Step(
                                    "step1",
                                    true,
                                    "message",
                                    new HealthCheckLog.Request("uri", "GET", Map.of("key", "value")),
                                    new HealthCheckLog.Response(200, "body", Map.of("key", "value"))
                                ),
                                new HealthCheckLog.Step(
                                    "step2",
                                    true,
                                    "message",
                                    new HealthCheckLog.Request("uri", "GET", null),
                                    new HealthCheckLog.Response(200, "body", null)
                                )
                            )
                        )
                    ),
                    2,
                    5,
                    15
                );

            // When
            Response response = healthCheckLogsTarget
                .queryParam("from", FROM.toEpochMilli())
                .queryParam("to", TO.toEpochMilli())
                .queryParam("page", "2")
                .queryParam("perPage", 5)
                .request()
                .get();

            // Then
            var target = healthCheckLogsTarget.queryParam("from", FROM.toEpochMilli()).queryParam("to", TO.toEpochMilli());
            MAPIAssertions
                .assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(ApiHealthLogsResponse.class)
                .extracting(ApiHealthLogsResponse::getLinks)
                .isEqualTo(
                    Links
                        .builder()
                        .self(target.queryParam("page", 2).queryParam("perPage", 5).getUri().toString())
                        .first(target.queryParam("page", 1).queryParam("perPage", 5).getUri().toString())
                        .last(target.queryParam("page", 3).queryParam("perPage", 5).getUri().toString())
                        .previous(target.queryParam("page", 1).queryParam("perPage", 5).getUri().toString())
                        .next(target.queryParam("page", 3).queryParam("perPage", 5).getUri().toString())
                        .build()
                );
        }

        @Test
        void should_return_empty_response_when_no_logs() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENVIRONMENT).build()));

            final Response response = healthCheckLogsTarget
                .queryParam("from", FROM.toEpochMilli())
                .queryParam("to", TO.toEpochMilli())
                .request()
                .get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiHealthLogsResponse.class)
                .extracting(ApiHealthLogsResponse::getData, ApiHealthLogsResponse::getPagination, ApiHealthLogsResponse::getLinks)
                .contains(
                    List.of(),
                    Pagination.builder().build(),
                    Links
                        .builder()
                        .self(
                            healthCheckLogsTarget
                                .queryParam("from", FROM.toEpochMilli())
                                .queryParam("to", TO.toEpochMilli())
                                .getUri()
                                .toString()
                        )
                        .build()
                );
        }
    }
}
