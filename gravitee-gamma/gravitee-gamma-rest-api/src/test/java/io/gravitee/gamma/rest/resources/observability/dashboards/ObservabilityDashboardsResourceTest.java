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
import io.gravitee.apim.core.UseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.observability.dashboard.inmemory.InMemoryDashboardRepository;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardFilter;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRange;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRangeType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resources.observability.dashboards.ObservabilityDashboardsResourceTest.DashboardsTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ContextConfiguration;

/**
 * Drives the resources through the <em>real</em> use cases, wired to an in-memory
 * {@code DashboardRepository}. Only the platform boundaries the resource can't run without
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
            assertThat(response.getEntityTag()).as("the tag a later If-Match must echo").isEqualTo(new EntityTag("3"));
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
            assertThat(response.getEntityTag()).as("a creator can edit without re-reading").isEqualTo(new EntityTag("1"));
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
                .header(HttpHeaders.IF_MATCH, "\"3\"")
                .put(
                    Entity.json(
                        """
                        {
                          "title": "Renamed",
                          "description": "new desc",
                          "timeRange": { "type": "absolute", "from": 1000, "to": 2000 },
                          "widgets": [{ "id": "w1", "type": "chart" }]
                        }
                        """
                    )
                );

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.getEntityTag()).isEqualTo(new EntityTag("4"));
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

        /** A weak validator is what a proxy may hand back; it still identifies the revision unambiguously here. */
        @Test
        void should_accept_a_weak_entity_tag() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "W/\"3\"")
                .put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_return_412_with_the_current_dashboard_when_the_version_is_stale() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "\"2\"")
                .put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.PRECONDITION_FAILED_412);
            assertThat(response.getEntityTag()).as("the tag to retry with").isEqualTo(new EntityTag("3"));
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("http_status").asInt()).isEqualTo(HttpStatusCode.PRECONDITION_FAILED_412);
            assertThat(body.get("message").asText()).contains("modified since you loaded it");
            assertThat(body.get("currentVersion").asInt()).isEqualTo(3);
            assertThat(body.get("dashboard").get("title").asText()).isEqualTo("Performance overview");
            assertThat(body.get("dashboard").get("version").asInt()).isEqualTo(3);

            var persisted = dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENVIRONMENT).orElseThrow();
            assertThat(persisted.title()).isEqualTo("Performance overview");
            assertThat(persisted.version()).isEqualTo(3);
            assertThat(persisted.updatedAt()).isEqualTo(Instant.parse("2026-06-11T00:00:00Z"));
        }

        @Test
        void should_return_428_when_if_match_is_absent() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID).request().put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(428);
            assertThat(response.readEntity(JsonNode.class).get("message").asText()).contains("If-Match is required");
            assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENVIRONMENT).orElseThrow().title()).isEqualTo(
                "Performance overview"
            );
        }

        /** The deliberate overwrite: applied over whatever revision is current, without a stale-version refusal. */
        @Test
        void should_apply_a_wildcard_if_match_over_whatever_revision_is_current() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.json("{ \"title\": \"Overwritten\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.getEntityTag()).isEqualTo(new EntityTag("4"));
            assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENVIRONMENT).orElseThrow().title()).isEqualTo(
                "Overwritten"
            );
        }

        /** The whole point of the wildcard: the answer to a 412 is one request, not a re-read that can race again. */
        @Test
        void should_let_a_wildcard_overwrite_resolve_a_conflict_in_one_request() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response refused = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "\"2\"")
                .put(Entity.json("{ \"title\": \"Mine\" }"));
            assertThat(refused.getStatus()).isEqualTo(HttpStatusCode.PRECONDITION_FAILED_412);

            Response forced = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.json("{ \"title\": \"Mine\" }"));

            assertThat(forced.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENVIRONMENT).orElseThrow().title()).isEqualTo("Mine");
        }

        /** RFC 9110 allows a list; any one matching is enough. */
        @Test
        void should_accept_an_if_match_listing_several_validators() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "\"2\", \"3\"")
                .put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.getEntityTag()).isEqualTo(new EntityTag("4"));
        }

        @Test
        void should_return_412_when_no_validator_in_the_list_matches() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "\"1\", \"2\"")
                .put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.PRECONDITION_FAILED_412);
        }

        @Test
        void should_return_400_on_an_if_match_that_is_not_a_version() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "\"not-a-version\"")
                .put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_404_when_dashboard_does_not_exist() {
            Response response = rootTarget("unknown")
                .request()
                .header(HttpHeaders.IF_MATCH, "\"3\"")
                .put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_404_when_dashboard_belongs_to_another_environment() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, OTHER_ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "\"3\"")
                .put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_400_when_title_is_blank() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "\"3\"")
                .put(Entity.json("{ \"title\": \" \" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        /** The same list, spelled as a repeated field rather than comma-separated. HTTP treats them identically. */
        @Test
        void should_accept_if_match_repeated_as_several_headers() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "\"1\"")
                .header(HttpHeaders.IF_MATCH, "\"3\"")
                .put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.getEntityTag()).isEqualTo(new EntityTag("4"));
        }

        /** `*` and a specific validator state two different intents; guessing could silently overwrite. */
        @Test
        void should_return_400_when_the_wildcard_is_mixed_with_a_validator() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*, \"3\"")
                .put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENVIRONMENT).orElseThrow().title()).isEqualTo(
                "Performance overview"
            );
        }

        @Test
        void should_return_428_when_if_match_is_present_but_empty() {
            dashboardRepository.givenDashboard(dashboard(DASHBOARD_ID, ENVIRONMENT));

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "")
                .put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(428);
        }

        /**
         * A dashboard with no stored version can only be saved with `*`, so the refusal must not hand back an ETag
         * the client would then be refused for echoing. No version, no tag — and no currentVersion in the body.
         */
        @Test
        void should_refuse_without_an_etag_when_the_dashboard_carries_no_version() {
            dashboardRepository.givenDashboard(unversionedDashboard());

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "\"1\"")
                .put(Entity.json("{ \"title\": \"Renamed\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.PRECONDITION_FAILED_412);
            assertThat(response.getEntityTag()).as("nothing to match on, so nothing to hand back").isNull();
            JsonNode body = response.readEntity(JsonNode.class);
            // Null fields are omitted, not serialized as null (GraviteeMapper sets NON_NULL).
            assertThat(body.has("currentVersion")).isFalse();
            assertThat(body.get("dashboard").get("id").asText()).isEqualTo(DASHBOARD_ID);
            assertThat(body.get("dashboard").has("version")).isFalse();
        }

        @Test
        void should_omit_the_etag_on_a_get_of_a_dashboard_carrying_no_version() {
            dashboardRepository.givenDashboard(unversionedDashboard());

            Response response = rootTarget(DASHBOARD_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.getEntityTag()).isNull();
            assertThat(response.readEntity(JsonNode.class).has("version")).isFalse();
        }

        /** ...and the wildcard is then the way out: it saves and starts the counter. */
        @Test
        void should_let_a_wildcard_save_a_dashboard_carrying_no_version() {
            dashboardRepository.givenDashboard(unversionedDashboard());

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.json("{ \"title\": \"Rescued\" }"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.getEntityTag()).isEqualTo(new EntityTag("1"));
            assertThat(response.readEntity(JsonNode.class).get("version").asInt()).isEqualTo(1);
        }

        @Test
        void should_return_403_when_caller_cannot_update_dashboards() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            Response response = rootTarget(DASHBOARD_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "\"3\"")
                .put(Entity.json("{ \"title\": \"Renamed\" }"));

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

    /** Not reachable through the API — every dashboard is created with version 1 — but the model still allows it. */
    private static Dashboard unversionedDashboard() {
        Dashboard versioned = dashboard(DASHBOARD_ID, ENVIRONMENT);
        return new Dashboard(
            versioned.id(),
            versioned.environmentId(),
            versioned.title(),
            versioned.description(),
            versioned.filters(),
            versioned.timeRange(),
            versioned.widgets(),
            null,
            versioned.createdBy(),
            versioned.createdAt(),
            versioned.updatedAt()
        );
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

    /**
     * Mirrors the production wiring of {@code GammaDashboardsConfiguration}: the use cases come from
     * the same {@code @UseCase}-filtered scan rather than being declared one by one, so this test
     * keeps exercising whatever the real context would build. Only the port is substituted —
     * production derives it from the {@code GammaDashboardRepository} SPI, which the repository
     * plugin registers at runtime and which does not exist in a unit-test context.
     */
    @Configuration
    @ComponentScan(
        basePackages = "io.gravitee.gamma.rest.core.observability.dashboard",
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class)
    )
    static class DashboardsTestConfiguration {

        @Bean
        InMemoryDashboardRepository dashboardRepository() {
            return new InMemoryDashboardRepository();
        }
    }
}
