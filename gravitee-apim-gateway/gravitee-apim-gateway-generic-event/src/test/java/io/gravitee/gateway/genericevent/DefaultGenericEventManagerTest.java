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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.api.genericevent.GenericEventSubscriber;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DefaultGenericEventManagerTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    private final DefaultGenericEventManager manager = new DefaultGenericEventManager(Clock.fixed(NOW, ZoneOffset.UTC));

    static class Recorder implements GenericEventSubscriber {

        final List<String> calls = new ArrayList<>();

        @Override
        public void onUpsert(GenericEvent event) {
            calls.add("upsert " + event.id() + " " + event.payload());
        }

        @Override
        public void onRemove(GenericEvent event) {
            calls.add("remove " + event.id());
        }

        @Override
        public void onCommit() {
            calls.add("commit");
        }
    }

    private static GenericEvent event(String kind, String id, Instant updatedAt, Instant expiresAt, String data) {
        return new GenericEvent(kind, id, "env", "org", expiresAt, updatedAt, data);
    }

    @Test
    void should_replay_current_events_of_the_prefix_then_commit() {
        manager.upsert(event("aihub.hub", "h1", NOW, null, "{}"));
        manager.upsert(event("approval.rule", "r1", NOW, null, "{}"));
        var recorder = new Recorder();

        manager.subscribe("aihub", recorder);

        assertThat(recorder.calls).containsExactly("upsert h1 {}", "commit");
    }

    @Test
    void should_forward_changes_and_commit_only_subscribers_that_changed() {
        var hub = new Recorder();
        var approval = new Recorder();
        manager.subscribe("aihub", hub);
        manager.subscribe("approval", approval);
        hub.calls.clear();
        approval.calls.clear();

        manager.upsert(event("aihub.key", "k1", NOW, null, "{\"a\":1}"));
        manager.remove("env", "aihub.key", "k1");
        manager.commit();

        assertThat(hub.calls).containsExactly("upsert k1 {\"a\":1}", "remove k1", "commit");
        assertThat(approval.calls).isEmpty();
    }

    @Test
    void should_ignore_an_older_update() {
        var recorder = new Recorder();
        manager.subscribe("aihub", recorder);
        manager.upsert(event("aihub.key", "k1", NOW, null, "new"));
        recorder.calls.clear();

        manager.upsert(event("aihub.key", "k1", NOW.minusSeconds(10), null, "old"));

        assertThat(recorder.calls).isEmpty();
    }

    @Test
    void should_ignore_the_same_update_received_again() {
        var recorder = new Recorder();
        manager.subscribe("aihub", recorder);
        manager.upsert(event("aihub.key", "k1", NOW, null, "v1"));
        manager.commit();
        recorder.calls.clear();

        manager.upsert(event("aihub.key", "k1", NOW, null, "v1"));
        manager.commit();

        assertThat(recorder.calls).isEmpty();
    }

    @Test
    void should_ignore_an_expired_event_it_does_not_hold() {
        var recorder = new Recorder();
        manager.subscribe("aihub", recorder);
        recorder.calls.clear();

        manager.upsert(event("aihub.key", "k1", NOW.minusSeconds(60), NOW.minusSeconds(1), "v1"));
        manager.commit();

        assertThat(recorder.calls).isEmpty();
        assertThat(manager.size()).isZero();
    }

    @Test
    void should_forward_an_expired_update_of_an_event_it_holds() {
        var recorder = new Recorder();
        manager.subscribe("aihub", recorder);
        manager.upsert(event("aihub.key", "k1", NOW.minusSeconds(60), NOW.plusSeconds(3600), "v1"));
        manager.commit();
        recorder.calls.clear();

        manager.upsert(event("aihub.key", "k1", NOW.minusSeconds(30), NOW.minusSeconds(1), "v2"));
        manager.commit();

        assertThat(recorder.calls).containsExactly("upsert k1 v2", "remove k1", "commit");
        assertThat(manager.size()).isZero();
    }

    @Test
    void should_remove_expired_events_at_commit() {
        var now = new AtomicReference<>(NOW);
        var clock = new Clock() {
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
        };
        var expiring = new DefaultGenericEventManager(clock);
        var recorder = new Recorder();
        expiring.subscribe("tracing", recorder);
        expiring.upsert(event("tracing.session", "s1", NOW, NOW.plusSeconds(60), "{}"));
        expiring.commit();
        recorder.calls.clear();
        assertThat(expiring.size()).isEqualTo(1);

        now.set(NOW.plusSeconds(120));
        var expired = expiring.commit();

        assertThat(recorder.calls).containsExactly("remove s1", "commit");
        assertThat(expired).extracting(GenericEvent::id).containsExactly("s1");
        assertThat(expiring.size()).isZero();
        var late = new Recorder();
        expiring.subscribe("tracing", late);
        assertThat(late.calls).containsExactly("commit");
    }

    @Test
    void should_keep_notifying_others_when_a_subscriber_fails() {
        var recorder = new Recorder();
        manager.subscribe(
            "aihub",
            new GenericEventSubscriber() {
                @Override
                public void onUpsert(GenericEvent event) {
                    throw new IllegalStateException("boom");
                }

                @Override
                public void onRemove(GenericEvent event) {}
            }
        );
        manager.subscribe("aihub", recorder);
        recorder.calls.clear();

        manager.upsert(event("aihub.hub", "h1", NOW, null, "{}"));

        assertThat(recorder.calls).containsExactly("upsert h1 {}");
    }

    @Test
    void should_keep_notifying_others_when_a_subscriber_throws_an_error() {
        var recorder = new Recorder();
        manager.subscribe(
            "aihub",
            new GenericEventSubscriber() {
                @Override
                public void onUpsert(GenericEvent event) {
                    throw new NoClassDefFoundError("closed classloader");
                }

                @Override
                public void onRemove(GenericEvent event) {}
            }
        );
        manager.subscribe("aihub", recorder);
        recorder.calls.clear();

        manager.upsert(event("aihub.hub", "h1", NOW, null, "{}"));

        assertThat(recorder.calls).containsExactly("upsert h1 {}");
    }

    @Test
    void should_match_whole_kind_segments() {
        var tracing = new Recorder();
        var session = new Recorder();
        manager.subscribe("tracing", tracing);
        manager.subscribe("tracing.session", session);
        tracing.calls.clear();
        session.calls.clear();

        manager.upsert(event("tracing-v2.session", "a", NOW, null, "{}"));
        manager.upsert(event("tracing.session", "b", NOW, null, "{}"));
        manager.upsert(event("tracing.sessions", "c", NOW, null, "{}"));

        assertThat(tracing.calls).containsExactly("upsert b {}", "upsert c {}");
        assertThat(session.calls).containsExactly("upsert b {}");
    }

    @Test
    void should_reject_a_prefix_that_is_not_whole_segments() {
        var recorder = new Recorder();
        assertThatThrownBy(() -> manager.subscribe(null, recorder)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager.subscribe("", recorder)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager.subscribe("aihub.", recorder)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager.subscribe(".aihub", recorder)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager.subscribe("aihub..key", recorder)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager.subscribe("AIHub", recorder)).isInstanceOf(IllegalArgumentException.class);
    }
}
