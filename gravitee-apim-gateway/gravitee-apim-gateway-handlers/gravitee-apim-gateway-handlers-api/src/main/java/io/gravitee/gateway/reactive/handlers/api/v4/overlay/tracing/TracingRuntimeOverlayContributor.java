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
package io.gravitee.gateway.reactive.handlers.api.v4.overlay.tracing;

import io.gravitee.gateway.reactive.core.v4.overlay.RuntimeOverlay;
import io.gravitee.gateway.reactive.core.v4.overlay.RuntimeOverlayContribution;
import io.gravitee.gateway.reactive.core.v4.overlay.RuntimeOverlayContributor;
import io.gravitee.node.api.opentelemetry.sampling.SamplingConfig;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;

/**
 * Replaces the gateway's tracing sampling for the targeted API while the runtime overlay is active. Payload, every
 * field optional: {@code ratio}, {@code respectRemoteParent}, {@code methods}, {@code pathPrefixes},
 * {@code keepStatuses}, {@code keepErrors}.
 */
@CustomLog
public class TracingRuntimeOverlayContributor implements RuntimeOverlayContributor {

    public static final String KIND = "tracing";

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public RuntimeOverlayContribution contribute(final RuntimeOverlay runtimeOverlay) {
        final Map<String, Object> payload = runtimeOverlay.payload() == null ? Map.of() : runtimeOverlay.payload();
        final SamplingConfig sampling = new SamplingConfig(
            payload.get("ratio") instanceof Number ratio ? ratio.doubleValue() : 1.0,
            !(payload.get("respectRemoteParent") instanceof Boolean respect) || respect,
            Set.copyOf(strings(payload.get("methods"))),
            strings(payload.get("pathPrefixes")),
            strings(payload.get("keepStatuses")).stream().map(Integer::valueOf).collect(Collectors.toSet()),
            Boolean.TRUE.equals(payload.get("keepErrors"))
        );
        log.info("🔭 Tracing contributor: API [{}] now samples with {}", runtimeOverlay.apiId(), sampling);
        return RuntimeOverlayContribution.ofSampling(sampling);
    }

    @Override
    public void release(final RuntimeOverlay runtimeOverlay) {
        log.info("🔭 Tracing contributor: API [{}] is back to the gateway sampling", runtimeOverlay.apiId());
    }

    private static List<String> strings(final Object value) {
        if (value instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
