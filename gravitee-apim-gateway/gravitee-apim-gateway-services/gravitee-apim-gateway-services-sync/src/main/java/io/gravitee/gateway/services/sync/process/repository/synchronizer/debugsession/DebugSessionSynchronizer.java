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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.debugsession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactive.core.v4.analytics.DebugSessionRegistry;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.DebugSessionEventFetcher;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Applies debug session events to the node's registry.
 *
 * There is nothing to deploy here — a session only raises the analytics detail of
 * an API already running — so this bypasses the deployer machinery and writes
 * straight to the registry.
 *
 * Events are applied oldest first, so when a window carries several changes for
 * the same API the last one wins. Expired sessions need no special case: the
 * registry drops them as it reads them, which also means a node restarting in the
 * middle of a session picks it back up.
 */
@RequiredArgsConstructor
@CustomLog
public class DebugSessionSynchronizer implements RepositorySynchronizer {

    static final String ACTION_CLOSE = "CLOSE";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DebugSessionEventFetcher debugSessionEventFetcher;
    private final DebugSessionRegistry debugSessionRegistry;
    private final ThreadPoolExecutor syncFetcherExecutor;

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        return debugSessionEventFetcher
            .fetchLatest(from, to, environments)
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .flatMap(events ->
                Flowable.fromIterable(events).sorted(Comparator.comparing(Event::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
            )
            .doOnNext(this::apply)
            .count()
            .doOnSuccess(count -> {
                if (count > 0) {
                    log.debug("{} debug session events applied", count);
                }
            })
            .ignoreElement();
    }

    private void apply(final Event event) {
        try {
            final JsonNode payload = MAPPER.readTree(event.getPayload());
            final String apiId = payload.path("apiId").asText(null);
            if (apiId == null) {
                log.warn("Ignoring debug session event {} without an API id", event.getId());
                return;
            }

            if (ACTION_CLOSE.equals(payload.path("action").asText())) {
                debugSessionRegistry.close(apiId);
            } else {
                debugSessionRegistry.open(apiId, payload.path("expiresAt").asLong(), payload.path("samplingPercent").asInt(100));
            }
        } catch (Exception e) {
            // One malformed event must not stop the others from being applied.
            log.warn("Unable to apply debug session event {}", event.getId(), e);
        }
    }

    @Override
    public int order() {
        return Order.DEBUG_SESSION.index();
    }
}
