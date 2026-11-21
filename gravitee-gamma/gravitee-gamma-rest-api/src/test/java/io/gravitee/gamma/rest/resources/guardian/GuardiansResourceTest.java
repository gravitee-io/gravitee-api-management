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
package io.gravitee.gamma.rest.resources.guardian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import inmemory.EnvironmentCrudServiceInMemory;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventPublicationDomainService;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventWriterDomainService;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.json.JsonSerializer;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.apim.infra.json.jackson.JacksonJsonSerializer;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryPolicyValidation;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryResourceValidation;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.ResourcePluginProvider;
import io.gravitee.gamma.rest.core.guardian.GuardianAuditFixture;
import io.gravitee.gamma.rest.core.guardian.GuardianFixtures;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resources.guardian.GuardiansResourceTest.GuardiansTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ContextConfiguration;

/**
 * The Guardian endpoints over HTTP, under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/guardians}.
 *
 * <p>Exercises the wire contract the console depends on: the paginated envelope, the one field the
 * DTO deliberately hides, the {@code 201} with its {@code Location}, and the refusals — including the
 * ones the request DTOs now declare as bean validation constraints rather than checking in the
 * domain, which is only a 400 if the host's mapper is really wired. The use cases run for real
 * against in-memory ports, so these tests fail if the routing, the DTO mapping or the environment
 * scoping break — not only if a mock stops being called.
 *
 * @author GraviteeSource Team
 */
