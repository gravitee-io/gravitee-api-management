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
package io.gravitee.gamma.rest.resources.observability.metricdefinitions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.UseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.InMemoryAdditionalMetricDefinitionRepository;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.InMemoryCatalogPorts;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricGrammarPort;
import io.gravitee.gamma.rest.core.observability.catalog.use_case.DeclareMetricDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.infra.adapter.AdditionalMetricGrammarPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resources.observability.metricdefinitions.MetricDefinitionsResourceTest.MetricDefinitionsTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ContextConfiguration;

/**
 * Drives the resources through the real use cases, wired to an in-memory record store and configurable stand-ins
 * for the built-in reads, the discovery pass and the traffic right.
 */
@ContextConfiguration(classes = { ResourceContextConfiguration.class, MetricDefinitionsTestConfiguration.class })
class MetricDefinitionsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final String OTHER_ENVIRONMENT = "other-env";
    private static final String BASKET = "additional-metrics.double_checkout_basket-value";
    private static final String SLASHED = "additional-metrics.keyword_my-policy_tools/call";

    @Inject
    private InMemoryAdditionalMetricDefinitionRepository repository;

    @Inject
    private InMemoryCatalogPorts ports;

    @Inject
    private DeclareMetricDefinitionUseCase declareUseCase;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/observability/metric-definitions";
    }

    @BeforeEach
    void prepareEnvironment() {
        for (String environment : List.of(ENVIRONMENT, OTHER_ENVIRONMENT)) {
            EnvironmentEntity env = new EnvironmentEntity();
            env.setId(environment);
            env.setOrganizationId(ORGANIZATION);
            when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, environment)).thenReturn(env);
        }
        ports.givenBuiltIn("keyword_mcp-proxy_tools/call", "MCP_PROXY_TOOL");
    }

    @AfterEach
    void reset() {
        repository.reset();
        ports.reset();
    }

    private WebTarget record(String name) {
        return rootTarget("record").queryParam("name", name);
    }

    private Response declare(Map<String, Object> body) {
        return rootTarget().request().post(Entity.json(body));
    }

    @Nested
    class Declare {

        @Test
        void should_declare_a_key_before_any_data_and_list_it_declared_without_last_seen() {
            Response created = declare(Map.of("name", BASKET, "label", "Basket value", "unit", "€"));

            assertThat(created.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            JsonNode body = created.readEntity(JsonNode.class);
            assertThat(body.get("name").asText()).isEqualTo(BASKET);
            assertThat(body.get("rawKey").asText()).isEqualTo("double_checkout_basket-value");
            assertThat(body.get("valueType").asText()).isEqualTo("DOUBLE");
            assertThat(body.get("exposed").asBoolean()).isTrue();
            assertThat(created.getLocation().getPath()).endsWith("/observability/metric-definitions/record");
            assertThat(created.getLocation().getQuery()).isEqualTo("name=" + BASKET);

            JsonNode listed = rootTarget().request().get().readEntity(JsonNode.class);
            assertThat(listed.get("refreshedAt").asText()).isEqualTo(InMemoryCatalogPorts.REFRESHED_AT.toString());
            JsonNode entry = entry(listed, BASKET);
            assertThat(entry.get("origin").asText()).isEqualTo("DECLARED");
            assertThat(entry.get("label").asText()).isEqualTo("Basket value");
            assertThat(entry.get("unit").asText()).isEqualTo("€");
            assertThat(entry.get("type").asText()).isEqualTo("number");
            assertThat(entry.has("lastSeen")).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(
            delimiter = '|',
            value = {
                "additional-metrics.keyword_mcp-proxy_tools/call | observability.metric_definition.built_in_key",
                "additional-metrics.double_custom_basket-value | observability.metric_definition.reserved_key",
                "additional-metrics.keyword_my\"key | observability.metric_definition.invalid_name",
                "additional-metrics.float_score | observability.metric_definition.unknown_type_prefix",
                "custom.basket | observability.metric_definition.invalid_name",
            }
        )
        void should_refuse_with_a_400_naming_the_rule(String name, String technicalCode) {
            Response response = declare(Map.of("name", name));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("technicalCode").asText()).isEqualTo(technicalCode);
        }

        @Test
        void should_locate_a_key_containing_a_slash_in_the_query() {
            Response created = declare(Map.of("name", SLASHED));

            assertThat(created.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            assertThat(created.getLocation().getPath()).endsWith("/observability/metric-definitions/record");
            assertThat(created.getLocation().getQuery()).isEqualTo("name=" + SLASHED);
        }

        @Test
        void should_refuse_a_post_without_a_body() {
            Response response = rootTarget().request().post(Entity.json(null));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("technicalCode").asText()).isEqualTo(
                "observability.metric_definition.invalid_body"
            );
            assertThat(repository.all()).isEmpty();
        }

        @Test
        void should_refuse_an_unknown_property() {
            Response response = declare(Map.of("name", BASKET, "origin", "BUILT_IN"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(repository.all()).isEmpty();
        }

        @Test
        void should_refuse_a_duplicate_with_a_400() {
            declare(Map.of("name", BASKET));

            Response response = declare(Map.of("name", BASKET));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("technicalCode").asText()).isEqualTo(
                "observability.metric_definition.duplicate"
            );
        }

        @Test
        void should_refuse_a_caller_without_the_environment_settings_update_right() {
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_SETTINGS, ENVIRONMENT);

            Response response = declare(Map.of("name", BASKET));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
            assertThat(repository.all()).isEmpty();
        }
    }

    @Nested
    class Record {

        @Test
        void should_round_trip_a_key_containing_a_slash_through_put_get_and_delete() {
            Response created = record(SLASHED).request().put(Entity.json(Map.of("label", "Policy tool")));
            assertThat(created.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);

            Response updated = record(SLASHED).request().put(Entity.json(Map.of("label", "Policy tool call", "exposed", false)));
            assertThat(updated.getStatus()).isEqualTo(HttpStatusCode.OK_200);

            JsonNode read = record(SLASHED).request().get().readEntity(JsonNode.class);
            assertThat(read.get("name").asText()).isEqualTo(SLASHED);
            assertThat(read.get("rawKey").asText()).isEqualTo("keyword_my-policy_tools/call");
            assertThat(read.get("label").asText()).isEqualTo("Policy tool call");
            assertThat(read.get("exposed").asBoolean()).isFalse();

            assertThat(record(SLASHED).request().delete().getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
            assertThat(record(SLASHED).request().get().getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_turn_a_discovered_key_into_a_declared_one_and_back_on_delete() {
            ports.givenDiscovered(
                List.of(
                    new DiscoveredAdditionalMetric(
                        "long_failover_count",
                        AdditionalMetricValueType.LONG,
                        Instant.parse("2026-10-29T08:00:00Z")
                    )
                ),
                false
            );
            String failover = "additional-metrics.long_failover_count";
            assertThat(entry(rootTarget().request().get().readEntity(JsonNode.class), failover).get("origin").asText()).isEqualTo(
                "DISCOVERED"
            );

            record(failover).request().put(Entity.json(Map.of("label", "Failovers")));
            JsonNode declared = entry(rootTarget().request().get().readEntity(JsonNode.class), failover);
            assertThat(declared.get("origin").asText()).isEqualTo("DECLARED");
            assertThat(declared.get("lastSeen").asText()).isEqualTo("2026-10-29T08:00:00Z");

            record(failover).request().delete();
            assertThat(entry(rootTarget().request().get().readEntity(JsonNode.class), failover).get("origin").asText()).isEqualTo(
                "DISCOVERED"
            );
        }

        @Test
        void should_answer_404_for_a_record_of_another_environment_on_every_verb() {
            declareUseCase.execute(new DeclareMetricDefinitionUseCase.Input(OTHER_ENVIRONMENT, BASKET, null, null, null, null));

            assertThat(record(BASKET).request().get().getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            assertThat(record(BASKET).request().delete().getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            assertThat(entries(rootTarget().request().get().readEntity(JsonNode.class))).noneMatch(entry ->
                entry.get("name").asText().equals(BASKET)
            );
            assertThat(repository.all()).hasSize(1);
        }

        @Test
        void should_refuse_writes_without_the_environment_settings_update_right() {
            declare(Map.of("name", BASKET));
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_SETTINGS, ENVIRONMENT);

            assertThat(record(BASKET).request().put(Entity.json(Map.of("label", "x"))).getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
            assertThat(record(BASKET).request().delete().getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
            assertThat(record(BASKET).request().get().getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_check_the_write_right_on_the_environment() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
            when(
                permissionService.hasPermission(
                    any(),
                    eq(RolePermission.ENVIRONMENT_SETTINGS),
                    eq(ENVIRONMENT),
                    eq(RolePermissionAction.UPDATE)
                )
            ).thenReturn(true);

            assertThat(record(BASKET).request().put(Entity.json(Map.of())).getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
        }

        @Test
        void should_refuse_a_put_without_a_body() {
            Response response = record(BASKET).request().put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("technicalCode").asText()).isEqualTo(
                "observability.metric_definition.invalid_body"
            );
            assertThat(repository.all()).isEmpty();
        }

        @Test
        void should_refuse_a_put_without_a_name() {
            Response response = rootTarget("record").request().put(Entity.json(Map.of("label", "x")));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("technicalCode").asText()).isEqualTo(
                "observability.metric_definition.invalid_name"
            );
        }
    }

    @Nested
    class List_ {

        @Test
        void should_hide_discovered_entries_and_last_seen_from_a_caller_who_does_not_read_traffic() {
            ports.givenDiscovered(
                List.of(
                    new DiscoveredAdditionalMetric(
                        "keyword_mcp-proxy_tools/call",
                        AdditionalMetricValueType.KEYWORD,
                        Instant.parse("2026-10-29T08:00:00Z")
                    ),
                    new DiscoveredAdditionalMetric(
                        "long_failover_count",
                        AdditionalMetricValueType.LONG,
                        Instant.parse("2026-10-29T08:00:00Z")
                    )
                ),
                true
            );
            ports.givenReadsTraffic(false);

            JsonNode listed = rootTarget().request().get().readEntity(JsonNode.class);

            assertThat(entries(listed))
                .extracting(entry -> entry.get("origin").asText())
                .containsExactly("BUILT_IN");
            JsonNode builtIn = entry(listed, "MCP_PROXY_TOOL");
            assertThat(builtIn.has("lastSeen")).isFalse();
            assertThat(builtIn.get("apiTypes")).extracting(JsonNode::asText).containsExactly("MCP");
            assertThat(listed.get("truncated").asBoolean()).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(
            delimiter = '|',
            value = {
                "additional-metrics.keyword_checkout_country | keyword",
                "additional-metrics.long_checkout_items | number",
                "additional-metrics.bool_checkout_express | boolean",
            }
        )
        void should_serve_the_filter_type_the_value_type_gives(String name, String type) {
            declare(Map.of("name", name));

            assertThat(entry(rootTarget().request().get().readEntity(JsonNode.class), name).get("type").asText()).isEqualTo(type);
        }

        @Test
        void should_serve_no_filter_type_for_a_detail_only_value_type() {
            String note = "additional-metrics.string_checkout_note";
            declare(Map.of("name", note));

            assertThat(entry(rootTarget().request().get().readEntity(JsonNode.class), note).has("type")).isFalse();
        }

        @Test
        void should_serve_the_filter_type_of_the_built_in_spec_over_the_value_type() {
            ports.givenBuiltIn("keyword_checkout_status", "HTTP_STATUS");

            assertThat(entry(rootTarget().request().get().readEntity(JsonNode.class), "HTTP_STATUS").get("type").asText()).isEqualTo(
                "number"
            );
        }

        @Test
        void should_need_a_login_only() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            assertThat(rootTarget().request().get().getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }
    }

    private static List<JsonNode> entries(JsonNode listed) {
        List<JsonNode> entries = new ArrayList<>();
        listed.get("data").forEach(entries::add);
        return entries;
    }

    private static JsonNode entry(JsonNode listed, String name) {
        return entries(listed)
            .stream()
            .filter(entry -> entry.get("name").asText().equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No entry " + name + " in " + listed));
    }

    @Configuration
    @ComponentScan(
        basePackages = "io.gravitee.gamma.rest.core.observability.catalog",
        includeFilters = {
            @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class),
            @ComponentScan.Filter(type = FilterType.ANNOTATION, value = DomainService.class),
        }
    )
    static class MetricDefinitionsTestConfiguration {

        @Bean
        InMemoryAdditionalMetricDefinitionRepository additionalMetricDefinitionRepository() {
            return new InMemoryAdditionalMetricDefinitionRepository();
        }

        @Bean
        InMemoryCatalogPorts catalogPorts() {
            return new InMemoryCatalogPorts();
        }

        @Bean
        AdditionalMetricGrammarPort additionalMetricGrammarPort() {
            return new AdditionalMetricGrammarPortAdapter();
        }

        @Bean
        FilterRegistry filterRegistry() {
            return new SpiFilterRegistry();
        }
    }
}
