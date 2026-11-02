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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.gravitee.gamma.rest.core.runtime_overlay.model.RuntimeOverlay;
import io.gravitee.gamma.rest.core.runtime_overlay.port.service_provider.RuntimeOverlayEventPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListRuntimeOverlaysUseCaseTest {

    private static final long NOW = 10_000_000L;

    private final List<Map<String, Object>> events = new ArrayList<>();
    private final RuntimeOverlayEventPort port = new RuntimeOverlayEventPort() {
        @Override
        public String publish(
            String organizationId,
            String environmentId,
            String apiId,
            String userId,
            String runtimeOverlayId,
            String action,
            Map<String, Object> payload
        ) {
            events.add(payload);
            return "event-" + events.size();
        }

        @Override
        public List<Map<String, Object>> payloads(String environmentId, long from, long to) {
            return events;
        }
    };
    private final StartRuntimeOverlayUseCase start = new StartRuntimeOverlayUseCase(port);
    private final StopRuntimeOverlayUseCase stop = new StopRuntimeOverlayUseCase(port);
    private final ListRuntimeOverlaysUseCase list = new ListRuntimeOverlaysUseCase(port);

    @Test
    void should_fold_the_events_of_each_runtime_overlay_into_its_status() {
        // Given
        var stopped = start.execute(input("api-1", 60, NOW - 30_000)).runtimeOverlay();
        stop.execute(new StopRuntimeOverlayUseCase.Input("org", "env", "user", "api-1", (String) stopped.get("id")));
        start.execute(input("api-2", 60, NOW - 120_000));
        var active = start.execute(input("api-3", 60, NOW - 10_000)).runtimeOverlay();

        // When
        var overlays = list.execute(new ListRuntimeOverlaysUseCase.Input("env", NOW)).runtimeOverlays();

        // Then
        assertThat(overlays)
            .extracting(RuntimeOverlay::apiId, overlay -> overlay.statusAt(NOW))
            .containsExactly(
                tuple("api-3", RuntimeOverlay.Status.ACTIVE),
                tuple("api-1", RuntimeOverlay.Status.STOPPED),
                tuple("api-2", RuntimeOverlay.Status.EXPIRED)
            );
        assertThat(overlays.getFirst().id()).isEqualTo(active.get("id"));
    }

    @Test
    void should_cap_the_duration_of_a_runtime_overlay() {
        // When
        var output = start.execute(input("api-1", 999_999, NOW));

        // Then
        assertThat(output.durationSeconds()).isEqualTo(StartRuntimeOverlayUseCase.MAX_DURATION_SECONDS);
        assertThat(output.runtimeOverlay()).containsEntry("endAt", NOW + 3_600_000L);
    }

    private static StartRuntimeOverlayUseCase.Input input(String apiId, int durationSeconds, long now) {
        return new StartRuntimeOverlayUseCase.Input("org", "env", "user", apiId, "demo", durationSeconds, Map.of("message", "hi"), now);
    }
}
