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
package io.gravitee.gamma.rest.core.guardian.domain_service;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.GUARDIAN_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.databind.node.ObjectNode;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.EventLatestQueryServiceInMemory;
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
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
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
class GuardianDeploymentDomainServiceTest {

    @Nested
    class EventStore {

        private final InMemoryGuardianRepository guardians = new InMemoryGuardianRepository();
        private final EventCrudInMemory history = new EventCrudInMemory();
        private final EventLatestCrudInMemory latest = new EventLatestCrudInMemory();
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
        private GuardianDeploymentDomainService service;

        @BeforeEach
        void set_up() {
            TimeProvider.overrideClock(Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC));
            var mapper = GraviteeJacksonMapper.getInstance();
            var writer = new GenericEventWriterDomainService(history, latest, mapper);
            var store = new GuardianPublicationDomainService(
                new JacksonJsonSerializer(),
                new JacksonJsonDeserializer(),
                new GenericEventPublicationDomainService(writer, mapper),
                query
            );
            service = new GuardianDeploymentDomainService(guardians, store, new JacksonJsonSerializer(), new JacksonJsonDeserializer());
        }

        @AfterEach
        void reset_clock() {
            TimeProvider.overrideClock(Clock.systemDefaultZone());
        }

        @Test
        void should_publish_a_definition_the_gateway_can_read_with_its_properties_and_label() throws Exception {
            var definition = aGuardianDefinition()
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
            var guardian = aGuardian().definition(definition).build();
            guardians.givenGuardian(guardian);

            var deployed = service.publish(guardian, "Reviewed", actor);
            var event = latest.storage().getFirst();
            var envelope = GraviteeJacksonMapper.getInstance().readTree(event.getPayload());
            var reloaded = GraviteeJacksonMapper.getInstance().treeToValue(envelope.get("payload"), Guardian.class);

            assertSoftly(softly -> {
                softly.assertThat(event.getType()).isEqualTo(EventType.PUBLISH_GENERIC_EVENT);
                softly
                    .assertThat(event.getProperties())
                    .containsEntry(Event.EventProperties.GENERIC_EVENT_KIND, "guardian.definition")
                    .containsEntry(
                        Event.EventProperties.GENERIC_EVENT_ID,
                        GenericEvent.latestEventId(ENVIRONMENT_ID, "guardian.definition", GUARDIAN_ID)
                    )
                    .containsEntry(Event.EventProperties.USER, "deployer")
                    .containsEntry(Event.EventProperties.DEPLOYMENT_LABEL, "Reviewed");
                softly.assertThat(reloaded).as("gateway payload round trip").isEqualTo(definition);
                softly.assertThat(deployed.at()).isEqualTo(Instant.parse(envelope.get("updatedAt").asText()));
                softly.assertThat(history.storage()).as("first deployment is historized").hasSize(1);
            });
        }

        @Test
        void should_keep_a_draft_saved_after_the_deployed_one_was_read() {
            var read = aGuardian().build();
            var savedMeanwhile = read
                .toBuilder()
                .definition(read.getDefinition().toBuilder().name("Saved meanwhile").build())
                .description("Saved meanwhile")
                .build();
            guardians.givenGuardian(savedMeanwhile);

            service.complete(read, service.publish(read, null, actor));

            assertThat(guardians.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID))
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
        void should_reload_the_publication_scoped_to_the_guardian_environment() {
            var guardian = aGuardian().build();
            guardians.givenGuardian(guardian);

            service.publish(guardian, null, actor);

            assertThat(service.current(guardian))
                .asInstanceOf(type(GuardianDeployment.Published.class))
                .extracting(GuardianDeployment.Published::definition)
                .isEqualTo(guardian.getDefinition());
            assertThat(service.current(aGuardian(definition -> definition.environmentId("another-environment")).build())).isNull();
        }

