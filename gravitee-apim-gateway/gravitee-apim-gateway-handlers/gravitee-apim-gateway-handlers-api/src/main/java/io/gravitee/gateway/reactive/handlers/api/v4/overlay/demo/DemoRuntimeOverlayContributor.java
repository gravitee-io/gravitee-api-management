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
package io.gravitee.gateway.reactive.handlers.api.v4.overlay.demo;

import io.gravitee.gateway.reactive.core.v4.overlay.RuntimeOverlay;
import io.gravitee.gateway.reactive.core.v4.overlay.RuntimeOverlayContribution;
import io.gravitee.gateway.reactive.core.v4.overlay.RuntimeOverlayContributor;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;

/** Proof of concept kind: tags every request of the targeted API with an internal attribute. */
@CustomLog
public class DemoRuntimeOverlayContributor implements RuntimeOverlayContributor {

    public static final String KIND = "demo";
    public static final String ATTR_INTERNAL_DEMO = "runtime-overlay.demo";
    private static final String DEFAULT_MESSAGE = "Hello from a runtime overlay";

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public RuntimeOverlayContribution contribute(final RuntimeOverlay runtimeOverlay) {
        final Object message = runtimeOverlay.payload() == null ? null : runtimeOverlay.payload().get("message");
        final DemoRuntimeOverlayState state = new DemoRuntimeOverlayState(
            runtimeOverlay.id(),
            message == null ? DEFAULT_MESSAGE : message.toString(),
            new AtomicLong()
        );
        log.info(
            "🎭 Demo contributor: API [{}] now carries internal attribute [{}] with message \"{}\"",
            runtimeOverlay.apiId(),
            ATTR_INTERNAL_DEMO,
            state.message()
        );
        return RuntimeOverlayContribution.ofInternalAttributes(Map.of(ATTR_INTERNAL_DEMO, state));
    }

    @Override
    public void release(final RuntimeOverlay runtimeOverlay) {
        log.info("🎭 Demo contributor: API [{}] no longer carries [{}]", runtimeOverlay.apiId(), ATTR_INTERNAL_DEMO);
    }
}
