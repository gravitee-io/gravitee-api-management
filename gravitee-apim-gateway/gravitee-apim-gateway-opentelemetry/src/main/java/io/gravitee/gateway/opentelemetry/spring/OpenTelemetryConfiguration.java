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
package io.gravitee.gateway.opentelemetry.spring;

import io.gravitee.common.util.Version;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.opentelemetry.InstrumenterTracerFactory;
import io.gravitee.node.api.opentelemetry.Tracer;
import io.gravitee.node.opentelemetry.OpenTelemetryFactory;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class OpenTelemetryConfiguration {

    @Bean
    public TracingContext gatewayTracer(
        final Node node,
        final io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration openTelemetryConfiguration,
        final OpenTelemetryFactory openTelemetryFactory,
        @Lazy final List<InstrumenterTracerFactory> instrumenterTracerFactories
    ) {
        Tracer tracer = openTelemetryFactory.createTracer(
            node.id(),
            node.application(),
            "GATEWAY",
            Version.RUNTIME_VERSION.MAJOR_VERSION,
            instrumenterTracerFactories
        );
        return new TracingContext(tracer, openTelemetryConfiguration.isTracesEnabled(), openTelemetryConfiguration.isVerboseEnabled());
    }
}
