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
package io.gravitee.gateway.opentelemetry;

import io.gravitee.common.service.AbstractService;
import io.gravitee.common.service.Service;
import io.gravitee.node.api.opentelemetry.Tracer;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class TracingContext extends AbstractService<TracingContext> {

    private final Tracer tracer;
    private final boolean enabled;
    private final boolean verbose;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (tracer != null) {
            tracer.start();
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (tracer != null) {
            tracer.stop();
        }
    }

    public Tracer opentelemetryTracer() {
        return tracer;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isVerbose() {
        return enabled && verbose;
    }

    public static TracingContext noop() {
        return new TracingContext(new NoOpTracer(), false, false);
    }
}
