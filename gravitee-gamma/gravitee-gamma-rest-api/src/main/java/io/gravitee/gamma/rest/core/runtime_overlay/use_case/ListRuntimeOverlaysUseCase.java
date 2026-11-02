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
import io.gravitee.gamma.rest.core.runtime_overlay.model.RuntimeOverlay;
import io.gravitee.gamma.rest.core.runtime_overlay.port.service_provider.RuntimeOverlayEventPort;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/** The runtime overlays started in the environment during the last hour, folded from their events. */
@UseCase
@RequiredArgsConstructor
public class ListRuntimeOverlaysUseCase {

    private static final long LOOK_BACK_MS = 3_600_000L;

    private final RuntimeOverlayEventPort runtimeOverlayEventPort;

    public record Input(String environmentId, long now) {}

    public record Output(List<RuntimeOverlay> runtimeOverlays) {}

    public Output execute(final Input input) {
        final Map<String, RuntimeOverlay> byId = new LinkedHashMap<>();
        runtimeOverlayEventPort
            .payloads(input.environmentId(), input.now() - LOOK_BACK_MS, input.now())
            .forEach(payload -> {
                final String id = (String) payload.get("id");
                if ("STOPPED".equals(payload.get("status"))) {
                    byId.computeIfPresent(id, (key, overlay) -> overlay.stop());
                } else {
                    byId.put(id, toRuntimeOverlay(payload));
                }
            });

        return new Output(byId.values().stream().sorted(Comparator.comparingLong(RuntimeOverlay::startAt).reversed()).toList());
    }

    @SuppressWarnings("unchecked")
    private static RuntimeOverlay toRuntimeOverlay(final Map<String, Object> payload) {
        return new RuntimeOverlay(
            (String) payload.get("id"),
            (String) payload.get("kind"),
            (String) payload.get("apiId"),
            ((Number) payload.get("startAt")).longValue(),
            ((Number) payload.get("endAt")).longValue(),
            false,
            (Map<String, Object>) payload.getOrDefault("payload", Map.of())
        );
    }
}
