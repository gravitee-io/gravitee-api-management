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
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.guardian.GuardianFixtures;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resources.guardian.GuardiansResourceTest.GuardiansTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
    }

    @AfterEach
    void resetRepository() {
        guardianRepository.reset();
    }

    private static Guardian guardian(String id, String environmentId, String name) {
        return GuardianFixtures.aGuardian(d -> d.id(id).environmentId(environmentId).name(name)).build();
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
        void should_refuse_a_missing_update_body_with_400() {
            assertThat(put("null").getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
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

        /**
         * Stored in another zone, written in UTC: otherwise the offset would depend on the server's time
         * zone and two nodes would describe the same instant differently.
         */
        @Test
        void should_write_an_instant_from_another_zone_in_utc() {
            guardianRepository.reset();
            guardianRepository.givenGuardian(
                GuardianFixtures.aGuardian(d -> d.id(GUARDIAN_ID).environmentId(ENVIRONMENT))
                    .createdAt(ZonedDateTime.parse("2026-02-01T21:22:02+01:00"))
                    .build()
            );

            JsonNode dto = rootTarget("/" + GUARDIAN_ID).request().get().readEntity(JsonNode.class);

            // the same instant as 20:22:02Z
            assertThat(dto.get("createdAt").asText()).isEqualTo(CREATED_AT);
        }

        /** The deployment stamp comes off the store as a {@code Date}, down a different path. */
        @Test
        void should_write_the_deployment_stamp_as_an_iso_8601_string_in_utc() {
            guardianRepository.reset();
            guardianRepository.givenGuardian(
                GuardianFixtures.aGuardian(d -> d.id(GUARDIAN_ID).environmentId(ENVIRONMENT))
                    .deployedAt(Date.from(Instant.parse("2026-02-03T20:22:02Z")))
                    .build()
            );

            JsonNode dto = rootTarget("/" + GUARDIAN_ID).request().get().readEntity(JsonNode.class);

            assertThat(dto.get("deployedAt").isTextual()).isTrue();
            assertThat(dto.get("deployedAt").asText()).isEqualTo("2026-02-03T20:22:02Z");
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
        void should_return_403_on_delete_without_the_delete_permission() {
            assertThat(rootTarget("/" + GUARDIAN_ID).request().delete().getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }
    }

    /**
     * Mirrors the production wiring of {@code GammaGuardiansConfiguration}: the use cases come from the
     * same scan rather than being declared one by one, so this test keeps exercising whatever the real
     * context would build. Only the two ports are substituted — production derives the repository from
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
        AuditDomainService auditDomainService() {
            // the domain writes its own audit log now; the trail itself is asserted at the use-case level
            return org.mockito.Mockito.mock(AuditDomainService.class);
        }
    }
}
