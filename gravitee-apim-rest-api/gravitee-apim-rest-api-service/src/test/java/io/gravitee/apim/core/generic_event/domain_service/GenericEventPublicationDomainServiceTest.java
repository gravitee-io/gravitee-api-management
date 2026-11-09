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
package io.gravitee.apim.core.generic_event.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.generic_event.model.GenericEvent;
import io.gravitee.apim.core.generic_event.model.PublicationOptions;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenericEventPublicationDomainServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("org")
        .environmentId("env")
        .actor(AuditActor.builder().userId("user").build())
        .build();

    private final EventCrudInMemory eventCrudService = new EventCrudInMemory();
    private final EventLatestCrudInMemory eventLatestCrudService = new EventLatestCrudInMemory();
    private GenericEventPublicationDomainService service;

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
        var writer = new GenericEventWriterDomainService(eventCrudService, eventLatestCrudService, GraviteeJacksonMapper.getInstance());
        service = new GenericEventPublicationDomainService(writer, GraviteeJacksonMapper.getInstance());
    }

    @AfterEach
    void tearDown() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_publish_the_envelope_with_data_as_json() {
        var genericEvent = GenericEvent.builder().kind("aihub.key").id("app-1").payload("{\"teamId\":\"t1\"}").build();

        var output = service.publish(genericEvent, PublicationOptions.latestOnly(), AUDIT_INFO);

        assertThat(output.environmentId()).isEqualTo("env");
        assertThat(output.organizationId()).isEqualTo("org");
        assertThat(output.updatedAt()).isEqualTo(NOW);
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

        service.publish(genericEvent, PublicationOptions.withHistory(), AUDIT_INFO);

        assertThat(eventCrudService.storage()).hasSize(1);
        var history = eventCrudService.storage().getFirst();
        assertThat(history.getType()).isEqualTo(EventType.PUBLISH_GENERIC_EVENT);
        assertThat(history.getPayload()).isEqualTo(eventLatestCrudService.storage().getFirst().getPayload());
    }

    @Test
    void should_record_the_deployment_label_in_history_and_latest() {
        var genericEvent = GenericEvent.builder().kind("guardian.definition").id("guardian-1").payload("{}").build();

        service.publish(genericEvent, PublicationOptions.withHistory("Reviewed guardrails"), AUDIT_INFO);

        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThat(eventCrudService.storage())
                .as("deployment history")
                .singleElement()
                .extracting(event -> event.getProperties().get(Event.EventProperties.DEPLOYMENT_LABEL))
                .isEqualTo("Reviewed guardrails");
            softly
                .assertThat(eventLatestCrudService.storage())
                .as("latest deployment")
                .singleElement()
                .extracting(event -> event.getProperties().get(Event.EventProperties.DEPLOYMENT_LABEL))
                .isEqualTo("Reviewed guardrails");
        });
    }

    @Test
    void should_record_the_label_when_history_is_disabled() {
        var genericEvent = GenericEvent.builder().kind("guardian.definition").id("guardian-1").payload("{}").build();

        service.publish(genericEvent, new PublicationOptions(false, "Emergency deployment"), AUDIT_INFO);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(eventCrudService.storage()).as("deployment history disabled").isEmpty();
            softly
                .assertThat(eventLatestCrudService.storage())
                .as("latest deployment")
                .singleElement()
                .extracting(event -> event.getProperties().get(Event.EventProperties.DEPLOYMENT_LABEL))
                .isEqualTo("Emergency deployment");
        });
    }

    @Test
    void should_keep_the_deployment_label_optional_for_existing_callers() {
        var genericEvent = GenericEvent.builder().kind("guardian.definition").id("guardian-1").payload("{}").build();

        service.publish(genericEvent, PublicationOptions.withHistory(), AUDIT_INFO);

        assertThat(eventLatestCrudService.storage())
            .as("unlabelled deployment")
            .singleElement()
            .satisfies(event -> assertThat(event.getProperties()).doesNotContainKey(Event.EventProperties.DEPLOYMENT_LABEL));
    }

    @Test
    void should_accept_a_deployment_label_at_the_size_limit() {
        var genericEvent = GenericEvent.builder().kind("guardian.definition").id("guardian-1").payload("{}").build();
        var label = "a".repeat(256);

        service.publish(genericEvent, PublicationOptions.withHistory(label), AUDIT_INFO);

        assertThat(eventLatestCrudService.storage())
            .as("deployment labelled at the size limit")
            .singleElement()
            .extracting(event -> event.getProperties().get(Event.EventProperties.DEPLOYMENT_LABEL))
            .isEqualTo(label);
    }

    @Test
    void should_refuse_an_oversized_label_before_writing_any_event() {
        var genericEvent = GenericEvent.builder().kind("guardian.definition").id("guardian-1").payload("{}").build();

        assertThatThrownBy(() -> service.publish(genericEvent, PublicationOptions.withHistory("a".repeat(257)), AUDIT_INFO))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage("A deployment label must not exceed 256 characters.");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(eventCrudService.storage()).as("no deployment history on validation failure").isEmpty();
            softly.assertThat(eventLatestCrudService.storage()).as("no latest deployment on validation failure").isEmpty();
        });
    }

    @Test
    void should_store_the_expiry_on_the_latest_event() {
        var genericEvent = GenericEvent.builder().kind("aihub.key").id("app-1").expiresAt(NOW.plusSeconds(60)).payload("{}").build();

        service.publish(genericEvent, PublicationOptions.latestOnly(), AUDIT_INFO);

        assertThat(eventLatestCrudService.storage().getFirst().getExpiresAt().toInstant()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void should_reject_a_kind_without_a_feature_prefix() {
        var genericEvent = GenericEvent.builder().kind("key").id("app-1").payload("{}").build();

        assertThatThrownBy(() -> service.publish(genericEvent, PublicationOptions.latestOnly(), AUDIT_INFO)).isInstanceOf(
            InvalidDataException.class
        );
    }

    @Test
    void should_reject_a_payload_that_is_not_json() {
        var genericEvent = GenericEvent.builder().kind("aihub.key").id("app-1").payload("{\"teamId\":").build();
        var trailing = GenericEvent.builder().kind("aihub.key").id("app-1").payload("{} {}").build();

        assertThatThrownBy(() -> service.publish(genericEvent, PublicationOptions.latestOnly(), AUDIT_INFO)).isInstanceOf(
            InvalidDataException.class
        );
        assertThatThrownBy(() -> service.publish(trailing, PublicationOptions.latestOnly(), AUDIT_INFO)).isInstanceOf(
            InvalidDataException.class
        );
        assertThat(eventLatestCrudService.storage()).isEmpty();
    }

    @Test
    void should_reject_a_payload_over_the_size_limit() {
        var payload = "\"" + "a".repeat(256 * 1024) + "\"";
        var genericEvent = GenericEvent.builder().kind("aihub.key").id("app-1").payload(payload).build();

        assertThatThrownBy(() -> service.publish(genericEvent, PublicationOptions.latestOnly(), AUDIT_INFO)).isInstanceOf(
            InvalidDataException.class
        );
        assertThat(eventLatestCrudService.storage()).isEmpty();
    }

    @Test
    void should_unpublish_on_the_same_latest_event() {
        service.withdraw(GenericEvent.builder().kind("aihub.key").id("app-1").build(), PublicationOptions.latestOnly(), AUDIT_INFO);

        var event = eventLatestCrudService.storage().getFirst();
        assertThat(event.getId()).isEqualTo(GenericEvent.latestEventId("env", "aihub.key", "app-1"));
        assertThat(event.getType()).isEqualTo(EventType.UNPUBLISH_GENERIC_EVENT);
    }

    @Test
    void should_replace_the_published_event_with_an_unpublish_the_gateway_can_resolve() {
        var genericEvent = GenericEvent.builder().kind("aihub.key").id("app-1").payload("{\"teamId\":\"t1\"}").build();
        service.publish(genericEvent, PublicationOptions.withHistory(), AUDIT_INFO);

        service.withdraw(GenericEvent.builder().kind("aihub.key").id("app-1").build(), PublicationOptions.withHistory(), AUDIT_INFO);

        assertThat(eventCrudService.storage()).hasSize(2);
        assertThat(eventLatestCrudService.storage()).as("one latest row per generic event").hasSize(1);
        var event = eventLatestCrudService.storage().getFirst();
        assertThat(event.getId()).isEqualTo(GenericEvent.latestEventId("env", "aihub.key", "app-1"));
        assertThat(event.getType()).isEqualTo(EventType.UNPUBLISH_GENERIC_EVENT);
        assertThat(event.getPayload())
            .contains("\"kind\":\"aihub.key\"")
            .contains("\"id\":\"app-1\"")
            .contains("\"environmentId\":\"env\"")
            .contains("\"organizationId\":\"org\"")
            .doesNotContain("teamId");
    }

    @Test
    void should_withdraw_with_a_payload_scoped_and_dated_like_a_publication() {
        var withdrawal = GenericEvent.builder().kind("guardian.definition").id("guardian-1").payload("{\"reason\":\"retired\"}").build();

        var output = service.withdraw(withdrawal, PublicationOptions.withHistory(), AUDIT_INFO);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(output.environmentId()).as("environment").isEqualTo("env");
            softly.assertThat(output.organizationId()).as("organization").isEqualTo("org");
            softly.assertThat(output.updatedAt()).as("withdrawal date").isEqualTo(NOW);
            softly
                .assertThat(eventLatestCrudService.storage())
                .as("latest withdrawal")
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getType()).isEqualTo(EventType.UNPUBLISH_GENERIC_EVENT);
                    assertThat(event.getPayload()).contains("\"payload\":{\"reason\":\"retired\"}");
                });
        });
    }

    @Test
    void should_validate_a_withdrawal_payload_like_a_publication() {
        var oversized = GenericEvent.builder().kind("aihub.key").id("app-1").payload("\"" + "a".repeat(256 * 1024) + "\"").build();
        var invalid = GenericEvent.builder().kind("aihub.key").id("app-1").payload("{\"teamId\":").build();
        var withoutId = GenericEvent.builder().kind("aihub.key").build();

        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> service.withdraw(oversized, PublicationOptions.latestOnly(), AUDIT_INFO))
                .as("oversized payload")
                .isInstanceOf(InvalidDataException.class);
            softly
                .assertThatThrownBy(() -> service.withdraw(invalid, PublicationOptions.latestOnly(), AUDIT_INFO))
                .as("invalid JSON payload")
                .isInstanceOf(InvalidDataException.class);
            softly
                .assertThatThrownBy(() -> service.withdraw(withoutId, PublicationOptions.latestOnly(), AUDIT_INFO))
                .as("missing id")
                .isInstanceOf(InvalidDataException.class);
            softly.assertThat(eventLatestCrudService.storage()).as("nothing written").isEmpty();
        });
    }
}
