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
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ApplicationQueryServiceInMemory;
import inmemory.ConnectionLogsCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsOverPeriodResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsRequestResponseTimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsResponseStatusOvertimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsResponseStatusRangesResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsTopAppsByRequestCountResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsTopFailedApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsTopHitsApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.TopHitApi;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.analytics.TopHitsApps;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.analytics.RequestResponseTime;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.model.v4.analytics.TopFailedApis;
import io.gravitee.rest.api.model.v4.analytics.TopHitsApis;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EnvironmentAnalyticsResourceScopeTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final long FROM = 1728981738L;
    private static final long TO = 1729068138L;

    private static final String READABLE_API_ID = "readable-api";
    private static final String HIDDEN_API_ID = "hidden-api";
    private static final String OTHER_ENVIRONMENT = "other-environment-id";

    @Inject
    ApiQueryServiceInMemory apiQueryService;

    @Inject
    ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService;

    @Inject
    ApplicationQueryServiceInMemory applicationQueryService;

    @Inject
    FakeAnalyticsQueryService analyticsQueryService;

    @Inject
    ConnectionLogsCrudServiceInMemory connectionLogsCrudService;

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

        apiQueryService.initWith(
            List.of(
                ApiFixtures.aProxyApiV4().toBuilder().id(READABLE_API_ID).name("Readable API").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id(HIDDEN_API_ID).name("Hidden API").build()
            )
        );
        givenAnalyticsHoldData();
    }

    @AfterEach
    void teardown() {
        Stream.of(apiQueryService, apiAuthorizationDomainService, applicationQueryService, connectionLogsCrudService).forEach(
            InMemoryAlternative::reset
        );
        analyticsQueryService.reset();
        analyticsQueryService.topHitsApis = null;
        analyticsQueryService.topHitsApps = null;
        reset(permissionService);
        GraviteeContext.cleanContext();
    }

    @Nested
    class UserReadingNoApi {

        @Test
        void should_return_no_response_status_range() {
            assertThat(get("response-status-ranges"))
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsResponseStatusRangesResponse.class)
                .extracting(EnvironmentAnalyticsResponseStatusRangesResponse::getRanges)
                .satisfies(ranges -> assertThat(ranges).isNullOrEmpty());
        }

        @Test
        void should_return_no_error_key() {
            assertThat(get("error-keys")).hasStatus(OK_200).asEntity(List.class).isEqualTo(List.of());
        }

        @Test
        void should_return_no_top_hit() {
            assertThat(get("top-hits"))
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsTopHitsApisResponse.class)
                .isEqualTo(new EnvironmentAnalyticsTopHitsApisResponse().data(List.of()));
        }

        @Test
        void should_return_no_request() {
            assertThat(get("request-response-time"))
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsRequestResponseTimeResponse.class)
                .isEqualTo(
                    new EnvironmentAnalyticsRequestResponseTimeResponse()
                        .requestsPerSecond(0d)
                        .requestsTotal(0)
                        .responseMinTime(0d)
                        .responseMaxTime(0d)
                        .responseAvgTime(0d)
                );
        }

        @Test
        void should_return_no_response_time() {
            assertThat(get("response-time-over-time"))
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsOverPeriodResponse.class)
                .extracting(EnvironmentAnalyticsOverPeriodResponse::getData)
                .isEqualTo(List.of());
        }

        @Test
        void should_return_no_response_status() {
            assertThat(get("response-status-overtime"))
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsResponseStatusOvertimeResponse.class)
                .extracting(EnvironmentAnalyticsResponseStatusOvertimeResponse::getData)
                .isEqualTo(Map.of());
        }

        @Test
        void should_return_no_top_app() {
            assertThat(get("top-apps-by-request-count"))
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsTopAppsByRequestCountResponse.class)
                .isEqualTo(new EnvironmentAnalyticsTopAppsByRequestCountResponse().data(List.of()));
        }

        @Test
        void should_return_no_top_failed_api() {
            assertThat(get("top-failed-apis"))
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsTopFailedApisResponse.class)
                .isEqualTo(new EnvironmentAnalyticsTopFailedApisResponse().data(List.of()));
        }
    }

    @Nested
    class ApiMember {

        @BeforeEach
        void setup() {
            apiAuthorizationDomainService.initWith(apiQueryService.storage());
            when(
                permissionService.hasPermission(
                    any(),
                    eq(USER_NAME),
                    eq(RolePermission.API_ANALYTICS),
                    eq(READABLE_API_ID),
                    eq(RolePermissionAction.READ)
                )
            ).thenReturn(true);
        }

        @Test
        void should_return_the_error_keys_of_the_apis_whose_analytics_the_user_can_read() {
            assertThat(get("error-keys")).hasStatus(OK_200).asEntity(List.class).isEqualTo(List.of("READABLE_API_ERROR"));
        }

        @Test
        void should_return_the_top_failed_apis_of_the_apis_whose_analytics_the_user_can_read() {
            assertThat(get("top-failed-apis"))
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsTopFailedApisResponse.class)
                .extracting(EnvironmentAnalyticsTopFailedApisResponse::getData)
                .satisfies(data -> assertThat(data).extracting("name").containsExactly("Readable API"));
        }

        @Test
        void should_return_the_top_hits_of_the_apis_whose_analytics_the_user_can_read() {
            assertThat(get("top-hits"))
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsTopHitsApisResponse.class)
                .extracting(EnvironmentAnalyticsTopHitsApisResponse::getData)
                .satisfies(data -> assertThat(data).extracting(TopHitApi::getName).containsExactly("Readable API"));
        }
    }

    @Nested
    class PlatformReader {

        @BeforeEach
        void setup() {
            givenUserReadsThePlatformOf(ENVIRONMENT);
        }

        @Test
        void should_return_the_error_keys_of_every_api_of_the_environment() {
            assertThat(get("error-keys"))
                .hasStatus(OK_200)
                .asEntity(List.class)
                .isEqualTo(List.of("HIDDEN_API_ERROR", "READABLE_API_ERROR"));
        }

        @Test
        void should_return_the_top_hits_of_every_api_of_the_environment() {
            assertThat(get("top-hits"))
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsTopHitsApisResponse.class)
                .extracting(EnvironmentAnalyticsTopHitsApisResponse::getData)
                .satisfies(data -> assertThat(data).extracting(TopHitApi::getName).containsExactly("Readable API", "Hidden API"));
        }
    }

    @Nested
    class PlatformReaderOfAnotherEnvironment {

        @Test
        void should_return_no_top_hit() {
            givenUserReadsThePlatformOf(OTHER_ENVIRONMENT);

            assertThat(get("top-hits"))
                .hasStatus(OK_200)
                .asEntity(EnvironmentAnalyticsTopHitsApisResponse.class)
                .isEqualTo(new EnvironmentAnalyticsTopHitsApisResponse().data(List.of()));
        }
    }

    private void givenUserReadsThePlatformOf(String environmentId) {
        when(
            permissionService.hasPermission(
                any(),
                eq(USER_NAME),
                eq(RolePermission.ENVIRONMENT_PLATFORM),
                eq(environmentId),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(true);
    }

    private Response get(String path) {
        return rootTarget().path(path).queryParam("from", FROM).queryParam("to", TO).request().get();
    }

    /** Every endpoint would have data to show: an empty answer can only come from the user's scope. */
    private void givenAnalyticsHoldData() {
        analyticsQueryService.responseStatusRanges = ResponseStatusRanges.builder().ranges(Map.of("200.0-300.0", 17L)).build();
        analyticsQueryService.topHitsApis = TopHitsApis.builder()
            .data(
                List.of(
                    TopHitsApis.TopHitApi.builder().id(READABLE_API_ID).count(17L).build(),
                    TopHitsApis.TopHitApi.builder().id(HIDDEN_API_ID).count(5L).build()
                )
            )
            .build();
        analyticsQueryService.requestResponseTime = RequestResponseTime.builder()
            .requestsPerSecond(3.7d)
            .requestsTotal(25600L)
            .responseMinTime(32.5d)
            .responseMaxTime(1220.87d)
            .responseAvgTime(159.2d)
            .build();
        analyticsQueryService.averageAggregate = new LinkedHashMap<>(Map.of("1970-01-01T00:00:00", 1.2D));
        analyticsQueryService.responseStatusOvertime = ResponseStatusOvertime.builder()
            .timeRange(new ResponseStatusOvertime.TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO), Duration.ofSeconds(1)))
            .data(Map.of("200", List.of(1L, 4L)))
            .build();
        applicationQueryService.initWith(
            List.of(BaseApplicationEntity.builder().id("app-id").name("App").environmentId(ENVIRONMENT).build())
        );
        analyticsQueryService.topHitsApps = TopHitsApps.builder()
            .data(List.of(TopHitsApps.TopHitApp.builder().id("app-id").count(12L).build()))
            .build();
        analyticsQueryService.topFailedApis = TopFailedApis.builder()
            .data(
                List.of(
                    TopFailedApis.TopFailedApi.builder().id(READABLE_API_ID).failedCalls(3L).failedCallsRatio(0.1).build(),
                    TopFailedApis.TopFailedApi.builder().id(HIDDEN_API_ID).failedCalls(2L).failedCallsRatio(0.2).build()
                )
            )
            .build();
        connectionLogsCrudService.initWith(
            List.of(
                BaseConnectionLog.builder()
                    .apiId(READABLE_API_ID)
                    .errorKey("READABLE_API_ERROR")
                    .timestamp(Instant.ofEpochMilli(FROM + 1000).toString())
                    .build(),
                BaseConnectionLog.builder()
                    .apiId(HIDDEN_API_ID)
                    .errorKey("HIDDEN_API_ERROR")
                    .timestamp(Instant.ofEpochMilli(FROM + 2000).toString())
                    .build()
            )
        );
    }
}
