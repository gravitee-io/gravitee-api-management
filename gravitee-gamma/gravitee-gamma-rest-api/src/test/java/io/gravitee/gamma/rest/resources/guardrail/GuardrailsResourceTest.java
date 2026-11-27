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
package io.gravitee.gamma.rest.resources.guardrail;

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
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.core.secret.domain_service.SecretReferencePermissionDomainService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.apim.infra.json.jackson.JacksonJsonSerializer;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryPolicyValidation;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryResourceValidation;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.ResourcePluginProvider;
import io.gravitee.gamma.rest.core.guardrail.GuardrailAuditFixture;
import io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures;
import io.gravitee.gamma.rest.core.guardrail.GuardrailPublicationFixture;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.ConditionSyntaxPort;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiReadPermissionPort;
import io.gravitee.gamma.rest.infra.adapter.GuardedApiReadPermissionPortAdapter;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resources.guardrail.GuardrailsResourceTest.GuardrailsTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
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
 * The Guardrail endpoints over HTTP, under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/guardrails}.
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
@ContextConfiguration(classes = { ResourceContextConfiguration.class, GuardrailsTestConfiguration.class })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardrailsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final String OTHER_ENVIRONMENT = "other-env";
    private static final String GUARDRAIL_ID = GuardrailFixtures.GUARDRAIL_ID;

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
    private InMemoryGuardrailRepository guardrailRepository;

    @Inject
    private InMemoryGuardedApiPort guardedApis;

    @Inject
    private GuardrailPublicationFixture guardrailPublications;

    @Inject
    private GuardrailAuditFixture withdrawalAudits;

    @Inject
    private InMemoryPolicyValidation policyConfigurations;

    @Inject
    private InMemoryResourceValidation resourceConfigurations;

    @Inject
    private LicenseManager licenseManager;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/guardrails";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
        givenGuardrailsLicensed(true);
        // a bean of the context, so a schema answer installed by one test would otherwise reach the next
        policyConfigurations.reset();
        resourceConfigurations.reset();
    }

    private void givenGuardrailsLicensed(boolean licensed) {
        License license = mock(License.class);
        when(license.isFeatureEnabled("apim-guardians")).thenReturn(licensed);
        when(licenseManager.getPlatformLicense()).thenReturn(license);
    }

    @AfterEach
    void resetRepository() {
        guardrailRepository.reset();
        guardedApis.reset();
        guardrailPublications.reset();
        withdrawalAudits.reset();
    }

    private static Guardrail guardrail(String id, String environmentId, String name) {
        return GuardrailFixtures.aGuardrail(d -> d.id(id).environmentId(environmentId).name(name)).build();
    }

    /** A guardrail-decision step whose configuration names this resource. */
    private static Map<String, Object> aDecisionStepNaming(String resource) {
        return Map.of(
            "name",
            "Ask the judge",
            "policy",
            GuardrailFixtures.DECISION_POLICY,
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
    class ListGuardrailApis {

        @Test
        void should_return_the_apis_running_the_guardrail() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));
            guardedApis.givenApi(
                GuardrailFixtures.aGuardedApi()
                    .environmentId(ENVIRONMENT)
                    .name("Support assistant")
                    .internalFlow(Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build())
                    .build()
            );

            Response response = rootTarget(GUARDRAIL_ID + "/apis").request().get();

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.OK_200);
            JsonNode row = response.readEntity(JsonNode.class).get("data").get(0);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(row.get("id").asText()).as("id").isEqualTo(GuardrailFixtures.API_ID);
                softly.assertThat(row.get("name").asText()).as("name").isEqualTo("Support assistant");
            });
        }

        @Test
        void should_return_404_for_a_guardrail_of_another_environment() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, OTHER_ENVIRONMENT, "Credential leakage"));

            Response response = rootTarget(GUARDRAIL_ID + "/apis").request().get();

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }
    }

    @Nested
    class ListGuardrails {

        @Test
        void should_return_200_with_the_paginated_envelope() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("data")).hasSize(1);
            assertThat(body.get("data").get(0).get("id").asText()).isEqualTo(GUARDRAIL_ID);
            assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(1L);
        }

        /**
         * A row is a summary, not a Guardrail. The sequence and the resources are the bulk of the
         * definition and no column renders one, so a page of twenty must not carry twenty copies of
         * them; {@code guardrailPolicyCount} is what a row says about the sequence instead.
         */
        @Test
        void should_return_a_summary_row_without_the_guardrail_policy_sequence() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

            JsonNode row = rootTarget().request().get().readEntity(JsonNode.class).get("data").get(0);

            assertThat(row.has("policies")).isFalse();
            assertThat(row.has("resources")).isFalse();
            assertThat(row.get("guardrailPolicyCount").asInt()).isEqualTo(1);
        }

        @Test
        void should_carry_everything_a_list_column_renders() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

            JsonNode row = rootTarget().request().get().readEntity(JsonNode.class).get("data").get(0);

            assertThat(row.get("id").asText()).isEqualTo(GUARDRAIL_ID);
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
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

            JsonNode dto = rootTarget("/" + GUARDRAIL_ID).request().get().readEntity(JsonNode.class);

            assertThat(dto.get("policies")).hasSize(1);
            assertThat(dto.has("environmentId")).isFalse();
        }

        @Test
        void should_count_an_empty_sequence_as_zero() {
            guardrailRepository.givenGuardrail(
                GuardrailFixtures.aGuardrail(d -> d.id(GUARDRAIL_ID).environmentId(ENVIRONMENT).policies(List.of())).build()
            );

            JsonNode row = rootTarget().request().get().readEntity(JsonNode.class).get("data").get(0);

            assertThat(row.get("guardrailPolicyCount").asInt()).isZero();
        }

        @Test
        void should_only_return_guardrails_of_the_context_environment() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Mine"));
            guardrailRepository.givenGuardrail(guardrail("elsewhere", OTHER_ENVIRONMENT, "Theirs"));

            JsonNode body = rootTarget().request().get().readEntity(JsonNode.class);

            assertThat(body.get("data")).hasSize(1);
            assertThat(body.get("data").get(0).get("id").asText()).isEqualTo(GUARDRAIL_ID);
        }

        @Test
        void should_list_only_the_deployed_guardrails_an_api_type_can_run() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Deployed"));
            guardrailRepository.givenGuardrail(guardrail("never-deployed", ENVIRONMENT, "Never deployed"));
            guardrailPublications.givenPublication(
                guardrailRepository.findByEnvironmentId(ENVIRONMENT, GUARDRAIL_ID).orElseThrow().getDefinition(),
                Instant.parse("2026-11-09T10:00:00Z")
            );

            JsonNode body = rootTarget().queryParam("deployedFor", "llm-proxy").request().get().readEntity(JsonNode.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(body.get("data")).as("guardrails offered").hasSize(1);
                softly.assertThat(body.get("data").get(0).get("id").asText()).as("guardrail offered").isEqualTo(GUARDRAIL_ID);
                softly.assertThat(body.get("pagination").get("totalCount").asLong()).as("guardrails offered in all").isEqualTo(1L);
            });
        }

        @Test
        void should_refuse_an_unknown_api_type_with_400() {
            Response response = rootTarget().queryParam("deployedFor", "no-such-type").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
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
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

            JsonNode pagination = rootTarget().request().get().readEntity(JsonNode.class).get("pagination");

            assertThat(pagination.get("perPage").asInt()).isEqualTo(10);
            assertThat(pagination.get("pageCount").asInt()).isEqualTo(1);
            assertThat(pagination.get("pageItemsCount").asInt()).isEqualTo(1);
        }

        @Test
        void should_report_the_default_page_size_when_perPage_is_zero() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

            JsonNode pagination = rootTarget().queryParam("perPage", 0).request().get().readEntity(JsonNode.class).get("pagination");

            // perPage=0 falls back to the default, so a pageCount of 0 would tell the console there is
            // nothing to show while the page carries a row
            assertThat(pagination.get("perPage").asInt()).isEqualTo(10);
            assertThat(pagination.get("pageCount").asInt()).isEqualTo(1);
        }

        @Test
        void should_keep_the_requested_page_size_on_a_partial_last_page() {
            guardrailRepository.givenGuardrail(guardrail("g1", ENVIRONMENT, "A"));
            guardrailRepository.givenGuardrail(guardrail("g2", ENVIRONMENT, "B"));
            guardrailRepository.givenGuardrail(guardrail("g3", ENVIRONMENT, "C"));

            JsonNode body = rootTarget().queryParam("page", 2).queryParam("perPage", 2).request().get().readEntity(JsonNode.class);

            // one row on the last page must not shrink perPage to 1, which would turn pageCount into 3
            assertThat(body.get("data")).hasSize(1);
            assertThat(body.get("pagination").get("perPage").asInt()).isEqualTo(2);
            assertThat(body.get("pagination").get("pageCount").asInt()).isEqualTo(2);
            assertThat(body.get("pagination").get("pageItemsCount").asInt()).isEqualTo(1);
        }
    }

    @Nested
    class CreateGuardrail {

        /** So the console can save its first edit without reading the Guardrail again. */
        @Test
        void should_name_the_version_of_the_created_guardrail_in_the_etag() {
            assertThat(post(PAYLOAD).getEntityTag()).isEqualTo(new EntityTag("1"));
        }

        @Test
        void should_return_201_with_a_location_and_an_undeployed_guardrail() {
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
        void should_store_the_guardrail_policy_sequence_the_payload_carries() {
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
            var resource = Map.of("name", "classifier", "type", GuardrailFixtures.INSTALLED_RESOURCE_TYPE, "configuration", Map.of());

            var response = post(payloadWith("resources", List.of(resource)));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("creation response").isEqualTo(HttpStatusCode.CREATED_201);
                JsonNode resources = response.readEntity(JsonNode.class).get("resources");
                softly.assertThat(resources).as("stored resources").hasSize(1);
                softly.assertThat(resources.at("/0/name").asText()).as("name").isEqualTo("classifier");
                softly.assertThat(resources.at("/0/type").asText()).as("type").isEqualTo(GuardrailFixtures.INSTALLED_RESOURCE_TYPE);
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
            var resource = Map.of("name", "classifier", "type", GuardrailFixtures.INSTALLED_RESOURCE_TYPE, "configuration", Map.of());

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
            resourceConfigurations.givenRefuses(GuardrailFixtures.INSTALLED_RESOURCE_TYPE, "$.model: is missing but it is required");
            var resource = Map.of("name", "classifier", "type", GuardrailFixtures.INSTALLED_RESOURCE_TYPE, "configuration", Map.of());

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
            policyConfigurations.givenRefuses(GuardrailFixtures.GUARDRAIL_POLICY, "$.rules: is missing but it is required");

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
        void should_refuse_a_guardrail_policy_naming_a_resource_the_guardrail_does_not_hold_with_400_naming_the_step() {
            var response = post(payloadWith("policies", List.of(aDecisionStepNaming("missing"))));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("creation response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .isEqualTo("Step [Ask the judge] references resource [missing], which the Guardrail does not hold.");
                softly.assertThat(guardrailRepository.storedGuardrails()).as("nothing stored").isEmpty();
            });
        }

        @Test
        void should_refuse_a_guardrail_policy_naming_a_resource_of_the_wrong_type_with_400_naming_the_step() {
            var payload = payloadWith("policies", List.of(aDecisionStepNaming("classifier")));
            payload.put(
                "resources",
                List.of(Map.of("name", "classifier", "type", GuardrailFixtures.INSTALLED_RESOURCE_TYPE, "configuration", Map.of()))
            );

            var response = post(payload);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("creation response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .isEqualTo(
                        "Step [Ask the judge] references a resource of the wrong type: [classifier] is of type [" +
                            GuardrailFixtures.INSTALLED_RESOURCE_TYPE +
                            "], where the policy accepts [" +
                            GuardrailFixtures.DECISION_RESOURCE_TYPE +
                            "]."
                    );
            });
        }

        @Test
        void should_create_a_guardrail_policy_naming_an_enabled_resource_of_the_accepted_type() {
            var payload = payloadWith("policies", List.of(aDecisionStepNaming("judge")));
            payload.put(
                "resources",
                List.of(Map.of("name", "judge", "type", GuardrailFixtures.DECISION_RESOURCE_TYPE, "configuration", Map.of()))
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
        void should_refuse_a_null_policy_with_400_without_storing_the_guardrail() {
            var response = post(payloadWith("policies", Collections.singletonList(null)));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("null policy is malformed input").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly.assertThat(guardrailRepository.storedGuardrails()).as("rejected input is not stored").isEmpty();
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
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));
        }

        private Response put(Object payload) {
            return rootTarget("/" + GUARDRAIL_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE));
        }

        /**
         * The version of the draft travels as an ETag, and an update states the one it was built on in
         * If-Match: a write built on a draft someone else changed since is refused, not applied over it.
         */
        @Nested
        class The_version_an_update_is_built_on {

            @BeforeEach
            void storedAtVersion3() {
                guardrailRepository.reset();
                guardrailRepository.givenGuardrail(
                    GuardrailFixtures.aGuardrail(d -> d.id(GUARDRAIL_ID).environmentId(ENVIRONMENT).name("Credential leakage"))
                        .version(3)
                        .build()
                );
            }

            private Response putBuiltOn(String ifMatch) {
                var request = rootTarget("/" + GUARDRAIL_ID).request();
                return (ifMatch == null ? request : request.header(HttpHeaders.IF_MATCH, ifMatch)).put(
                    Entity.entity(payloadWith("name", "Renamed"), MediaType.APPLICATION_JSON_TYPE)
                );
            }

            @Test
            void should_name_the_version_of_a_guardrail_it_reads_in_the_etag_and_in_the_body() {
                var response = rootTarget("/" + GUARDRAIL_ID).request().get();

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(response.getEntityTag()).as("ETag").isEqualTo(new EntityTag("3"));
                    softly.assertThat(response.readEntity(JsonNode.class).at("/version").asInt()).as("version").isEqualTo(3);
                });
            }

            @Test
            void should_store_an_update_built_on_the_current_version_and_answer_the_next_one() {
                var response = putBuiltOn("\"3\"");

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.OK_200);
                    softly.assertThat(response.getEntityTag()).as("ETag").isEqualTo(new EntityTag("4"));
                    softly.assertThat(response.readEntity(JsonNode.class).at("/name").asText()).as("name").isEqualTo("Renamed");
                });
            }

            @Test
            void should_refuse_an_update_built_on_an_older_version_with_412_and_the_guardrail_as_it_is_now() {
                var response = putBuiltOn("\"2\"");
                var body = response.readEntity(JsonNode.class);

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(response.getStatus()).as("status").isEqualTo(412);
                    softly.assertThat(response.getEntityTag()).as("the ETag to state next").isEqualTo(new EntityTag("3"));
                    softly.assertThat(body.at("/http_status").asInt()).as("http_status").isEqualTo(412);
                    softly.assertThat(body.at("/currentVersion").asInt()).as("currentVersion").isEqualTo(3);
                    softly.assertThat(body.at("/guardrail/name").asText()).as("the stored name").isEqualTo("Credential leakage");
                    softly.assertThat(body.at("/message").asText()).as("message").contains("modified since it was read");
                });
                assertThat(guardrailRepository.storedGuardrails())
                    .singleElement()
                    .extracting(Guardrail::getName)
                    .isEqualTo("Credential leakage");
            }

            /** Refused rather than read as an overwrite: an implicit force is what this exists to prevent. */
            @Test
            void should_refuse_an_update_that_states_no_version_with_428() {
                var response = putBuiltOn(null);

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(response.getStatus()).as("status").isEqualTo(428);
                    softly
                        .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                        .as("message")
                        .contains("If-Match is required");
                });
                assertThat(guardrailRepository.storedGuardrails())
                    .singleElement()
                    .extracting(Guardrail::getName)
                    .isEqualTo("Credential leakage");
            }

            /** Not a failed precondition: an overwrite states none, so there is no version to hand back. */
            @Test
            void should_answer_409_to_an_overwrite_other_writers_overtake_every_time() {
                guardrailRepository.givenAnotherWriterAlwaysGetsInFirst(stored ->
                    stored.toBuilder().description("Saved meanwhile").version(stored.getVersion() + 1).build()
                );

                var response = putBuiltOn("*");

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.CONFLICT_409);
                    softly.assertThat(response.getEntityTag()).as("no ETag to state").isNull();
                    softly.assertThat(response.readEntity(JsonNode.class).at("/message").asText()).as("message").contains("try again");
                });
                assertThat(guardrailRepository.storedGuardrails())
                    .singleElement()
                    .extracting(Guardrail::getName)
                    .as("the overwrite was not stored")
                    .isEqualTo("Credential leakage");
            }

            @Test
            void should_overwrite_whatever_is_stored_when_asked_to_with_a_star() {
                var response = putBuiltOn("*");

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.OK_200);
                    softly.assertThat(response.getEntityTag()).as("ETag").isEqualTo(new EntityTag("4"));
                });
            }

            /** A proxy may weaken a validator or a client send several: both name versions all the same. */
            @ParameterizedTest
            @ValueSource(strings = { "W/\"3\"", "3", "\"1\", \"3\"" })
            void should_read_every_way_http_writes_the_current_version(String ifMatch) {
                assertThat(putBuiltOn(ifMatch).getStatus()).isEqualTo(HttpStatusCode.OK_200);
            }

            @ParameterizedTest
            @ValueSource(strings = { "\"abc\"", "*, \"3\"" })
            void should_refuse_a_validator_this_api_did_not_issue_with_400(String ifMatch) {
                assertThat(putBuiltOn(ifMatch).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            }

            /** Sending back what GET returned must keep working, the version it now carries included. */
            @Test
            void should_ignore_a_version_sent_in_the_body() {
                var response = rootTarget("/" + GUARDRAIL_ID)
                    .request()
                    .header(HttpHeaders.IF_MATCH, "\"3\"")
                    .put(Entity.entity(payloadWith("version", 99), MediaType.APPLICATION_JSON_TYPE));

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.OK_200);
                    softly.assertThat(response.readEntity(JsonNode.class).at("/version").asInt()).as("version").isEqualTo(4);
                });
            }

            /** Stored before the counter existed: nothing to match on, so nothing is promised in an ETag. */
            @Test
            void should_carry_no_etag_for_a_guardrail_that_has_no_version_yet() {
                guardrailRepository.reset();
                guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

                var response = rootTarget("/" + GUARDRAIL_ID).request().get();

                assertThat(response.getEntityTag()).isNull();
            }
        }

        @Test
        void should_return_200_on_get() {
            Response response = rootTarget("/" + GUARDRAIL_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.readEntity(JsonNode.class).get("id").asText()).isEqualTo(GUARDRAIL_ID);
        }

        /** What is stored has to be what the plugin validated, so an update answers the corrected configuration. */
        @Test
        void should_store_the_resource_configuration_its_plugin_accepts_on_put() {
            resourceConfigurations.givenRewrites(GuardrailFixtures.INSTALLED_RESOURCE_TYPE, "{\"threshold\":0.5}");
            var resource = Map.of("name", "classifier", "type", GuardrailFixtures.INSTALLED_RESOURCE_TYPE, "configuration", Map.of());

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
        void should_return_404_on_an_unknown_guardrail() {
            Response response = rootTarget("/nope").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_replace_the_whole_definition_on_put() {
            Response response = put(payloadWith("policies", List.of()));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("name").asText()).isEqualTo("Prompt injection shield");
            // a PUT replaces: the guardrail policy the stored definition carried is gone because the payload
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
            JsonNode read = rootTarget("/" + GUARDRAIL_ID).request().get().readEntity(JsonNode.class);

            Response response = put(read);

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_ignore_an_id_in_the_update_body_and_keep_the_path_one() {
            // the path owns it, so a second one in the body is dropped rather than obeyed
            Response response = put(payloadWith("id", "11111111-2222-3333-4444-555555555555"));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.readEntity(JsonNode.class).get("id").asText()).isEqualTo(GUARDRAIL_ID);
        }

        /** Ignored must mean dropped, not applied: a body cannot deploy a Guardrail or rewrite its history. */
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
            Response response = rootTarget("/" + GUARDRAIL_ID).request().delete();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
            assertThat(guardrailRepository.storedGuardrails()).isEmpty();
        }

        @Test
        void should_refuse_deletion_of_a_deployed_guardrail_with_409() {
            guardrailPublications.givenPublication(
                guardrailRepository.findByEnvironmentId(ENVIRONMENT, GUARDRAIL_ID).orElseThrow().getDefinition(),
                Instant.parse("2026-11-09T10:00:00Z")
            );

            var response = rootTarget("/" + GUARDRAIL_ID).request().delete();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("active publication prevents deletion").isEqualTo(HttpStatusCode.CONFLICT_409);
                softly
                    .assertThat(response.readEntity(JsonNode.class).path("message").asText())
                    .as("actionable conflict")
                    .contains("active publication");
                softly.assertThat(guardrailRepository.storedGuardrails()).as("draft preserved").hasSize(1);
                softly.assertThat(guardrailPublications.withdrawn(ENVIRONMENT, GUARDRAIL_ID)).as("publication preserved").isFalse();
            });
        }

        @Test
        void should_refuse_deletion_of_a_guardrail_an_api_runs_with_409() {
            givenAnApiRunningTheGuardrail();

            var response = rootTarget("/" + GUARDRAIL_ID).request().delete();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("API running it prevents deletion").isEqualTo(HttpStatusCode.CONFLICT_409);
                softly
                    .assertThat(response.readEntity(JsonNode.class).path("message").asText())
                    .as("conflict counting the APIs")
                    .contains("1 API runs it");
                softly.assertThat(guardrailRepository.storedGuardrails()).as("draft preserved").hasSize(1);
            });
        }

        @Test
        void should_refuse_to_move_a_guardrail_an_api_runs_to_another_phase_with_409() {
            givenAnApiRunningTheGuardrail();
            var movedToTheOtherPhase = new HashMap<String, Object>(PAYLOAD);
            movedToTheOtherPhase.put("name", "Credential leakage");
            movedToTheOtherPhase.put("phase", "RESPONSE");

            var response = put(movedToTheOtherPhase);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("API running it prevents the move").isEqualTo(HttpStatusCode.CONFLICT_409);
                softly
                    .assertThat(response.readEntity(JsonNode.class).path("message").asText())
                    .as("conflict counting the APIs")
                    .contains("1 API runs it");
                softly
                    .assertThat(guardrailRepository.findByEnvironmentId(ENVIRONMENT, GUARDRAIL_ID).orElseThrow().getPhase())
                    .as("stored phase")
                    .isEqualTo(Phase.REQUEST);
            });
        }

        @Test
        void should_refuse_a_missing_update_body_with_400() {
            assertThat(put("null").getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        private void givenAnApiRunningTheGuardrail() {
            guardedApis.givenApi(
                GuardrailFixtures.aGuardedApi()
                    .environmentId(ENVIRONMENT)
                    .internalFlow(Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build())
                    .build()
            );
        }
    }

    @Nested
    class DeployGuardrail {

        @BeforeEach
        void seed() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));
        }

        private Response deploy(Object body) {
            return rootTarget("/" + GUARDRAIL_ID + "/_deploy").request().post(Entity.json(body));
        }

        @Test
        void should_return_the_deployed_guardrail_and_publication_date() {
            Response response = deploy(Map.of("deploymentLabel", "Reviewed"));
            assertThat(response.getStatus()).as("deployment response").isEqualTo(HttpStatusCode.OK_200);
            var body = response.readEntity(JsonNode.class);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(body.get("lifecycleState").asText()).isEqualTo("DEPLOYED");
                softly
                    .assertThat(Instant.parse(body.get("deployedAt").asText()))
                    .as("ISO 8601 publication date at stored millisecond precision")
                    .isEqualTo(Date.from(guardrailPublications.published(ENVIRONMENT, GUARDRAIL_ID).at()).toInstant());

                softly.assertThat(guardrailPublications.lastLabel()).isEqualTo("Reviewed");
            });
        }

        @Test
        void should_report_the_revision_the_gateways_run() {
            var neverDeployed = rootTarget("/" + GUARDRAIL_ID).request().get().readEntity(JsonNode.class);
            deploy(Map.of());
            var deployed = deploy(Map.of()).readEntity(JsonNode.class);
            var read = rootTarget("/" + GUARDRAIL_ID).request().get().readEntity(JsonNode.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(neverDeployed.has("deployedRevisionId")).as("never deployed").isFalse();
                softly
                    .assertThat(deployed.at("/deployedRevisionId").asText())
                    .as("revision answered by the deployment")
                    .isEqualTo(guardrailPublications.lastPublicationId());
                softly
                    .assertThat(read.at("/deployedRevisionId").asText())
                    .as("revision read back")
                    .isEqualTo(guardrailPublications.lastPublicationId());
            });
        }

        @Test
        void should_accept_the_get_response_of_a_deployed_guardrail_as_an_update_body() {
            deploy(Map.of());
            var read = rootTarget("/" + GUARDRAIL_ID).request().get().readEntity(JsonNode.class);

            var response = rootTarget("/" + GUARDRAIL_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.entity(read, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).as("update with the read deployed revision").isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_reject_an_unknown_property_before_publication() {
            var response = deploy(Map.of("deploymentLable", "Reviewed"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("unknown deployment property").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly.assertThat(guardrailPublications.publications()).as("no revision is published").isZero();
            });
        }

        @Test
        void should_accept_an_absent_body() {
            var response = rootTarget("/" + GUARDRAIL_ID + "/_deploy").request().method("POST");
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_reject_an_overlong_label_before_publication() {
            assertThat(deploy(Map.of("deploymentLabel", "x".repeat(257))).getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(guardrailPublications.publications()).isZero();
        }

        @Test
        void should_return_400_with_a_readable_message_when_the_sequence_has_no_enabled_guardrail_policy() {
            guardrailRepository.reset();
            guardrailRepository.givenGuardrail(
                GuardrailFixtures.aGuardrail(d -> d.id(GUARDRAIL_ID).environmentId(ENVIRONMENT).policies(List.of())).build()
            );

            var response = deploy(Map.of());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("deployment response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .as("readable message")
                    .contains("at least one enabled guardrail policy");
                softly.assertThat(guardrailPublications.publications()).as("no revision is published").isZero();
            });
        }

        @Test
        void should_return_400_naming_the_step_and_the_schema_error_when_the_stored_draft_no_longer_conforms() {
            policyConfigurations.givenRefuses(GuardrailFixtures.GUARDRAIL_POLICY, "$.rules: is missing but it is required");

            var response = deploy(Map.of());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("deployment response").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                softly
                    .assertThat(response.readEntity(JsonNode.class).at("/message").asText())
                    .as("the step and the schema error, readable")
                    .contains("Detect credentials", "$.rules: is missing but it is required");
                softly.assertThat(guardrailPublications.publications()).as("no revision is published").isZero();
            });
        }

        /** A draft stored before the rule existed is refused rather than published broken. */
        @Test
        void should_return_400_naming_the_step_when_the_stored_draft_names_a_resource_the_guardrail_does_not_hold() {
            guardrailRepository.reset();
            guardrailRepository.givenGuardrail(
                GuardrailFixtures.aGuardrail(d ->
                    d
                        .id(GUARDRAIL_ID)
                        .environmentId(ENVIRONMENT)
                        .name("Credential leakage")
                        .policies(
                            List.of(
                                Step.builder()
                                    .name("Ask the judge")
                                    .policy(GuardrailFixtures.DECISION_POLICY)
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
                    .isEqualTo("Step [Ask the judge] references resource [missing], which the Guardrail does not hold.");
                softly.assertThat(guardrailPublications.publications()).as("no revision is published").isZero();
            });
        }

        @Test
        void should_return_404_for_an_unknown_guardrail() {
            var response = rootTarget("/unknown/_deploy").request().post(Entity.json(Map.of()));
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            assertThat(guardrailPublications.publications()).isZero();
        }

        @Test
        void should_return_404_for_a_guardrail_from_another_environment() {
            guardrailRepository.reset();
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, OTHER_ENVIRONMENT, "Elsewhere"));
            assertThat(deploy(Map.of()).getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            assertThat(guardrailPublications.publications()).isZero();
        }

        @Test
        void should_refuse_deployment_without_the_update_permission() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
            assertThat(deploy(Map.of()).getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
            assertThat(guardrailPublications.publications()).isZero();
        }
    }

    /**
     * Two Guardrails of one environment may not share a name. A 409 rather than a 400: the request is
     * well formed and the caller can retry it with another name.
     */
    @Nested
    class NameUniqueness {

        @Test
        void should_refuse_a_creation_whose_name_is_taken_with_409() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

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
         * variant was a new name: one environment ended up with Guardrails that read identically in the
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
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

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
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));
            guardrailRepository.givenGuardrail(guardrail("second-id", ENVIRONMENT, "Prompt injection"));

            Response response = rootTarget("/second-id")
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.entity(payloadWith("name", " Credential leakage "), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CONFLICT_409);
        }

        /** Its own name, padded, is still its own name — not a clash with itself, and stored trimmed. */
        @Test
        void should_let_a_guardrail_keep_its_own_padded_name_on_update() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

            Response response = rootTarget("/" + GUARDRAIL_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
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
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

            Response response = rootTarget("/" + GUARDRAIL_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.entity(payloadWith("name", blank), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        /**
         * The update carries its name in the definition, which declares no maximum length of its own —
         * the column does, and a longer name used to reach it as a technical failure.
         */
        @Test
        void should_refuse_an_update_whose_name_is_longer_than_the_column_with_400() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));
            String tooLong = "x".repeat(513);

            Response response = rootTarget("/" + GUARDRAIL_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
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
                "Guardrail name must be at most 512 characters"
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
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

            assertThat(post(payloadWith("name", "CREDENTIAL LEAKAGE")).getStatus()).isEqualTo(HttpStatusCode.CONFLICT_409);
        }

        /** A Guardrail is scoped to its environment, so the name is only taken inside one. */
        @Test
        void should_allow_the_same_name_in_another_environment() {
            guardrailRepository.givenGuardrail(guardrail("other-id", OTHER_ENVIRONMENT, "Credential leakage"));

            assertThat(post(payloadWith("name", "Credential leakage")).getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
        }

        @Test
        void should_refuse_renaming_a_guardrail_onto_a_taken_name_with_409() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));
            guardrailRepository.givenGuardrail(guardrail("second-id", ENVIRONMENT, "Prompt injection"));

            Response response = rootTarget("/second-id")
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.entity(payloadWith("name", "Credential leakage"), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CONFLICT_409);
        }

        /**
         * Two case variants can already sit in one environment: the unique indexes compare exactly, so
         * "Foo" beside "foo" survives data that predates this rule or two creates that raced past it.
         * Editing either must still work — otherwise neither Guardrail could ever be saved again — and
         * the answer must not depend on which row the store returns first.
         */
        @Test
        void should_let_a_guardrail_be_edited_while_a_case_variant_of_its_name_exists() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));
            guardrailRepository.givenGuardrail(guardrail("variant-id", ENVIRONMENT, "credential leakage"));

            Response response = rootTarget("/" + GUARDRAIL_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.entity(payloadWith("name", "Credential leakage"), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        /** But renaming onto a name a case variant holds is still refused, whichever row comes back. */
        @Test
        void should_refuse_a_rename_onto_a_name_a_case_variant_holds() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));
            guardrailRepository.givenGuardrail(guardrail("variant-id", ENVIRONMENT, "credential leakage"));
            guardrailRepository.givenGuardrail(guardrail("third-id", ENVIRONMENT, "Prompt injection"));

            Response response = rootTarget("/third-id")
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.entity(payloadWith("name", "CREDENTIAL LEAKAGE"), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CONFLICT_409);
        }

        /**
         * A body with no name is a malformed body, not a server fault. The uniqueness check reads the
         * name before the definition is validated, so it must not be the thing that fails first.
         */
        @Test
        void should_refuse_an_update_body_without_a_name_with_400() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));
            var noName = new HashMap<String, Object>(PAYLOAD);
            noName.remove("name");

            Response response = rootTarget("/" + GUARDRAIL_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.entity(noName, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        /** The one case a naive check gets wrong: a Guardrail clashing with itself. */
        @Test
        void should_let_a_guardrail_keep_its_own_name_on_update() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

            Response response = rootTarget("/" + GUARDRAIL_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.entity(payloadWith("name", "Credential leakage"), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_let_a_guardrail_keep_its_own_name_in_another_case_on_update() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));

            Response response = rootTarget("/" + GUARDRAIL_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
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
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));
        }

        @Test
        void should_write_the_timestamps_as_iso_8601_strings_in_utc() {
            JsonNode dto = rootTarget("/" + GUARDRAIL_ID).request().get().readEntity(JsonNode.class);

            assertThat(dto.get("createdAt").asText()).isEqualTo(CREATED_AT);
            assertThat(dto.get("updatedAt").asText()).isEqualTo(UPDATED_AT);
        }

        /** The regression guard: an epoch number parses as text too, so assert the JSON type itself. */
        @Test
        void should_not_write_the_timestamps_as_numbers() {
            JsonNode dto = rootTarget("/" + GUARDRAIL_ID).request().get().readEntity(JsonNode.class);

            assertThat(dto.get("createdAt").isTextual()).isTrue();
            assertThat(dto.get("createdAt").isNumber()).isFalse();
            assertThat(dto.get("updatedAt").isTextual()).isTrue();
            assertThat(dto.get("updatedAt").isNumber()).isFalse();
        }

        @Test
        void should_write_the_deployment_stamp_as_an_iso_8601_string_in_utc() {
            guardrailRepository.reset();
            guardrailRepository.givenGuardrail(
                GuardrailFixtures.aGuardrail(d -> d.id(GUARDRAIL_ID).environmentId(ENVIRONMENT))
                    .deployedAt(Instant.parse("2026-02-03T20:22:02Z"))
                    .build()
            );

            guardrailPublications.givenPublication(
                guardrailRepository.findByEnvironmentId(ENVIRONMENT, GUARDRAIL_ID).orElseThrow().getDefinition(),
                Instant.parse("2026-02-03T20:22:02Z")
            );

            JsonNode dto = rootTarget("/" + GUARDRAIL_ID).request().get().readEntity(JsonNode.class);

            assertThat(dto.get("deployedAt").isTextual()).isTrue();
            assertThat(dto.get("deployedAt").asText()).isEqualTo("2026-02-03T20:22:02Z");
        }
    }

    @Nested
    class UndeployGuardrail {

        @Test
        void should_withdraw_without_a_request_body_and_return_the_preserved_draft() {
            var guardrail = guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage");
            guardrailRepository.givenGuardrail(guardrail);
            guardrailPublications.givenPublication(guardrail.getDefinition(), Instant.parse("2026-11-09T10:00:00Z"));

            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
            when(
                permissionService.hasPermission(
                    any(),
                    eq(RolePermission.ENVIRONMENT_GUARDRAIL),
                    eq(ENVIRONMENT),
                    eq(RolePermissionAction.UPDATE)
                )
            ).thenReturn(true);

            var response = rootTarget("/" + GUARDRAIL_ID + "/_undeploy").request().post(null);

            assertThat(response.getStatus()).as("withdrawal response").isEqualTo(HttpStatusCode.OK_200);
            var body = response.readEntity(JsonNode.class);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(body.path("lifecycleState").asText()).as("withdrawn state").isEqualTo("UNDEPLOYED");
                softly.assertThat(body.path("name").asText()).as("draft preserved").isEqualTo(guardrail.getName());
                softly.assertThat(guardrailPublications.withdrawn(ENVIRONMENT, GUARDRAIL_ID)).as("withdrawal published").isTrue();
            });
        }

        @Test
        void should_return_409_while_an_api_runs_the_guardrail() {
            var guardrail = guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage");
            guardrailRepository.givenGuardrail(guardrail);
            guardrailPublications.givenPublication(guardrail.getDefinition(), Instant.parse("2026-11-09T10:00:00Z"));
            guardedApis.givenApi(
                GuardrailFixtures.aGuardedApi()
                    .environmentId(ENVIRONMENT)
                    .internalFlow(Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build())
                    .build()
            );

            var response = rootTarget("/" + GUARDRAIL_ID + "/_undeploy").request().post(null);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("withdrawal response").isEqualTo(HttpStatusCode.CONFLICT_409);
                softly.assertThat(guardrailPublications.withdrawn(ENVIRONMENT, GUARDRAIL_ID)).as("withdrawal published").isFalse();
            });
        }

        @Test
        void should_return_404_for_unknown_and_foreign_guardrails() {
            guardrailRepository.givenGuardrail(guardrail("foreign", OTHER_ENVIRONMENT, "Theirs"));

            SoftAssertions.assertSoftly(softly -> {
                softly
                    .assertThat(rootTarget("/unknown/_undeploy").request().post(null).getStatus())
                    .as("unknown guardrail")
                    .isEqualTo(HttpStatusCode.NOT_FOUND_404);
                softly
                    .assertThat(rootTarget("/foreign/_undeploy").request().post(null).getStatus())
                    .as("foreign guardrail")
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
            // prevents the operation, not merely that the Guardrail happens to be missing
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));
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
            assertThat(rootTarget("/" + GUARDRAIL_ID).request().get().getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }

        @Test
        void should_return_403_on_update_without_the_update_permission() {
            Response response = rootTarget("/" + GUARDRAIL_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.entity(PAYLOAD, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }

        @Test
        void should_return_403_on_undeploy_without_the_update_permission() {
            assertThat(rootTarget("/" + GUARDRAIL_ID + "/_undeploy").request().post(null).getStatus())
                .as("withdrawal requires update permission")
                .isEqualTo(HttpStatusCode.FORBIDDEN_403);
            assertThat(guardrailPublications.find(ENVIRONMENT, GUARDRAIL_ID)).as("forbidden withdrawal has no effect").isEmpty();
        }

        @Test
        void should_return_403_on_delete_without_the_delete_permission() {
            assertThat(rootTarget("/" + GUARDRAIL_ID).request().delete().getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }
    }

    @Nested
    class WithoutGuardrailsLicense {

        @BeforeEach
        void removeTheFeatureFromTheLicense() {
            guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Created while licensed"));
            givenGuardrailsLicensed(false);
        }

        @Test
        void should_refuse_every_guardrail_endpoint_with_403() {
            var guardrail = rootTarget("/" + GUARDRAIL_ID);
            Map<String, Response> responses = Map.ofEntries(
                Map.entry("list", rootTarget().request().get()),
                Map.entry("create", post(PAYLOAD)),
                Map.entry("get", guardrail.request().get()),
                Map.entry(
                    "update",
                    guardrail.request().header(HttpHeaders.IF_MATCH, "*").put(Entity.entity(PAYLOAD, MediaType.APPLICATION_JSON_TYPE))
                ),
                Map.entry("deploy", rootTarget("/" + GUARDRAIL_ID + "/_deploy").request().post(Entity.json(Map.of()))),
                Map.entry("undeploy", rootTarget("/" + GUARDRAIL_ID + "/_undeploy").request().post(null)),
                Map.entry("list deployments", rootTarget("/" + GUARDRAIL_ID + "/deployments").request().get()),
                Map.entry("get deployment", rootTarget("/" + GUARDRAIL_ID + "/deployments/a-deployment").request().get()),
                Map.entry(
                    "rollback",
                    rootTarget("/" + GUARDRAIL_ID + "/deployments/a-deployment/_rollback").request().post(Entity.json(Map.of()))
                ),
                Map.entry("list apis", rootTarget("/" + GUARDRAIL_ID + "/apis").request().get()),
                Map.entry("delete", guardrail.request().delete())
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
        void should_leave_the_stored_guardrails_untouched() {
            post(PAYLOAD);
            rootTarget("/" + GUARDRAIL_ID)
                .request()
                .header(HttpHeaders.IF_MATCH, "*")
                .put(Entity.entity(PAYLOAD, MediaType.APPLICATION_JSON_TYPE));
            rootTarget("/" + GUARDRAIL_ID + "/_deploy").request().post(Entity.json(Map.of()));
            rootTarget("/" + GUARDRAIL_ID).request().delete();

            assertThat(guardrailRepository.storedGuardrails())
                .singleElement()
                .satisfies(stored -> assertThat(stored.getName()).isEqualTo("Created while licensed"));
            assertThat(guardrailPublications.find(ENVIRONMENT, GUARDRAIL_ID)).isEmpty();
        }
    }

    /**
     * Mirrors the production wiring of {@code GammaGuardrailsConfiguration}: the use cases come from the
     * same scan rather than being declared one by one, so this test keeps exercising whatever the real
     * context would build. Only the repository and the event store are substituted — production derives the repository from
     * the {@code GuardrailRepository} SPI, which the repository plugin registers at runtime and which
     * does not exist in a unit-test context.
     */
    @Configuration
    @ComponentScan(
        basePackages = "io.gravitee.gamma.rest.core.guardrail",
        includeFilters = {
            @ComponentScan.Filter(type = FilterType.ANNOTATION, value = io.gravitee.apim.core.DomainService.class),
            @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class),
        }
    )
    static class GuardrailsTestConfiguration {

        @Bean
        InMemoryGuardrailRepository guardrailRepository() {
            return new InMemoryGuardrailRepository();
        }

        @Bean
        InMemoryGuardedApiPort guardedApiPort() {
            return new InMemoryGuardedApiPort();
        }

        @Bean
        ConditionSyntaxPort conditionSyntaxPort() {
            return GuardrailFixtures.aConditionSyntax();
        }

        @Bean
        GuardrailPublicationFixture guardrailPublicationFixture() {
            return new GuardrailPublicationFixture();
        }

        /** Every API readable, so a test asserting on the names of a list is about the list and not about roles. */
        @Bean
        PermissionDomainService permissionDomainService() {
            var permissions = mock(PermissionDomainService.class);
            when(permissions.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
            return permissions;
        }

        /** The real adapter over the mock above, so the wiring the resource runs through is the production one. */
        @Bean
        GuardedApiReadPermissionPort guardedApiReadPermissionPort(PermissionDomainService permissions) {
            return new GuardedApiReadPermissionPortAdapter(permissions);
        }

        @Bean
        GenericEventPublicationDomainService genericEventPublicationDomainService(GuardrailPublicationFixture publications) {
            return publications.publisher;
        }

        @Bean
        GenericEventWriterDomainService genericEventWriterDomainService(GuardrailPublicationFixture publications) {
            return publications.writer;
        }

        @Bean
        EventLatestQueryService eventLatestQueryService(GuardrailPublicationFixture publications) {
            return publications.query;
        }

        @Bean
        EventQueryService eventQueryService(GuardrailPublicationFixture publications) {
            return publications.historyQuery;
        }

        @Bean
        UserCrudService userCrudService(GuardrailAuditFixture audits) {
            return audits.users;
        }

        @Bean
        PolicyPluginProvider policyPluginProvider() {
            return GuardrailFixtures.aPolicyPluginProvider();
        }

        /** The Guardrail checks each resource type against what the catalog of its organization offers. */
        @Bean
        ResourcePluginProvider resourcePluginProvider() {
            var provider = mock(ResourcePluginProvider.class);
            when(provider.findAll(ORGANIZATION)).thenReturn(
                Set.of(
                    GuardrailFixtures.aResourceType(GuardrailFixtures.INSTALLED_RESOURCE_TYPE, true),
                    GuardrailFixtures.aResourceType(GuardrailFixtures.DECISION_RESOURCE_TYPE, true)
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

        /** The Guardrail validates its resources as an API does, so the context needs that validator too. */
        @Bean
        InMemoryResourceValidation validateResourceDomainService() {
            return new InMemoryResourceValidation();
        }

        @Bean
        InMemoryPolicyValidation policyValidationDomainService() {
            return new InMemoryPolicyValidation();
        }

        @Bean
        GuardrailAuditFixture guardrailAuditFixture() {
            return new GuardrailAuditFixture();
        }

        @Bean
        AuditDomainService auditDomainService(GuardrailAuditFixture audits) {
            return audits.service;
        }

        @Bean
        SecretReferencePermissionDomainService secretReferencePermissionDomainService() {
            return mock(SecretReferencePermissionDomainService.class);
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
