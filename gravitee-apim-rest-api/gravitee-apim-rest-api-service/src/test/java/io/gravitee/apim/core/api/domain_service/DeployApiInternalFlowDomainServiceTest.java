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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.databind.JsonNode;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.EventType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * What the gateways are given when the Guardians of an API change: the deployment they are already running, with the
 * internal flow replaced and nothing else touched.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeployApiInternalFlowDomainServiceTest {

    private static final String API_ID = "api-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final AuditInfo CALLER = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("publisher").build())
        .build();

    private final EventCrudInMemory events = new EventCrudInMemory();
    private final EventLatestCrudInMemory latestEvents = new EventLatestCrudInMemory();
    private final EventLatestQueryServiceInMemory deployments = new EventLatestQueryServiceInMemory();
    private final DeployApiInternalFlowDomainService service = new DeployApiInternalFlowDomainService(
        events,
        latestEvents,
        deployments,
        GraviteeJacksonMapper.getInstance()
    );

    @BeforeEach
    void setUp() {
        events.reset();
        latestEvents.reset();
        deployments.reset();
    }

    @Nested
    class WhatIsSent {

        /**
         * The whole point: a publisher who turned a Guardian on did not press deploy, so everything they saved
         * without deploying — here a plan and a flow — must stay where it is. The payload therefore comes from the
         * event the gateways were given, patched, and never from the definition stored on the API.
         */
        @Test
        void should_keep_everything_but_the_internal_flow_of_what_the_gateways_run() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            var patched = service.prepare(API_ID, guardians("guardian-2")).orElseThrow();

            assertSoftly(softly -> {
                softly
                    .assertThat(definitionOf(patched.payload()))
                    .as("the definition sent, around its internal flow")
                    .usingRecursiveComparison()
                    .ignoringFields("internalFlow")
                    .isEqualTo(deployedDefinition(guardians("guardian-1")));
                softly
                    .assertThat(definitionOf(patched.payload()).getInternalFlow())
                    .as("internal flow sent")
                    .isEqualTo(guardians("guardian-2"));
                softly
                    .assertThat(definitionOf(patched.payload()).getPlans())
                    .as("plans sent: those of the deployment, not those saved since")
                    .extracting(Plan::getId)
                    .containsExactly("plan-deployed");
            });
        }

        @Test
        void should_send_no_internal_flow_at_all_when_every_guardian_is_turned_off() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            var patched = service.prepare(API_ID, null).orElseThrow();

            assertThat(definitionOf(patched.payload()).getInternalFlow()).as("internal flow sent").isNull();
        }

        @Test
        void should_keep_the_payload_around_the_definition_as_it_is() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            var patched = service.prepare(API_ID, guardians("guardian-2")).orElseThrow();

            assertSoftly(softly -> {
                softly.assertThat(patched.payload().get("id").asText()).as("api id of the payload").isEqualTo(API_ID);
                softly.assertThat(patched.payload().get("name").asText()).as("api name of the payload").isEqualTo("Support assistant");
            });
        }
    }

    @Nested
    class WhenNothingRunsIt {

        /** E1: nothing runs on the gateways, so there is nothing to apply. The API stays out of sync. */
        @Test
        void should_send_nothing_for_an_api_that_was_never_deployed() {
            assertThat(service.prepare(API_ID, guardians("guardian-1"))).as("deployment to send").isEmpty();
        }

        /**
         * E2: {@code events_latest} holds one row per API, so a stopped API's row is the {@code STOP_API}. Sending a
         * {@code PUBLISH_API} here would restart an API somebody stopped.
         */
        @Test
        void should_send_nothing_for_a_stopped_api() {
            givenDeployed(EventType.STOP_API, deployedDefinition(guardians("guardian-1")));

            assertThat(service.prepare(API_ID, guardians("guardian-2"))).as("deployment to send").isEmpty();
        }

        @Test
        void should_send_nothing_for_an_unpublished_api() {
            givenDeployed(EventType.UNPUBLISH_API, deployedDefinition(guardians("guardian-1")));

            assertThat(service.prepare(API_ID, guardians("guardian-2"))).as("deployment to send").isEmpty();
        }
    }

    @Nested
    class WhenItCannotBeRead {

        @Test
        void should_fail_on_a_payload_that_is_not_a_deployment() {
            givenDeployedPayload(EventType.PUBLISH_API, "not json at all");

            assertThatThrownBy(() -> service.prepare(API_ID, guardians("guardian-1")))
                .as("a payload that cannot be read")
                .isInstanceOf(TechnicalDomainException.class);
        }

        @Test
        void should_fail_on_a_deployment_that_carries_no_definition() {
            givenDeployedPayload(EventType.PUBLISH_API, "{\"id\":\"api-id\"}");

            assertThatThrownBy(() -> service.prepare(API_ID, guardians("guardian-1")))
                .as("a deployment without a definition")
                .isInstanceOf(TechnicalDomainException.class);
        }

        @Test
        void should_fail_on_a_definition_that_cannot_be_read() {
            givenDeployedPayload(EventType.PUBLISH_API, "{\"id\":\"api-id\",\"definition\":\"not json either\"}");

            assertThatThrownBy(() -> service.prepare(API_ID, guardians("guardian-1")))
                .as("a definition that cannot be read")
                .isInstanceOf(TechnicalDomainException.class);
        }

        @Test
        void should_send_nothing_for_a_deployment_with_an_empty_payload() {
            givenDeployedPayload(EventType.PUBLISH_API, "");

            assertThat(service.prepare(API_ID, guardians("guardian-1"))).as("deployment to send").isEmpty();
        }
    }

    @Nested
    class HowItIsSent {

        @Test
        void should_deploy_an_event_of_the_type_the_api_was_deployed_with() {
            givenDeployed(EventType.START_API, deployedDefinition(guardians("guardian-1")));

            service.deploy(CALLER, service.prepare(API_ID, guardians("guardian-2")).orElseThrow());

            assertThat(events.storage())
                .as("events written")
                .singleElement()
                .satisfies(event -> assertThat(event.getType()).as("event type").isEqualTo(EventType.START_API));
        }

        @Test
        void should_deploy_it_as_the_caller_on_the_deployment_it_patches() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")), "7");

            service.deploy(CALLER, service.prepare(API_ID, guardians("guardian-2")).orElseThrow());

            assertThat(events.storage())
                .as("events written")
                .singleElement()
                .satisfies(event ->
                    assertSoftly(softly -> {
                        softly
                            .assertThat(event.getProperties())
                            .as("event properties")
                            .containsEntry(Event.EventProperties.USER, "publisher")
                            .containsEntry(Event.EventProperties.API_ID, API_ID)
                            .containsEntry(Event.EventProperties.DEPLOYMENT_NUMBER, "7");
                        softly.assertThat(event.getEnvironments()).as("environments").containsExactly(ENVIRONMENT_ID);
                    })
                );
        }

        @Test
        void should_deploy_it_without_a_deployment_number_the_previous_one_did_not_carry() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            service.deploy(CALLER, service.prepare(API_ID, guardians("guardian-2")).orElseThrow());

            assertThat(events.storage())
                .as("events written")
                .singleElement()
                .satisfies(event ->
                    assertThat(event.getProperties()).as("event properties").containsEntry(Event.EventProperties.DEPLOYMENT_NUMBER, "0")
                );
        }

        /** The gateways read one row per API: the republished event replaces it rather than adding one. */
        @Test
        void should_keep_one_latest_event_per_api() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));
            latestEvents.initWith(List.of(Event.builder().id(API_ID).type(EventType.PUBLISH_API).build()));

            service.deploy(CALLER, service.prepare(API_ID, guardians("guardian-2")).orElseThrow());

            assertThat(latestEvents.storage())
                .as("latest events")
                .singleElement()
                .satisfies(event -> assertThat(event.getId()).as("latest event id").isEqualTo(API_ID));
        }

        @Test
        void should_deploy_the_patched_payload_and_not_the_one_it_read() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            service.deploy(CALLER, service.prepare(API_ID, guardians("guardian-2")).orElseThrow());

            assertThat(events.storage())
                .as("events written")
                .singleElement()
                .satisfies(event ->
                    assertThat(event.getPayload()).as("payload written").contains("guardian-2").doesNotContain("guardian-1")
                );
        }
    }

    private void givenDeployed(EventType type, io.gravitee.definition.model.v4.Api definition) {
        givenDeployed(type, definition, null);
    }

    @SneakyThrows
    private void givenDeployed(EventType type, io.gravitee.definition.model.v4.Api definition, String deploymentNumber) {
        var payload = io.gravitee.repository.management.model.Api.builder()
            .id(API_ID)
            .name("Support assistant")
            .environmentId(ENVIRONMENT_ID)
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.LLM_PROXY)
            .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(definition))
            .build();
        givenDeployedPayload(type, GraviteeJacksonMapper.getInstance().writeValueAsString(payload), deploymentNumber);
    }

    private void givenDeployedPayload(EventType type, String payload) {
        givenDeployedPayload(type, payload, null);
    }

    private void givenDeployedPayload(EventType type, String payload, String deploymentNumber) {
        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.API_ID, API_ID);
        if (deploymentNumber != null) {
            properties.put(Event.EventProperties.DEPLOYMENT_NUMBER, deploymentNumber);
        }
        deployments.initWith(
            List.of(
                Event.builder()
                    .id("event-id")
                    .type(type)
                    .payload(payload)
                    .properties(properties)
                    .environments(Set.of(ENVIRONMENT_ID))
                    .build()
            )
        );
    }

    /** A deployment holds the plans and the flows the gateways run, which a Guardian change must not touch. */
    private static io.gravitee.definition.model.v4.Api deployedDefinition(Flow internalFlow) {
        return io.gravitee.definition.model.v4.Api.builder()
            .id(API_ID)
            .name("Support assistant")
            .apiVersion("1.0")
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.LLM_PROXY)
            .internalFlow(internalFlow)
            .flows(List.of(Flow.builder().name("flow-deployed").build()))
            .plans(
                Map.of(
                    "plan-deployed",
                    Plan.builder()
                        .id("plan-deployed")
                        .name("Deployed plan")
                        .mode(PlanMode.STANDARD)
                        .status(PlanStatus.PUBLISHED)
                        .security(PlanSecurity.builder().type("key-less").build())
                        .build()
                )
            )
            .build();
    }

    private static Flow guardians(String... guardianIds) {
        return Flow.builder()
            .request(
                List.of(guardianIds)
                    .stream()
                    .map(id -> GuardianSteps.of(id, id))
                    .toList()
            )
            .build();
    }

    @SneakyThrows
    private static io.gravitee.definition.model.v4.Api definitionOf(JsonNode payload) {
        return GraviteeJacksonMapper.getInstance().readValue(payload.get("definition").asText(), io.gravitee.definition.model.v4.Api.class);
    }
}
