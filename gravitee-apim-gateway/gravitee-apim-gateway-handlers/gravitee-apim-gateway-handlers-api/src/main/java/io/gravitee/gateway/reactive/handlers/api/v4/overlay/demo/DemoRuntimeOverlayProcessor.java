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

import static io.gravitee.gateway.reactive.handlers.api.v4.overlay.demo.DemoRuntimeOverlayContributor.ATTR_INTERNAL_DEMO;

import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;

/** Reads the attribute contributed by the demo kind, logs it and exposes it to the client. Knows nothing about runtime overlays themselves. */
@CustomLog
public class DemoRuntimeOverlayProcessor implements Processor {

    public static final String ID = "processor-runtime-overlay-demo";
    public static final String HEADER_RUNTIME_OVERLAY = "X-Runtime-Overlay";
    private static final DemoRuntimeOverlayProcessor INSTANCE = new DemoRuntimeOverlayProcessor();

    private DemoRuntimeOverlayProcessor() {}

    public static DemoRuntimeOverlayProcessor instance() {
        return INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            final DemoRuntimeOverlayState state = ctx.getInternalAttribute(ATTR_INTERNAL_DEMO);
            if (state != null) {
                final long call = state.calls().incrementAndGet();
                ctx.response().headers().set(HEADER_RUNTIME_OVERLAY, state.runtimeOverlayId() + " #" + call);
                ctx
                    .withLogger(log)
                    .info(
                        "🎭🔥 Runtime overlay [{}] seen on {} {} (call #{}): {}",
                        state.runtimeOverlayId(),
                        ctx.request().method(),
                        ctx.request().path(),
                        call,
                        state.message()
                    );
            }
        });
    }
}
