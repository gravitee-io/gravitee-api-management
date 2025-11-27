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
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.Mockito.when;

import inmemory.InMemoryAlternative;
import inmemory.MessageLogsCrudServiceInMemory;
import io.gravitee.apim.core.log.model.MessageLog;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.resource.api.log.param.SearchMessageLogsParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiMessageLogsResourceTest extends ApiResourceTest {

    @Inject
    MessageLogsCrudServiceInMemory messageMetricsCrudServiceInMemory;

    WebTarget messageMetricsTarget;

    @BeforeEach
    void setup() {
        messageMetricsTarget = rootTarget();

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();

        Stream.of(messageMetricsCrudServiceInMemory).forEach(InMemoryAlternative::reset);
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/logs/messages";
    }

    @Test
    void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_LOG, API, RolePermissionAction.READ)
        ).thenReturn(false);

        final Response response = messageMetricsTarget.request().get();

        assertThat(response)
            .hasStatus(FORBIDDEN_403)
            .asError()
            .hasHttpStatus(FORBIDDEN_403)
            .hasMessage("You do not have sufficient rights to access this resource");
    }

    @Test
    void should_return_message_metrics() {
        messageMetricsCrudServiceInMemory.initWith(List.of(MessageLog.builder().apiId(API).timestamp("2025-11-11T06:57:44.893Z").build()));

        final Response response = messageMetricsTarget.request().get();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiMessageLogsResponse.class)
            .satisfies(metricResponse -> {
                assertThat(metricResponse.getPagination()).isEqualTo(
                    new Pagination().page(1).perPage(10).pageCount(1).pageItemsCount(1).totalCount(1L)
                );
                assertThat(metricResponse.getLinks()).isEqualTo(new Links().self(messageMetricsTarget.getUri().toString()));
                assertThat(metricResponse.getData())
                    .hasSize(1)
                    .first()
                    .satisfies(mm -> {
                        assertThat(mm.getApiId()).isEqualTo(API);
                        assertThat(mm.getTimestamp()).isEqualTo(
                            OffsetDateTime.of(2025, 11, 11, 6, 57, 44, 893 * 1_000_000, ZoneOffset.UTC)
                        );
                    });
            });
    }

    @Test
    void should_compute_pagination() {
        var total = 20L;
        var pageSize = 5;
        messageMetricsCrudServiceInMemory.initWith(
            LongStream.range(0, total)
                .mapToObj(i -> MessageLog.builder().timestamp("2025-11-11T06:57:44.893Z").apiId(API).build())
                .toList()
        );

        final Response response = messageMetricsTarget
            .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 2)
            .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, pageSize)
            .request()
            .get();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiMessageLogsResponse.class)
            .extracting(ApiMessageLogsResponse::getPagination)
            .isEqualTo(new Pagination().page(2).perPage(pageSize).pageCount(4).pageItemsCount(pageSize).totalCount(total));
    }

    @Test
    void should_compute_links() {
        var total = 20L;
        var page = 2;
        var pageSize = 5;
        messageMetricsCrudServiceInMemory.initWith(
            LongStream.range(0, total)
                .mapToObj(i -> MessageLog.builder().timestamp("2025-11-11T06:57:44.893Z").apiId(API).build())
                .toList()
        );

        final Response response = messageMetricsTarget
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
                    .self(messageMetricsTarget.queryParam("page", page).queryParam("perPage", pageSize).getUri().toString())
                    .first(messageMetricsTarget.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                    .last(messageMetricsTarget.queryParam("page", 4).queryParam("perPage", pageSize).getUri().toString())
                    .previous(messageMetricsTarget.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                    .next(messageMetricsTarget.queryParam("page", 3).queryParam("perPage", pageSize).getUri().toString())
            );
    }

    @Test
    void should_filter_on_api_id() {
        messageMetricsCrudServiceInMemory.initWith(
            List.of(
                MessageLog.builder().apiId(API).timestamp("2025-11-11T06:57:44.893Z").build(),
                MessageLog.builder().apiId("other-api").timestamp("2025-11-11T06:57:44.893Z").build()
            )
        );

        final Response response = messageMetricsTarget.request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(ApiMessageLogsResponse.class)
            .extracting(ApiMessageLogsResponse::getData)
            .satisfies(data -> {
                assertThat(data).hasSize(1);
                assertThat(data)
                    .first()
                    .satisfies(m -> assertThat(m.getApiId()).isEqualTo(API));
            });
    }

    @Test
    void should_filter_on_all_param() {
        String expectedCorrelationId = "this is the one we want to find";
        String expectedConnectorType = "entrypoint";
        String expectedConnectorId = "webhook";
        String expectedOperation = "subscribe";
        String expectedRequestId = "123456789";
        messageMetricsCrudServiceInMemory.initWith(
            List.of(
                MessageLog.builder()
                    .apiId(API)
                    .timestamp("2025-11-11T06:57:44.893Z")
                    .connectorType(expectedConnectorType)
                    .connectorId(expectedConnectorId)
                    .operation(expectedOperation)
                    .requestId(expectedRequestId)
                    .correlationId(expectedCorrelationId)
                    .build(),
                MessageLog.builder()
                    .apiId(API)
                    .timestamp("2025-11-11T06:57:44.893Z")
                    .connectorType("endpoint")
                    .connectorId(expectedConnectorId)
                    .operation(expectedOperation)
                    .requestId(expectedRequestId)
                    .correlationId("don't want this one (endpoint)")
                    .build(),
                MessageLog.builder()
                    .apiId(API)
                    .timestamp("2025-11-11T06:57:44.893Z")
                    .connectorType(expectedConnectorType)
                    .connectorId("http-get")
                    .operation(expectedOperation)
                    .requestId(expectedRequestId)
                    .correlationId("don't want this one (http-get)")
                    .build(),
                MessageLog.builder()
                    .apiId(API)
                    .timestamp("2025-11-11T06:57:44.893Z")
                    .connectorType(expectedConnectorType)
                    .connectorId(expectedConnectorId)
                    .operation("publish")
                    .requestId(expectedRequestId)
                    .correlationId("don't want this one (publish)")
                    .build(),
                MessageLog.builder()
                    .apiId(API)
                    .timestamp("2025-11-11T06:57:44.893Z")
                    .connectorType(expectedConnectorType)
                    .connectorId(expectedConnectorId)
                    .operation(expectedOperation)
                    .requestId("0000000000")
                    .correlationId("don't want this one (wrong request id)")
                    .build()
            )
        );

        final Response response = messageMetricsTarget
            .queryParam(SearchMessageLogsParam.CONNECTOR_ID_PARAM_NAME, expectedConnectorId)
            .queryParam(SearchMessageLogsParam.CONNECTOR_TYPE_PARAM_NAME, ConnectorType.ENTRYPOINT.getValue())
            .queryParam(SearchMessageLogsParam.OPERATION_PARAM_NAME, MessageOperation.SUBSCRIBE.getValue())
            .queryParam(SearchMessageLogsParam.REQUEST_ID_PARAM_NAME, expectedRequestId)
            .request()
            .get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(ApiMessageLogsResponse.class)
            .extracting(ApiMessageLogsResponse::getData)
            .satisfies(data -> {
                assertThat(data).hasSize(1);
                assertThat(data)
                    .first()
                    .satisfies(m -> assertThat(m.getCorrelationId()).isEqualTo(expectedCorrelationId));
            });
    }

    @Test
    void should_filter_on_time_range() {
        String expectedCorrelationId = "this is the one we want to find";
        messageMetricsCrudServiceInMemory.initWith(
            List.of(
                MessageLog.builder()
                    .apiId(API)
                    .timestamp("2025-11-11T05:59:44.893Z")
                    .connectorType("entrypoint")
                    .connectorId("webhook")
                    .operation("subscribe")
                    .requestId("123456789")
                    .correlationId("don't want this one (too soon)")
                    .build(),
                MessageLog.builder()
                    .apiId(API)
                    .timestamp("2025-11-11T06:30:45.893Z")
                    .connectorType("entrypoint")
                    .connectorId("webhook")
                    .operation("subscribe")
                    .requestId("123456789")
                    .correlationId(expectedCorrelationId)
                    .build(),
                MessageLog.builder()
                    .apiId(API)
                    .timestamp("2025-11-11T07:00:44.893Z")
                    .connectorType("entrypoint")
                    .connectorId("webhook")
                    .operation("subscribe")
                    .requestId("123456789")
                    .correlationId("don't want this one (too late)")
                    .build()
            )
        );

        final Response response = messageMetricsTarget
            .queryParam(SearchMessageLogsParam.CONNECTOR_ID_PARAM_NAME, "webhook")
            .queryParam(SearchMessageLogsParam.CONNECTOR_TYPE_PARAM_NAME, ConnectorType.ENTRYPOINT.getValue())
            .queryParam(SearchMessageLogsParam.OPERATION_PARAM_NAME, MessageOperation.SUBSCRIBE.getValue())
            .queryParam(SearchMessageLogsParam.REQUEST_ID_PARAM_NAME, "123456789")
            .queryParam(
                SearchMessageLogsParam.FROM_QUERY_PARAM_NAME,
                OffsetDateTime.of(2025, 11, 11, 6, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
            )
            .queryParam(
                SearchMessageLogsParam.TO_QUERY_PARAM_NAME,
                OffsetDateTime.of(2025, 11, 11, 7, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
            )
            .request()
            .get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(ApiMessageLogsResponse.class)
            .extracting(ApiMessageLogsResponse::getData)
            .satisfies(data -> {
                assertThat(data).hasSize(1);
                assertThat(data)
                    .first()
                    .satisfies(m -> assertThat(m.getCorrelationId()).isEqualTo(expectedCorrelationId));
            });
    }

    @Test
    void should_get_400_error_on_invalid_connector_type() {
        final Response response = messageMetricsTarget
            .queryParam(SearchMessageLogsParam.CONNECTOR_TYPE_PARAM_NAME, "invalid-connector-type")
            .request()
            .get();

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        assertThat(response.readEntity(String.class)).contains("invalid-connector-type").contains("ENTRYPOINT");
    }

    @Test
    void should_get_400_error_on_invalid_operation() {
        final Response response = messageMetricsTarget
            .queryParam(SearchMessageLogsParam.OPERATION_PARAM_NAME, "invalid-operation")
            .request()
            .get();
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        assertThat(response.readEntity(String.class)).contains("invalid-operation").contains("PUBLISH");
    }
}
