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
package io.gravitee.apim.core.generic_event.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventWriterDomainService;
import io.gravitee.apim.core.generic_event.model.GenericEvent;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublishGenericEventUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("org")
        .environmentId("env")
        .actor(AuditActor.builder().userId("user").build())
        .build();

    private final EventCrudInMemory eventCrudService = new EventCrudInMemory();
    private final EventLatestCrudInMemory eventLatestCrudService = new EventLatestCrudInMemory();
    private PublishGenericEventUseCase publish;
    private DeleteGenericEventUseCase delete;

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
        var writer = new GenericEventWriterDomainService(eventCrudService, eventLatestCrudService, GraviteeJacksonMapper.getInstance());
        publish = new PublishGenericEventUseCase(writer, GraviteeJacksonMapper.getInstance());
        delete = new DeleteGenericEventUseCase(writer);
    }

    @AfterEach
    void tearDown() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_publish_the_envelope_with_data_as_json() {
        var genericEvent = GenericEvent.builder().kind("aihub.key").id("app-1").payload("{\"teamId\":\"t1\"}").build();

        var output = publish.execute(new PublishGenericEventUseCase.Input(genericEvent, AUDIT_INFO));

        assertThat(output.genericEvent().environmentId()).isEqualTo("env");
        assertThat(output.genericEvent().organizationId()).isEqualTo("org");
        assertThat(output.genericEvent().updatedAt()).isEqualTo(NOW);
        assertThat(eventCrudService.storage()).isEmpty();
        var latest = eventLatestCrudService.storage();
        assertThat(latest).hasSize(1);
        var event = latest.getFirst();
        assertThat(event.getId()).isEqualTo(GenericEvent.latestEventId("env", "aihub.key", "app-1"));
        assertThat(event.getType()).isEqualTo(EventType.PUBLISH_GENERIC_EVENT);
        assertThat(event.getProperties())
            .containsEntry(Event.EventProperties.GENERIC_EVENT_ID, event.getId())
            .containsEntry(Event.EventProperties.GENERIC_EVENT_KIND, "aihub.key");
        assertThat(event.getExpiresAt()).isNull();
        assertThat(event.getPayload())
            .contains("\"organizationId\":\"org\"")
            .contains("\"payload\":{\"teamId\":\"t1\"}")
            .contains("\"updatedAt\":\"2026-10-23T10:00:00Z\"");
        assertThat(event.getUpdatedAt().toInstant()).isEqualTo(NOW);
        assertThat(event.getEnvironments()).containsExactly("env");
    }

    @Test
    void should_also_record_a_historized_event_in_the_events_history() {
        var genericEvent = GenericEvent.builder().kind("aihub.key").id("app-1").payload("{\"teamId\":\"t1\"}").build();

        publish.execute(new PublishGenericEventUseCase.Input(genericEvent, true, AUDIT_INFO));

        assertThat(eventCrudService.storage()).hasSize(1);
        var history = eventCrudService.storage().getFirst();
        assertThat(history.getType()).isEqualTo(EventType.PUBLISH_GENERIC_EVENT);
        assertThat(history.getPayload()).isEqualTo(eventLatestCrudService.storage().getFirst().getPayload());
    }

    @Test
    void should_store_the_expiry_on_the_latest_event() {
        var genericEvent = GenericEvent.builder().kind("aihub.key").id("app-1").expiresAt(NOW.plusSeconds(60)).payload("{}").build();

        publish.execute(new PublishGenericEventUseCase.Input(genericEvent, AUDIT_INFO));

        assertThat(eventLatestCrudService.storage().getFirst().getExpiresAt().toInstant()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void should_reject_a_kind_without_a_feature_prefix() {
        var genericEvent = GenericEvent.builder().kind("key").id("app-1").payload("{}").build();

        assertThatThrownBy(() -> publish.execute(new PublishGenericEventUseCase.Input(genericEvent, AUDIT_INFO))).isInstanceOf(
            InvalidDataException.class
        );
    }

    @Test
    void should_reject_a_payload_that_is_not_json() {
        var genericEvent = GenericEvent.builder().kind("aihub.key").id("app-1").payload("{\"teamId\":").build();
        var trailing = GenericEvent.builder().kind("aihub.key").id("app-1").payload("{} {}").build();

        assertThatThrownBy(() -> publish.execute(new PublishGenericEventUseCase.Input(genericEvent, AUDIT_INFO))).isInstanceOf(
            InvalidDataException.class
        );
        assertThatThrownBy(() -> publish.execute(new PublishGenericEventUseCase.Input(trailing, AUDIT_INFO))).isInstanceOf(
            InvalidDataException.class
        );
        assertThat(eventLatestCrudService.storage()).isEmpty();
    }

    @Test
    void should_reject_a_payload_over_the_size_limit() {
        var payload = "\"" + "a".repeat(PublishGenericEventUseCase.MAX_PAYLOAD_BYTES) + "\"";
        var genericEvent = GenericEvent.builder().kind("aihub.key").id("app-1").payload(payload).build();

        assertThatThrownBy(() -> publish.execute(new PublishGenericEventUseCase.Input(genericEvent, AUDIT_INFO))).isInstanceOf(
            InvalidDataException.class
        );
        assertThat(eventLatestCrudService.storage()).isEmpty();
    }

    @Test
    void should_unpublish_on_the_same_latest_event() {
        delete.execute(new DeleteGenericEventUseCase.Input("aihub.key", "app-1", AUDIT_INFO));

        var event = eventLatestCrudService.storage().getFirst();
        assertThat(event.getId()).isEqualTo(GenericEvent.latestEventId("env", "aihub.key", "app-1"));
        assertThat(event.getType()).isEqualTo(EventType.UNPUBLISH_GENERIC_EVENT);
    }

    @Test
    void should_replace_the_published_event_with_an_unpublish_the_gateway_can_resolve() {
        var genericEvent = GenericEvent.builder().kind("aihub.key").id("app-1").payload("{\"teamId\":\"t1\"}").build();
        publish.execute(new PublishGenericEventUseCase.Input(genericEvent, true, AUDIT_INFO));

        delete.execute(new DeleteGenericEventUseCase.Input("aihub.key", "app-1", true, AUDIT_INFO));

        assertThat(eventCrudService.storage()).hasSize(2);
        // The in-memory store keeps both rows; the repositories replace the published one, which shares this id.
        var event = eventLatestCrudService.storage().getLast();
        assertThat(event.getId()).isEqualTo(GenericEvent.latestEventId("env", "aihub.key", "app-1"));
        assertThat(event.getType()).isEqualTo(EventType.UNPUBLISH_GENERIC_EVENT);
        assertThat(event.getPayload())
            .contains("\"kind\":\"aihub.key\"")
            .contains("\"id\":\"app-1\"")
            .contains("\"environmentId\":\"env\"")
            .contains("\"organizationId\":\"org\"")
            .doesNotContain("teamId");
    }
}
