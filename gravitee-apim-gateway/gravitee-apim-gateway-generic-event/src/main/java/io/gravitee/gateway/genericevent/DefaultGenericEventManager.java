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

import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.api.genericevent.GenericEventSubscriber;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.CustomLog;

/**
 * Keeps the current generic events and forwards their changes to the subscribers of their kind.
 * <p>
 * Calls are serialized, so a subscriber receives one change at a time.
 * <p>
 * Expired events are removed at commit, the way an unpublished one is.
 */
@CustomLog
public class DefaultGenericEventManager implements GenericEventManager {

    private record Key(String environmentId, String kind, String id) {
        static Key of(GenericEvent event) {
            return new Key(event.environmentId(), event.kind(), event.id());
        }
    }

    private static final class Registration implements Subscription {

        private final String kindPrefix;
        private final GenericEventSubscriber subscriber;
        private final List<Registration> registrations;
        private boolean dirty;

        private Registration(String kindPrefix, GenericEventSubscriber subscriber, List<Registration> registrations) {
            this.kindPrefix = kindPrefix;
            this.subscriber = subscriber;
            this.registrations = registrations;
        }

        private boolean matches(GenericEvent event) {
            var kind = event.kind();
            return kind.startsWith(kindPrefix) && (kind.length() == kindPrefix.length() || kind.charAt(kindPrefix.length()) == '.');
        }

        @Override
        public void cancel() {
            registrations.remove(this);
        }
    }

    /** The segments of a kind as the control plane accepts it, so any other prefix could never match. */
    private static final Pattern KIND_PREFIX = Pattern.compile("[a-z0-9-]+(\\.[a-z0-9-]+)*");

    private final Map<Key, GenericEvent> events = new HashMap<>();
    private final List<Registration> registrations = new CopyOnWriteArrayList<>();
    private final Clock clock;

    public DefaultGenericEventManager() {
        this(Clock.systemUTC());
    }

    DefaultGenericEventManager(Clock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized Subscription subscribe(String kindPrefix, GenericEventSubscriber subscriber) {
        if (kindPrefix == null || !KIND_PREFIX.matcher(kindPrefix).matches()) {
            throw new IllegalArgumentException("A kind prefix must be whole dot-separated segments, got: " + kindPrefix);
        }
        Objects.requireNonNull(subscriber, "subscriber");
        var registration = new Registration(kindPrefix, subscriber, registrations);
        registrations.add(registration);
        var now = clock.instant();
        events
            .values()
            .stream()
            .filter(registration::matches)
            .filter(event -> !event.isExpired(now))
            .forEach(event -> notify(registration, s -> s.onUpsert(event)));
        notify(registration, GenericEventSubscriber::onCommit);
        return registration;
    }

    @Override
    public synchronized void upsert(GenericEvent event) {
        var key = Key.of(event);
        var existing = events.get(key);
        // The sync reads overlapping time windows, so the same event comes back on several cycles.
        if (existing != null && !isNewer(event, existing)) {
            log.debug("Ignoring generic event [{}] of kind [{}]: not newer than the one held", event.id(), event.kind());
            return;
        }
        if (existing == null && event.isExpired(clock.instant())) {
            log.debug("Ignoring generic event [{}] of kind [{}]: already expired", event.id(), event.kind());
            return;
        }
        events.put(key, event);
        forward(event, s -> s.onUpsert(event));
    }

    @Override
    public synchronized void remove(String environmentId, String kind, String id) {
        var removed = events.remove(new Key(environmentId, kind, id));
        if (removed != null) {
            forward(removed, s -> s.onRemove(removed));
        }
    }

    @Override
    public synchronized List<GenericEvent> commit() {
        var now = clock.instant();
        var expired = new ArrayList<GenericEvent>();
        events
            .values()
            .removeIf(event -> {
                if (event.isExpired(now)) {
                    expired.add(event);
                    return true;
                }
                return false;
            });
        expired.forEach(event -> forward(event, s -> s.onRemove(event)));
        for (var registration : registrations) {
            if (registration.dirty) {
                registration.dirty = false;
                notify(registration, GenericEventSubscriber::onCommit);
            }
        }
        return expired;
    }

    /**
     * Number of events held, expired ones included until the next commit.
     */
    public synchronized int size() {
        return events.size();
    }

    private static boolean isNewer(GenericEvent incoming, GenericEvent existing) {
        return incoming.updatedAt() == null || existing.updatedAt() == null || incoming.updatedAt().isAfter(existing.updatedAt());
    }

    private void forward(GenericEvent event, Consumer<GenericEventSubscriber> call) {
        for (var registration : registrations) {
            if (registration.matches(event)) {
                registration.dirty = true;
                notify(registration, call);
            }
        }
    }

    private static void notify(Registration registration, Consumer<GenericEventSubscriber> call) {
        try {
            call.accept(registration.subscriber);
        } catch (Throwable t) {
            // Throwable, since a plugin whose classloader is closed throws a LinkageError, which would stall the sync.
            log.error("Generic event subscriber of [{}] failed", registration.kindPrefix, t);
        }
    }
}
