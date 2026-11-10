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
package io.gravitee.gateway.genericevent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.approval.ApprovalEventKinds;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateEventSubscriber;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateManagerImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * A state leaves the gateway's memory only when its event expires, which the generic event manager detects at its next
 * commit. In the manager's package for its constructor taking a clock.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalStateEventExpiryTest {

    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");
    private static final Instant KEEP_UNTIL = NOW.plus(Duration.ofMinutes(30));
    private static final String STATE = """
        {"approvalRequestId":"ar-1","environmentId":"env-1","apiId":"api-1","ruleId":"rule-1","openKey":"key-1",
         "ruleUpdatedAt":"2026-11-01T10:00:00Z","status":"PENDING","createdAt":"2026-11-02T09:55:00Z","keepUntil":"2026-11-02T10:30:00Z",
         "updatedAt":"2026-11-02T09:55:00Z","version":1}
        """;

    private final AtomicReference<Instant> now = new AtomicReference<>(NOW);
    private final DefaultGenericEventManager genericEventManager = new DefaultGenericEventManager(
        new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        }
    );
    // Its own clock stays before keepUntil, so a state still held would be found: only its removal hides it.
    private final ApprovalStateManagerImpl states = new ApprovalStateManagerImpl(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void should_remove_a_state_at_the_first_commit_after_its_event_expires() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        states.bindTo(registry);
        genericEventManager.subscribe(ApprovalEventKinds.STATE, new ApprovalStateEventSubscriber(states, new ObjectMapper()));
        genericEventManager.upsert(new GenericEvent(ApprovalEventKinds.STATE, "ar-1", "env-1", "org-1", KEEP_UNTIL, NOW, STATE));
        genericEventManager.commit();
        assertThat(states.byId("ar-1").isEmpty()).isFalse();

        now.set(KEEP_UNTIL.minusMillis(1));
        genericEventManager.commit();
        assertThat(states.byId("ar-1").isEmpty()).isFalse();

        now.set(KEEP_UNTIL);
        genericEventManager.commit();

        assertThat(states.byId("ar-1").isEmpty()).isTrue();
        assertThat(states.byOpenKey("key-1").isEmpty()).isTrue();
        assertThat(registry.get("human_approval_states").gauge().value()).isZero();
    }
}