@ContextConfiguration(classes = { ResourceContextConfiguration.class, GuardiansTestConfiguration.class })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardiansResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final String OTHER_ENVIRONMENT = "other-env";
    private static final String GUARDIAN_ID = GuardianFixtures.GUARDIAN_ID;

    private static final Map<String, Object> A_POLICY = Map.of(
        "name",
        "Detect credentials",
        "policy",
        "data-logging-masking",
        "enabled",
        true
    );

    /** A whole definition, which is what both the creation and the update now take. */
    private static final Map<String, Object> PAYLOAD = Map.of(
        "name",
        "Prompt injection shield",
        "description",
        "Flags a prompt that tries to override the system instructions.",
        "apiTypes",
        List.of("llm-proxy"),
        "phase",
        "REQUEST",
        "policies",
        List.of(A_POLICY)
    );

    @Inject
    private InMemoryGuardianRepository guardianRepository;

    @Inject
    private GuardianPublicationFixture guardianPublications;

    @Inject
    private GuardianAuditFixture withdrawalAudits;

    @Inject
    private InMemoryPolicyValidation policyConfigurations;

    @Inject
    private InMemoryResourceValidation resourceConfigurations;

    @Inject
    private LicenseManager licenseManager;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/guardians";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
        givenGuardiansLicensed(true);
        // a bean of the context, so a schema answer installed by one test would otherwise reach the next
        policyConfigurations.reset();
        resourceConfigurations.reset();
    }

    private void givenGuardiansLicensed(boolean licensed) {
        License license = mock(License.class);
        when(license.isFeatureEnabled("apim-guardians")).thenReturn(licensed);
        when(licenseManager.getPlatformLicense()).thenReturn(license);
    }

    @AfterEach
    void resetRepository() {
        guardianRepository.reset();
        guardianPublications.reset();
        withdrawalAudits.reset();
    }

    private static Guardian guardian(String id, String environmentId, String name) {
        return GuardianFixtures.aGuardian(d -> d.id(id).environmentId(environmentId).name(name)).build();
    }

    /** A guardrail-decision step whose configuration names this resource. */
    private static Map<String, Object> aDecisionStepNaming(String resource) {
        return Map.of(
            "name",
            "Ask the judge",
            "policy",
            GuardianFixtures.DECISION_POLICY,
            "enabled",
            true,
            "configuration",
            Map.of("resource", resource)
        );
    }

    private static Map<String, Object> payloadWith(String key, Object value) {
        var payload = new HashMap<String, Object>(PAYLOAD);
        payload.put(key, value);
        return payload;
    }

    private Response post(Object payload) {
        return rootTarget().request().post(Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE));
    }

    @Nested
    class ListGuardians {

        @Test
        void should_return_200_with_the_paginated_envelope() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("data")).hasSize(1);
            assertThat(body.get("data").get(0).get("id").asText()).isEqualTo(GUARDIAN_ID);
            assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(1L);
        }

        /**
         * A row is a summary, not a Guardian. The sequence and the resources are the bulk of the
         * definition and no column renders one, so a page of twenty must not carry twenty copies of
         * them; {@code guardrailCount} is what a row says about the sequence instead.
         */
        @Test
        void should_return_a_summary_row_without_the_guardrail_sequence() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            JsonNode row = rootTarget().request().get().readEntity(JsonNode.class).get("data").get(0);

            assertThat(row.has("policies")).isFalse();
            assertThat(row.has("resources")).isFalse();
            assertThat(row.get("guardrailCount").asInt()).isEqualTo(1);
        }

        @Test
        void should_carry_everything_a_list_column_renders() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            JsonNode row = rootTarget().request().get().readEntity(JsonNode.class).get("data").get(0);

            assertThat(row.get("id").asText()).isEqualTo(GUARDIAN_ID);
            assertThat(row.get("name").asText()).isEqualTo("Credential leakage");
            assertThat(row.get("description").asText()).isEqualTo("Blocks a reply that echoes an API key.");
            assertThat(row.get("phase").asText()).isEqualTo("REQUEST");
            assertThat(row.get("apiTypes").get(0).asText()).isEqualTo("llm-proxy");
            assertThat(row.get("lifecycleState").asText()).isEqualTo("UNDEPLOYED");
            assertThat(row.get("createdAt").asText()).isEqualTo("2026-02-01T20:22:02Z");
            assertThat(row.get("updatedAt").asText()).isEqualTo("2026-02-02T20:22:02Z");
            // already in the path, so repeating it in every row says nothing
            assertThat(row.has("environmentId")).isFalse();
        }

        /** The whole definition is still one request away. */
        @Test
        void should_still_expose_the_whole_definition_on_a_single_read() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            JsonNode dto = rootTarget("/" + GUARDIAN_ID).request().get().readEntity(JsonNode.class);

            assertThat(dto.get("policies")).hasSize(1);
            assertThat(dto.has("environmentId")).isFalse();
        }

        @Test
        void should_count_an_empty_sequence_as_zero() {
            guardianRepository.givenGuardian(
                GuardianFixtures.aGuardian(d -> d.id(GUARDIAN_ID).environmentId(ENVIRONMENT).policies(List.of())).build()
            );

            JsonNode row = rootTarget().request().get().readEntity(JsonNode.class).get("data").get(0);

            assertThat(row.get("guardrailCount").asInt()).isZero();
        }

        @Test
        void should_only_return_guardians_of_the_context_environment() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Mine"));
            guardianRepository.givenGuardian(guardian("elsewhere", OTHER_ENVIRONMENT, "Theirs"));

            JsonNode body = rootTarget().request().get().readEntity(JsonNode.class);

            assertThat(body.get("data")).hasSize(1);
            assertThat(body.get("data").get(0).get("id").asText()).isEqualTo(GUARDIAN_ID);
        }

        @Test
        void should_refuse_an_unknown_sort_field_with_400() {
            // the API types are a set in the definition, so there is nothing to order rows by
            Response response = rootTarget().queryParam("sortBy", "apiType").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_refuse_a_non_numeric_page_with_400_naming_the_parameter() {
            Response response = rootTarget().queryParam("page", "abc").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("message").asText()).isEqualTo("Query parameter page does not have a valid format");
            assertThat(body.get("http_status").asInt()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_refuse_a_non_numeric_per_page_with_400_naming_the_parameter() {
            Response response = rootTarget().queryParam("perPage", "abc").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("message").asText()).isEqualTo(
                "Query parameter perPage does not have a valid format"
            );
        }

        @Test
        void should_report_the_default_page_size_when_perPage_is_omitted() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            JsonNode pagination = rootTarget().request().get().readEntity(JsonNode.class).get("pagination");

            assertThat(pagination.get("perPage").asInt()).isEqualTo(10);
            assertThat(pagination.get("pageCount").asInt()).isEqualTo(1);
            assertThat(pagination.get("pageItemsCount").asInt()).isEqualTo(1);
        }

        @Test
        void should_report_the_default_page_size_when_perPage_is_zero() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            JsonNode pagination = rootTarget().queryParam("perPage", 0).request().get().readEntity(JsonNode.class).get("pagination");

            // perPage=0 falls back to the default, so a pageCount of 0 would tell the console there is
            // nothing to show while the page carries a row
            assertThat(pagination.get("perPage").asInt()).isEqualTo(10);
            assertThat(pagination.get("pageCount").asInt()).isEqualTo(1);
        }

        @Test
        void should_keep_the_requested_page_size_on_a_partial_last_page() {
            guardianRepository.givenGuardian(guardian("g1", ENVIRONMENT, "A"));
            guardianRepository.givenGuardian(guardian("g2", ENVIRONMENT, "B"));
            guardianRepository.givenGuardian(guardian("g3", ENVIRONMENT, "C"));

            JsonNode body = rootTarget().queryParam("page", 2).queryParam("perPage", 2).request().get().readEntity(JsonNode.class);

            // one row on the last page must not shrink perPage to 1, which would turn pageCount into 3
            assertThat(body.get("data")).hasSize(1);
            assertThat(body.get("pagination").get("perPage").asInt()).isEqualTo(2);
            assertThat(body.get("pagination").get("pageCount").asInt()).isEqualTo(2);
            assertThat(body.get("pagination").get("pageItemsCount").asInt()).isEqualTo(1);
        }
    }

    @Nested
    class CreateGuardian {

        @Test
        void should_return_201_with_a_location_and_an_undeployed_guardian() {
            Response response = post(PAYLOAD);

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("lifecycleState").asText()).isEqualTo("UNDEPLOYED");
            assertThat(response.getLocation().toString()).endsWith(body.get("id").asText());
        }

        @Test
        void should_carry_the_description_the_payload_sends() {
            JsonNode body = post(PAYLOAD).readEntity(JsonNode.class);

            assertThat(body.get("description").asText()).isEqualTo("Flags a prompt that tries to override the system instructions.");
        }

        @Test
        void should_refuse_a_description_longer_than_its_column_with_400() {
            assertThat(post(payloadWith("description", "d".repeat(1025))).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_store_the_guardrail_sequence_the_payload_carries() {
            JsonNode body = post(PAYLOAD).readEntity(JsonNode.class);

            assertThat(body.get("policies")).hasSize(1);
            assertThat(body.get("policies").get(0).get("policy").asText()).isEqualTo("data-logging-masking");
        }

        @Test
        void should_spell_the_api_type_the_way_the_rest_of_apim_does() {
            // The definition enum serializes llm-proxy through @JsonValue. Two spellings for one
            // concept would make every consumer special-case this API.
            JsonNode body = post(PAYLOAD).readEntity(JsonNode.class);

            assertThat(body.get("apiTypes").get(0).asText()).isEqualTo("llm-proxy");
        }

        @Test
        void should_refuse_an_api_type_other_than_llm_proxy_with_400() {
            assertThat(post(payloadWith("apiTypes", List.of("mcp-proxy"))).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_store_the_resources_the_payload_carries() {
            var resource = Map.of("name", "classifier", "type", GuardianFixtures.INSTALLED_RESOURCE_TYPE, "configuration", Map.of());

            var response = post(payloadWith("resources", List.of(resource)));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("creation response").isEqualTo(HttpStatusCode.CREATED_201);
                JsonNode resources = response.readEntity(JsonNode.class).get("resources");
                softly.assertThat(resources).as("stored resources").hasSize(1);
                softly.assertThat(resources.at("/0/name").asText()).as("name").isEqualTo("classifier");
                softly.assertThat(resources.at("/0/type").asText()).as("type").isEqualTo(GuardianFixtures.INSTALLED_RESOURCE_TYPE);
            });
        }

        @Test
        void should_refuse_a_resource_of_a_type_the_node_does_not_have_with_400_naming_it() {
            var resource = Map.of("name", "classifier", "type", "absent-from-the-node", "configuration", Map.of());

            var response = post(payloadWith("resources", List.of(resource)));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("creation response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .as("the resource and its type, readable")
                    .contains("classifier", "absent-from-the-node", "not installed");
            });
        }

        /** The gateway keeps resources by name, so the second would silently replace the first. */
        @Test
        void should_refuse_two_enabled_resources_sharing_a_name_with_400() {
            var resource = Map.of("name", "classifier", "type", GuardianFixtures.INSTALLED_RESOURCE_TYPE, "configuration", Map.of());

            var response = post(payloadWith("resources", List.of(resource, resource)));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("creation response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .as("the shared name")
                    .isEqualTo("Resource name [classifier] is used by more than one enabled resource.");
            });
        }

        @Test
        void should_refuse_a_resource_configuration_the_plugin_schema_rejects_with_400_naming_the_resource() {
            resourceConfigurations.givenRefuses(GuardianFixtures.INSTALLED_RESOURCE_TYPE, "$.model: is missing but it is required");
            var resource = Map.of("name", "classifier", "type", GuardianFixtures.INSTALLED_RESOURCE_TYPE, "configuration", Map.of());

            var response = post(payloadWith("resources", List.of(resource)));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("creation response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .as("the refused resource, named")
                    .contains("Resource [classifier] has an invalid configuration: $.model: is missing but it is required.");
            });
        }

        @Test
        void should_refuse_a_step_configuration_the_policy_schema_rejects_with_400_naming_the_step() {
            policyConfigurations.givenRefuses(GuardianFixtures.GUARDRAIL_POLICY, "$.rules: is missing but it is required");

            var response = post(PAYLOAD);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("creation response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .as("the step and the schema error, readable")
                    .contains("Detect credentials", "$.rules: is missing but it is required");
            });
        }

        @Test
        void should_refuse_a_guardrail_naming_a_resource_the_guardian_does_not_hold_with_400_naming_the_step() {
            var response = post(payloadWith("policies", List.of(aDecisionStepNaming("missing"))));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("creation response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .isEqualTo("Step [Ask the judge] references resource [missing], which the Guardian does not hold.");
                softly.assertThat(guardianRepository.storedGuardians()).as("nothing stored").isEmpty();
            });
        }

        @Test
        void should_refuse_a_guardrail_naming_a_resource_of_the_wrong_type_with_400_naming_the_step() {
            var payload = payloadWith("policies", List.of(aDecisionStepNaming("classifier")));
            payload.put(
                "resources",
                List.of(Map.of("name", "classifier", "type", GuardianFixtures.INSTALLED_RESOURCE_TYPE, "configuration", Map.of()))
            );

            var response = post(payload);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("creation response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .isEqualTo(
                        "Step [Ask the judge] references a resource of the wrong type: [classifier] is of type [" +
                            GuardianFixtures.INSTALLED_RESOURCE_TYPE +
                            "], where the policy accepts [" +
                            GuardianFixtures.DECISION_RESOURCE_TYPE +
                            "]."
                    );
            });
        }

        @Test
        void should_create_a_guardrail_naming_an_enabled_resource_of_the_accepted_type() {
            var payload = payloadWith("policies", List.of(aDecisionStepNaming("judge")));
            payload.put(
                "resources",
                List.of(Map.of("name", "judge", "type", GuardianFixtures.DECISION_RESOURCE_TYPE, "configuration", Map.of()))
            );

            assertThat(post(payload).getStatus()).as("creation response").isEqualTo(HttpStatusCode.CREATED_201);
        }

        @Test
        void should_refuse_an_empty_api_type_list_with_400() {
            assertThat(post(payloadWith("apiTypes", List.of())).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_refuse_a_blank_name_with_400() {
            assertThat(post(payloadWith("name", "  ")).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_refuse_a_name_longer_than_its_column_with_400() {
            assertThat(post(payloadWith("name", "n".repeat(513))).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_refuse_a_missing_phase_with_400() {
            var payload = new HashMap<String, Object>(PAYLOAD);
            payload.remove("phase");

            assertThat(post(payload).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_refuse_an_id_that_is_not_a_uuid_with_400() {
            // the id lands in the Location header of the creation and in every later path, and the
            // server generates UUIDs itself, so accepting anything else would mix two id shapes
            assertThat(post(payloadWith("id", "chosen-id")).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_keep_a_uuid_the_caller_supplies() {
            var chosen = "0b0c2a6e-3f1d-4a7b-9c8e-5d4f3a2b1c0d";

            JsonNode body = post(payloadWith("id", chosen)).readEntity(JsonNode.class);

            assertThat(body.get("id").asText()).isEqualTo(chosen);
        }

        @Test
        void should_refuse_a_missing_body_with_400() {
            // the JSON null literal, which Jackson maps to a null DTO: that is the branch under test,
            // and @NotNull on the parameter is what answers it. Entity.entity(null, ...) cannot serve
            // here, the Jersey client refuses it before sending.
            assertThat(post("null").getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_refuse_a_null_policy_with_400_without_storing_the_guardian() {
            var response = post(payloadWith("policies", Collections.singletonList(null)));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("null policy is malformed input").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly.assertThat(guardianRepository.storedGuardians()).as("rejected input is not stored").isEmpty();
            });
        }

        @Test
        void should_refuse_an_unknown_key_with_400() {
            assertThat(post(payloadWith("colour", "blue")).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }
    }

    @Nested
    class ReadUpdateDelete {

        @BeforeEach
        void seed() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));
        }

        private Response put(Object payload) {
            return rootTarget("/" + GUARDIAN_ID).request().put(Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE));
        }

        @Test
        void should_return_200_on_get() {
            Response response = rootTarget("/" + GUARDIAN_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.readEntity(JsonNode.class).get("id").asText()).isEqualTo(GUARDIAN_ID);
        }

        /** What is stored has to be what the plugin validated, so an update answers the corrected configuration. */
        @Test
        void should_store_the_resource_configuration_its_plugin_accepts_on_put() {
            resourceConfigurations.givenRewrites(GuardianFixtures.INSTALLED_RESOURCE_TYPE, "{\"threshold\":0.5}");
            var resource = Map.of("name", "classifier", "type", GuardianFixtures.INSTALLED_RESOURCE_TYPE, "configuration", Map.of());

            var response = put(payloadWith("resources", List.of(resource)));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("update response").isEqualTo(HttpStatusCode.OK_200);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/resources/0/configuration/threshold").asDouble())
                    .as("the configuration the plugin accepts")
                    .isEqualTo(0.5);
            });
        }

        @Test
        void should_return_404_on_an_unknown_guardian() {
            Response response = rootTarget("/nope").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_replace_the_whole_definition_on_put() {
            Response response = put(payloadWith("policies", List.of()));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("name").asText()).isEqualTo("Prompt injection shield");
            // a PUT replaces: the guardrail the stored definition carried is gone because the payload
            // did not carry it
            assertThat(body.get("policies")).isEmpty();
        }

        @Test
        void should_drop_the_description_when_the_put_payload_omits_it() {
            var payload = new HashMap<String, Object>(PAYLOAD);
            payload.remove("description");

            JsonNode body = put(payload).readEntity(JsonNode.class);

            // @JsonInclude(NON_NULL) on the DTO: a cleared description leaves the key out entirely
            assertThat(body.has("description")).isFalse();
        }

        /**
         * The spec asks a client to send back everything it read, so the fields only the store owns are
         * ignored rather than refused. Before AIAM-1644 the obedient client got a 400 naming {@code id}.
         */
        @Test
        void should_accept_its_own_get_response_as_an_update_body() {
            JsonNode read = rootTarget("/" + GUARDIAN_ID).request().get().readEntity(JsonNode.class);

            Response response = put(read);

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_ignore_an_id_in_the_update_body_and_keep_the_path_one() {
            // the path owns it, so a second one in the body is dropped rather than obeyed
            Response response = put(payloadWith("id", "11111111-2222-3333-4444-555555555555"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.readEntity(JsonNode.class).get("id").asText()).isEqualTo(GUARDIAN_ID);
        }

        /** Ignored must mean dropped, not applied: a body cannot deploy a Guardian or rewrite its history. */
        @Test
        void should_not_let_the_update_body_set_the_fields_the_store_owns() {
            var forged = new HashMap<String, Object>(PAYLOAD);
            forged.put("lifecycleState", "DEPLOYED");
            forged.put("createdAt", "1999-01-01T00:00:00Z");
            forged.put("deployedAt", "1999-01-01T00:00:00Z");

            JsonNode body = put(forged).readEntity(JsonNode.class);

            assertThat(body.get("lifecycleState").asText()).isEqualTo("UNDEPLOYED");
            assertThat(body.get("createdAt").asText()).isEqualTo("2026-02-01T20:22:02Z");
            assertThat(body.has("deployedAt")).isFalse();
        }

        /** Only the five read-only fields are tolerated; the strictness this type exists for stays. */
        @Test
        void should_still_refuse_an_unknown_key_on_update_with_400() {
            assertThat(put(payloadWith("colour", "blue")).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_refuse_a_partial_update_body_with_400() {
            // a PUT sets everything, so a body carrying only a rename is not a valid one
            assertThat(put(Map.of("name", "Renamed")).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_204_on_delete() {
            Response response = rootTarget("/" + GUARDIAN_ID).request().delete();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
            assertThat(guardianRepository.storedGuardians()).isEmpty();
        }

        @Test
        void should_refuse_deletion_of_a_deployed_guardian_with_409() {
            guardianPublications.givenPublication(
                guardianRepository.findByEnvironmentId(ENVIRONMENT, GUARDIAN_ID).orElseThrow().getDefinition(),
                Instant.parse("2026-11-09T10:00:00Z")
            );

            var response = rootTarget("/" + GUARDIAN_ID).request().delete();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("active publication prevents deletion").isEqualTo(HttpStatusCode.CONFLICT_409);
                softly
                    .assertThat(response.readEntity(JsonNode.class).path("message").asText())
                    .as("actionable conflict")
                    .contains("active publication");
                softly.assertThat(guardianRepository.storedGuardians()).as("draft preserved").hasSize(1);
                softly.assertThat(guardianPublications.withdrawn(ENVIRONMENT, GUARDIAN_ID)).as("publication preserved").isFalse();
            });
        }

        @Test
        void should_refuse_a_missing_update_body_with_400() {
            assertThat(put("null").getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }
    }

    @Nested
    class DeployGuardian {

        @BeforeEach
        void seed() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));
        }

        private Response deploy(Object body) {
            return rootTarget("/" + GUARDIAN_ID + "/_deploy").request().post(Entity.json(body));
        }

        @Test
        void should_return_the_deployed_guardian_and_publication_date() {
            Response response = deploy(Map.of("deploymentLabel", "Reviewed"));
            assertThat(response.getStatus()).as("deployment response").isEqualTo(HttpStatusCode.OK_200);
            var body = response.readEntity(JsonNode.class);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(body.get("lifecycleState").asText()).isEqualTo("DEPLOYED");
                softly
                    .assertThat(Instant.parse(body.get("deployedAt").asText()))
                    .as("ISO 8601 publication date at stored millisecond precision")
                    .isEqualTo(Date.from(guardianPublications.published(ENVIRONMENT, GUARDIAN_ID).at()).toInstant());

                softly.assertThat(guardianPublications.lastLabel()).isEqualTo("Reviewed");
            });
        }

        @Test
        void should_report_the_revision_the_gateways_run() {
            var neverDeployed = rootTarget("/" + GUARDIAN_ID).request().get().readEntity(JsonNode.class);
            deploy(Map.of());
            var deployed = deploy(Map.of()).readEntity(JsonNode.class);
            var read = rootTarget("/" + GUARDIAN_ID).request().get().readEntity(JsonNode.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(neverDeployed.has("deployedRevisionId")).as("never deployed").isFalse();
                softly
                    .assertThat(deployed.at("/deployedRevisionId").asText())
                    .as("revision answered by the deployment")
                    .isEqualTo(guardianPublications.lastPublicationId());
                softly
                    .assertThat(read.at("/deployedRevisionId").asText())
                    .as("revision read back")
                    .isEqualTo(guardianPublications.lastPublicationId());
            });
        }

        @Test
        void should_accept_the_get_response_of_a_deployed_guardian_as_an_update_body() {
            deploy(Map.of());
            var read = rootTarget("/" + GUARDIAN_ID).request().get().readEntity(JsonNode.class);

            var response = rootTarget("/" + GUARDIAN_ID).request().put(Entity.entity(read, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).as("update with the read deployed revision").isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_reject_an_unknown_property_before_publication() {
            var response = deploy(Map.of("deploymentLable", "Reviewed"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("unknown deployment property").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly.assertThat(guardianPublications.publications()).as("no revision is published").isZero();
            });
        }

        @Test
        void should_accept_an_absent_body() {
            var response = rootTarget("/" + GUARDIAN_ID + "/_deploy").request().method("POST");
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_reject_an_overlong_label_before_publication() {
            assertThat(deploy(Map.of("deploymentLabel", "x".repeat(257))).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(guardianPublications.publications()).isZero();
        }

        @Test
        void should_return_400_with_a_readable_message_when_the_sequence_has_no_enabled_guardrail() {
            guardianRepository.reset();
            guardianRepository.givenGuardian(
                GuardianFixtures.aGuardian(d -> d.id(GUARDIAN_ID).environmentId(ENVIRONMENT).policies(List.of())).build()
            );

            var response = deploy(Map.of());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("deployment response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .as("readable message")
                    .contains("at least one enabled guardrail");
                softly.assertThat(guardianPublications.publications()).as("no revision is published").isZero();
            });
        }

        @Test
        void should_return_400_naming_the_step_and_the_schema_error_when_the_stored_draft_no_longer_conforms() {
            policyConfigurations.givenRefuses(GuardianFixtures.GUARDRAIL_POLICY, "$.rules: is missing but it is required");

            var response = deploy(Map.of());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("deployment response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .as("the step and the schema error, readable")
                    .contains("Detect credentials", "$.rules: is missing but it is required");
                softly.assertThat(guardianPublications.publications()).as("no revision is published").isZero();
            });
        }

        /** A draft stored before the rule existed is refused rather than published broken. */
        @Test
        void should_return_400_naming_the_step_when_the_stored_draft_names_a_resource_the_guardian_does_not_hold() {
            guardianRepository.reset();
            guardianRepository.givenGuardian(
                GuardianFixtures.aGuardian(d ->
                    d
                        .id(GUARDIAN_ID)
                        .environmentId(ENVIRONMENT)
                        .name("Credential leakage")
                        .policies(
                            List.of(
                                Step.builder()
                                    .name("Ask the judge")
                                    .policy(GuardianFixtures.DECISION_POLICY)
                                    .enabled(true)
                                    .configuration("{\"resource\":\"missing\"}")
                                    .build()
                            )
                        )
                ).build()
            );

            var response = deploy(Map.of());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("deployment response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .isEqualTo("Step [Ask the judge] references resource [missing], which the Guardian does not hold.");
                softly.assertThat(guardianPublications.publications()).as("no revision is published").isZero();
            });
        }

        @Test
        void should_return_404_for_an_unknown_guardian() {
            var response = rootTarget("/unknown/_deploy").request().post(Entity.json(Map.of()));
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            assertThat(guardianPublications.publications()).isZero();
        }

        @Test
        void should_return_404_for_a_guardian_from_another_environment() {
            guardianRepository.reset();
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, OTHER_ENVIRONMENT, "Elsewhere"));
            assertThat(deploy(Map.of()).getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            assertThat(guardianPublications.publications()).isZero();
        }

        @Test
        void should_refuse_deployment_without_the_update_permission() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
            assertThat(deploy(Map.of()).getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
            assertThat(guardianPublications.publications()).isZero();
        }
    }

    /**
     * Two Guardians of one environment may not share a name. A 409 rather than a 400: the request is
     * well formed and the caller can retry it with another name.
     */
    @Nested
    class NameUniqueness {

        @Test
        void should_refuse_a_creation_whose_name_is_taken_with_409() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            Response response = post(payloadWith("name", "Credential leakage"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CONFLICT_409);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("message").asText()).contains("Credential leakage");
            // the documented Error shape of this surface is message + http_status and nothing else; the
            // two-argument ConflictDomainException would add a parameters.id holding the name
            assertThat(body.has("parameters")).isFalse();
            assertThat(body.get("http_status").asInt()).isEqualTo(HttpStatusCode.CONFLICT_409);
        }

        /**
         * The name is compared, and kept, without the whitespace around it. Without that, every padded
         * variant was a new name: one environment ended up with Guardians that read identically in the
         * console and in the decision logs (AIAM-1747).
         */
        @ParameterizedTest(name = "[{0}] is the same name")
        @ValueSource(
            strings = {
                "Credential leakage ",
                " Credential leakage",
                "  Credential leakage  ",
                "Credential leakage\t",
                "Credential leakage\u00A0",
                "\u00A0Credential leakage",
                "Credential leakage\u3000",
            }
        )
        void should_refuse_a_creation_whose_name_is_taken_but_for_the_whitespace_around_it(String padded) {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            assertThat(post(payloadWith("name", padded)).getStatus()).isEqualTo(HttpStatusCode.CONFLICT_409);
        }

        /** Padding is not kept either, so the stored name is the one the caller meant. */
        @Test
        void should_store_a_created_name_without_the_whitespace_around_it() {
            Response response = post(payloadWith("name", "  Credential leakage  "));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            assertThat(response.readEntity(JsonNode.class).get("name").asText()).isEqualTo("Credential leakage");
        }

        @Test
        void should_refuse_a_rename_onto_a_taken_name_but_for_the_whitespace_around_it() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));
            guardianRepository.givenGuardian(guardian("second-id", ENVIRONMENT, "Prompt injection"));

            Response response = rootTarget("/second-id")
                .request()
                .put(Entity.entity(payloadWith("name", " Credential leakage "), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CONFLICT_409);
        }

        /** Its own name, padded, is still its own name — not a clash with itself, and stored trimmed. */
        @Test
        void should_let_a_guardian_keep_its_own_padded_name_on_update() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            Response response = rootTarget("/" + GUARDIAN_ID)
                .request()
                .put(Entity.entity(payloadWith("name", "  Credential leakage  "), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.readEntity(JsonNode.class).get("name").asText()).isEqualTo("Credential leakage");
        }

        /**
         * The update path has to refuse it too, and it does not share the payload's constraints: the
         * definition's own {@code @NotBlank} answers for it, once the name has been stripped.
         */
        @ParameterizedTest(name = "[{0}] is no name on update either")
        @ValueSource(strings = { " ", "\t", "\u00A0", "\u3000" })
        void should_refuse_an_update_whose_name_is_only_whitespace_with_400(String blank) {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            Response response = rootTarget("/" + GUARDIAN_ID)
                .request()
                .put(Entity.entity(payloadWith("name", blank), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        /**
         * The update carries its name in the definition, which declares no maximum length of its own —
         * the column does, and a longer name used to reach it as a technical failure.
         */
        @Test
        void should_refuse_an_update_whose_name_is_longer_than_the_column_with_400() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));
            String tooLong = "x".repeat(513);

            Response response = rootTarget("/" + GUARDIAN_ID)
                .request()
                .put(Entity.entity(payloadWith("name", tooLong), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("message").asText()).contains("512");
        }

        /** The creation says the same thing one step earlier, and says it in the same words. */
        @Test
        void should_refuse_a_creation_whose_name_is_longer_than_the_column_with_400() {
            Response response = post(payloadWith("name", "x".repeat(513)));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("message").asText()).contains(
                "Guardian name must be at most 512 characters"
            );
        }

        /**
         * The padding is not part of the name, so it is not part of its length either: a name at the
         * limit is accepted however it was padded. This is what the normalisation has to happen before
         * the structure check for, and not only before the store.
         */
        @Test
        void should_accept_a_name_at_the_column_length_sent_with_padding() {
            Response response = post(payloadWith("name", "  " + "x".repeat(512) + "  "));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            assertThat(response.readEntity(JsonNode.class).get("name").asText()).hasSize(512);
        }

        /** The limit itself is reachable: 512 is accepted, so the refusal is the length and not the rule. */
        @Test
        void should_accept_a_name_of_exactly_the_column_length() {
            assertThat(post(payloadWith("name", "x".repeat(512))).getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
        }

        /** Blank once trimmed is no name at all, and that is a 400, not a 409. */
        @ParameterizedTest(name = "[{0}] is no name")
        @ValueSource(strings = { " ", "   ", "\t", "\u00A0", "\u3000", "\u00A0 \u00A0" })
        void should_refuse_a_name_that_is_only_whitespace_with_400(String blank) {
            assertThat(post(payloadWith("name", blank)).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        /** The rule ignores case: a difference of case is not a different name in a list. */
        @Test
        void should_refuse_a_name_that_differs_only_by_case_with_409() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            assertThat(post(payloadWith("name", "CREDENTIAL LEAKAGE")).getStatus()).isEqualTo(HttpStatusCode.CONFLICT_409);
        }

        /** A Guardian is scoped to its environment, so the name is only taken inside one. */
        @Test
        void should_allow_the_same_name_in_another_environment() {
            guardianRepository.givenGuardian(guardian("other-id", OTHER_ENVIRONMENT, "Credential leakage"));

            assertThat(post(payloadWith("name", "Credential leakage")).getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
        }

        @Test
        void should_refuse_renaming_a_guardian_onto_a_taken_name_with_409() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));
            guardianRepository.givenGuardian(guardian("second-id", ENVIRONMENT, "Prompt injection"));

            Response response = rootTarget("/second-id")
                .request()
                .put(Entity.entity(payloadWith("name", "Credential leakage"), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CONFLICT_409);
        }

        /**
         * Two case variants can already sit in one environment: the unique indexes compare exactly, so
         * "Foo" beside "foo" survives data that predates this rule or two creates that raced past it.
         * Editing either must still work — otherwise neither Guardian could ever be saved again — and
         * the answer must not depend on which row the store returns first.
         */
        @Test
        void should_let_a_guardian_be_edited_while_a_case_variant_of_its_name_exists() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));
            guardianRepository.givenGuardian(guardian("variant-id", ENVIRONMENT, "credential leakage"));

            Response response = rootTarget("/" + GUARDIAN_ID)
                .request()
                .put(Entity.entity(payloadWith("name", "Credential leakage"), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        /** But renaming onto a name a case variant holds is still refused, whichever row comes back. */
        @Test
        void should_refuse_a_rename_onto_a_name_a_case_variant_holds() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));
            guardianRepository.givenGuardian(guardian("variant-id", ENVIRONMENT, "credential leakage"));
            guardianRepository.givenGuardian(guardian("third-id", ENVIRONMENT, "Prompt injection"));

            Response response = rootTarget("/third-id")
                .request()
                .put(Entity.entity(payloadWith("name", "CREDENTIAL LEAKAGE"), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CONFLICT_409);
        }

        /**
         * A body with no name is a malformed body, not a server fault. The uniqueness check reads the
         * name before the definition is validated, so it must not be the thing that fails first.
         */
        @Test
        void should_refuse_an_update_body_without_a_name_with_400() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));
            var noName = new HashMap<String, Object>(PAYLOAD);
            noName.remove("name");

            Response response = rootTarget("/" + GUARDIAN_ID).request().put(Entity.entity(noName, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        /** The one case a naive check gets wrong: a Guardian clashing with itself. */
        @Test
        void should_let_a_guardian_keep_its_own_name_on_update() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            Response response = rootTarget("/" + GUARDIAN_ID)
                .request()
                .put(Entity.entity(payloadWith("name", "Credential leakage"), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_let_a_guardian_keep_its_own_name_in_another_case_on_update() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));

            Response response = rootTarget("/" + GUARDIAN_ID)
                .request()
                .put(Entity.entity(payloadWith("name", "credential leakage"), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }
    }

    /**
     * The wire format of the three timestamps. Worth its own class because the shape does not come from
     * this module: the inherited rest-api mapper enables {@code WRITE_DATES_AS_TIMESTAMPS}, so without
     * {@code @JsonFormat} on the DTO these come out as fractional {@code <epoch_seconds>.<nanos>}
     * numbers, which is the defect AIAM-1644 reports. A number here also silently breaks the console,
     * whose wire types declare strings.
     */
    @Nested
    class Timestamps {

        private static final String CREATED_AT = "2026-02-01T20:22:02Z";
        private static final String UPDATED_AT = "2026-02-02T20:22:02Z";

        @BeforeEach
        void seed() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));
        }

        @Test
        void should_write_the_timestamps_as_iso_8601_strings_in_utc() {
            JsonNode dto = rootTarget("/" + GUARDIAN_ID).request().get().readEntity(JsonNode.class);

            assertThat(dto.get("createdAt").asText()).isEqualTo(CREATED_AT);
            assertThat(dto.get("updatedAt").asText()).isEqualTo(UPDATED_AT);
        }

        /** The regression guard: an epoch number parses as text too, so assert the JSON type itself. */
        @Test
        void should_not_write_the_timestamps_as_numbers() {
            JsonNode dto = rootTarget("/" + GUARDIAN_ID).request().get().readEntity(JsonNode.class);

            assertThat(dto.get("createdAt").isTextual()).isTrue();
            assertThat(dto.get("createdAt").isNumber()).isFalse();
            assertThat(dto.get("updatedAt").isTextual()).isTrue();
            assertThat(dto.get("updatedAt").isNumber()).isFalse();
        }

        @Test
        void should_write_the_deployment_stamp_as_an_iso_8601_string_in_utc() {
            guardianRepository.reset();
            guardianRepository.givenGuardian(
                GuardianFixtures.aGuardian(d -> d.id(GUARDIAN_ID).environmentId(ENVIRONMENT))
                    .deployedAt(Instant.parse("2026-02-03T20:22:02Z"))
                    .build()
            );

            guardianPublications.givenPublication(
                guardianRepository.findByEnvironmentId(ENVIRONMENT, GUARDIAN_ID).orElseThrow().getDefinition(),
                Instant.parse("2026-02-03T20:22:02Z")
            );

            JsonNode dto = rootTarget("/" + GUARDIAN_ID).request().get().readEntity(JsonNode.class);

            assertThat(dto.get("deployedAt").isTextual()).isTrue();
            assertThat(dto.get("deployedAt").asText()).isEqualTo("2026-02-03T20:22:02Z");
        }
    }

    @Nested
    class UndeployGuardian {

        @Test
        void should_withdraw_without_a_request_body_and_return_the_preserved_draft() {
            var guardian = guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage");
            guardianRepository.givenGuardian(guardian);
            guardianPublications.givenPublication(guardian.getDefinition(), Instant.parse("2026-11-09T10:00:00Z"));

            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
            when(
                permissionService.hasPermission(
                    any(),
                    eq(RolePermission.ENVIRONMENT_GUARDIAN),
                    eq(ENVIRONMENT),
                    eq(RolePermissionAction.UPDATE)
                )
            ).thenReturn(true);

            var response = rootTarget("/" + GUARDIAN_ID + "/_undeploy").request().post(null);

            assertThat(response.getStatus()).as("withdrawal response").isEqualTo(HttpStatusCode.OK_200);
            var body = response.readEntity(JsonNode.class);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(body.path("lifecycleState").asText()).as("withdrawn state").isEqualTo("UNDEPLOYED");
                softly.assertThat(body.path("name").asText()).as("draft preserved").isEqualTo(guardian.getName());
                softly.assertThat(guardianPublications.withdrawn(ENVIRONMENT, GUARDIAN_ID)).as("withdrawal published").isTrue();
            });
        }

        @Test
        void should_return_404_for_unknown_and_foreign_guardians() {
            guardianRepository.givenGuardian(guardian("foreign", OTHER_ENVIRONMENT, "Theirs"));

            SoftAssertions.assertSoftly(softly -> {
                softly
                    .assertThat(rootTarget("/unknown/_undeploy").request().post(null).getStatus())
                    .as("unknown guardian")
                    .isEqualTo(HttpStatusCode.NOT_FOUND_404);
                softly
                    .assertThat(rootTarget("/foreign/_undeploy").request().post(null).getStatus())
                    .as("foreign guardian")
                    .isEqualTo(HttpStatusCode.NOT_FOUND_404);
            });
        }
    }

    /**
     * The ticket's acceptance criterion, verbatim: "a user without the permission ... gets 403 on the
     * endpoints". One test per operation rather than one for the lot, because {@code @Permissions} is
     * declared per method with its own ACL &mdash; a single test would pass while another verb silently
     * lost its guard.
     */
    @Nested
    class Authorization {

        @BeforeEach
        void seedAndDenyEveryPermission() {
            // seeded so the unguarded request would succeed: the test then proves the permission
            // prevents the operation, not merely that the Guardian happens to be missing
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Credential leakage"));
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        }

        @Test
        void should_return_403_on_list_without_the_read_permission() {
            assertThat(rootTarget().request().get().getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }

        @Test
        void should_return_403_on_create_without_the_create_permission() {
            assertThat(post(PAYLOAD).getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }

        @Test
        void should_return_403_on_get_without_the_read_permission() {
            assertThat(rootTarget("/" + GUARDIAN_ID).request().get().getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }

        @Test
        void should_return_403_on_update_without_the_update_permission() {
            Response response = rootTarget("/" + GUARDIAN_ID).request().put(Entity.entity(PAYLOAD, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }

        @Test
        void should_return_403_on_undeploy_without_the_update_permission() {
            assertThat(rootTarget("/" + GUARDIAN_ID + "/_undeploy").request().post(null).getStatus())
                .as("withdrawal requires update permission")
                .isEqualTo(HttpStatusCode.FORBIDDEN_403);
            assertThat(guardianPublications.find(ENVIRONMENT, GUARDIAN_ID)).as("forbidden withdrawal has no effect").isEmpty();
        }

        @Test
        void should_return_403_on_delete_without_the_delete_permission() {
            assertThat(rootTarget("/" + GUARDIAN_ID).request().delete().getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }
    }

    @Nested
    class WithoutGuardiansLicense {

        @BeforeEach
        void removeTheFeatureFromTheLicense() {
            guardianRepository.givenGuardian(guardian(GUARDIAN_ID, ENVIRONMENT, "Created while licensed"));
            givenGuardiansLicensed(false);
        }

        @Test
        void should_refuse_every_guardian_endpoint_with_403() {
            var guardian = rootTarget("/" + GUARDIAN_ID);
            Map<String, Response> responses = Map.of(
                "list",
                rootTarget().request().get(),
                "create",
                post(PAYLOAD),
                "get",
                guardian.request().get(),
                "update",
                guardian.request().put(Entity.entity(PAYLOAD, MediaType.APPLICATION_JSON_TYPE)),
                "deploy",
                rootTarget("/" + GUARDIAN_ID + "/_deploy").request().post(Entity.json(Map.of())),
                "undeploy",
                rootTarget("/" + GUARDIAN_ID + "/_undeploy").request().post(null),
                "list deployments",
                rootTarget("/" + GUARDIAN_ID + "/deployments").request().get(),
                "get deployment",
                rootTarget("/" + GUARDIAN_ID + "/deployments/a-deployment").request().get(),
                "rollback",
                rootTarget("/" + GUARDIAN_ID + "/deployments/a-deployment/_rollback").request().post(Entity.json(Map.of())),
                "delete",
                guardian.request().delete()
            );

            SoftAssertions.assertSoftly(soft ->
                responses.forEach((endpoint, response) -> {
                    soft.assertThat(response.getStatus()).as(endpoint).isEqualTo(HttpStatusCode.FORBIDDEN_403);
                    soft
                        .assertThat(response.readEntity(JsonNode.class).path("technicalCode").asText())
                        .as(endpoint)
                        .isEqualTo("feature.missing");
                })
            );
        }

        @Test
        void should_leave_the_stored_guardians_untouched() {
            post(PAYLOAD);
            rootTarget("/" + GUARDIAN_ID).request().put(Entity.entity(PAYLOAD, MediaType.APPLICATION_JSON_TYPE));
            rootTarget("/" + GUARDIAN_ID + "/_deploy").request().post(Entity.json(Map.of()));
            rootTarget("/" + GUARDIAN_ID).request().delete();

            assertThat(guardianRepository.storedGuardians())
                .singleElement()
                .satisfies(stored -> assertThat(stored.getName()).isEqualTo("Created while licensed"));
            assertThat(guardianPublications.find(ENVIRONMENT, GUARDIAN_ID)).isEmpty();
        }
    }

    /**
     * Mirrors the production wiring of {@code GammaGuardiansConfiguration}: the use cases come from the
     * same scan rather than being declared one by one, so this test keeps exercising whatever the real
     * context would build. Only the repository and the event store are substituted — production derives the repository from
     * the {@code GuardianRepository} SPI, which the repository plugin registers at runtime and which
     * does not exist in a unit-test context.
     */
    @Configuration
    @ComponentScan(
        basePackages = "io.gravitee.gamma.rest.core.guardian",
        includeFilters = {
            @ComponentScan.Filter(type = FilterType.ANNOTATION, value = io.gravitee.apim.core.DomainService.class),
            @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class),
        }
    )
    static class GuardiansTestConfiguration {

        @Bean
        InMemoryGuardianRepository guardianRepository() {
            return new InMemoryGuardianRepository();
        }

        @Bean
        GuardianPublicationFixture guardianPublicationFixture() {
            return new GuardianPublicationFixture();
        }

        @Bean
        GenericEventPublicationDomainService genericEventPublicationDomainService(GuardianPublicationFixture publications) {
            return publications.publisher;
        }

        @Bean
        GenericEventWriterDomainService genericEventWriterDomainService(GuardianPublicationFixture publications) {
            return publications.writer;
        }

        @Bean
        EventLatestQueryService eventLatestQueryService(GuardianPublicationFixture publications) {
            return publications.query;
        }

        @Bean
        EventQueryService eventQueryService(GuardianPublicationFixture publications) {
            return publications.historyQuery;
        }

        @Bean
        UserCrudService userCrudService(GuardianAuditFixture audits) {
            return audits.users;
        }

        @Bean
        PolicyPluginProvider policyPluginProvider() {
            return GuardianFixtures.aPolicyPluginProvider();
        }

        /** The Guardian checks each resource type against what the catalog of its organization offers. */
        @Bean
        ResourcePluginProvider resourcePluginProvider() {
            var provider = mock(ResourcePluginProvider.class);
            when(provider.findAll(ORGANIZATION)).thenReturn(
                Set.of(
                    GuardianFixtures.aResourceType(GuardianFixtures.INSTALLED_RESOURCE_TYPE, true),
                    GuardianFixtures.aResourceType(GuardianFixtures.DECISION_RESOURCE_TYPE, true)
                )
            );
            return provider;
        }

        /** The organization whose license decides which resource types are enabled is read from the environment. */
        @Bean
        EnvironmentCrudService environmentCrudService() {
            var environments = new EnvironmentCrudServiceInMemory();
            environments.initWith(List.of(Environment.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build()));
            return environments;
        }

        /** The Guardian validates its resources as an API does, so the context needs that validator too. */
        @Bean
        InMemoryResourceValidation validateResourceDomainService() {
            return new InMemoryResourceValidation();
        }

        @Bean
        InMemoryPolicyValidation policyValidationDomainService() {
            return new InMemoryPolicyValidation();
        }

        @Bean
        GuardianAuditFixture guardianAuditFixture() {
            return new GuardianAuditFixture();
        }

        @Bean
        AuditDomainService auditDomainService(GuardianAuditFixture audits) {
            return audits.service;
        }

        @Bean
        JsonSerializer jsonSerializer() {
            return new JacksonJsonSerializer();
        }

        @Bean
        JsonDeserializer jsonDeserializer() {
            return new JacksonJsonDeserializer();
        }
    }
}
