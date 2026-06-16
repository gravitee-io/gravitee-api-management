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
package io.gravitee.gamma.rest.resource.observability.logs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.observability.logs.model.HttpPayload;
import io.gravitee.gamma.rest.core.observability.logs.model.LogDetail;
import io.gravitee.gamma.rest.core.observability.logs.model.LogEntry;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsPage;
import io.gravitee.gamma.rest.core.observability.logs.use_case.GetObservabilityLogDetailUseCase;
import io.gravitee.gamma.rest.core.observability.logs.use_case.SearchObservabilityLogsUseCase;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resource.observability.logs.LogsResourceTest.LogsTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { ResourceContextConfiguration.class, LogsTestConfiguration.class })
class LogsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";

    @Inject
    private SearchObservabilityLogsUseCase searchLogsUseCase;

    @Inject
    private GetObservabilityLogDetailUseCase getLogDetailUseCase;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/observability/logs";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
    }

    @AfterEach
    void resetUseCases() {
        reset(searchLogsUseCase, getLogDetailUseCase);
    }

    @Nested
    class SearchLogs {

        @Test
        void should_return_200_with_paginated_envelope() {
            var entry = LogEntry.builder()
                .apiId("api-1")
                .apiName("Petstore")
                .requestId("req-1")
                .timestamp(Instant.parse("2026-06-10T10:00:00Z"))
                .status(200)
                .method("GET")
                .uri("/pets")
                .gatewayResponseTime(42)
                .build();
            when(searchLogsUseCase.execute(any())).thenReturn(
                new SearchObservabilityLogsUseCase.Output(new LogsPage(List.of(entry), 1), 1, 20)
            );

            Response response = rootTarget("search").request().post(Entity.entity(Map.of(), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("data")).hasSize(1);
            assertThat(body.get("data").get(0).get("apiId").asText()).isEqualTo("api-1");
            assertThat(body.get("data").get(0).get("apiName").asText()).isEqualTo("Petstore");
            assertThat(body.get("data").get(0).get("status").asInt()).isEqualTo(200);
            assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(1L);
            assertThat(body.get("pagination").get("page").asInt()).isEqualTo(1);
        }

        @Test
        void should_forward_page_and_perPage_to_use_case() {
            when(searchLogsUseCase.execute(any())).thenReturn(new SearchObservabilityLogsUseCase.Output(LogsPage.EMPTY, 2, 5));

            rootTarget("search")
                .queryParam("page", 2)
                .queryParam("perPage", 5)
                .request()
                .post(Entity.entity(Map.of(), MediaType.APPLICATION_JSON_TYPE));

            var captor = ArgumentCaptor.forClass(SearchObservabilityLogsUseCase.Input.class);
            Mockito.verify(searchLogsUseCase).execute(captor.capture());
            assertThat(captor.getValue().page()).isEqualTo(2);
            assertThat(captor.getValue().perPage()).isEqualTo(5);
        }

        @Test
        void should_forward_filters_and_time_range_to_use_case() {
            when(searchLogsUseCase.execute(any())).thenReturn(new SearchObservabilityLogsUseCase.Output(LogsPage.EMPTY, 1, 20));

            var body = Map.of(
                "timeRange",
                Map.of("from", "2026-06-10T00:00:00Z", "to", "2026-06-11T00:00:00Z"),
                "filters",
                List.of(Map.of("name", "HTTP_STATUS", "operator", "EQ", "value", "200"))
            );

            rootTarget("search").request().post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));

            var captor = ArgumentCaptor.forClass(SearchObservabilityLogsUseCase.Input.class);
            verify(searchLogsUseCase).execute(captor.capture());
            assertThat(captor.getValue().from()).isNotNull();
            assertThat(captor.getValue().to()).isNotNull();
            assertThat(captor.getValue().filters()).hasSize(1);
            assertThat(captor.getValue().filters().getFirst().name()).isEqualTo("HTTP_STATUS");
        }

        @Test
        void should_return_200_with_empty_body() {
            when(searchLogsUseCase.execute(any())).thenReturn(new SearchObservabilityLogsUseCase.Output(LogsPage.EMPTY, 1, 20));

            Response response = rootTarget("search").request().post(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_return_403_when_caller_cannot_read_observability() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            Response response = rootTarget("search").request().post(Entity.entity(Map.of(), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
            verifyNoInteractions(searchLogsUseCase);
        }
    }

    @Nested
    class GetLogDetail {

        private static final String API_ID = "api-1";
        private static final String REQUEST_ID = "req-123";

        @Test
        void should_return_200_with_merged_detail() {
            var detail = LogDetail.builder()
                .requestId(REQUEST_ID)
                .apiId(API_ID)
                .transactionId("txn-1")
                .timestamp(Instant.parse("2026-06-10T14:32:01Z"))
                .status(200)
                .method("GET")
                .uri("/pets/42")
                .planName("Gold")
                .applicationName("Mobile App")
                .gatewayHostname("gw-eu.example.com")
                .gatewayResponseTime(12L)
                .entrypointRequest(
                    HttpPayload.builder().method("GET").uri("/pets/42").headers(Map.of("Accept", List.of("application/json"))).build()
                )
                .entrypointResponse(HttpPayload.builder().status(200).body("{\"id\":42}").build())
                .build();
            when(getLogDetailUseCase.execute(any())).thenReturn(new GetObservabilityLogDetailUseCase.Output(Optional.of(detail)));

            Response response = rootTarget(REQUEST_ID).queryParam("apiId", API_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("requestId").asText()).isEqualTo(REQUEST_ID);
            assertThat(body.get("apiId").asText()).isEqualTo(API_ID);
            assertThat(body.get("status").asInt()).isEqualTo(200);
            assertThat(body.get("planName").asText()).isEqualTo("Gold");
            assertThat(body.get("entrypointRequest").get("method").asText()).isEqualTo("GET");
            assertThat(body.get("entrypointResponse").get("body").asText()).isEqualTo("{\"id\":42}");
        }

        @Test
        void should_return_400_when_apiId_is_missing() {
            Response response = rootTarget(REQUEST_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            verifyNoInteractions(getLogDetailUseCase);
        }

        @Test
        void should_return_404_when_log_not_found() {
            when(getLogDetailUseCase.execute(any())).thenReturn(new GetObservabilityLogDetailUseCase.Output(Optional.empty()));

            Response response = rootTarget(REQUEST_ID).queryParam("apiId", API_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_404_when_caller_has_no_api_log_permission() {
            when(permissionService.hasPermission(any(), eq(RolePermission.API_LOG), eq(API_ID), eq(RolePermissionAction.READ))).thenReturn(
                false
            );

            Response response = rootTarget(REQUEST_ID).queryParam("apiId", API_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            verifyNoInteractions(getLogDetailUseCase);
        }

        @Test
        void should_forward_apiId_and_requestId_to_use_case() {
            when(getLogDetailUseCase.execute(any())).thenReturn(new GetObservabilityLogDetailUseCase.Output(Optional.empty()));

            rootTarget(REQUEST_ID).queryParam("apiId", API_ID).request().get();

            var captor = ArgumentCaptor.forClass(GetObservabilityLogDetailUseCase.Input.class);
            verify(getLogDetailUseCase).execute(captor.capture());
            assertThat(captor.getValue().apiId()).isEqualTo(API_ID);
            assertThat(captor.getValue().requestId()).isEqualTo(REQUEST_ID);
        }
    }

    @Configuration
    static class LogsTestConfiguration {

        @Bean
        SearchObservabilityLogsUseCase searchObservabilityLogsUseCase() {
            return mock(SearchObservabilityLogsUseCase.class);
        }

        @Bean
        GetObservabilityLogDetailUseCase getObservabilityLogDetailUseCase() {
            return mock(GetObservabilityLogDetailUseCase.class);
        }
    }
}
