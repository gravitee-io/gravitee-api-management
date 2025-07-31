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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.log.model.MessageLogFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.repository.ConnectionLogDetailFixtures;
import fixtures.repository.ConnectionLogFixtures;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.ConnectionLogsCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MessageLogCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import io.gravitee.apim.core.log.model.MessageOperation;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.rest.api.management.v2.rest.model.ApiLog;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogRequestContent;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogResponseContent;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogsResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiMessageLog;
import io.gravitee.rest.api.management.v2.rest.model.ApiMessageLogContent;
import io.gravitee.rest.api.management.v2.rest.model.ApiMessageLogsResponse;
import io.gravitee.rest.api.management.v2.rest.model.BaseApplication;
import io.gravitee.rest.api.management.v2.rest.model.BasePlan;
import io.gravitee.rest.api.management.v2.rest.model.HttpMethod;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.model.PlanMode;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurity;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
import io.gravitee.rest.api.management.v2.rest.resource.api.log.param.SearchLogsParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ApiLogsResourceTest extends ApiResourceTest {

    private static final Plan PLAN_1 = PlanFixtures.aPlanHttpV4().toBuilder().id("plan1").name("1st plan").apiId(API).build();
    private static final Plan PLAN_2 = PlanFixtures.aPlanHttpV4().toBuilder().id("plan2").name("2nd plan").apiId(API).build();
    private static final BaseApplicationEntity APPLICATION = BaseApplicationEntity.builder().id("app1").name("an application name").build();
    public static final String REQUEST_ID = "request-id";

    final ConnectionLogFixtures connectionLogFixtures = new ConnectionLogFixtures(API, APPLICATION.getId(), PLAN_1.getId());

    @Inject
    ConnectionLogsCrudServiceInMemory connectionLogStorageService;

    @Inject
    MessageLogCrudServiceInMemory messageLogStorageService;

    @Inject
    PlanCrudServiceInMemory planStorageService;

    @Inject
    ApplicationCrudServiceInMemory applicationStorageService;

    WebTarget connectionLogsTarget;
    WebTarget messageLogsTarget;
    WebTarget connectionLogTarget;

    @BeforeEach
    public void setup() {
        connectionLogsTarget = rootTarget();
        messageLogsTarget = rootTarget().path(REQUEST_ID).path("messages");
        connectionLogTarget = rootTarget().path(REQUEST_ID);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        planStorageService.initWith(List.of(PLAN_1, PLAN_2));
        applicationStorageService.initWith(List.of(APPLICATION));
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();

        Stream
            .of(applicationStorageService, connectionLogStorageService, messageLogStorageService, planStorageService)
            .forEach(InMemoryAlternative::reset);
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/logs";
    }

    @Nested
    class ConnectionLogs {

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_LOG),
                    eq(API),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);

            final Response response = connectionLogsTarget.request().get();

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_connection_logs() {
            connectionLogStorageService.initWith(List.of(connectionLogFixtures.aConnectionLog("req1")));

            final Response response = connectionLogsTarget.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiLogsResponse.class)
                .isEqualTo(
                    new ApiLogsResponse()
                        .data(
                            List.of(
                                new ApiLog()
                                    .application(new BaseApplication().id(APPLICATION.getId()).name(APPLICATION.getName()))
                                    .plan(
                                        new BasePlan()
                                            .id(PLAN_1.getId())
                                            .name(PLAN_1.getName())
                                            .apiId(API)
                                            .description(PLAN_1.getDescription())
                                            .security(new PlanSecurity().type(PlanSecurityType.KEY_LESS))
                                            .mode(PlanMode.STANDARD)
                                    )
                                    .method(HttpMethod.GET)
                                    .status(200)
                                    .clientIdentifier("client-identifier")
                                    .requestEnded(true)
                                    .requestId("req1")
                                    .transactionId("transaction-id")
                                    .timestamp(Instant.parse("2020-02-01T20:00:00.00Z").atOffset(ZoneOffset.UTC))
                                    .gatewayResponseTime(42)
                                    .uri("/my-api")
                                    .endpoint("https://my-api-example.com")
                            )
                        )
                        .pagination(new Pagination().page(1).perPage(10).pageCount(1).pageItemsCount(1).totalCount(1L))
                        .links(new Links().self(connectionLogsTarget.getUri().toString()))
                );
        }

        @Test
        public void should_compute_pagination() {
            var total = 20L;
            var pageSize = 5;
            connectionLogStorageService.initWithConnectionLogs(
                LongStream.range(0, total).mapToObj(i -> connectionLogFixtures.aConnectionLog()).toList()
            );

            connectionLogsTarget =
                connectionLogsTarget
                    .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 2)
                    .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, pageSize);
            final Response response = connectionLogsTarget.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiLogsResponse.class)
                .extracting(ApiLogsResponse::getPagination)
                .isEqualTo(new Pagination().page(2).perPage(pageSize).pageCount(4).pageItemsCount(pageSize).totalCount(total));
        }

        @Test
        public void should_compute_links() {
            var total = 20L;
            var page = 2;
            var pageSize = 5;
            connectionLogStorageService.initWithConnectionLogs(
                LongStream.range(0, total).mapToObj(i -> connectionLogFixtures.aConnectionLog()).toList()
            );

            final Response response = connectionLogsTarget
                .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, page)
                .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, pageSize)
                .request()
                .get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiLogsResponse.class)
                .extracting(ApiLogsResponse::getLinks)
                .isEqualTo(
                    new Links()
                        .self(connectionLogsTarget.queryParam("page", page).queryParam("perPage", pageSize).getUri().toString())
                        .first(connectionLogsTarget.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                        .last(connectionLogsTarget.queryParam("page", 4).queryParam("perPage", pageSize).getUri().toString())
                        .previous(connectionLogsTarget.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                        .next(connectionLogsTarget.queryParam("page", 3).queryParam("perPage", pageSize).getUri().toString())
                );
        }

        @Test
        public void should_return_400_if_negative_interval_params() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_LOG),
                    eq(API),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(true);

            connectionLogsTarget =
                connectionLogsTarget
                    .queryParam(SearchLogsParam.FROM_QUERY_PARAM_NAME, -1)
                    .queryParam(SearchLogsParam.TO_QUERY_PARAM_NAME, -1);
            final Response response = connectionLogsTarget.request().get();

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessage("Validation error");
        }

        @Test
        public void should_return_400_if_to_is_before_from_param() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_LOG),
                    eq(API),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(true);

            connectionLogsTarget =
                connectionLogsTarget
                    .queryParam(SearchLogsParam.FROM_QUERY_PARAM_NAME, 2)
                    .queryParam(SearchLogsParam.TO_QUERY_PARAM_NAME, 1);
            final Response response = connectionLogsTarget.request().get();

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessage("Validation error");
        }

        @Test
        public void should_return_connection_logs_filtered_by_interval() {
            connectionLogStorageService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().timestamp("2020-02-01T20:00:00.00Z").build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().timestamp("2020-02-02T20:00:00.00Z").build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().timestamp("2020-02-04T20:00:00.00Z").build()
                )
            );

            connectionLogsTarget =
                connectionLogsTarget
                    .queryParam(SearchLogsParam.FROM_QUERY_PARAM_NAME, Instant.parse("2020-02-01T00:01:00.00Z").toEpochMilli())
                    .queryParam(SearchLogsParam.TO_QUERY_PARAM_NAME, Instant.parse("2020-02-03T23:59:59.00Z").toEpochMilli());
            final Response response = connectionLogsTarget.request().get();

            Supplier<ApiLog> expectedApiLog = () ->
                new ApiLog()
                    .application(new BaseApplication().id(APPLICATION.getId()).name(APPLICATION.getName()))
                    .plan(
                        new BasePlan()
                            .id(PLAN_1.getId())
                            .name(PLAN_1.getName())
                            .apiId(API)
                            .description(PLAN_1.getDescription())
                            .security(new PlanSecurity().type(PlanSecurityType.KEY_LESS))
                            .mode(PlanMode.STANDARD)
                    )
                    .method(HttpMethod.GET)
                    .status(200)
                    .clientIdentifier("client-identifier")
                    .requestEnded(true)
                    .gatewayResponseTime(42)
                    .uri("/my-api")
                    .transactionId("transaction-id")
                    .endpoint("https://my-api-example.com");

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiLogsResponse.class)
                .isEqualTo(
                    new ApiLogsResponse()
                        .data(
                            List.of(
                                expectedApiLog
                                    .get()
                                    .requestId("req2")
                                    .timestamp(Instant.parse("2020-02-02T20:00:00.00Z").atOffset(ZoneOffset.UTC)),
                                expectedApiLog
                                    .get()
                                    .requestId("req1")
                                    .timestamp(Instant.parse("2020-02-01T20:00:00.00Z").atOffset(ZoneOffset.UTC))
                            )
                        )
                        .pagination(new Pagination().page(1).perPage(10).pageCount(1).pageItemsCount(2).totalCount(2L))
                        .links(new Links().self(connectionLogsTarget.getUri().toString()))
                );
        }

        @Test
        public void should_return_connection_logs_filtered_by_applications() {
            connectionLogStorageService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().applicationId("app1").build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().applicationId("app1").build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().applicationId("app2").build()
                )
            );

            connectionLogsTarget = connectionLogsTarget.queryParam(SearchLogsParam.APPLICATION_IDS_QUERY_PARAM_NAME, "app1");
            final Response response = connectionLogsTarget.request().get();

            Supplier<ApiLog> expectedApiLog = () ->
                new ApiLog()
                    .application(new BaseApplication().id("app1").name(APPLICATION.getName()))
                    .plan(
                        new BasePlan()
                            .id(PLAN_1.getId())
                            .name(PLAN_1.getName())
                            .apiId(API)
                            .description(PLAN_1.getDescription())
                            .security(new PlanSecurity().type(PlanSecurityType.KEY_LESS))
                            .mode(PlanMode.STANDARD)
                    )
                    .method(HttpMethod.GET)
                    .status(200)
                    .clientIdentifier("client-identifier")
                    .requestEnded(true)
                    .transactionId("transaction-id")
                    .gatewayResponseTime(42)
                    .uri("/my-api")
                    .timestamp(Instant.parse("2020-02-01T20:00:00.00Z").atOffset(ZoneOffset.UTC))
                    .endpoint("https://my-api-example.com");

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiLogsResponse.class)
                .isEqualTo(
                    new ApiLogsResponse()
                        .data(List.of(expectedApiLog.get().requestId("req1"), expectedApiLog.get().requestId("req2")))
                        .pagination(new Pagination().page(1).perPage(10).pageCount(1).pageItemsCount(2).totalCount(2L))
                        .links(new Links().self(connectionLogsTarget.getUri().toString()))
                );
        }

        @Test
        public void should_return_connection_logs_filtered_by_plans() {
            connectionLogStorageService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().planId(PLAN_1.getId()).build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().planId(PLAN_1.getId()).build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().planId(PLAN_2.getId()).build()
                )
            );

            connectionLogsTarget = connectionLogsTarget.queryParam(SearchLogsParam.PLAN_IDS_QUERY_PARAM_NAME, PLAN_1.getId());
            final Response response = connectionLogsTarget.request().get();

            Supplier<ApiLog> expectedApiLog = () ->
                new ApiLog()
                    .application(new BaseApplication().id("app1").name(APPLICATION.getName()))
                    .plan(
                        new BasePlan()
                            .id(PLAN_1.getId())
                            .name(PLAN_1.getName())
                            .apiId(API)
                            .description(PLAN_1.getDescription())
                            .security(new PlanSecurity().type(PlanSecurityType.KEY_LESS))
                            .mode(PlanMode.STANDARD)
                    )
                    .method(HttpMethod.GET)
                    .status(200)
                    .clientIdentifier("client-identifier")
                    .requestEnded(true)
                    .transactionId("transaction-id")
                    .gatewayResponseTime(42)
                    .uri("/my-api")
                    .timestamp(Instant.parse("2020-02-01T20:00:00.00Z").atOffset(ZoneOffset.UTC))
                    .endpoint("https://my-api-example.com");

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiLogsResponse.class)
                .isEqualTo(
                    new ApiLogsResponse()
                        .data(List.of(expectedApiLog.get().requestId("req1"), expectedApiLog.get().requestId("req2")))
                        .pagination(new Pagination().page(1).perPage(10).pageCount(1).pageItemsCount(2).totalCount(2L))
                        .links(new Links().self(connectionLogsTarget.getUri().toString()))
                );
        }

        @Test
        public void should_return_connection_logs_filtered_by_methods() {
            connectionLogStorageService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().method(io.gravitee.common.http.HttpMethod.POST).build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().method(io.gravitee.common.http.HttpMethod.GET).build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().method(io.gravitee.common.http.HttpMethod.POST).build()
                )
            );

            connectionLogsTarget = connectionLogsTarget.queryParam(SearchLogsParam.METHODS_QUERY_PARAM_NAME, HttpMethod.GET);
            final Response response = connectionLogsTarget.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiLogsResponse.class)
                .isEqualTo(
                    new ApiLogsResponse()
                        .data(
                            List.of(
                                new ApiLog()
                                    .application(new BaseApplication().id("app1").name(APPLICATION.getName()))
                                    .plan(
                                        new BasePlan()
                                            .id(PLAN_1.getId())
                                            .name(PLAN_1.getName())
                                            .apiId(API)
                                            .description(PLAN_1.getDescription())
                                            .security(new PlanSecurity().type(PlanSecurityType.KEY_LESS))
                                            .mode(PlanMode.STANDARD)
                                    )
                                    .method(HttpMethod.GET)
                                    .status(200)
                                    .clientIdentifier("client-identifier")
                                    .requestEnded(true)
                                    .transactionId("transaction-id")
                                    .timestamp(Instant.parse("2020-02-01T20:00:00.00Z").atOffset(ZoneOffset.UTC))
                                    .requestId("req2")
                                    .gatewayResponseTime(42)
                                    .uri("/my-api")
                                    .endpoint("https://my-api-example.com")
                            )
                        )
                        .pagination(new Pagination().page(1).perPage(10).pageCount(1).pageItemsCount(1).totalCount(1L))
                        .links(new Links().self(connectionLogsTarget.getUri().toString()))
                );
        }

        @Test
        public void should_return_connection_logs_filtered_by_statuses() {
            connectionLogStorageService.initWith(
                List.of(
                    connectionLogFixtures.aConnectionLog("req1").toBuilder().status(200).build(),
                    connectionLogFixtures.aConnectionLog("req2").toBuilder().status(202).build(),
                    connectionLogFixtures.aConnectionLog("req3").toBuilder().status(200).build()
                )
            );

            connectionLogsTarget = connectionLogsTarget.queryParam(SearchLogsParam.STATUSES_QUERY_PARAM_NAME, 202);
            final Response response = connectionLogsTarget.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiLogsResponse.class)
                .isEqualTo(
                    new ApiLogsResponse()
                        .data(
                            List.of(
                                new ApiLog()
                                    .application(new BaseApplication().id("app1").name(APPLICATION.getName()))
                                    .plan(
                                        new BasePlan()
                                            .id(PLAN_1.getId())
                                            .name(PLAN_1.getName())
                                            .apiId(API)
                                            .description(PLAN_1.getDescription())
                                            .security(new PlanSecurity().type(PlanSecurityType.KEY_LESS))
                                            .mode(PlanMode.STANDARD)
                                    )
                                    .method(HttpMethod.GET)
                                    .status(202)
                                    .clientIdentifier("client-identifier")
                                    .requestEnded(true)
                                    .transactionId("transaction-id")
                                    .timestamp(Instant.parse("2020-02-01T20:00:00.00Z").atOffset(ZoneOffset.UTC))
                                    .requestId("req2")
                                    .gatewayResponseTime(42)
                                    .uri("/my-api")
                                    .endpoint("https://my-api-example.com")
                            )
                        )
                        .pagination(new Pagination().page(1).perPage(10).pageCount(1).pageItemsCount(1).totalCount(1L))
                        .links(new Links().self(connectionLogsTarget.getUri().toString()))
                );
        }
    }

    @Nested
    class MessageLogs {

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_LOG),
                    eq(API),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);

            final Response response = messageLogsTarget.request().get();

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_message_logs() {
            messageLogStorageService.initWith(List.of(MessageLogFixtures.aMessageLog(API, REQUEST_ID)));

            final Response response = messageLogsTarget.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiMessageLogsResponse.class)
                .isEqualTo(
                    new ApiMessageLogsResponse()
                        .data(
                            List.of(
                                new ApiMessageLog()
                                    .requestId(REQUEST_ID)
                                    .clientIdentifier("client-identifier")
                                    .timestamp(Instant.parse("2020-02-01T20:00:00.00Z").atOffset(ZoneOffset.UTC))
                                    .correlationId("correlation-id")
                                    .parentCorrelationId("parent-correlation-id")
                                    .operation(MessageOperation.SUBSCRIBE.name())
                                    .entrypoint(
                                        new ApiMessageLogContent()
                                            .connectorId("http-get")
                                            .id("message-id")
                                            .payload("message-payload")
                                            .headers(Map.of("X-Header", List.of("header-value")))
                                            .metadata(Map.of("X-Metdata", "metadata-value"))
                                    )
                                    .endpoint(
                                        new ApiMessageLogContent()
                                            .connectorId("kafka")
                                            .id("message-id")
                                            .payload("message-payload")
                                            .headers(Map.of("X-Header", List.of("header-value")))
                                            .metadata(Map.of("X-Metdata", "metadata-value"))
                                    )
                            )
                        )
                        .pagination(new Pagination().page(1).perPage(10).pageCount(1).pageItemsCount(1).totalCount(1L))
                        .links(new Links().self(messageLogsTarget.getUri().toString()))
                );
        }

        @Test
        public void should_compute_pagination() {
            var total = 20L;
            var pageSize = 5;
            messageLogStorageService.initWith(
                LongStream
                    .range(0, total)
                    .mapToObj(i -> MessageLogFixtures.aMessageLog(API, REQUEST_ID).toBuilder().correlationId(String.valueOf(i)).build())
                    .toList()
            );

            messageLogsTarget =
                messageLogsTarget
                    .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 2)
                    .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, pageSize);
            final Response response = messageLogsTarget.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiMessageLogsResponse.class)
                .extracting(ApiMessageLogsResponse::getPagination)
                .isEqualTo(new Pagination().page(2).perPage(pageSize).pageCount(4).pageItemsCount(pageSize).totalCount(total));
        }

        @Test
        public void should_compute_links() {
            var total = 20L;
            var page = 2;
            var pageSize = 5;
            messageLogStorageService.initWith(
                LongStream
                    .range(0, total)
                    .mapToObj(i -> MessageLogFixtures.aMessageLog(API, REQUEST_ID).toBuilder().correlationId(String.valueOf(i)).build())
                    .toList()
            );

            final Response response = messageLogsTarget
                .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, page)
                .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, pageSize)
                .request()
                .get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiMessageLogsResponse.class)
                .extracting(ApiMessageLogsResponse::getLinks)
                .isEqualTo(
                    new Links()
                        .self(messageLogsTarget.queryParam("page", page).queryParam("perPage", pageSize).getUri().toString())
                        .first(messageLogsTarget.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                        .last(messageLogsTarget.queryParam("page", 4).queryParam("perPage", pageSize).getUri().toString())
                        .previous(messageLogsTarget.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                        .next(messageLogsTarget.queryParam("page", 3).queryParam("perPage", pageSize).getUri().toString())
                );
        }
    }

    @Nested
    class ConnectionLog {

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_LOG,
                    API,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = connectionLogTarget.request().get();

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_connection_log() {
            final ConnectionLogDetail connectionLogDetail = new ConnectionLogDetailFixtures(API, REQUEST_ID).aConnectionLogDetail();
            connectionLogStorageService.initWithConnectionLogDetails(List.of(connectionLogDetail));

            final Response response = connectionLogTarget.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiLogResponse.class)
                .isEqualTo(
                    new ApiLogResponse()
                        .requestId(connectionLogDetail.getRequestId())
                        .apiId(connectionLogDetail.getApiId())
                        .timestamp(Instant.parse(connectionLogDetail.getTimestamp()).atOffset(ZoneOffset.UTC))
                        .requestEnded(connectionLogDetail.isRequestEnded())
                        .clientIdentifier(connectionLogDetail.getClientIdentifier())
                        .entrypointRequest(
                            new ApiLogRequestContent()
                                .uri(connectionLogDetail.getEntrypointRequest().getUri())
                                .method(HttpMethod.valueOf(connectionLogDetail.getEntrypointRequest().getMethod()))
                                .headers(connectionLogDetail.getEntrypointRequest().getHeaders())
                        )
                        .endpointRequest(
                            new ApiLogRequestContent()
                                .uri(connectionLogDetail.getEndpointRequest().getUri())
                                .method(HttpMethod.valueOf(connectionLogDetail.getEndpointRequest().getMethod()))
                                .headers(connectionLogDetail.getEndpointRequest().getHeaders())
                        )
                        .endpointResponse(
                            new ApiLogResponseContent()
                                .status(connectionLogDetail.getEndpointResponse().getStatus())
                                .headers(connectionLogDetail.getEndpointResponse().getHeaders())
                        )
                        .entrypointResponse(
                            new ApiLogResponseContent()
                                .status(connectionLogDetail.getEntrypointResponse().getStatus())
                                .headers(connectionLogDetail.getEntrypointResponse().getHeaders())
                        )
                );
        }

        @Test
        void should_return_404_if_no_connection_log() {
            final ConnectionLogDetail connectionLogDetail = new ConnectionLogDetailFixtures(API, "other-request-id").aConnectionLogDetail();
            connectionLogStorageService.initWithConnectionLogDetails(List.of(connectionLogDetail));

            final Response response = connectionLogTarget.request().get();

            assertThat(response)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("No log found for api: " + API + " and requestId: request-id");
        }
    }
}
