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
package io.gravitee.gamma.rest.core.guardrail.domain_service;

import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.GUARDRAIL_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailDefinition;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.databind.node.ObjectNode;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import inmemory.EventQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventPublicationDomainService;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventWriterDomainService;
import io.gravitee.apim.core.generic_event.model.GenericEvent;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.apim.infra.json.jackson.JacksonJsonSerializer;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardrail.Guardrail;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.guardrail.GuardrailPublicationFixture;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailDeployment;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailLifecycleState;
import io.gravitee.rest.api.model.EventType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardrailDeploymentDomainServiceTest {

    @Nested
    class EventStore {

        private final InMemoryGuardrailRepository guardrails = new InMemoryGuardrailRepository();
        private final EventCrudInMemory history = new EventCrudInMemory();
        private final EventLatestCrudInMemory latest = GuardrailPublicationFixture.latestEvents();
        private final EventLatestQueryServiceInMemory query = new EventLatestQueryServiceInMemory() {
            @Override
            public List<Event> findLatestByEntityIds(Set<String> ids, EventType type, Event.EventProperties key) {
                reset();
                initWith(latest.storage());
                return super.findLatestByEntityIds(ids, type, key);
            }
        };
        private final AuditInfo actor = AuditInfo.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .actor(AuditActor.builder().userId("deployer").build())
            .build();
        private GuardrailDeploymentDomainService service;

        @BeforeEach
        void set_up() {
            TimeProvider.overrideClock(Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC));
            var mapper = GraviteeJacksonMapper.getInstance();
            var writer = new GenericEventWriterDomainService(history, latest, mapper);
            var store = new GuardrailPublicationDomainService(
                new JacksonJsonSerializer(),
                new JacksonJsonDeserializer(),
                new GenericEventPublicationDomainService(writer, mapper),
                query,
                new EventQueryServiceInMemory(),
                new UserCrudServiceInMemory()
            );
            service = new GuardrailDeploymentDomainService(guardrails, store, new JacksonJsonSerializer(), new JacksonJsonDeserializer());
        }

        @AfterEach
        void reset_clock() {
            TimeProvider.overrideClock(Clock.systemDefaultZone());
        }

        @Test
        void should_publish_a_definition_the_gateway_can_read_with_its_properties_and_label() throws Exception {
            var definition = aGuardrailDefinition()
                .policies(
                    List.of(
                        Step.builder()
                            .name("Detect")
                            .policy("guardrail-rule")
                            .configuration("{\"scope\":{\"protocol\":\"LLM\"},\"patterns\":[\"a\",\"b\"]}")
                            .build()
                    )
                )
                .resources(
                    List.of(
                        Resource.builder()
                            .name("test-resource")
                            .type("test-resource")
                            .configuration("{\"nested\":{\"enabled\":true}}")
                            .build()
                    )
                )
                .build();
            var guardrail = aGuardrail().definition(definition).build();
            guardrails.givenGuardrail(guardrail);

            var deployed = service.publish(guardrail, "Reviewed", actor);
            var event = latest.storage().getFirst();
            var envelope = GraviteeJacksonMapper.getInstance().readTree(event.getPayload());
            var reloaded = GraviteeJacksonMapper.getInstance().treeToValue(envelope.get("payload"), Guardrail.class);

            assertSoftly(softly -> {
                softly.assertThat(event.getType()).isEqualTo(EventType.PUBLISH_GENERIC_EVENT);
                softly
                    .assertThat(event.getProperties())
                    .containsEntry(Event.EventProperties.GENERIC_EVENT_KIND, "guardrail.definition")
                    .containsEntry(
                        Event.EventProperties.GENERIC_EVENT_ID,
                        GenericEvent.latestEventId(ENVIRONMENT_ID, "guardrail.definition", GUARDRAIL_ID)
                    )
                    .containsEntry(Event.EventProperties.USER, "deployer")
                    .containsEntry(Event.EventProperties.DEPLOYMENT_LABEL, "Reviewed");
                softly.assertThat(deployed.revisionId()).as("revision of the deployment").isEqualTo(history.storage().getFirst().getId());
                softly.assertThat(reloaded).as("gateway payload round trip").isEqualTo(definition);
                softly.assertThat(deployed.at()).isEqualTo(Instant.parse(envelope.get("updatedAt").asText()));
                softly.assertThat(history.storage()).as("first deployment is historized").hasSize(1);
            });
        }

        @Test
        void should_keep_a_draft_saved_after_the_deployed_one_was_read() {
            var read = aGuardrail().build();
            var savedMeanwhile = read
                .toBuilder()
                .definition(read.getDefinition().toBuilder().name("Saved meanwhile").build())
                .description("Saved meanwhile")
                .build();
            guardrails.givenGuardrail(savedMeanwhile);

            service.complete(read, service.publish(read, null, actor));

            assertThat(guardrails.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID))
                .get()
                .satisfies(stored ->
                    assertSoftly(softly -> {
                        softly.assertThat(stored.getName()).as("draft definition").isEqualTo("Saved meanwhile");
                        softly.assertThat(stored.getDescription()).as("draft description").isEqualTo("Saved meanwhile");
                        softly
                            .assertThat(stored.getUpdatedAt())
                            .as("advanced to the deployment")
                            .isEqualTo(Instant.parse("2026-11-09T10:00:00Z"));
                    })
                );
        }

        @Test
        void should_reload_the_publication_scoped_to_the_guardrail_environment() {
            var guardrail = aGuardrail().build();
            guardrails.givenGuardrail(guardrail);

            service.publish(guardrail, null, actor);

            assertThat(service.current(guardrail))
                .asInstanceOf(type(GuardrailDeployment.Published.class))
                .extracting(GuardrailDeployment.Published::definition)
                .isEqualTo(guardrail.getDefinition());
            assertThat(service.current(aGuardrail(definition -> definition.environmentId("another-environment")).build())).isNull();
        }

        @Test
        void should_withdraw_the_same_guardrail_and_record_the_withdrawal() {
            var guardrail = aGuardrail().build();
            guardrails.givenGuardrail(guardrail);
            service.publish(guardrail, null, actor);
            var publishedId = latest.storage().getFirst().getProperties().get(Event.EventProperties.GENERIC_EVENT_ID);

            var withdrawal = service.withdraw(
                aGuardrail().build(),
                new GuardrailDeployment.Published(aGuardrailDefinition().build(), Instant.parse("2026-11-08T10:00:00Z"), "revision-id"),
                actor
            );
            var recovered = service.current(guardrail);

            assertSoftly(softly -> {
                softly
                    .assertThat(latest.storage())
                    .singleElement()
                    .satisfies(event -> {
                        assertThat(event.getType()).isEqualTo(EventType.UNPUBLISH_GENERIC_EVENT);
                        assertThat(event.getProperties()).containsEntry(Event.EventProperties.GENERIC_EVENT_ID, publishedId);
                    });
                softly.assertThat(recovered).as("withdrawn").isInstanceOf(GuardrailDeployment.Withdrawn.class);
                softly.assertThat(history.storage()).hasSize(2);
                softly.assertThat(recovered).as("durable withdrawal round trip").isEqualTo(withdrawal);
            });
        }

        @Test
        void should_publish_only_the_deployment_date_with_a_withdrawal() throws Exception {
            service.withdraw(
                aGuardrail().build(),
                new GuardrailDeployment.Published(aGuardrailDefinition().build(), Instant.parse("2026-11-08T10:00:00Z"), "revision-id"),
                actor
            );
            var envelope = GraviteeJacksonMapper.getInstance().readTree(latest.storage().getFirst().getPayload());

            assertThat(envelope.path("payload").properties())
                .as("withdrawal payload fields, the date being the envelope's")
                .extracting(Map.Entry::getKey)
                .containsExactly("deployedAt");
        }

        @ParameterizedTest
        @ValueSource(strings = { "id", "environmentId" })
        void should_refuse_a_withdrawal_of_another_guardrail(String identity) throws Exception {
            var guardrail = aGuardrail().build();
            service.withdraw(
                guardrail,
                new GuardrailDeployment.Published(guardrail.getDefinition(), Instant.parse("2026-11-08T10:00:00Z"), "revision-id"),
                actor
            );
            var event = latest.storage().getFirst();
            var mapper = GraviteeJacksonMapper.getInstance();
            var envelope = (ObjectNode) mapper.readTree(event.getPayload());
            envelope.put(identity, "another-guardrail");
            event.setPayload(mapper.writeValueAsString(envelope));

            assertThatThrownBy(() -> service.current(guardrail))
                .as("withdrawal remains scoped to its event")
                .isInstanceOf(TechnicalDomainException.class);
        }

        @Test
        void should_read_a_withdrawal_whose_payload_carries_other_fields() throws Exception {
            var guardrail = aGuardrail().build();
            service.withdraw(
                guardrail,
                new GuardrailDeployment.Published(guardrail.getDefinition(), Instant.parse("2026-11-08T10:00:00Z"), "revision-id"),
                actor
            );
            var event = latest.storage().getFirst();
            var mapper = GraviteeJacksonMapper.getInstance();
            var envelope = (ObjectNode) mapper.readTree(event.getPayload());
            envelope.set("payload", mapper.readTree("{\"guardrail\":{\"description\":\"An earlier withdrawal\"}}"));
            event.setPayload(mapper.writeValueAsString(envelope));

            assertThat(service.current(guardrail))
                .as("withdrawal written with an earlier payload")
                .asInstanceOf(type(GuardrailDeployment.Withdrawn.class))
                .extracting(GuardrailDeployment.Withdrawn::deployedAt)
                .isNull();
        }

        @Test
        void should_read_a_withdrawal_without_payload_as_removed() throws Exception {
            var guardrail = aGuardrail().build();
            service.withdraw(
                guardrail,
                new GuardrailDeployment.Published(guardrail.getDefinition(), Instant.parse("2026-11-08T10:00:00Z"), "revision-id"),
                actor
            );
            var event = latest.storage().getFirst();
            var mapper = GraviteeJacksonMapper.getInstance();
            var envelope = (ObjectNode) mapper.readTree(event.getPayload());
            envelope.remove("payload");
            event.setPayload(mapper.writeValueAsString(envelope));

            assertThat(service.current(guardrail))
                .as("withdrawal without deployment date")
                .asInstanceOf(type(GuardrailDeployment.Withdrawn.class))
                .extracting(GuardrailDeployment.Withdrawn::deployedAt)
                .isNull();
        }
    }

    @Nested
    class LifecycleState {

        private final Guardrail publishedWithoutLists = aGuardrailDefinition().policies(null).resources(null).apiTypes(null).build();
        private final GuardrailDeploymentDomainService service = new GuardrailDeploymentDomainService(
            new InMemoryGuardrailRepository(),
            null,
            new JacksonJsonSerializer(),
            new JacksonJsonDeserializer()
        );

        private GuardrailLifecycleState stateOf(Guardrail draft, Guardrail published) {
            return service
                .state(
                    aGuardrail().definition(draft).build(),
                    new GuardrailDeployment.Published(published, Instant.parse("2026-11-08T10:00:00Z"), "revision-id")
                )
                .getLifecycleState();
        }

        @Test
        void should_show_a_draft_saving_empty_lists_as_deployed_when_the_revision_has_none() {
            var published = publishedWithoutLists;

            assertSoftly(softly -> {
                softly
                    .assertThat(stateOf(published.toBuilder().policies(List.of()).build(), published))
                    .as("empty policies against absent ones")
                    .isEqualTo(GuardrailLifecycleState.DEPLOYED);
                softly
                    .assertThat(stateOf(published.toBuilder().resources(List.of()).build(), published))
                    .as("empty resources against absent ones")
                    .isEqualTo(GuardrailLifecycleState.DEPLOYED);
                softly
                    .assertThat(stateOf(published.toBuilder().apiTypes(Set.of()).build(), published))
                    .as("empty API types against absent ones")
                    .isEqualTo(GuardrailLifecycleState.DEPLOYED);
                softly
                    .assertThat(
                        stateOf(published, published.toBuilder().policies(List.of()).resources(List.of()).apiTypes(Set.of()).build())
                    )
                    .as("absent lists against empty published ones")
                    .isEqualTo(GuardrailLifecycleState.DEPLOYED);
            });
        }

        @Test
        void should_show_a_draft_filling_a_list_the_revision_has_not_as_pending() {
            var published = publishedWithoutLists;
            var resource = Resource.builder().name("cache").type("cache").configuration("{}").build();

            assertSoftly(softly -> {
                softly
                    .assertThat(stateOf(published.toBuilder().policies(List.of(aPolicy())).build(), published))
                    .as("policy added")
                    .isEqualTo(GuardrailLifecycleState.PENDING);
                softly
                    .assertThat(stateOf(published.toBuilder().resources(List.of(resource)).build(), published))
                    .as("resource added")
                    .isEqualTo(GuardrailLifecycleState.PENDING);
                softly
                    .assertThat(stateOf(published.toBuilder().apiTypes(Set.of(ApiType.LLM_PROXY)).build(), published))
                    .as("API type restricted")
                    .isEqualTo(GuardrailLifecycleState.PENDING);
                softly
                    .assertThat(stateOf(published.toBuilder().name("Renamed").resources(List.of()).build(), published))
                    .as("renamed with an empty list")
                    .isEqualTo(GuardrailLifecycleState.PENDING);
            });
        }
    }
}
