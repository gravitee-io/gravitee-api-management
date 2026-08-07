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
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.CreateObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.DeleteObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.GetObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.ListObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.UpdateObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resources.observability.dashboards.ObservabilityDashboardsResourceTest.DashboardsTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
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

    @Nested
    class CreateDashboard {

        @Test
        void should_return_201_with_location_and_the_created_dashboard() {
            Response response = rootTarget()
                .request()
                .post(
                    Entity.json(
                        """
                        {
                          "id": "dash-new",
                          "title": "Performance overview",
                          "description": "desc",
                          "filters": [
                            { "name": "API_TYPE", "label": "API Type", "operator": "eq", "value": "MCP" },
                            { "name": "HTTP_STATUS", "label": "Status Code", "operator": "IN", "value": [], "editable": true }
                          ],
                          "timeRange": { "type": "relative", "period": "24h" },
                          "widgets": [{ "id": "w1", "type": "metric" }]
                        }
                        """
                    )
                );

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            assertThat(response.getHeaderString("Location")).endsWith("/observability/dashboards/dash-new");
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("id").asText()).isEqualTo("dash-new");
            assertThat(body.get("version").asInt()).isEqualTo(1);
            assertThat(body.get("createdAt").isNumber()).isTrue();
            assertThat(body.get("updatedAt").isNumber()).isTrue();
            JsonNode firstFilter = body.get("filters").get(0);
            assertThat(firstFilter.get("operator").asText()).isEqualTo("EQ");
            assertThat(firstFilter.get("value").get(0).asText()).isEqualTo("MCP");
            assertThat(firstFilter.get("editable").asBoolean()).isFalse();
            JsonNode secondFilter = body.get("filters").get(1);
            assertThat(secondFilter.get("value")).isEmpty();
            assertThat(secondFilter.get("editable").asBoolean()).isTrue();
            assertThat(body.get("widgets").get(0).get("type").asText()).isEqualTo("metric");

            var persisted = dashboardRepository.findByIdAndEnvironmentId("dash-new", ENVIRONMENT).orElseThrow();
            assertThat(persisted.createdBy()).isEqualTo(USER_NAME);

            JsonNode listed = rootTarget().request().get().readEntity(JsonNode.class);
            assertThat(listed.get("data")).hasSize(1);
            assertThat(listed.get("data").get(0).get("id").asText()).isEqualTo("dash-new");
        }

        @Test
        void should_ignore_server_owned_fields_sent_in_the_body() {
            Response response = rootTarget()
                .request()
                .post(
                    Entity.json(
                        """
                        {
                          "id": "dash-new",
                          "title": "Performance overview",
                          "version": 42,
                          "createdBy": "someone-else",
                          "createdAt": 1,
                          "updatedAt": 1,
                          "environmentId": "other-env"
                        }
                        """
                    )
                );

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("version").asInt()).isEqualTo(1);
            assertThat(dashboardRepository.findByIdAndEnvironmentId("dash-new", ENVIRONMENT)).isPresent();
        }

        @Test
        void should_return_400_when_title_is_blank() {
            Response response = rootTarget().request().post(Entity.json("{ \"id\": \"dash-new\", \"title\": \"  \" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_accept_fifty_widgets_and_reject_fifty_one() {
            Response accepted = rootTarget().request().post(Entity.json(payloadWithWidgets(50)));
            assertThat(accepted.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);

            Response rejected = rootTarget().request().post(Entity.json(payloadWithWidgets(51)));
            assertThat(rejected.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_400_on_duplicate_widget_ids() {
            Response response = rootTarget()
                .request()
                .post(Entity.json("{ \"id\": \"dash-new\", \"title\": \"Perf\", \"widgets\": [{ \"id\": \"w1\" }, { \"id\": \"w1\" }] }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_400_on_an_incoherent_time_range() {
            Response response = rootTarget()
                .request()
                .post(Entity.json("{ \"id\": \"dash-new\", \"title\": \"Perf\", \"timeRange\": { \"type\": \"relative\" } }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_400_on_a_null_filter_entry() {
            Response response = rootTarget()
                .request()
                .post(Entity.json("{ \"id\": \"dash-new\", \"title\": \"Perf\", \"filters\": [null] }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_400_on_an_unknown_filter_operator() {
            Response response = rootTarget()
                .request()
                .post(
                    Entity.json(
                        "{ \"id\": \"dash-new\", \"title\": \"Perf\", \"filters\": [{ \"name\": \"API_TYPE\", \"operator\": \"between\", \"value\": \"x\" }] }"
                    )
                );

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_403_when_caller_cannot_create_dashboards() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            Response response = rootTarget().request().post(Entity.json("{ \"id\": \"dash-new\", \"title\": \"Perf\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }

        private static String payloadWithWidgets(int count) {
            StringBuilder widgets = new StringBuilder("[");
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    widgets.append(',');
                }
                widgets.append("{\"id\":\"w").append(i).append("\"}");
            }
            widgets.append(']');
            return "{ \"id\": \"dash-new\", \"title\": \"Perf\", \"widgets\": " + widgets + " }";
        }
    }

    @Nested
    class UpdateDashboard {

        @Test
        void should_return_200_preserve_creation_fields_bump_version_and_updated_at() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .put(
                    Entity.json(
                        """
                        {
                          "title": "Renamed",
                          "description": "new desc",
                          "version": 42,
                          "timeRange": { "type": "absolute", "from": 1000, "to": 2000 },
                          "widgets": [{ "id": "w1", "type": "chart" }]
                        }
                        """
                    )
                );

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("title").asText()).isEqualTo("Renamed");
            assertThat(body.get("version").asInt()).isEqualTo(4);
            assertThat(body.get("createdAt").asLong()).isEqualTo(Instant.parse("2026-06-10T00:00:00Z").toEpochMilli());
            assertThat(body.get("updatedAt").asLong()).isGreaterThan(Instant.parse("2026-06-11T00:00:00Z").toEpochMilli());
            assertThat(body.get("timeRange").get("type").asText()).isEqualTo("absolute");
            assertThat(body.get("widgets").get(0).get("type").asText()).isEqualTo("chart");

            var persisted = dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENVIRONMENT).orElseThrow();
            assertThat(persisted.createdBy()).isEqualTo("user-1");
            assertThat(persisted.version()).isEqualTo(4);
        }

        @Test
        void should_return_404_when_dashboard_does_not_exist() {
            Response response = rootTarget("unknown").request().put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_404_when_dashboard_belongs_to_another_environment() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, OTHER_ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID).request().put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_400_when_title_is_blank() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID).request().put(Entity.json("{ \"title\": \" \" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_403_when_caller_cannot_update_dashboards() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            Response response = rootTarget(DASHBOARD_ID).request().put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }
    }

    @Nested
    class DeleteDashboard {

        @Test
        void should_return_204_and_remove_the_dashboard() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID).request().delete();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
            JsonNode listed = rootTarget().request().get().readEntity(JsonNode.class);
            assertThat(listed.get("data")).isEmpty();
        }

        @Test
        void should_return_404_when_dashboard_does_not_exist() {
            Response response = rootTarget("unknown").request().delete();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_404_and_keep_the_dashboard_when_it_belongs_to_another_environment() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, OTHER_ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID).request().delete();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, OTHER_ENVIRONMENT)).isPresent();
        }

        @Test
        void should_return_403_when_caller_cannot_delete_dashboards() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            Response response = rootTarget(DASHBOARD_ID).request().delete();

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

        @Bean
        CreateObservabilityDashboardUseCase createObservabilityDashboardUseCase(DashboardRepository dashboardRepository) {
            return new CreateObservabilityDashboardUseCase(dashboardRepository);
        }

        @Bean
        UpdateObservabilityDashboardUseCase updateObservabilityDashboardUseCase(DashboardRepository dashboardRepository) {
            return new UpdateObservabilityDashboardUseCase(dashboardRepository);
        }

        @Bean
        DeleteObservabilityDashboardUseCase deleteObservabilityDashboardUseCase(DashboardRepository dashboardRepository) {
            return new DeleteObservabilityDashboardUseCase(dashboardRepository);
        }
    }
}
