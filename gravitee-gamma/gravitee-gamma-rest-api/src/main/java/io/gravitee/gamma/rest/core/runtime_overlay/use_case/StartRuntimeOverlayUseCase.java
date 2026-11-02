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
package io.gravitee.gamma.rest.core.runtime_overlay.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.runtime_overlay.port.service_provider.RuntimeOverlayEventPort;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Publishes a runtime overlay: gateways pick it up through their sync and change the behavior of the API until its
 * end, without any redeploy. Nothing is written to the API definition.
 */
@UseCase
@RequiredArgsConstructor
public class StartRuntimeOverlayUseCase {

    public static final int MAX_DURATION_SECONDS = 3_600;
    public static final int DEFAULT_DURATION_SECONDS = 60;

    private final RuntimeOverlayEventPort runtimeOverlayEventPort;

    public record Input(
        String organizationId,
        String environmentId,
        String userId,
        String apiId,
        String kind,
        Integer durationSeconds,
        Map<String, Object> payload,
        long now
    ) {}

    public record Output(String eventId, int durationSeconds, Map<String, Object> runtimeOverlay) {}

    public Output execute(final Input input) {
        final int duration = input.durationSeconds() == null || input.durationSeconds() < 1
            ? DEFAULT_DURATION_SECONDS
            : Math.min(input.durationSeconds(), MAX_DURATION_SECONDS);

        final Map<String, Object> runtimeOverlay = new LinkedHashMap<>();
        runtimeOverlay.put("id", UUID.randomUUID().toString());
        runtimeOverlay.put("kind", input.kind());
        runtimeOverlay.put("apiId", input.apiId());
        runtimeOverlay.put("startAt", input.now());
        runtimeOverlay.put("endAt", input.now() + duration * 1_000L);
        runtimeOverlay.put("status", "ACTIVE");
        runtimeOverlay.put("payload", input.payload() == null ? Map.of() : input.payload());

        final String eventId = runtimeOverlayEventPort.publish(
            input.organizationId(),
            input.environmentId(),
            input.apiId(),
            input.userId(),
            (String) runtimeOverlay.get("id"),
            RuntimeOverlayEventPort.ACTION_APPLY,
            runtimeOverlay
        );
        return new Output(eventId, duration, runtimeOverlay);
    }
}
