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
package io.gravitee.gateway.handlers.api.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;

import appender.MemoryAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.approval.ApprovalOpenKey;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalStateEventSubscriberTest {

    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");
    private static final long RULE_VERSION = Instant.parse("2026-11-01T10:00:00Z").toEpochMilli();
    private static final String STATE = """
        {"approvalRequestId":"ar-1","aliasIds":["ar-2"],"environmentId":"env-1","apiId":"api-1","ruleId":"rule-1",
         "ruleUpdatedAt":"2026-11-01T10:00:00Z","openKey":"key-1","status":"PENDING","createdAt":"2026-11-02T09:55:00Z",
         "expiresAt":"2026-11-02T10:15:00Z","keepUntil":"2026-11-02T10:30:00Z","updatedAt":"2026-11-02T09:55:00Z","version":3}
        """;

    private final MemoryAppender logs = new MemoryAppender();
    private ApprovalStateManagerImpl manager;
    private ApprovalStateEventSubscriber subscriber;

    @BeforeEach
    void setUp() {
        manager = new ApprovalStateManagerImpl(Clock.fixed(NOW, ZoneOffset.UTC));
        subscriber = new ApprovalStateEventSubscriber(manager, new ObjectMapper());
        logs.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logs.start();
        subscriberLogger().addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        subscriberLogger().detachAppender(logs);
        logs.stop();
    }

    @Test
    void should_deploy_a_readable_state_at_once_under_its_id_and_aliases() {
        subscriber.onUpsert(event("ar-1", "env-1", STATE, "2026-11-02T10:30:00Z"));

        assertThat(manager.byId("ar-1").orElseThrow()).satisfies(state -> {
            assertThat(state.id()).isEqualTo("ar-1");
            assertThat(state.aliasIds()).containsExactly("ar-2");
            assertThat(state.status()).isEqualTo("PENDING");
            assertThat(state.version()).isEqualTo(3L);
            assertThat(state.ruleUpdatedAt()).isEqualTo(RULE_VERSION);
            assertThat(state.keepUntil()).isEqualTo(Instant.parse("2026-11-02T10:30:00Z").toEpochMilli());
            assertThat(state.expiresAt()).isEqualTo(Instant.parse("2026-11-02T10:15:00Z").toEpochMilli());
            assertThat(state.grant()).isNull();
        });
        assertThat(manager.byId("ar-2").orElseThrow().id()).isEqualTo("ar-1");
        assertThat(manager.byOpenKey("key-1").orElseThrow().id()).isEqualTo("ar-1");
    }

    @Test
    void should_map_a_time_based_grant_and_an_executor_lease() {
        String approved = STATE.replace("\"status\":\"PENDING\"", "\"status\":\"APPROVED\"").replace(
            "\"version\":3",
            """
            "grant":{"scope":"TIMED","coversUntil":"2026-11-02T10:25:00Z","caller":{"applicationId":"app-1","subject":"alice"},"argumentsDigest":"abc"},
            "executor":{"nodeId":"node-1","leaseId":"lease-1","leaseUntil":"2026-11-02T10:01:00Z"},"version":4"""
        );

        subscriber.onUpsert(event("ar-1", "env-1", approved, "2026-11-02T10:30:00Z"));

        DeployedApprovalState state = manager.byId("ar-1").orElseThrow();
        assertThat(state.grant()).isEqualTo(
            new DeployedApprovalState.Grant("TIMED", Instant.parse("2026-11-02T10:25:00Z").toEpochMilli(), "app-1", "alice", "abc")
        );
        assertThat(state.executor()).isEqualTo(
            new DeployedApprovalState.ExecutorLease("node-1", "lease-1", Instant.parse("2026-11-02T10:01:00Z").toEpochMilli())
        );
        assertThat(aliceGrant().orElseThrow().id()).isEqualTo("ar-1");
    }

    @Test
    void should_wake_the_calls_waiting_for_the_request_when_a_newer_copy_arrives() {
        TestScheduler scheduler = new TestScheduler();
        subscriber.onUpsert(event("ar-1", "env-1", STATE, "2026-11-02T10:30:00Z"));
        TestObserver<Void> waiting = manager.awaitChange("ar-1", Duration.ofSeconds(20), scheduler).test();
        waiting.assertNotComplete();

        subscriber.onUpsert(event("ar-1", "env-1", STATE.replace("\"version\":3", "\"version\":4"), "2026-11-02T10:30:00Z"));
        scheduler.triggerActions();

        waiting.assertComplete();
    }

    @Test
    void should_forget_a_state_when_its_event_is_removed_or_expires() {
        subscriber.onUpsert(event("ar-1", "env-1", STATE, "2026-11-02T10:30:00Z"));

        subscriber.onRemove(event("ar-1", "env-1", null, "2026-11-02T10:30:00Z"));

        assertThat(manager.byId("ar-1").isEmpty()).isTrue();
        assertThat(manager.byOpenKey("key-1").isEmpty()).isTrue();
    }

    @Test
    void should_ignore_the_properties_a_newer_control_plane_adds() {
        subscriber.onUpsert(
            event("ar-1", "env-1", STATE.replace("\"version\":3", "\"version\":3,\"addedLater\":[1]"), "2026-11-02T10:30:00Z")
        );

        assertThat(manager.byId("ar-1").isEmpty()).isFalse();
    }

    /**
     * The copy held may be the one a newer copy ends, a grant revoked early for one: kept, it would let calls run that
     * the control plane no longer allows. Forgotten, a held call waits or asks again.
     */
    @Test
    void should_forget_the_copy_it_had_when_a_newer_copy_cannot_be_read() {
        for (String broken : new String[] {
            "{not json",
            STATE.replace("\"status\":\"PENDING\"", "\"status\":\"\""),
            STATE.replace("\"keepUntil\":\"2026-11-02T10:30:00Z\"", "\"keepUntil\":\"soon\""),
            STATE.replace("\"updatedAt\":\"2026-11-02T09:55:00Z\"", "\"updatedAt\":null"),
            STATE.replace("\"ruleUpdatedAt\":\"2026-11-01T10:00:00Z\",", ""),
            STATE.replace("\"ruleUpdatedAt\":\"2026-11-01T10:00:00Z\"", "\"ruleUpdatedAt\":\"yesterday\""),
            STATE.replace(
                "\"version\":3",
                "\"version\":9,\"grant\":{\"scope\":\"TIMED\",\"argumentsDigest\":\"abc\",\"caller\":{\"applicationId\":\"app-1\"}}"
            ),
            STATE.replace("\"version\":3", "\"version\":9,\"executor\":{\"nodeId\":\"n\",\"leaseId\":\"l\"}"),
            STATE.replace("\"aliasIds\":[\"ar-2\"]", "\"aliasIds\":[null]"),
            STATE.replace("\"approvalRequestId\":\"ar-1\"", "\"approvalRequestId\":\"ar-9\""),
        }) {
            subscriber.onUpsert(event("ar-1", "env-1", STATE, "2026-11-02T10:30:00Z"));

            subscriber.onUpsert(event("ar-1", "env-1", broken, "2026-11-02T10:30:00Z"));

            assertThat(manager.byId("ar-1").isEmpty()).as(broken).isTrue();
            assertThat(manager.byId("ar-2").isEmpty()).as(broken).isTrue();
            assertThat(manager.byOpenKey("key-1").isEmpty()).as(broken).isTrue();
        }
    }

    @Test
    void should_stop_granting_and_wake_the_waiting_calls_when_a_newer_copy_of_the_grant_cannot_be_read() {
        String approved = STATE.replace("\"status\":\"PENDING\"", "\"status\":\"APPROVED\"").replace(
            "\"version\":3",
            "\"grant\":{\"scope\":\"TIMED\",\"coversUntil\":\"2026-11-02T10:25:00Z\",\"caller\":{\"applicationId\":\"app-1\",\"subject\":\"alice\"},\"argumentsDigest\":\"abc\"},\"version\":4"
        );
        subscriber.onUpsert(event("ar-1", "env-1", approved, "2026-11-02T10:30:00Z"));
        assertThat(aliceGrant().isEmpty()).isFalse();
        TestScheduler scheduler = new TestScheduler();
        TestObserver<Void> waiting = manager.awaitChange("ar-1", Duration.ofSeconds(20), scheduler).test();

        subscriber.onUpsert(
            event(
                "ar-1",
                "env-1",
                approved.replace("\"status\":\"APPROVED\"", "\"status\":null").replace("\"version\":4", "\"version\":5"),
                "2026-11-02T10:30:00Z"
            )
        );
        scheduler.triggerActions();

        assertThat(aliceGrant().isEmpty()).isTrue();
        waiting.assertComplete();
    }

    /** A state names the end user a grant is for: an unreadable one is logged by its ids and the fields at fault only. */
    @Test
    void should_never_log_the_values_of_an_unreadable_payload() {
        subscriber.onUpsert(
            event("ar-1", "env-1", STATE.replace("\"version\":3", "\"version\":\"alice@example.com\""), "2026-11-02T10:30:00Z")
        );
        subscriber.onUpsert(
            event(
                "ar-1",
                "env-1",
                STATE.replace("\"aliasIds\":[\"ar-2\"]", "\"aliasIds\":[\"alice@example.com\",null]"),
                "2026-11-02T10:30:00Z"
            )
        );

        assertThat(logs.getLoggedEvents())
            .hasSize(2)
            .allSatisfy(logged -> assertThat(loggedText(logged)).contains("[ar-1]", "[env-1]").doesNotContain("alice"));
        assertThat(logs.getLoggedEvents().get(1).getFormattedMessage()).contains("aliasIds");
    }

    @Test
    void should_ignore_a_state_that_names_another_request_or_environment_than_its_event() {
        subscriber.onUpsert(event("ar-9", "env-1", STATE, "2026-11-02T10:30:00Z"));
        subscriber.onUpsert(event("ar-1", "env-2", STATE, "2026-11-02T10:30:00Z"));

        assertThat(manager.byId("ar-1").isEmpty()).isTrue();
        assertThat(manager.byId("ar-9").isEmpty()).isTrue();
    }

    /** Without an expiry the gateway would never remove the state: it would stay in memory for good. */
    @Test
    void should_ignore_a_state_whose_event_has_no_expiry() {
        subscriber.onUpsert(event("ar-1", "env-1", STATE, null));

        assertThat(manager.byId("ar-1").isEmpty()).isTrue();
    }

    @Test
    void should_ignore_an_event_without_payload() {
        subscriber.onUpsert(event("ar-1", "env-1", null, "2026-11-02T10:30:00Z"));

        assertThat(manager.byId("ar-1").isEmpty()).isTrue();
    }

    private Optional<DeployedApprovalState> aliceGrant() {
        return manager.activeGrant(ApprovalOpenKey.of("env-1", "api-1", "rule-1", "app-1", "alice", "abc"), RULE_VERSION);
    }

    private static Logger subscriberLogger() {
        return (Logger) LoggerFactory.getLogger(ApprovalStateEventSubscriber.class);
    }

    /** The message of a log event and of every exception it carries. */
    private static String loggedText(ILoggingEvent logged) {
        List<String> text = new ArrayList<>(List.of(logged.getFormattedMessage()));
        for (IThrowableProxy thrown = logged.getThrowableProxy(); thrown != null; thrown = thrown.getCause()) {
            text.add(thrown.getMessage());
        }
        return String.join("\n", text);
    }

    private static GenericEvent event(String id, String environmentId, String payload, String expiresAt) {
        return new GenericEvent(
            "approval.state",
            id,
            environmentId,
            "org-1",
            expiresAt == null ? null : Instant.parse(expiresAt),
            Instant.parse("2026-11-02T09:55:00Z"),
            payload
        );
    }
}
