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
import com.fasterxml.jackson.databind.node.ObjectNode;
import inmemory.ApiDefinitionCrudServiceInMemory;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.EventType;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
 * internal flow replaced and nothing else touched. And nothing at all when the definition was not written.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeployApiInternalFlowDomainServiceTest {

    private static final String API_ID = "api-id";
    private static final String UNKNOWN_FIELD = "aFieldTheModelNoLongerDeclares";
    private static final String UNKNOWN_FIELD_OF_A_DEFINITION = "aDefinitionFieldTheModelNoLongerDeclares";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final ZonedDateTime STORED_AT = ZonedDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final String LABEL = "Guardians updated";
    private static final AuditInfo CALLER = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("publisher").build())
        .build();

    private final EventCrudInMemory events = new EventCrudInMemory();
    private final EventLatestCrudInMemory latestEvents = new EventLatestCrudInMemory();
    private final EventLatestQueryServiceInMemory deployments = new EventLatestQueryServiceInMemory();
    private final ApiDefinitionCrudServiceInMemory apis = new ApiDefinitionCrudServiceInMemory();
    private final DeployApiInternalFlowDomainService service = new DeployApiInternalFlowDomainService(
        apis,
        events,
        latestEvents,
        deployments
    );

    @BeforeEach
    void setUp() {
        events.reset();
        latestEvents.reset();
        deployments.reset();
        apis.reset();
        givenTheStoredApiRuns(guardians("guardian-1"));
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

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertSoftly(softly -> {
                softly
                    .assertThat(definitionSent())
                    .as("the definition sent, around its internal flow")
                    .usingRecursiveComparison()
                    .ignoringFields("internalFlow")
                    .isEqualTo(deployedDefinition(guardians("guardian-1")));
                softly.assertThat(definitionSent().getInternalFlow()).as("internal flow sent").isEqualTo(guardians("guardian-2"));
                softly
                    .assertThat(definitionSent().getPlans())
                    .as("plans sent: those of the deployment, not those saved since")
                    .extracting(Plan::getId)
                    .containsExactly("plan-deployed");
            });
        }

        @Test
        void should_send_no_internal_flow_at_all_when_every_guardian_is_turned_off() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            service.replaceInternalFlow(CALLER, apiRunning(null), STORED_AT, LABEL);

            assertThat(definitionSent().getInternalFlow()).as("internal flow sent").isNull();
        }

        /** A V2 API holds no internal flow, so the one the gateways run is taken away rather than replaced. */
        @Test
        void should_send_no_internal_flow_at_all_for_an_api_whose_definition_cannot_hold_one() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));
            var v2Api = io.gravitee.apim.core.api.model.Api.builder()
                .id(API_ID)
                .updatedAt(STORED_AT)
                .apiDefinitionValue(io.gravitee.definition.model.Api.builder().id(API_ID).build())
                .build();

            service.replaceInternalFlow(CALLER, v2Api, STORED_AT, LABEL);

            assertThat(definitionSent().getInternalFlow()).as("internal flow sent").isNull();
        }

        /**
         * The payload is re-expressed in the current model, so what it keeps is not "everything": it is the fields
         * the gateways read of a deployment, plus the API it names.
         */
        @Test
        void should_keep_the_payload_fields_the_gateways_read() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertSoftly(softly -> {
                softly.assertThat(payloadSent().get("id").asText()).as("api id of the payload").isEqualTo(API_ID);
                softly.assertThat(payloadSent().get("name").asText()).as("api name of the payload").isEqualTo("Support assistant");
                softly
                    .assertThat(payloadSent().get("definitionVersion").asText())
                    .as("definition version of the payload, which tells the gateways how to read the definition")
                    .isEqualTo(DefinitionVersion.V4.getLabel());
                softly
                    .assertThat(payloadSent().get("type").asText())
                    .as("api type of the payload, which the gateways reject the event without")
                    .isEqualTo(ApiType.LLM_PROXY.getLabel());
                softly
                    .assertThat(payloadSent().get("environmentId").asText())
                    .as("environment id of the payload, which the gateways drop the event without")
                    .isEqualTo(ENVIRONMENT_ID);
            });
        }

        /**
         * An API migrated to V4 and never redeployed still has a V2 payload on the gateways, and a V2 definition
         * holds no internal flow: it goes back as it was deployed rather than losing its definition on the way.
         */
        @Test
        void should_republish_a_v2_deployment_unchanged() {
            var deployment = v2DeploymentPayload();
            givenDeployedPayload(EventType.PUBLISH_API, deployment);

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertSoftly(softly -> {
                softly
                    .assertThat(payloadSent().get("definitionVersion").asText())
                    .as("definition version of the payload, republished as it was deployed")
                    .isEqualTo(DefinitionVersion.V2.getLabel());
                softly
                    .assertThat(v2DefinitionSent())
                    .as("the V2 definition sent, which the gateways run")
                    .usingRecursiveComparison()
                    .isEqualTo(v2DefinitionOf(deployment));
            });
        }

        /**
         * The deployment is re-expressed in the current model, so a field an older rest-api wrote and the model no
         * longer declares is dropped rather than carried over. Accepted, and decided here rather than discovered:
         * pressing deploy rebuilds the payload from the current model too, so refusing would make a Guardian change
         * stricter than a deployment.
         */
        @Test
        void should_drop_a_field_of_the_deployment_the_current_model_no_longer_declares() {
            givenDeployedPayload(EventType.PUBLISH_API, deploymentCarryingAnUnknownField());

            var written = service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertSoftly(softly -> {
                softly.assertThat(written).as("definition written").isTrue();
                softly.assertThat(payloadSent().has(UNKNOWN_FIELD)).as("the unknown field of the payload, republished").isFalse();
                softly
                    .assertThat(definitionSentAsWritten().has(UNKNOWN_FIELD_OF_A_DEFINITION))
                    .as("the unknown field inside the definition, which is what the gateways run, republished")
                    .isFalse();
                softly.assertThat(definitionSent().getInternalFlow()).as("internal flow sent").isEqualTo(guardians("guardian-2"));
            });
        }
    }

    @Nested
    class WhenNothingRunsIt {

        /** E1: nothing runs on the gateways, so there is nothing to apply. The definition is still written. */
        @Test
        void should_send_nothing_for_an_api_that_was_never_deployed() {
            var written = service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertSoftly(softly -> {
                softly.assertThat(written).as("definition written").isTrue();
                softly.assertThat(events.storage()).as("events written").isEmpty();
                softly.assertThat(internalFlowStored()).as("internal flow stored").isEqualTo(guardians("guardian-2"));
            });
        }

        /**
         * E2: {@code events_latest} holds one row per API, so a stopped API's row is the {@code STOP_API}. Sending a
         * {@code PUBLISH_API} here would restart an API somebody stopped.
         */
        @Test
        void should_send_nothing_for_a_stopped_api() {
            givenDeployed(EventType.STOP_API, deployedDefinition(guardians("guardian-1")));

            var written = service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertSoftly(softly -> {
                softly.assertThat(written).as("definition written").isTrue();
                softly.assertThat(events.storage()).as("events written").isEmpty();
            });
        }

        @Test
        void should_send_nothing_for_an_unpublished_api() {
            givenDeployed(EventType.UNPUBLISH_API, deployedDefinition(guardians("guardian-1")));

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertThat(events.storage()).as("events written").isEmpty();
        }

        /**
         * The deployment is read for the environment of the caller: a deployment of an API of the same id in another
         * environment is another tenant's, and republishing it would hand it to the gateways of this one.
         */
        @Test
        void should_send_nothing_for_a_deployment_of_another_environment() {
            givenDeployedPayloadInEnvironment(
                "another-environment",
                "event-id",
                EventType.PUBLISH_API,
                deploymentPayload(deployedDefinition(guardians("guardian-1"))),
                null
            );

            var written = service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertSoftly(softly -> {
                softly.assertThat(written).as("definition written").isTrue();
                softly.assertThat(events.storage()).as("events written").isEmpty();
            });
        }
    }

    /**
     * The deployment is patched before the definition is written, so a payload that cannot be read leaves the API as
     * it was instead of half changed.
     */
    @Nested
    class WhenItCannotBeRead {

        @Test
        void should_fail_on_a_payload_that_is_not_a_deployment() {
            givenDeployedPayload(EventType.PUBLISH_API, "not json at all");

            assertThatThrownBy(() -> service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL))
                .as("a payload that cannot be read")
                .isInstanceOf(TechnicalDomainException.class);
            assertThat(internalFlowStored()).as("internal flow stored").isEqualTo(guardians("guardian-1"));
        }

        @Test
        void should_fail_on_a_deployment_that_carries_no_definition() {
            givenDeployedPayload(EventType.PUBLISH_API, "{\"id\":\"api-id\"}");

            assertThatThrownBy(() -> service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL))
                .as("a deployment without a definition")
                .isInstanceOf(TechnicalDomainException.class);
            assertThat(internalFlowStored()).as("internal flow stored").isEqualTo(guardians("guardian-1"));
        }

        @Test
        void should_fail_on_a_definition_that_cannot_be_read() {
            givenDeployedPayload(EventType.PUBLISH_API, "{\"id\":\"api-id\",\"definition\":\"not json either\"}");

            assertThatThrownBy(() -> service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL))
                .as("a definition that cannot be read")
                .isInstanceOf(TechnicalDomainException.class);
            assertThat(internalFlowStored()).as("internal flow stored").isEqualTo(guardians("guardian-1"));
        }

        @Test
        void should_send_nothing_for_a_deployment_with_an_empty_payload() {
            givenDeployedPayload(EventType.PUBLISH_API, "");

            var written = service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertSoftly(softly -> {
                softly.assertThat(written).as("definition written").isTrue();
                softly.assertThat(events.storage()).as("events written").isEmpty();
            });
        }
    }

    /**
     * The write is a compare-and-swap, and a write that does not happen sends nothing: the gateways never run an
     * internal flow the store does not hold.
     */
    @Nested
    class WhenTheWriteDoesNotHappen {

        @Test
        void should_send_nothing_when_another_writer_moved_the_api() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            var written = service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT.minusMinutes(1), LABEL);

            assertSoftly(softly -> {
                softly.assertThat(written).as("definition written").isFalse();
                softly.assertThat(events.storage()).as("events written").isEmpty();
                softly.assertThat(latestEvents.storage()).as("latest events written").isEmpty();
                softly.assertThat(internalFlowStored()).as("internal flow stored").isEqualTo(guardians("guardian-1"));
            });
        }

        /**
         * A caller may hand over the very date it expects — a rollback writes the API, then its internal flow, in the
         * same millisecond. The write must still move the date, or a writer that read the API in between would pass
         * its own compare-and-swap and overwrite this one.
         */
        @Test
        void should_refuse_a_later_write_conditioned_on_the_date_this_one_expected() {
            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            var overwritten = service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-3")), STORED_AT, LABEL);

            assertSoftly(softly -> {
                softly.assertThat(overwritten).as("write conditioned on the date the first one expected").isFalse();
                softly.assertThat(internalFlowStored()).as("internal flow stored").isEqualTo(guardians("guardian-2"));
            });
        }

        @Test
        void should_send_nothing_for_an_api_that_is_gone() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));
            apis.reset();

            var written = service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertSoftly(softly -> {
                softly.assertThat(written).as("definition written").isFalse();
                softly.assertThat(events.storage()).as("events written").isEmpty();
            });
        }
    }

    /**
     * The window between the definition write and the event write. The compare-and-swap does not cover it: a
     * deployment the publisher makes in it is built from the store, and writing the deployment read before it would
     * send the gateways back to the plans and the flows they ran before.
     */
    @Nested
    class WhenTheApiIsDeployedInBetween {

        @Test
        void should_patch_the_newer_deployment_rather_than_overwrite_it() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            serviceDeployingWhileWriting(() ->
                givenDeployed("deployed-since", EventType.PUBLISH_API, deployedDefinition(null, "plan-of-the-deployment-made-since"))
            ).replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertSoftly(softly -> {
                softly
                    .assertThat(definitionSent().getPlans())
                    .as("plans sent: those of the deployment made in between")
                    .extracting(Plan::getId)
                    .containsExactly("plan-of-the-deployment-made-since");
                softly.assertThat(definitionSent().getInternalFlow()).as("internal flow sent").isEqualTo(guardians("guardian-2"));
            });
        }

        @Test
        void should_send_nothing_when_the_api_was_stopped_in_between() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            serviceDeployingWhileWriting(() ->
                givenDeployed("stopped-since", EventType.STOP_API, deployedDefinition(null))
            ).replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertThat(events.storage()).as("events written, a stopped API being restarted by none").isEmpty();
        }

        /** The same service, over a store that lets {@code deployment} happen as the definition is written. */
        private DeployApiInternalFlowDomainService serviceDeployingWhileWriting(Runnable deployment) {
            var racing = new ApiDefinitionCrudServiceInMemory() {
                @Override
                public boolean replaceDefinition(io.gravitee.apim.core.api.model.Api api, ZonedDateTime expectedUpdatedAt) {
                    var written = super.replaceDefinition(api, expectedUpdatedAt);
                    deployment.run();
                    return written;
                }
            };
            racing.initWith(List.of(apiRunning(guardians("guardian-1"))));
            return new DeployApiInternalFlowDomainService(racing, events, latestEvents, deployments);
        }
    }

    @Nested
    class HowItIsSent {

        @Test
        void should_deploy_an_event_of_the_type_the_api_was_deployed_with() {
            givenDeployed(EventType.START_API, deployedDefinition(guardians("guardian-1")));

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertThat(events.storage())
                .as("events written")
                .singleElement()
                .satisfies(event -> assertThat(event.getType()).as("event type").isEqualTo(EventType.START_API));
        }

        /** A caller the security context does not name as a user — a technical call — still has its change sent. */
        @Test
        void should_deploy_it_without_a_user_for_a_caller_that_is_none() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));
            var anonymous = AuditInfo.builder()
                .organizationId(ORGANIZATION_ID)
                .environmentId(ENVIRONMENT_ID)
                .actor(AuditActor.builder().build())
                .build();

            service.replaceInternalFlow(anonymous, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertThat(events.storage())
                .as("events written")
                .singleElement()
                .satisfies(event -> assertThat(event.getProperties()).as("event properties").doesNotContainKey(Event.EventProperties.USER));
        }

        @Test
        void should_deploy_it_as_the_caller() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertThat(events.storage())
                .as("events written")
                .singleElement()
                .satisfies(event ->
                    assertSoftly(softly -> {
                        softly
                            .assertThat(event.getProperties())
                            .as("event properties")
                            .containsEntry(Event.EventProperties.USER, "publisher")
                            .containsEntry(Event.EventProperties.API_ID, API_ID);
                        softly.assertThat(event.getEnvironments()).as("environments").containsExactly(ENVIRONMENT_ID);
                    })
                );
        }

        /**
         * The republished event is an entry of its own in the API's deployment history, under a number of its own,
         * and it can be rolled back to. The label is what tells it apart from a deployment the publisher made.
         */
        @Test
        void should_label_it_as_a_guardian_change_rather_than_a_deployment() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")), "7");

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertThat(events.storage())
                .as("events written")
                .singleElement()
                .satisfies(event ->
                    assertThat(event.getProperties()).as("event properties").containsEntry(Event.EventProperties.DEPLOYMENT_LABEL, LABEL)
                );
        }

        /** The gateways read one row per API: the republished event replaces it rather than adding one. */
        @Test
        void should_keep_one_latest_event_per_api() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));
            latestEvents.initWith(List.of(Event.builder().id(API_ID).type(EventType.PUBLISH_API).build()));

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertThat(latestEvents.storage())
                .as("latest events")
                .singleElement()
                .satisfies(event -> assertThat(event.getId()).as("latest event id").isEqualTo(API_ID));
        }

        /**
         * The two definitions are compared as typed objects and never as strings: the deployment was serialized by
         * the mapper of the event service and the republished one by {@code GraviteeJacksonMapper}, so field order
         * and null inclusion may differ with nothing lost.
         */
        @Test
        void should_deploy_the_patched_payload_and_not_the_one_it_read() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertThat(definitionSent().getInternalFlow()).as("internal flow sent").isEqualTo(guardians("guardian-2"));
        }
    }

    /**
     * A gateway applies a deployment of an API it already runs only when its number changed, so the republished event
     * takes the number the next deployment would have taken: under the number of the deployment it patches, every
     * gateway running that deployment would ignore it, and a Guardian would neither start nor stop.
     */
    @Nested
    class HowItIsNumbered {

        /**
         * The latest event is what the gateways read and what the next deployment numbers itself one past, so both
         * carry the new number: the next deployment then takes a greater one still.
         */
        @Test
        void should_number_it_past_the_deployment_it_patches() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")), "7");

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertSoftly(softly -> {
                softly.assertThat(deploymentNumbersOf(events.storage())).as("deployment number of the event written").containsExactly("8");
                softly
                    .assertThat(deploymentNumbersOf(latestEvents.storage()))
                    .as("deployment number of the latest event, which the next deployment numbers itself past")
                    .containsExactly("8");
            });
        }

        /** Republished as a {@code START_API}: the type of the deployment is kept, its number is not. */
        @Test
        void should_number_a_started_api_past_the_deployment_it_patches() {
            givenDeployed(EventType.START_API, deployedDefinition(guardians("guardian-1")), "7");

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertThat(deploymentNumbersOf(events.storage())).as("deployment number of the event written").containsExactly("8");
        }

        /** As a deployment does: a previous event carrying no number counts as number 0. */
        @Test
        void should_number_it_one_when_the_deployment_it_patches_carries_no_number() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")));

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-2")), STORED_AT, LABEL);

            assertThat(deploymentNumbersOf(events.storage())).as("deployment number of the event written").containsExactly("1");
        }

        /** A Guardian turned on, then off again: the second change reaches the gateways as the first one did. */
        @Test
        void should_number_each_change_past_the_previous_one() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")), "7");
            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-1", "guardian-2")), STORED_AT, LABEL);
            givenTheGatewaysRunTheLatestEvent();

            service.replaceInternalFlow(CALLER, apiRunning(guardians("guardian-1")), storedUpdatedAt(), LABEL);

            assertThat(deploymentNumbersOf(events.storage())).as("deployment numbers of the events written").containsExactly("8", "9");
        }

        @Test
        void should_number_the_clearing_of_the_internal_flow_past_the_deployment_it_patches() {
            givenDeployed(EventType.PUBLISH_API, deployedDefinition(guardians("guardian-1")), "7");

            service.clearInternalFlow(CALLER, apiRunning(guardians("guardian-1")), STORED_AT, LABEL);

            assertThat(deploymentNumbersOf(events.storage())).as("deployment number of the event written").containsExactly("8");
        }

        private void givenTheGatewaysRunTheLatestEvent() {
            var latest = List.copyOf(latestEvents.storage());
            deployments.reset();
            deployments.initWith(latest);
        }

        private ZonedDateTime storedUpdatedAt() {
            return apis.storage().getFirst().getUpdatedAt();
        }

        private static List<String> deploymentNumbersOf(List<Event> written) {
            return written
                .stream()
                .map(event -> event.getProperties().get(Event.EventProperties.DEPLOYMENT_NUMBER))
                .toList();
        }
    }

    private void givenTheStoredApiRuns(Flow internalFlow) {
        apis.initWith(List.of(apiRunning(internalFlow)));
    }

    /** The API as a caller hands it over: the definition to write, internal flow included, and the date to stamp. */
    private static io.gravitee.apim.core.api.model.Api apiRunning(Flow internalFlow) {
        return io.gravitee.apim.core.api.model.Api.builder()
            .id(API_ID)
            .updatedAt(STORED_AT)
            .apiDefinitionValue(deployedDefinition(internalFlow))
            .build();
    }

    private void givenDeployed(EventType type, io.gravitee.definition.model.v4.Api definition) {
        givenDeployed("event-id", type, definition, null);
    }

    private void givenDeployed(String eventId, EventType type, io.gravitee.definition.model.v4.Api definition) {
        givenDeployed(eventId, type, definition, null);
    }

    private void givenDeployed(EventType type, io.gravitee.definition.model.v4.Api definition, String deploymentNumber) {
        givenDeployed("event-id", type, definition, deploymentNumber);
    }

    private void givenDeployed(String eventId, EventType type, io.gravitee.definition.model.v4.Api definition, String deploymentNumber) {
        givenDeployedPayload(eventId, type, deploymentPayload(definition), deploymentNumber);
    }

    /** One unknown field at the root of the payload, one inside the definition — the same decision, two places. */
    @SneakyThrows
    private static String deploymentCarryingAnUnknownField() {
        var mapper = GraviteeJacksonMapper.getInstance();
        var payload = (ObjectNode) mapper.readTree(deploymentPayload(deployedDefinition(guardians("one"))));
        var definition = (ObjectNode) mapper.readTree(payload.get("definition").asText());
        definition.put(UNKNOWN_FIELD_OF_A_DEFINITION, "written by a rest-api of another version");
        payload.put("definition", definition.toString());
        return payload.put(UNKNOWN_FIELD, "written by a rest-api of another version").toString();
    }

    @SneakyThrows
    private static String v2DeploymentPayload() {
        return GraviteeJacksonMapper.getInstance().writeValueAsString(
            io.gravitee.repository.management.model.Api.builder()
                .id(API_ID)
                .name("Support assistant")
                .environmentId(ENVIRONMENT_ID)
                .definitionVersion(DefinitionVersion.V2)
                .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(deployedV2Definition()))
                .build()
        );
    }

    /** A V2 definition carries a proxy or its deserializer refuses it, so the minimal one is not empty. */
    private static io.gravitee.definition.model.Api deployedV2Definition() {
        return io.gravitee.definition.model.Api.builder()
            .id(API_ID)
            .name("Support assistant")
            .version("1.0")
            .definitionVersion(DefinitionVersion.V2)
            .proxy(Proxy.builder().virtualHosts(List.of(new VirtualHost("/support"))).build())
            .build();
    }

    @SneakyThrows
    private static String deploymentPayload(io.gravitee.definition.model.v4.Api definition) {
        return GraviteeJacksonMapper.getInstance().writeValueAsString(
            io.gravitee.repository.management.model.Api.builder()
                .id(API_ID)
                .name("Support assistant")
                .environmentId(ENVIRONMENT_ID)
                .definitionVersion(DefinitionVersion.V4)
                .type(ApiType.LLM_PROXY)
                .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(definition))
                .build()
        );
    }

    private void givenDeployedPayload(EventType type, String payload) {
        givenDeployedPayload(type, payload, null);
    }

    private void givenDeployedPayload(EventType type, String payload, String deploymentNumber) {
        givenDeployedPayload("event-id", type, payload, deploymentNumber);
    }

    private void givenDeployedPayload(String eventId, EventType type, String payload, String deploymentNumber) {
        givenDeployedPayloadInEnvironment(ENVIRONMENT_ID, eventId, type, payload, deploymentNumber);
    }

    /** {@code events_latest} holds one row per API, so what the gateways run is replaced rather than added to. */
    private void givenDeployedPayloadInEnvironment(
        String environmentId,
        String eventId,
        EventType type,
        String payload,
        String deploymentNumber
    ) {
        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.API_ID, API_ID);
        if (deploymentNumber != null) {
            properties.put(Event.EventProperties.DEPLOYMENT_NUMBER, deploymentNumber);
        }
        deployments.reset();
        deployments.initWith(
            List.of(
                Event.builder().id(eventId).type(type).payload(payload).properties(properties).environments(Set.of(environmentId)).build()
            )
        );
    }

    /** A deployment holds the plans and the flows the gateways run, which a Guardian change must not touch. */
    private static io.gravitee.definition.model.v4.Api deployedDefinition(Flow internalFlow) {
        return deployedDefinition(internalFlow, "plan-deployed");
    }

    private static io.gravitee.definition.model.v4.Api deployedDefinition(Flow internalFlow, String planId) {
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
                    planId,
                    Plan.builder()
                        .id(planId)
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

    private Flow internalFlowStored() {
        return apis.storage().getFirst().getApiDefinitionHttpV4().getInternalFlow();
    }

    @SneakyThrows
    private JsonNode payloadSent() {
        return GraviteeJacksonMapper.getInstance().readTree(events.storage().getFirst().getPayload());
    }

    @SneakyThrows
    private io.gravitee.definition.model.v4.Api definitionSent() {
        return GraviteeJacksonMapper.getInstance().readValue(
            payloadSent().get("definition").asText(),
            io.gravitee.definition.model.v4.Api.class
        );
    }

    /** The definition as the gateways receive it, so a field the model would swallow on a read is still visible. */
    @SneakyThrows
    private JsonNode definitionSentAsWritten() {
        return GraviteeJacksonMapper.getInstance().readTree(payloadSent().get("definition").asText());
    }

    private io.gravitee.definition.model.Api v2DefinitionSent() {
        return v2DefinitionOf(events.storage().getFirst().getPayload());
    }

    /** Both sides read with the same reader, so the defaults the V2 model fills in are not read as a loss. */
    @SneakyThrows
    private static io.gravitee.definition.model.Api v2DefinitionOf(String payload) {
        var mapper = GraviteeJacksonMapper.getInstance();
        return mapper.readValue(mapper.readTree(payload).get("definition").asText(), io.gravitee.definition.model.Api.class);
    }
}
