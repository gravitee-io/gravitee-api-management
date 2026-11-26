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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.UseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.InMemoryAdditionalMetricDefinitionRepository;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.InMemoryCatalogPorts;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.catalog.model.MetricDefinitionNames;
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
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
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
import org.junit.jupiter.params.provider.ValueSource;
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
    private static final String DECOY = "additional-metrics.keyword_custom.fraud-tier";
    private static final String FRAUD_TIER = "custom.fraud-tier";
    private static final Instant SEEN = Instant.parse("2026-10-29T08:00:00Z");

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
                "additional-metrics.keyword_custom.fraud-tier | observability.metric_definition.reserved_key",
                "additional-metrics.keyword_my\"key | observability.metric_definition.invalid_name",
                "additional-metrics.float_score | observability.metric_definition.unknown_type_prefix",
                "custom.a..b | observability.metric_definition.invalid_name",
                "custom. | observability.metric_definition.invalid_name",
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
        void should_refuse_an_unknown_property_with_the_invalid_body_code_naming_it() {
            Response response = declare(Map.of("name", BASKET, "origin", "BUILT_IN"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("technicalCode").asText()).isEqualTo("observability.metric_definition.invalid_body");
            assertThat(body.get("message").asText()).isEqualTo("Property [origin] is not recognized as a valid property");
            assertThat(repository.all()).isEmpty();
        }

        @Test
        void should_declare_a_custom_key_and_serve_the_same_raw_key_as_its_list_entry() {
            Response created = declare(
                Map.of("name", FRAUD_TIER, "label", "Fraud level", "description", "Set by the check", "exposed", true)
            );

            assertThat(created.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            JsonNode body = created.readEntity(JsonNode.class);
            assertThat(body.get("name").asText()).isEqualTo(FRAUD_TIER);
            assertThat(body.get("rawKey").asText()).isEqualTo(FRAUD_TIER);
            assertThat(body.get("valueType").asText()).isEqualTo("KEYWORD");
            assertThat(body.get("label").asText()).isEqualTo("Fraud level");
            assertThat(body.get("description").asText()).isEqualTo("Set by the check");
            assertThat(body.has("unit")).isFalse();
            assertThat(body.get("exposed").asBoolean()).isTrue();

            JsonNode listed = entry(rootTarget().request().get().readEntity(JsonNode.class), FRAUD_TIER);
            assertThat(listed.get("origin").asText()).isEqualTo("DECLARED");
            assertThat(listed.get("namespace").asText()).isEqualTo("custom");
            assertThat(listed.get("label").asText()).isEqualTo("Fraud level");
            assertThat(listed.get("rawKey").asText()).isEqualTo(body.get("rawKey").asText());
        }

        @Test
        void should_refuse_a_unit_on_a_custom_key_with_a_400_naming_the_rule() {
            Response response = declare(Map.of("name", FRAUD_TIER, "unit", "COUNT"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("technicalCode").asText()).isEqualTo("observability.metric_definition.unit_not_allowed");
            assertThat(body.get("message").asText()).isEqualTo("'custom.fraud-tier' is a custom metric, which takes no unit");
            assertThat(repository.all()).isEmpty();
        }

        /** The unit refusal is the custom root's: a blank unit reads as absent, and an additional keyword key keeps one. */
        @Test
        void should_accept_a_blank_unit_on_a_custom_key_and_a_unit_on_an_additional_keyword_key() {
            Response declared = declare(Map.of("name", FRAUD_TIER, "unit", ""));
            Response saved = record(FRAUD_TIER).request().put(Entity.json(Map.of("label", "Fraud level", "unit", "  ")));
            Response additional = declare(Map.of("name", "additional-metrics.keyword_checkout_country", "unit", "COUNT"));

            assertThat(declared.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            assertThat(declared.readEntity(JsonNode.class).has("unit")).isFalse();
            assertThat(saved.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(saved.readEntity(JsonNode.class).has("unit")).isFalse();
            assertThat(additional.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            assertThat(additional.readEntity(JsonNode.class).get("unit").asText()).isEqualTo("COUNT");
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = {
                // The reserved typed family is additional-metrics.<type>_custom_<name> (OBS-172), not this custom key.
                "custom.keyword_custom_x",
                // Built-ins read additional-metrics.keyword_mcp-proxy_tools/call, not this custom key.
                "custom.keyword_mcp-proxy_tools/call",
            }
        )
        void should_declare_a_custom_key_spelt_like_a_refused_additional_key(String name) {
            assertThat(declare(Map.of("name", name)).getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
        }

        @Test
        void should_hold_a_custom_key_and_an_additional_key_of_the_same_spelling() {
            assertThat(declare(Map.of("name", "custom.keyword_x", "label", "Custom")).getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            assertThat(declare(Map.of("name", "additional-metrics.keyword_x", "label", "Additional")).getStatus()).isEqualTo(
                HttpStatusCode.CREATED_201
            );

            assertThat(record("custom.keyword_x").request().get().readEntity(JsonNode.class).get("label").asText()).isEqualTo("Custom");
            assertThat(record("additional-metrics.keyword_x").request().get().readEntity(JsonNode.class).get("label").asText()).isEqualTo(
                "Additional"
            );
            assertThat(record("custom.keyword_x").request().delete().getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
            assertThat(record("additional-metrics.keyword_x").request().get().getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_accept_a_custom_name_of_256_characters_and_refuse_257() {
            String atLimit = "custom." + "a".repeat(256);

            assertThat(declare(Map.of("name", atLimit)).getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            Response tooLong = declare(Map.of("name", atLimit + "a"));
            assertThat(tooLong.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(tooLong.readEntity(JsonNode.class).get("technicalCode").asText()).isEqualTo(
                "observability.metric_definition.invalid_name"
            );
        }

        /** The Location of a created record reads it back, whatever the name holds that a query value must encode. */
        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = {
                "custom.fraud tier",
                "custom.a+b",
                "custom.a&b=c",
                "custom.tag#1",
                "custom.50%off",
                "custom.100%25",
                "custom.why?",
                "custom.niveau-é",
                "custom.\uD83D\uDE80",
            }
        )
        void should_read_and_delete_a_custom_record_through_its_location(String name) throws Exception {
            Response created = declare(Map.of("name", name));
            assertThat(created.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);

            try (Client client = ClientBuilder.newClient()) {
                Response read = client.target(created.getLocation()).request().get();
                assertThat(read.getStatus()).isEqualTo(HttpStatusCode.OK_200);
                assertThat(new ObjectMapper().readTree(read.readEntity(String.class)).get("name").asText()).isEqualTo(name);
                assertThat(client.target(created.getLocation()).request().delete().getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
            }
            assertThat(repository.all()).isEmpty();
        }

        /**
         * A name the custom grammar reads but whose characters would reach messages and logs raw is never declared. The
         * body is written as JSON text: an unpaired surrogate only travels as a JSON escape, UTF-8 cannot carry it.
         */
        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "custom.fraud\\r\\ntier", "custom.fraud\\u0000tier", "custom.fraud\\uD83Dtier" })
        void should_refuse_a_control_character_or_an_unpaired_surrogate_on_post(String jsonName) {
            Response response = rootTarget().request().post(Entity.json("{\"name\":\"" + jsonName + "\"}"));

            assertRefusedAsUnprintable(response);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "custom.fraud\r\ntier", "custom.fraud\u0000tier" })
        void should_refuse_a_control_character_on_put(String name) {
            Response response = record(name).request().put(Entity.json(Map.of("label", "x")));

            assertRefusedAsUnprintable(response);
        }

        private void assertRefusedAsUnprintable(Response response) {
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("technicalCode").asText()).isEqualTo("observability.metric_definition.invalid_name");
            assertThat(body.get("message").asText())
                .endsWith(": " + MetricDefinitionNames.UNPRINTABLE_RULE)
                .doesNotContainPattern("[\\p{Cntrl}\\x{D800}-\\x{DFFF}]");
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
        void should_refuse_a_put_naming_a_custom_metric_after_its_type_prefix() {
            Response response = record(DECOY).request().put(Entity.json(Map.of("label", "Fraud tier")));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("technicalCode").asText()).isEqualTo(
                "observability.metric_definition.reserved_key"
            );
            assertThat(repository.all()).isEmpty();
        }

        /** A decoy declared before S1 is refused on writes only: it can still be read, then removed. */
        @Test
        void should_read_and_delete_a_decoy_declared_before_the_refusal() {
            repository.create(
                new AdditionalMetricDefinition(
                    "decoy-id",
                    ENVIRONMENT,
                    new AdditionalMetricField("keyword_custom.fraud-tier", AdditionalMetricValueType.KEYWORD),
                    null,
                    null,
                    null,
                    true,
                    Instant.EPOCH,
                    Instant.EPOCH
                )
            );

            Response read = record(DECOY).request().get();
            assertThat(read.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(read.readEntity(JsonNode.class).get("name").asText()).isEqualTo(DECOY);

            assertThat(record(DECOY).request().delete().getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
            assertThat(repository.all()).isEmpty();
        }

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
        void should_create_read_replace_and_delete_a_custom_record() {
            Response created = record(FRAUD_TIER).request().put(Entity.json(Map.of("label", "Fraud tier")));
            assertThat(created.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);

            Response replaced = record(FRAUD_TIER).request().put(Entity.json(Map.of("label", "Fraud level", "exposed", false)));
            assertThat(replaced.getStatus()).isEqualTo(HttpStatusCode.OK_200);

            JsonNode read = record(FRAUD_TIER).request().get().readEntity(JsonNode.class);
            assertThat(read.get("rawKey").asText()).isEqualTo(FRAUD_TIER);
            assertThat(read.get("label").asText()).isEqualTo("Fraud level");
            assertThat(read.get("exposed").asBoolean()).isFalse();

            Response withUnit = record(FRAUD_TIER).request().put(Entity.json(Map.of("label", "Fraud level", "unit", "€")));
            assertThat(withUnit.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(withUnit.readEntity(JsonNode.class).get("technicalCode").asText()).isEqualTo(
                "observability.metric_definition.unit_not_allowed"
            );

            assertThat(record(FRAUD_TIER).request().delete().getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
            assertThat(record(FRAUD_TIER).request().get().getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_turn_a_discovered_custom_key_into_a_declared_one_and_back_on_delete_keeping_its_last_seen() {
            ports.givenDiscovered(
                List.of(
                    new DiscoveredAdditionalMetric(AdditionalMetricField.Root.CUSTOM, "fraud-tier", AdditionalMetricValueType.KEYWORD, SEEN)
                ),
                false
            );

            record(FRAUD_TIER).request().put(Entity.json(Map.of("label", "Fraud level")));
            JsonNode declared = entry(rootTarget().request().get().readEntity(JsonNode.class), FRAUD_TIER);
            assertThat(declared.get("origin").asText()).isEqualTo("DECLARED");
            assertThat(declared.get("label").asText()).isEqualTo("Fraud level");
            assertThat(declared.get("lastSeen").asText()).isEqualTo("2026-10-29T08:00:00Z");

            record(FRAUD_TIER).request().delete();
            JsonNode discovered = entry(rootTarget().request().get().readEntity(JsonNode.class), FRAUD_TIER);
            assertThat(discovered.get("origin").asText()).isEqualTo("DISCOVERED");
            assertThat(discovered.get("label").asText()).isEqualTo("Fraud tier");
            assertThat(discovered.get("lastSeen").asText()).isEqualTo("2026-10-29T08:00:00Z");
        }

        /** Reads stay as written: such a key is listed, never editable, and its 404 never echoes a raw control character. */
        @Test
        void should_list_a_discovered_key_holding_a_line_break_without_letting_it_be_edited() {
            String name = "custom.fraud\r\ntier";
            ports.givenDiscovered(
                List.of(
                    new DiscoveredAdditionalMetric(
                        AdditionalMetricField.Root.CUSTOM,
                        "fraud\r\ntier",
                        AdditionalMetricValueType.KEYWORD,
                        SEEN
                    )
                ),
                false
            );

            assertThat(entry(rootTarget().request().get().readEntity(JsonNode.class), name).get("origin").asText()).isEqualTo("DISCOVERED");
            assertThat(record(name).request().put(Entity.json(Map.of("label", "x"))).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            Response notFound = record(name).request().get();
            assertThat(notFound.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            assertThat(notFound.readEntity(JsonNode.class).get("message").asText())
                .contains("custom.fraud\\u000D\\u000Atier")
                .doesNotContainPattern("\\p{Cntrl}");
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

        /** The flag the pickers read a log's custom value with: the wire name, never the bare key. */
        @Test
        void should_serve_a_discovered_custom_key_with_its_wire_name_as_raw_key_and_its_formatted_name_as_label() {
            ports.givenDiscovered(
                List.of(
                    new DiscoveredAdditionalMetric(
                        AdditionalMetricField.Root.CUSTOM,
                        "fraud-tier",
                        AdditionalMetricValueType.KEYWORD,
                        Instant.parse("2026-10-29T08:00:00Z")
                    )
                ),
                false
            );

            JsonNode custom = entry(rootTarget().request().get().readEntity(JsonNode.class), "custom.fraud-tier");

            assertThat(custom.get("rawKey").asText()).isEqualTo("custom.fraud-tier");
            assertThat(custom.get("label").asText()).isEqualTo("Fraud tier");
            assertThat(custom.get("namespace").asText()).isEqualTo("custom");
            assertThat(custom.get("origin").asText()).isEqualTo("DISCOVERED");
            assertThat(custom.get("valueType").asText()).isEqualTo("KEYWORD");
            assertThat(custom.get("apiTypes")).isEmpty();
            assertThat(custom.get("lastSeen").asText()).isEqualTo("2026-10-29T08:00:00Z");
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
        void should_refuse_an_unknown_property_on_a_put_with_the_invalid_body_code_naming_it() {
            Response response = record(BASKET).request().put(Entity.json(Map.of("label", "x", "rawKey", "double_checkout_basket-value")));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("technicalCode").asText()).isEqualTo("observability.metric_definition.invalid_body");
            assertThat(body.get("message").asText()).isEqualTo("Property [rawKey] is not recognized as a valid property");
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
            assertThat(builtIn.has("unit")).isFalse();
            assertThat(builtIn.get("apiTypes")).extracting(JsonNode::asText).containsExactly("MCP");
            assertThat(listed.get("truncated").asBoolean()).isFalse();
        }

        @Test
        void should_show_a_declared_custom_key_without_last_seen_to_a_caller_who_does_not_read_traffic() {
            declare(Map.of("name", FRAUD_TIER, "label", "Fraud level"));
            ports.givenDiscovered(
                List.of(
                    new DiscoveredAdditionalMetric(
                        AdditionalMetricField.Root.CUSTOM,
                        "fraud-tier",
                        AdditionalMetricValueType.KEYWORD,
                        SEEN
                    ),
                    new DiscoveredAdditionalMetric(AdditionalMetricField.Root.CUSTOM, "basket-id", AdditionalMetricValueType.KEYWORD, SEEN)
                ),
                false
            );
            ports.givenReadsTraffic(false);

            JsonNode listed = rootTarget().request().get().readEntity(JsonNode.class);

            assertThat(entries(listed))
                .filteredOn(entry -> entry.get("origin").asText().equals("DECLARED"))
                .extracting(entry -> entry.get("name").asText())
                .containsExactly(FRAUD_TIER);
            assertThat(entry(listed, FRAUD_TIER).has("lastSeen")).isFalse();
            assertThat(entries(listed))
                .extracting(entry -> entry.get("name").asText())
                .doesNotContain("custom.basket-id");
        }

        /** A declared custom key is listed as declared, so it never takes one of the 200 discovered places. */
        @Test
        void should_keep_a_declared_custom_key_out_of_the_discovery_cap() {
            declare(Map.of("name", FRAUD_TIER));
            List<DiscoveredAdditionalMetric> found = new ArrayList<>();
            found.add(
                new DiscoveredAdditionalMetric(AdditionalMetricField.Root.CUSTOM, "fraud-tier", AdditionalMetricValueType.KEYWORD, SEEN)
            );
            for (int i = 0; i < 200; i++) {
                found.add(new DiscoveredAdditionalMetric("long_key-" + i, AdditionalMetricValueType.LONG, SEEN.minusSeconds(i + 1)));
            }
            ports.givenDiscovered(found, false);

            JsonNode listed = rootTarget().request().get().readEntity(JsonNode.class);

            assertThat(entries(listed))
                .filteredOn(entry -> entry.get("origin").asText().equals("DISCOVERED"))
                .hasSize(200);
            assertThat(entry(listed, FRAUD_TIER).get("origin").asText()).isEqualTo("DECLARED");
            assertThat(listed.get("truncated").asBoolean()).isFalse();
            assertThat(ports.lastUncountedKeys()).contains(FRAUD_TIER);
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

        @Test
        void should_serve_the_namespace_and_kinds_of_a_dynamic_entry_and_none_on_an_unknown_namespace() {
            ports.givenDiscovered(
                List.of(
                    new DiscoveredAdditionalMetric(
                        "long_llm-proxy_tokens-cached",
                        AdditionalMetricValueType.LONG,
                        Instant.parse("2026-11-09T08:00:00Z")
                    )
                ),
                false
            );
            declare(Map.of("name", BASKET, "description", "Amount of the basket at checkout"));

            JsonNode listed = rootTarget().request().get().readEntity(JsonNode.class);

            JsonNode cached = entry(listed, "additional-metrics.long_llm-proxy_tokens-cached");
            assertThat(cached.get("label").asText()).isEqualTo("Tokens cached");
            assertThat(cached.get("namespace").asText()).isEqualTo("llm-proxy");
            assertThat(cached.get("apiTypes")).extracting(JsonNode::asText).containsExactly("LLM");
            JsonNode basket = entry(listed, BASKET);
            assertThat(basket.has("namespace")).isFalse();
            assertThat(basket.get("apiTypes")).isEmpty();
            assertThat(basket.get("description").asText()).isEqualTo("Amount of the basket at checkout");
            assertThat(entry(listed, "MCP_PROXY_TOOL").has("namespace")).isFalse();
        }

        @Test
        void should_keep_the_entries_of_the_asked_origins_only() {
            ports.givenDiscovered(
                List.of(
                    new DiscoveredAdditionalMetric(
                        "long_failover_count",
                        AdditionalMetricValueType.LONG,
                        Instant.parse("2026-11-09T08:00:00Z")
                    )
                ),
                false
            );
            declare(Map.of("name", BASKET));

            JsonNode discoveredOnly = rootTarget().queryParam("origin", "discovered").request().get().readEntity(JsonNode.class);
            JsonNode builtInAndDeclared = rootTarget()
                .queryParam("origin", "BUILT_IN")
                .queryParam("origin", "DECLARED")
                .request()
                .get()
                .readEntity(JsonNode.class);

            assertThat(entries(discoveredOnly))
                .extracting(entry -> entry.get("name").asText())
                .containsExactly("additional-metrics.long_failover_count");
            assertThat(entries(builtInAndDeclared))
                .extracting(entry -> entry.get("name").asText())
                .containsExactly("MCP_PROXY_TOOL", BASKET);
        }

        @Test
        void should_refuse_an_unknown_origin_with_a_400() {
            Response response = rootTarget().queryParam("origin", "NOPE").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("message").asText()).isEqualTo("Unknown origin value 'NOPE'");
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
