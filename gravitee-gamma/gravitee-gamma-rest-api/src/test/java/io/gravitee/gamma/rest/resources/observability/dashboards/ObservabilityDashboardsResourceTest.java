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
package io.gravitee.gamma.rest.resources.observability.dashboards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.observability.dashboard.inmemory.InMemoryDashboardRepository;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardFilter;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRange;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRangeType;
import io.gravitee.gamma.rest.core.observability.dashboard.port.repository.DashboardRepository;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.GetObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.ListObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resources.observability.dashboards.ObservabilityDashboardsResourceTest.DashboardsTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

/**
 * Drives the resources through the <em>real</em> use cases, wired to an in-memory
 * {@link DashboardRepository}. Only the platform boundaries the resource can't run without
 * ({@code PermissionService}, {@code EnvironmentService}) stay mocked, via
 * {@link ResourceContextConfiguration} — so behaviour like environment isolation and pagination is
 * exercised end-to-end rather than stubbed at the use-case boundary.
 */
@ContextConfiguration(classes = { ResourceContextConfiguration.class, DashboardsTestConfiguration.class })
class ObservabilityDashboardsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final String OTHER_ENVIRONMENT = "other-env";
    private static final String DASHBOARD_ID = "dash-1";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    private InMemoryDashboardRepository dashboardRepository;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/observability/dashboards";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
    }

    @AfterEach
    void resetRepository() {
        dashboardRepository.reset();
    }

    @Nested
    class ListDashboards {

        @Test
        void should_return_200_with_data_envelope_and_label_and_uppercase_operator() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("data")).hasSize(1);
            JsonNode dto = body.get("data").get(0);
            assertThat(dto.get("id").asText()).isEqualTo(DASHBOARD_ID);
            assertThat(dto.get("version").asInt()).isEqualTo(3);
            assertThat(dto.get("timeRange").get("type").asText()).isEqualTo("relative");
            JsonNode filter = dto.get("filters").get(0);
            assertThat(filter.get("name").asText()).isEqualTo("API_TYPE");
            assertThat(filter.get("label").asText()).isEqualTo("API Type");
            assertThat(filter.get("operator").asText()).isEqualTo("EQ");
            assertThat(filter.get("value").get(0).asText()).isEqualTo("MCP");
            assertThat(filter.get("editable").asBoolean()).isFalse();
            assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(1L);
            assertThat(body.get("pagination").get("page").asInt()).isEqualTo(1);
            assertThat(body.get("pagination").get("perPage").asInt()).isEqualTo(20);
        }

        @Test
        void should_only_return_dashboards_of_the_context_environment() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));
            dashboardRepository.givenDashboard(dashboard("other-dash", OTHER_ENVIRONMENT));

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("data")).hasSize(1);
            assertThat(body.get("data").get(0).get("id").asText()).isEqualTo(DASHBOARD_ID);
            assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(1L);
        }

        @Test
        void should_return_the_requested_page() {
            for (int i = 1; i <= 5; i++) {
                dashboardRepository.givenDashboard(dashboard("dash-" + i, ENVIRONMENT));
            }

            Response response = rootTarget().queryParam("page", 2).queryParam("perPage", 2).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("data")).hasSize(2);
            assertThat(body.get("data").get(0).get("id").asText()).isEqualTo("dash-3");
            assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(5L);
            assertThat(body.get("pagination").get("page").asInt()).isEqualTo(2);
            assertThat(body.get("pagination").get("perPage").asInt()).isEqualTo(2);
        }

        @Test
        void should_return_an_empty_page_when_no_dashboard_exists() {
            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("data")).isEmpty();
            assertThat(body.get("pagination").get("totalCount").asLong()).isZero();
        }

        @Test
        void should_return_403_when_caller_cannot_read_dashboards() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }
    }

    @Nested
    class GetDashboard {

        @Test
        void should_return_200_with_the_dashboard_and_its_widgets_verbatim() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("id").asText()).isEqualTo(DASHBOARD_ID);
            assertThat(body.get("title").asText()).isEqualTo("Performance overview");
            assertThat(body.get("widgets").get(0).get("type").asText()).isEqualTo("metric");
            assertThat(body.get("timeRange").get("period").asText()).isEqualTo("24h");
        }

        @Test
        void should_return_404_when_dashboard_does_not_exist() {
            Response response = rootTarget("unknown").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_404_when_dashboard_belongs_to_another_environment() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, OTHER_ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_403_when_caller_cannot_read_dashboards() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            Response response = rootTarget(DASHBOARD_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }
    }

    private static Dashboard dashboard(String id, String environmentId) {
        try {
            return new Dashboard(
                id,
                environmentId,
                "Performance overview",
                "desc",
                List.of(new DashboardFilter(new FilterCondition("API_TYPE", FilterOperator.EQ, List.of("MCP")), "API Type", false)),
                new TimeRange(TimeRangeType.RELATIVE, "24h", null, null),
                MAPPER.readTree("[{\"type\":\"metric\"}]"),
                3,
                "user-1",
                Instant.parse("2026-06-10T00:00:00Z"),
                Instant.parse("2026-06-11T00:00:00Z")
            );
        } catch (Exception e) {
            throw new IllegalStateException("Invalid widgets fixture", e);
        }
    }

    @Configuration
    static class DashboardsTestConfiguration {

        @Bean
        InMemoryDashboardRepository dashboardRepository() {
            return new InMemoryDashboardRepository();
        }

        @Bean
        ListObservabilityDashboardUseCase listObservabilityDashboardUseCase(DashboardRepository dashboardRepository) {
            return new ListObservabilityDashboardUseCase(dashboardRepository);
        }

        @Bean
        GetObservabilityDashboardUseCase getObservabilityDashboardUseCase(DashboardRepository dashboardRepository) {
            return new GetObservabilityDashboardUseCase(dashboardRepository);
        }
    }
}
