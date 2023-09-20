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
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.repository.ConnectionLogFixtures;
import fixtures.repository.MessageLogFixtures;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.ConnectionLogCrudServiceInMemory;
import inmemory.InMemoryCrudService;
import inmemory.MessageLogCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import io.gravitee.rest.api.management.v2.rest.model.ApiLog;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogsResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiMessageLog;
import io.gravitee.rest.api.management.v2.rest.model.ApiMessageLogContent;
import io.gravitee.rest.api.management.v2.rest.model.ApiMessageLogsResponse;
import io.gravitee.rest.api.management.v2.rest.model.BaseApplication;
import io.gravitee.rest.api.management.v2.rest.model.BasePlan;
import io.gravitee.rest.api.management.v2.rest.model.HttpMethod;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.connector.ConnectorType;
import io.gravitee.rest.api.model.v4.log.message.MessageOperation;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ApiLogsResourceTest extends ApiResourceTest {

    private static final BasePlanEntity PLAN = BasePlanEntity.builder().id("plan1").name("1st plan").apiId(API).build();
    private static final BaseApplicationEntity APPLICATION = BaseApplicationEntity.builder().id("app1").name("an application name").build();
    public static final String REQUEST_ID = "request-id";

    final ConnectionLogFixtures connectionLogFixtures = new ConnectionLogFixtures(API, APPLICATION.getId(), PLAN.getId());
    final MessageLogFixtures messageLogFixtures = new MessageLogFixtures(API, REQUEST_ID);

    @Inject
    ConnectionLogCrudServiceInMemory connectionLogStorageService;

    @Inject
    MessageLogCrudServiceInMemory messageLogStorageService;

    @Inject
    PlanCrudServiceInMemory planStorageService;

    @Inject
    ApplicationCrudServiceInMemory applicationStorageService;

    WebTarget connectionLogsTarget;
    WebTarget messageLogsTarget;

    @BeforeEach
    public void setup() {
        connectionLogsTarget = rootTarget();
        messageLogsTarget = rootTarget().path(REQUEST_ID).path("messages");

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        planStorageService.initWith(List.of(PLAN));
        applicationStorageService.initWith(List.of(APPLICATION));
    }

    @Override
    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();

        Stream
            .of(applicationStorageService, connectionLogStorageService, messageLogStorageService, planStorageService)
            .forEach(InMemoryCrudService::reset);
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
                    ApiLogsResponse
                        .builder()
                        .data(
                            List.of(
                                ApiLog
                                    .builder()
                                    .application(BaseApplication.builder().id(APPLICATION.getId()).name(APPLICATION.getName()).build())
                                    .plan(BasePlan.builder().id(PLAN.getId()).name(PLAN.getName()).apiId(API).build())
                                    .method(HttpMethod.GET)
                                    .status(200)
                                    .clientIdentifier("client-identifier")
                                    .requestEnded(true)
                                    .requestId("req1")
                                    .transactionId("transaction-id")
                                    .timestamp(Instant.parse("2020-02-01T20:00:00.00Z").atOffset(ZoneOffset.UTC))
                                    .build()
                            )
                        )
                        .pagination(Pagination.builder().page(1).perPage(10).pageCount(1).pageItemsCount(1).totalCount(1L).build())
                        .links(Links.builder().self(connectionLogsTarget.getUri().toString()).build())
                        .build()
                );
        }

        @Test
        public void should_compute_pagination() {
            var total = 20L;
            var pageSize = 5;
            connectionLogStorageService.initWith(LongStream.range(0, total).mapToObj(i -> connectionLogFixtures.aConnectionLog()).toList());

            connectionLogsTarget =
                connectionLogsTarget
                    .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 2)
                    .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, pageSize);
            final Response response = connectionLogsTarget.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiLogsResponse.class)
                .extracting(ApiLogsResponse::getPagination)
                .isEqualTo(Pagination.builder().page(2).perPage(pageSize).pageCount(4).pageItemsCount(pageSize).totalCount(total).build());
        }

        @Test
        public void should_compute_links() {
            var total = 20L;
            var page = 2;
            var pageSize = 5;
            connectionLogStorageService.initWith(LongStream.range(0, total).mapToObj(i -> connectionLogFixtures.aConnectionLog()).toList());

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
                    Links
                        .builder()
                        .self(connectionLogsTarget.queryParam("page", page).queryParam("perPage", pageSize).getUri().toString())
                        .first(connectionLogsTarget.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                        .last(connectionLogsTarget.queryParam("page", 4).queryParam("perPage", pageSize).getUri().toString())
                        .previous(connectionLogsTarget.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                        .next(connectionLogsTarget.queryParam("page", 3).queryParam("perPage", pageSize).getUri().toString())
                        .build()
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
            messageLogStorageService.initWith(List.of(messageLogFixtures.aMessageLog(REQUEST_ID)));

            final Response response = messageLogsTarget.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiMessageLogsResponse.class)
                .isEqualTo(
                    ApiMessageLogsResponse
                        .builder()
                        .data(
                            List.of(
                                ApiMessageLog
                                    .builder()
                                    .requestId(REQUEST_ID)
                                    .clientIdentifier("client-identifier")
                                    .timestamp(Instant.parse("2020-02-01T20:00:00.00Z").atOffset(ZoneOffset.UTC))
                                    .correlationId("correlation-id")
                                    .parentCorrelationId("parent-correlation-id")
                                    .connectorType(ConnectorType.ENTRYPOINT.name())
                                    .connectorId("http-get")
                                    .operation(MessageOperation.SUBSCRIBE.name())
                                    .message(
                                        ApiMessageLogContent
                                            .builder()
                                            .id("message-id")
                                            .payload("message-payload")
                                            .headers(Map.of("X-Header", List.of("header-value")))
                                            .metadata(Map.of("X-Metdata", "metadata-value"))
                                            .build()
                                    )
                                    .build()
                            )
                        )
                        .pagination(Pagination.builder().page(1).perPage(10).pageCount(1).pageItemsCount(1).totalCount(1L).build())
                        .links(Links.builder().self(messageLogsTarget.getUri().toString()).build())
                        .build()
                );
        }

        @Test
        public void should_compute_pagination() {
            var total = 20L;
            var pageSize = 5;
            messageLogStorageService.initWith(
                LongStream.range(0, total).mapToObj(i -> messageLogFixtures.aMessageLogWithMessageId(String.valueOf(i))).toList()
            );

            messageLogsTarget =
                messageLogsTarget
                    .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 2)
                    .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, pageSize);
            final Response response = messageLogsTarget.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiLogsResponse.class)
                .extracting(ApiLogsResponse::getPagination)
                .isEqualTo(Pagination.builder().page(2).perPage(pageSize).pageCount(4).pageItemsCount(pageSize).totalCount(total).build());
        }

        @Test
        public void should_compute_links() {
            var total = 20L;
            var page = 2;
            var pageSize = 5;
            messageLogStorageService.initWith(
                LongStream.range(0, total).mapToObj(i -> messageLogFixtures.aMessageLogWithMessageId(String.valueOf(i))).toList()
            );

            final Response response = messageLogsTarget
                .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, page)
                .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, pageSize)
                .request()
                .get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiLogsResponse.class)
                .extracting(ApiLogsResponse::getLinks)
                .isEqualTo(
                    Links
                        .builder()
                        .self(messageLogsTarget.queryParam("page", page).queryParam("perPage", pageSize).getUri().toString())
                        .first(messageLogsTarget.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                        .last(messageLogsTarget.queryParam("page", 4).queryParam("perPage", pageSize).getUri().toString())
                        .previous(messageLogsTarget.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                        .next(messageLogsTarget.queryParam("page", 3).queryParam("perPage", pageSize).getUri().toString())
                        .build()
                );
        }
    }
}
