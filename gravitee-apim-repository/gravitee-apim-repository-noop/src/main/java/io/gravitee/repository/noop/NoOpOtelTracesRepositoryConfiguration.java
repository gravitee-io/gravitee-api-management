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
package io.gravitee.repository.noop;

import io.gravitee.repository.noop.tracing.NoOpTracingRepository;
import io.gravitee.repository.tracing.api.TracingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loaded for the {@code OTEL_TRACES} scope when {@code repositories.otel-traces.type=none} — exposes
 * a no-op {@link TracingRepository} bean so consumers (gamma APIM trace resource, future use cases)
 * can rely on the SPI being present in the context without each implementing their own NoOp fallback.
 *
 * @author GraviteeSource Team
 */
@Configuration
public class NoOpOtelTracesRepositoryConfiguration {

    @Bean
    public TracingRepository tracingRepository() {
        return new NoOpTracingRepository();
    }
}
