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
package io.gravitee.gamma.rest.core.observability.debug.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.rest.api.model.EventType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Opens and closes debug sessions on an API.
 *
 * A session raises the analytics detail of the API — verbose tracing and payload
 * capture — for a bounded window. It is published as an event rather than pushed
 * to the gateways: APIM synchronizes by polling, which is what lets a gateway the
 * control plane cannot reach (hybrid deployments) pick the session up all the same.
 *
 * Nothing is written to the API definition, so an investigation never modifies the
 * API it is investigating.
 */
@UseCase
@RequiredArgsConstructor
public class ManageDebugSessionUseCase {

    /** Longest window the control plane will ask for. The gateway caps it again. */
    public static final int MAX_TTL_SECONDS = 3_600;
    public static final int DEFAULT_TTL_SECONDS = 600;
    public static final int DEFAULT_SAMPLING_PERCENT = 100;

    private final EventCrudService eventCrudService;

    public Output open(final Input input, final int ttlSeconds, final int samplingPercent) {
        final int ttl = clamp(ttlSeconds, 1, MAX_TTL_SECONDS, DEFAULT_TTL_SECONDS);
        final int sampling = clamp(samplingPercent, 1, 100, DEFAULT_SAMPLING_PERCENT);
        final long expiresAt = System.currentTimeMillis() + ttl * 1_000L;

        publish(input, Map.of("apiId", input.apiId(), "action", "OPEN", "expiresAt", expiresAt, "samplingPercent", sampling));

        return new Output(input.apiId(), expiresAt, sampling);
    }

    public void close(final Input input) {
        publish(input, Map.of("apiId", input.apiId(), "action", "CLOSE"));
    }

    private void publish(final Input input, final Map<String, Object> payload) {
        final var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.API_ID, input.apiId());
        properties.put(Event.EventProperties.USER, input.userId());

        eventCrudService.createEvent(
            input.organizationId(),
            input.environmentId(),
            Set.of(input.environmentId()),
            EventType.DEBUG_SESSION,
            payload,
            properties
        );
    }

    private static int clamp(final int value, final int min, final int max, final int fallback) {
        if (value < min) {
            return fallback;
        }
        return Math.min(value, max);
    }

    public record Input(String organizationId, String environmentId, String apiId, String userId) {}

    public record Output(String apiId, long expiresAt, int samplingPercent) {}
}