        @Test
        void should_withdraw_the_same_guardian_and_record_the_withdrawal() {
            var guardian = aGuardian().build();
            guardians.givenGuardian(guardian);
            service.publish(guardian, null, actor);
            var publishedId = latest.storage().getFirst().getProperties().get(Event.EventProperties.GENERIC_EVENT_ID);

            var withdrawal = service.withdraw(
                aGuardian().build(),
                new GuardianDeployment.Published(aGuardianDefinition().build(), Instant.parse("2026-11-08T10:00:00Z")),
                actor
            );
            var recovered = service.current(guardian);

            assertSoftly(softly -> {
                softly
                    .assertThat(latest.storage())
                    .singleElement()
                    .satisfies(event -> {
                        assertThat(event.getType()).isEqualTo(EventType.UNPUBLISH_GENERIC_EVENT);
                        assertThat(event.getProperties()).containsEntry(Event.EventProperties.GENERIC_EVENT_ID, publishedId);
                    });
                softly.assertThat(recovered).as("withdrawn").isInstanceOf(GuardianDeployment.Withdrawn.class);
                softly.assertThat(history.storage()).hasSize(2);
                softly.assertThat(recovered).as("durable withdrawal round trip").isEqualTo(withdrawal);
            });
        }

        @Test
        void should_publish_only_the_deployment_date_with_a_withdrawal() throws Exception {
            service.withdraw(
                aGuardian().build(),
                new GuardianDeployment.Published(aGuardianDefinition().build(), Instant.parse("2026-11-08T10:00:00Z")),
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
        void should_refuse_a_withdrawal_of_another_guardian(String identity) throws Exception {
            var guardian = aGuardian().build();
            service.withdraw(
                guardian,
                new GuardianDeployment.Published(guardian.getDefinition(), Instant.parse("2026-11-08T10:00:00Z")),
                actor
            );
            var event = latest.storage().getFirst();
            var mapper = GraviteeJacksonMapper.getInstance();
            var envelope = (ObjectNode) mapper.readTree(event.getPayload());
            envelope.put(identity, "another-guardian");
            event.setPayload(mapper.writeValueAsString(envelope));

            assertThatThrownBy(() -> service.current(guardian))
                .as("withdrawal remains scoped to its event")
                .isInstanceOf(TechnicalDomainException.class);
        }

        @Test
        void should_read_a_withdrawal_whose_payload_carries_other_fields() throws Exception {
            var guardian = aGuardian().build();
            service.withdraw(
                guardian,
                new GuardianDeployment.Published(guardian.getDefinition(), Instant.parse("2026-11-08T10:00:00Z")),
                actor
            );
            var event = latest.storage().getFirst();
            var mapper = GraviteeJacksonMapper.getInstance();
            var envelope = (ObjectNode) mapper.readTree(event.getPayload());
            envelope.set("payload", mapper.readTree("{\"guardian\":{\"description\":\"An earlier withdrawal\"}}"));
            event.setPayload(mapper.writeValueAsString(envelope));

            assertThat(service.current(guardian))
                .as("withdrawal written with an earlier payload")
                .asInstanceOf(type(GuardianDeployment.Withdrawn.class))
                .extracting(GuardianDeployment.Withdrawn::deployedAt)
                .isNull();
        }

        @Test
        void should_read_a_withdrawal_without_payload_as_removed() throws Exception {
            var guardian = aGuardian().build();
            service.withdraw(
                guardian,
                new GuardianDeployment.Published(guardian.getDefinition(), Instant.parse("2026-11-08T10:00:00Z")),
                actor
            );
            var event = latest.storage().getFirst();
            var mapper = GraviteeJacksonMapper.getInstance();
            var envelope = (ObjectNode) mapper.readTree(event.getPayload());
            envelope.remove("payload");
            event.setPayload(mapper.writeValueAsString(envelope));

            assertThat(service.current(guardian))
                .as("withdrawal without deployment date")
                .asInstanceOf(type(GuardianDeployment.Withdrawn.class))
                .extracting(GuardianDeployment.Withdrawn::deployedAt)
                .isNull();
        }
    }
}
