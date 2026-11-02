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
package io.gravitee.gateway.reactive.handlers.api.v4.overlay;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactive.core.v4.generic.GenericEvent;
import io.gravitee.gateway.reactive.core.v4.generic.GenericEventObserver;
import io.gravitee.gateway.reactive.core.v4.overlay.RuntimeOverlay;
import io.gravitee.gateway.reactive.core.v4.overlay.RuntimeOverlays;
import java.time.Instant;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/** Brings the {@code runtime-overlay} generic events to {@link RuntimeOverlays}: the only place their payload is read. */
@CustomLog
@RequiredArgsConstructor
public class RuntimeOverlayEventObserver implements GenericEventObserver {

    public static final String KIND = "runtime-overlay";

    private final RuntimeOverlays runtimeOverlays;
    private final ObjectMapper objectMapper;

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public boolean onEvent(final GenericEvent event) {
        if (event.isRemove()) {
            runtimeOverlays.unregister(event.documentId(), "stopped");
            return true;
        }
        try {
            final RuntimeOverlay runtimeOverlay = objectMapper.readValue(event.payload(), RuntimeOverlay.class);
            log.info(
                "📥 Runtime overlay [{}] received: kind [{}], API [{}], until {}, payload {}",
                runtimeOverlay.id(),
                runtimeOverlay.kind(),
                runtimeOverlay.apiId(),
                Instant.ofEpochMilli(runtimeOverlay.endAt()),
                runtimeOverlay.payload()
            );
            runtimeOverlays.register(runtimeOverlay);
            return true;
        } catch (Exception e) {
            // A payload this gateway cannot read will not become readable on the next cycle: mark it done.
            log.warn("Unable to read runtime overlay event [{}], ignored", event.id(), e);
            return true;
        }
    }
}
