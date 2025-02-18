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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.Mockito.when;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ApplicationQueryServiceInMemory;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.rest.api.management.v2.rest.model.AnalyticTimeRange;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsOverPeriodResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsRequestResponseTimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsResponseStatusOvertimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsResponseStatusRangesResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsTopAppsByRequestCountResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsTopHitsApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.TopApp;
import io.gravitee.rest.api.management.v2.rest.model.TopHitApi;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.analytics.TopHitsApps;
import io.gravitee.rest.api.model.v4.analytics.RequestResponseTime;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.model.v4.analytics.TopHitsApis;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EnvironmentAnalyticsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final long FROM = 1728981738L;
    private static final long TO = 1729068138L;

    @Inject
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();

    @Inject
    private final ApplicationQueryServiceInMemory applicationQueryService = new ApplicationQueryServiceInMemory();

    @Inject
    FakeAnalyticsQueryService analyticsQueryService;

    WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/analytics";
    }

    @BeforeEach
    void setup() {
        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    void teardown() {
        analyticsQueryService.reset();
        apiQueryService.reset();
    }

    @Nested
    class StatusRanges {

        @BeforeEach
        void setup() {
            target = rootTarget().path("response-status-ranges");
        }

        @Test
        public void should_return_200_with_valid_status_ranges() {
            //Given
            var proxyApiV4 = ApiFixtures.aProxyApiV4();
            var messageApiV4 = ApiFixtures.aMessageApiV4();

            apiQueryService.initWith(List.of(proxyApiV4, messageApiV4));
            analyticsQueryService.responseStatusRanges =
                ResponseStatusRanges
                    .builder()
                    .ranges(Map.of("100.0-200.0", 1L, "200.0-300.0", 17L, "300.0-400.0", 0L, "400.0-500.0", 0L, "500.0-600.0", 0L))
                    .build();

            //When

            Response response = target.queryParam("from", FROM).queryParam("to", TO).request().get();

            //Then

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsResponseStatusRangesResponse.class)
                .isEqualTo(
                    EnvironmentAnalyticsResponseStatusRangesResponse
                        .builder()
                        .ranges(Map.of("100.0-200.0", 1, "200.0-300.0", 17, "300.0-400.0", 0, "400.0-500.0", 0, "500.0-600.0", 0))
                        .build()
                );
        }
    }

    @Nested
    class TopHits {

        @BeforeEach
        void setup() {
            target = rootTarget().path("top-hits");
        }

        @Test
        void should_return_200_with_valid_top_hits() {
            //Given
            var topHitApi1Id = "top-hit-api-1";
            var topHitApi2Id = "top-hit-api-2";
            var proxyApiV4 = ApiFixtures.aProxyApiV4().toBuilder().id(topHitApi1Id).name("Top Hit API 1").build();
            var messageApiV4 = ApiFixtures.aMessageApiV4().toBuilder().id(topHitApi2Id).name("Top Hit API 2").build();

            apiQueryService.initWith(List.of(proxyApiV4, messageApiV4));
            analyticsQueryService.topHitsApis =
                TopHitsApis
                    .builder()
                    .data(
                        List.of(
                            TopHitsApis.TopHitApi.builder().id(topHitApi1Id).count(7L).build(),
                            TopHitsApis.TopHitApi.builder().id(topHitApi2Id).count(13L).build()
                        )
                    )
                    .build();

            //When

            Response response = target.queryParam("from", FROM).queryParam("to", TO).request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsTopHitsApisResponse.class)
                .isEqualTo(
                    EnvironmentAnalyticsTopHitsApisResponse
                        .builder()
                        .data(
                            List.of(
                                TopHitApi.builder().id(topHitApi2Id).name("Top Hit API 2").count(13L).build(),
                                TopHitApi.builder().id(topHitApi1Id).name("Top Hit API 1").count(7L).build()
                            )
                        )
                        .build()
                );
        }

        @Test
        void should_return_200_with_empty_list_if_no_apis_found() {
            apiQueryService.initWith(List.of());
            analyticsQueryService.topHitsApis = TopHitsApis.builder().data(List.of()).build();

            //When

            Response response = target.queryParam("from", FROM).queryParam("to", TO).request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsTopHitsApisResponse.class)
                .isEqualTo(EnvironmentAnalyticsTopHitsApisResponse.builder().data(List.of()).build());
        }
    }

    @Nested
    class RequestResponseTimeAnalytics {

        @BeforeEach
        void setup() {
            target = rootTarget().path("request-response-time");
        }

        @Test
        void should_return_200_with_valid_request_response_time_analytics() {
            //Given
            var topHitApi1Id = "request-response-time-api";
            var apiV4 = ApiFixtures.aProxyApiV4().toBuilder().id(topHitApi1Id).name("Request Response Time API").build();

            apiQueryService.initWith(List.of(apiV4));
            analyticsQueryService.requestResponseTime =
                RequestResponseTime
                    .builder()
                    .requestsPerSecond(3.7d)
                    .requestsTotal(25600L)
                    .responseMinTime(32.5d)
                    .responseMaxTime(1220.87d)
                    .responseAvgTime(159.2d)
                    .build();

            //When

            Response response = target.queryParam("from", FROM).queryParam("to", TO).request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsRequestResponseTimeResponse.class)
                .isEqualTo(
                    EnvironmentAnalyticsRequestResponseTimeResponse
                        .builder()
                        .requestsPerSecond(3.7)
                        .requestsTotal(25600)
                        .responseMinTime(32.5)
                        .responseMaxTime(1220.87)
                        .responseAvgTime(159.2)
                        .build()
                );
        }
    }

    @Nested
    class RangeParamValidation {

        @ParameterizedTest
        @ValueSource(strings = { "top-hits", "response-status-ranges", "request-response-time" })
        public void should_return_400_if_time_ranges_parameters_are_not_present(String path) {
            target = rootTarget().path(path);
            Response response = target.queryParam("from", FROM).request().get();

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessage("Validation error");
        }

        @ParameterizedTest
        @ValueSource(strings = { "top-hits", "response-status-ranges", "request-response-time" })
        public void should_return_400_if_time_range_parameter_is_less_than_zero(String path) {
            target = rootTarget().path(path);
            var lessThanZeroFromValue = -12L;
            Response response = target.queryParam("from", lessThanZeroFromValue).queryParam("to", TO).request().get();

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessage("Validation error");
        }
    }

    @Nested
    class ResponseTimeOverTimeAnalytics {

        @BeforeEach
        void setup() {
            target = rootTarget().path("response-time-over-time");
        }

        @Test
        void should_return_200_with_valid_request_response_time_analytics() {
            //Given
            var topHitApi1Id = "my-api";
            var api1 = ApiFixtures.aProxyApiV4().toBuilder().id("api-1").build();
            var api2 = ApiFixtures.aProxyApiV4().toBuilder().id("api-2").build();

            apiQueryService.initWith(List.of(api1, api2));
            analyticsQueryService.averageAggregate = new LinkedHashMap<>();
            analyticsQueryService.averageAggregate.put("1970-01-01T00:00:00", 1.2D);
            analyticsQueryService.averageAggregate.put("1970-01-01T00:30:00", 1.6D);

            //When

            Response response = target.queryParam("from", FROM).queryParam("to", TO).request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsOverPeriodResponse.class)
                .isEqualTo(
                    EnvironmentAnalyticsOverPeriodResponse
                        .builder()
                        .timeRange(AnalyticTimeRange.builder().from(FROM).to(TO).interval(1000L).build())
                        .data(List.of(1L, 2L))
                        .build()
                );
        }
    }

    @Nested
    class ResponseStatusOverTimeAnalytics {

        @BeforeEach
        void setup() {
            target = rootTarget().path("response-status-overtime");
        }

        @Test
        void should_return_200_with_valid_request_response_time_analytics() {
            //Given
            var api1 = ApiFixtures.aProxyApiV4().toBuilder().id("api-1").build();
            var api2 = ApiFixtures.aProxyApiV4().toBuilder().id("api-2").build();

            apiQueryService.initWith(List.of(api1, api2));
            analyticsQueryService.responseStatusOvertime =
                ResponseStatusOvertime
                    .builder()
                    .timeRange(
                        new ResponseStatusOvertime.TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO), Duration.ofSeconds(1))
                    )
                    .data(Map.of("200", List.of(0L, 0L, 0L, 1L, 4L, 0L, 0L)))
                    .build();

            //When

            Response response = target.queryParam("from", FROM).queryParam("to", TO).request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsResponseStatusOvertimeResponse.class)
                .isEqualTo(
                    EnvironmentAnalyticsResponseStatusOvertimeResponse
                        .builder()
                        .timeRange(AnalyticTimeRange.builder().from(FROM).to(TO).interval(1000L).build())
                        .data(Map.of("200", List.of(0L, 0L, 0L, 1L, 4L, 0L, 0L)))
                        .build()
                );
        }
    }

    @Nested
    class TopAppsByRequestCount {

        @BeforeEach
        void setup() {
            target = rootTarget().path("top-apps-by-request-count");
        }

        @Test
        void should_return_200_with_valid_top_apps_by_request_count() {
            //Given
            var topHitApp1 = BaseApplicationEntity
                .builder()
                .id("top-hit-app-id-1")
                .name("Top Hit App 1")
                .environmentId(ENVIRONMENT)
                .build();
            var topHitApp2 = BaseApplicationEntity
                .builder()
                .id("top-hit-app-id-2")
                .name("Top Hit App 2")
                .environmentId(ENVIRONMENT)
                .build();
            var proxyApiV4 = ApiFixtures.aProxyApiV4().toBuilder().id("api-1").name("API 1").build();
            var messageApiV4 = ApiFixtures.aMessageApiV4().toBuilder().id("api-2").name("API 2").build();

            apiQueryService.initWith(List.of(proxyApiV4, messageApiV4));
            applicationQueryService.initWith(List.of(topHitApp1, topHitApp2));

            analyticsQueryService.topHitsApps =
                TopHitsApps
                    .builder()
                    .data(
                        List.of(
                            TopHitsApps.TopHitApp.builder().id(topHitApp1.getId()).count(7L).build(),
                            TopHitsApps.TopHitApp.builder().id(topHitApp2.getId()).count(13L).build()
                        )
                    )
                    .build();

            //When
            Response response = target.queryParam("from", FROM).queryParam("to", TO).request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsTopAppsByRequestCountResponse.class)
                .isEqualTo(
                    EnvironmentAnalyticsTopAppsByRequestCountResponse
                        .builder()
                        .data(
                            List.of(
                                TopApp.builder().id("top-hit-app-id-2").name("Top Hit App 2").count(13L).build(),
                                TopApp.builder().id("top-hit-app-id-1").name("Top Hit App 1").count(7L).build()
                            )
                        )
                        .build()
                );
        }

        @Test
        void should_return_200_with_empty_list_if_no_apis_found() {
            apiQueryService.initWith(List.of());
            analyticsQueryService.topHitsApis = TopHitsApis.builder().data(List.of()).build();

            //When
            Response response = target.queryParam("from", FROM).queryParam("to", TO).request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsTopAppsByRequestCountResponse.class)
                .isEqualTo(EnvironmentAnalyticsTopAppsByRequestCountResponse.builder().data(List.of()).build());
        }
    }
}
