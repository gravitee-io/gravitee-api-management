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
package io.gravitee.rest.api.management.v2.rest.resource.logs;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import fixtures.core.model.RepositoryFixtures;
import fixtures.repository.ConnectionLogFixtures;
import inmemory.ConnectionLogsCrudServiceInMemory;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.apim.infra.domain_service.user.UserContextLoaderImpl;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.ApiLog;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.ArrayFilter;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.Filter;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.FilterName;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.NumericFilter;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.Operator;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.SearchLogsRequest;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.SearchLogsResponse;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.StringFilter;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.TimeRange;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LogsSearchResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";
    private static final String API_1 = "api1";
    private static final String API_2 = "api2";
    private static final Plan PLAN_1 = PlanFixtures.aPlanHttpV4().toBuilder().id("plan1").name("1st plan").apiId(API_1).build();
    private static final Plan PLAN_2 = PlanFixtures.aPlanHttpV4().toBuilder().id("plan2").name("2nd plan").apiId(API_1).build();
    private static final BaseApplicationEntity APPLICATION_1 = BaseApplicationEntity.builder().id("app1").name("Application 1").build();
    private static final BaseApplicationEntity APPLICATION_2 = BaseApplicationEntity.builder().id("app2").name("Application 2").build();
    private static final Api GIO_API_1 = RepositoryFixtures.aProxyApiV4()
        .toBuilder()
        .id(API_1)
        .name("API1")
        .environmentId(ENVIRONMENT)
        .build();
    private static final Api GIO_API_2 = RepositoryFixtures.aProxyApiV4()
        .toBuilder()
        .id(API_2)
        .name("API2")
        .environmentId(ENVIRONMENT)
        .build();

    final ConnectionLogFixtures connectionLogFixtures = new ConnectionLogFixtures(API_1, APPLICATION_1.getId(), PLAN_1.getId());

    @Inject
    private ApiAuthorizationService apiAuthorizationService;

    @Inject
    private ApiRepository apiRepository;

    @Inject
    private UserContextLoader userContextLoader;

    @Inject
    ConnectionLogsCrudServiceInMemory connectionLogsCrudService;

    WebTarget searchTarget;

    @BeforeEach
    void setup() {
        searchTarget = rootTarget().path("search");

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);

        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        when(apiAuthorizationService.findApiIdsByUserId(any(), any(), any(), anyBoolean())).thenReturn(Set.of(API_1, API_2));

        when(apiRepository.search(any(), any())).thenReturn(List.of(GIO_API_1, GIO_API_2));

        var realContextManager = new UserContextLoaderImpl(apiAuthorizationService, apiRepository);
        when(userContextLoader.loadApis(any(UserContext.class))).thenAnswer(invocation ->
            realContextManager.loadApis(invocation.getArgument(0))
        );
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        connectionLogsCrudService.reset();
        Mockito.reset(userContextLoader);
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/logs";
    }

    @Nested
    class BasicFunctionality {

        @Test
        void should_return_connection_logs() {
            connectionLogsCrudService.initWith(List.of(connectionLogFixtures.aConnectionLog("req1")));

            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z")).to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
            );

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(1);
                    assertThat(r.getData().getFirst().getRequestId()).isEqualTo("req1");
                    assertThat(r.getData().getFirst().getMethod()).isEqualTo(
                        io.gravitee.rest.api.management.v2.rest.model.logs.engine.HttpMethod.GET
                    );
                    assertThat(r.getData().getFirst().getStatus()).isEqualTo(200);
                    assertThat(r.getData().getFirst().getApplication()).isNotNull();
                    assertThat(r.getData().getFirst().getApplication().getId()).isEqualTo(APPLICATION_1.getId());
                    assertThat(r.getData().getFirst().getPlan()).isNotNull();
                    assertThat(r.getData().getFirst().getPlan().getId()).isEqualTo(PLAN_1.getId());
                });
        }

        @Test
        void should_compute_pagination() {
            var total = 20L;
            var page = 2;
            var pageSize = 5;
            connectionLogsCrudService.initWithConnectionLogs(
                LongStream.range(0, total)
                    .mapToObj(i -> connectionLogFixtures.aConnectionLog())
                    .toList()
            );

            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z")).to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
            );

            var response = searchTarget
                .queryParam(LogsSearchResource.PAGE_QUERY_PARAM_NAME, page)
                .queryParam(LogsSearchResource.PER_PAGE_QUERY_PARAM_NAME, pageSize)
                .request()
                .post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .extracting(SearchLogsResponse::getPagination)
                .satisfies(p -> {
                    assertThat(p.getPage()).isEqualTo(page);
                    assertThat(p.getPerPage()).isEqualTo(pageSize);
                    assertThat(p.getPageCount()).isEqualTo(4);
                    assertThat(p.getPageItemsCount()).isEqualTo(pageSize);
                    assertThat(p.getTotalCount()).isEqualTo(total);
                });
        }

        @Test
        void should_return_empty_results_when_no_logs_match() {
            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z")).to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
            );

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).isEmpty();
                    assertThat(r.getPagination()).isNotNull();
                    assertThat(r.getPagination().getTotalCount()).isEqualTo(0L);
                    assertThat(r.getLinks()).isNotNull();
                    assertThat(r.getLinks().getSelf()).isNotNull();
                    assertThat(r.getLinks().getFirst()).isNull();
                    assertThat(r.getLinks().getLast()).isNull();
                    assertThat(r.getLinks().getNext()).isNull();
                    assertThat(r.getLinks().getPrevious()).isNull();
                });
        }
    }

    @Nested
    class RequestValidation {

        @Test
        void should_return_400_if_time_range_missing() {
            var request = new SearchLogsRequest();

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_if_page_is_zero() {
            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z")).to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
            );

            var response = searchTarget.queryParam(LogsSearchResource.PAGE_QUERY_PARAM_NAME, 0).request().post(Entity.json(request));

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_200_if_page_is_one() {
            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z")).to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
            );

            var response = searchTarget.queryParam(LogsSearchResource.PAGE_QUERY_PARAM_NAME, 1).request().post(Entity.json(request));

            assertThat(response).hasStatus(OK_200);
        }

        @Test
        void should_return_400_if_page_size_is_zero() {
            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z")).to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
            );

            var response = searchTarget.queryParam(LogsSearchResource.PER_PAGE_QUERY_PARAM_NAME, 0).request().post(Entity.json(request));

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_200_if_page_size_is_one() {
            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z")).to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
            );

            var response = searchTarget.queryParam(LogsSearchResource.PER_PAGE_QUERY_PARAM_NAME, 1).request().post(Entity.json(request));

            assertThat(response).hasStatus(OK_200);
        }

        @Test
        void should_return_200_if_page_size_is_100() {
            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z")).to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
            );

            var response = searchTarget.queryParam(LogsSearchResource.PER_PAGE_QUERY_PARAM_NAME, 100).request().post(Entity.json(request));

            assertThat(response).hasStatus(OK_200);
        }

        @Test
        void should_return_200_if_page_size_is_too_large() {
            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z")).to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
            );

            var response = searchTarget.queryParam(LogsSearchResource.PER_PAGE_QUERY_PARAM_NAME, 101).request().post(Entity.json(request));

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }
    }

    @Nested
    class Filtering {

        @Test
        void should_accept_empty_filters() {
            connectionLogsCrudService.initWith(
                List.of(connectionLogFixtures.aConnectionLog("req1"), connectionLogFixtures.aConnectionLog("req2"))
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .filters(List.of());

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> assertThat(r.getData()).hasSize(2));
        }

        @Test
        void should_handle_null_filters() {
            connectionLogsCrudService.initWith(
                List.of(connectionLogFixtures.aConnectionLog("req1"), connectionLogFixtures.aConnectionLog("req2"))
            );

            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z")).to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
            );

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> assertThat(r.getData()).hasSize(2));
        }

        @Test
        void should_filter_logs_by_time_range() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().timestamp("2020-02-01T20:00:00.00Z").build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().timestamp("2020-02-02T20:00:00.00Z").build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().timestamp("2020-02-04T20:00:00.00Z").build()
                )
            );

            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-02-01T00:01:00.00Z")).to(OffsetDateTime.parse("2020-02-03T23:59:59.00Z"))
            );

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(2);
                    assertThat(r.getData().get(0).getRequestId()).isEqualTo("req2");
                    assertThat(r.getData().get(1).getRequestId()).isEqualTo("req1");
                });
        }

        @Test
        void should_filter_logs_by_api() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().apiId(API_1).build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().apiId(API_1).build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().apiId(API_2).build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(new Filter(new ArrayFilter().name(FilterName.API).operator(Operator.IN).value(List.of(API_1))));

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(2);
                    assertThat(r.getData().stream().map(ApiLog::getRequestId)).containsExactlyInAnyOrder("req1", "req2");
                });
        }

        @Test
        void should_filter_logs_by_application() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().applicationId(APPLICATION_1.getId()).build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().applicationId(APPLICATION_1.getId()).build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().applicationId(APPLICATION_2.getId()).build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(
                    new Filter(new ArrayFilter().name(FilterName.APPLICATION).operator(Operator.IN).value(List.of(APPLICATION_1.getId())))
                );

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(2);
                    assertThat(r.getData().stream().map(ApiLog::getRequestId)).containsExactlyInAnyOrder("req1", "req2");
                });
        }

        @Test
        void should_filter_logs_by_plan() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().planId(PLAN_1.getId()).build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().planId(PLAN_1.getId()).build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().planId(PLAN_2.getId()).build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(new Filter(new ArrayFilter().name(FilterName.PLAN).operator(Operator.IN).value(List.of(PLAN_1.getId()))));

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(2);
                    assertThat(r.getData().stream().map(ApiLog::getRequestId)).containsExactlyInAnyOrder("req1", "req2");
                });
        }

        @Test
        void should_filter_logs_by_http_method() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().method(io.gravitee.common.http.HttpMethod.POST).build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().method(io.gravitee.common.http.HttpMethod.GET).build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().method(io.gravitee.common.http.HttpMethod.POST).build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(new Filter(new ArrayFilter().name(FilterName.HTTP_METHOD).operator(Operator.IN).value(List.of("GET"))));

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(1);
                    assertThat(r.getData().getFirst().getRequestId()).isEqualTo("req2");
                    assertThat(r.getData().getFirst().getMethod()).isEqualTo(
                        io.gravitee.rest.api.management.v2.rest.model.logs.engine.HttpMethod.GET
                    );
                });
        }

        @Test
        void should_filter_logs_by_http_status() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().status(200).build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().status(202).build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().status(200).build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(new Filter(new ArrayFilter().name(FilterName.HTTP_STATUS).operator(Operator.IN).value(List.of("202"))));

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(1);
                    assertThat(r.getData().getFirst().getRequestId()).isEqualTo("req2");
                    assertThat(r.getData().getFirst().getStatus()).isEqualTo(202);
                });
        }

        @Test
        void should_filter_logs_by_entrypoint() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().entrypointId("entrypoint1").build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().entrypointId("entrypoint1").build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().entrypointId("entrypoint2").build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(
                    new Filter(new ArrayFilter().name(FilterName.ENTRYPOINT).operator(Operator.IN).value(List.of("entrypoint1")))
                );

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(2);
                    assertThat(r.getData().stream().map(ApiLog::getRequestId)).containsExactlyInAnyOrder("req1", "req2");
                });
        }

        @Test
        void should_and_filters_with_same_name() {
            // When the same filter name appears multiple times, they should be AND-ed
            // together
            // This means the result should only include logs that match ALL filter criteria
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures
                        .aConnectionLog("req1")
                        .toBuilder()
                        .applicationId(APPLICATION_1.getId())
                        .planId(PLAN_1.getId())
                        .build(),
                    connectionLogFixtures
                        .aConnectionLog("req2")
                        .toBuilder()
                        .applicationId(APPLICATION_2.getId())
                        .planId(PLAN_1.getId())
                        .build(),
                    connectionLogFixtures
                        .aConnectionLog("req3")
                        .toBuilder()
                        .applicationId(APPLICATION_2.getId())
                        .planId(PLAN_2.getId())
                        .build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(
                    new Filter(
                        new ArrayFilter()
                            .name(FilterName.APPLICATION)
                            .operator(Operator.IN)
                            .value(List.of(APPLICATION_1.getId(), APPLICATION_2.getId()))
                    )
                )
                .addFiltersItem(
                    new Filter(new StringFilter().name(FilterName.APPLICATION).operator(Operator.EQ).value(APPLICATION_1.getId()))
                );

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    // Only req1 should match (app1 AND plan1)
                    assertThat(r.getData()).hasSize(1);
                    assertThat(r.getData().getFirst().getRequestId()).isEqualTo("req1");
                    assertThat(Objects.requireNonNull(r.getData().getFirst().getApplication()).getId()).isEqualTo(APPLICATION_1.getId());
                    assertThat(Objects.requireNonNull(r.getData().getFirst().getPlan()).getId()).isEqualTo(PLAN_1.getId());
                });
        }

        @Test
        void should_filter_logs_by_transaction_id() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().transactionId("transaction1").build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().transactionId("transaction2").build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().transactionId("transaction3").build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(
                    new Filter(
                        new ArrayFilter()
                            .name(FilterName.TRANSACTION_ID)
                            .operator(Operator.IN)
                            .value(List.of("transaction1", "transaction2"))
                    )
                );

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(2);
                    assertThat(r.getData().stream().map(ApiLog::getRequestId)).containsExactlyInAnyOrder("req1", "req2");
                });
        }

        @Test
        void should_filter_logs_by_Request_Id() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(
                    new Filter(new ArrayFilter().name(FilterName.REQUEST_ID).operator(Operator.IN).value(List.of("req1", "req3")))
                );

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(2);
                    assertThat(r.getData().stream().map(ApiLog::getRequestId)).containsExactlyInAnyOrder("req1", "req3");
                });
        }

        @Test
        void should_filter_logs_by_uri() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().uri("/foo").build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().uri("/bar").build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().uri("/foo/bar").build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(new Filter(new StringFilter().name(FilterName.URI).operator(Operator.EQ).value("/foo*")));

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(2);
                    assertThat(r.getData().stream().map(ApiLog::getRequestId)).containsExactlyInAnyOrder("req1", "req3");
                });
        }

        @Test
        void should_filter_logs_by_response_time_gte() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().gatewayResponseTime(50).build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().gatewayResponseTime(150).build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().gatewayResponseTime(300).build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(new Filter(new NumericFilter().name(FilterName.RESPONSE_TIME).operator(Operator.GTE).value(100)));

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(2);
                    assertThat(r.getData().stream().map(ApiLog::getRequestId)).containsExactlyInAnyOrder("req2", "req3");
                });
        }

        @Test
        void should_filter_logs_by_response_time_lte() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().gatewayResponseTime(50).build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().gatewayResponseTime(150).build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().gatewayResponseTime(300).build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(new Filter(new NumericFilter().name(FilterName.RESPONSE_TIME).operator(Operator.LTE).value(200)));

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(2);
                    assertThat(r.getData().stream().map(ApiLog::getRequestId)).containsExactlyInAnyOrder("req1", "req2");
                });
        }

        @Test
        void should_filter_logs_by_response_time_range() {
            connectionLogsCrudService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().gatewayResponseTime(50).build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().gatewayResponseTime(150).build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().gatewayResponseTime(300).build(),
                    connectionLogFixtures.aConnectionLog("req4").toBuilder().gatewayResponseTime(500).build()
                )
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .addFiltersItem(new Filter(new NumericFilter().name(FilterName.RESPONSE_TIME).operator(Operator.GTE).value(150)))
                .addFiltersItem(new Filter(new NumericFilter().name(FilterName.RESPONSE_TIME).operator(Operator.LTE).value(300)));

            var response = searchTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .satisfies(r -> {
                    assertThat(r.getData()).hasSize(2);
                    assertThat(r.getData().stream().map(ApiLog::getRequestId)).containsExactlyInAnyOrder("req2", "req3");
                });
        }
    }

    @Nested
    class Links {

        @Test
        void should_return_links() {
            var total = 20L;
            var pageSize = 5;
            connectionLogsCrudService.initWithConnectionLogs(
                LongStream.range(0, total)
                    .mapToObj(i -> connectionLogFixtures.aConnectionLog())
                    .toList()
            );

            var request = new SearchLogsRequest()
                .timeRange(
                    new TimeRange()
                        .from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z"))
                        .to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
                )
                .filters(List.of(new Filter(new StringFilter().name(FilterName.API).operator(Operator.EQ).value("api1"))));

            var response = searchTarget
                .queryParam(LogsSearchResource.PAGE_QUERY_PARAM_NAME, 2)
                .queryParam(LogsSearchResource.PER_PAGE_QUERY_PARAM_NAME, pageSize)
                .request()
                .post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .extracting(SearchLogsResponse::getLinks)
                .satisfies(links -> {
                    assertThat(links.getSelf()).isNotNull();
                    assertThat(links.getSelf()).contains("page=2").contains("perPage=5");

                    assertThat(links.getFirst()).isNotNull();
                    assertThat(links.getFirst()).contains("page=1").contains("perPage=5");

                    assertThat(links.getLast()).isNotNull();
                    assertThat(links.getLast()).contains("page=4").contains("perPage=5");

                    assertThat(links.getNext()).isNotNull();
                    assertThat(links.getNext()).contains("page=3").contains("perPage=5");

                    assertThat(links.getPrevious()).isNotNull();
                    assertThat(links.getPrevious()).contains("page=1").contains("perPage=5");
                });
        }

        @Test
        void should_not_return_previous_link_on_first_page() {
            var total = 10L;
            connectionLogsCrudService.initWithConnectionLogs(
                LongStream.range(0, total)
                    .mapToObj(i -> connectionLogFixtures.aConnectionLog())
                    .toList()
            );

            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z")).to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
            );

            var response = searchTarget
                .queryParam(LogsSearchResource.PAGE_QUERY_PARAM_NAME, 1)
                .queryParam(LogsSearchResource.PER_PAGE_QUERY_PARAM_NAME, 5)
                .request()
                .post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .extracting(SearchLogsResponse::getLinks)
                .satisfies(links -> {
                    assertThat(links.getPrevious()).isNull();
                    assertThat(links.getNext()).isNotNull();
                });
        }

        @Test
        void should_not_return_next_link_on_last_page() {
            var total = 10L;
            connectionLogsCrudService.initWithConnectionLogs(
                LongStream.range(0, total)
                    .mapToObj(i -> connectionLogFixtures.aConnectionLog())
                    .toList()
            );

            var request = new SearchLogsRequest().timeRange(
                new TimeRange().from(OffsetDateTime.parse("2020-01-01T00:00:00.00Z")).to(OffsetDateTime.parse("2020-12-31T23:59:59.00Z"))
            );

            var response = searchTarget
                .queryParam(LogsSearchResource.PAGE_QUERY_PARAM_NAME, 2)
                .queryParam(LogsSearchResource.PER_PAGE_QUERY_PARAM_NAME, 5)
                .request()
                .post(Entity.json(request));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(SearchLogsResponse.class)
                .extracting(SearchLogsResponse::getLinks)
                .satisfies(links -> {
                    assertThat(links.getNext()).isNull();
                    assertThat(links.getPrevious()).isNotNull();
                });
        }
    }
}
