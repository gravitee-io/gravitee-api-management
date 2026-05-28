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
package io.gravitee.gamma.rest.infra.config;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.OtelLogPort;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterRegistry;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TracingPort;
import io.gravitee.gamma.rest.infra.adapter.OtelLogPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.SpiTraceFilterRegistry;
import io.gravitee.gamma.rest.infra.adapter.TracingPortAdapter;
import io.gravitee.repository.otel.log.api.OtelLogRepository;
import io.gravitee.repository.tracing.api.TracingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Lazy;

/**
 * Spring wiring for the gamma trace explorer domain.
 * <p>
 * Use cases are picked up by {@link ComponentScan} filtered on {@link UseCase} — the apim-side
 * {@code UsecaseSpringConfiguration} only scans {@code io.gravitee.apim.core}, so per-domain configs
 * in this module have to opt their own package in. Adapters are wired via explicit {@code @Bean}s
 * because they're plain POJOs (no Spring stereotype).
 * <p>
 * The {@code TracingRepository} / {@code OtelLogRepository} SPI beans are loaded by the platform's
 * repository plugin handler — either the real Elasticsearch impl when the operator configured
 * {@code repositories.otel-traces.type=elasticsearch} / {@code repositories.otel-logs.type=elasticsearch},
 * or the no-op fallback from {@code gravitee-apim-repository-noop} when the type is set to
 * {@code none}.
 * <p>
 * The {@code @Lazy} on the factory-method parameters is load-bearing: the repository plugin handler
 * registers its beans <em>after</em> the rest-api Spring context refresh completes, so eager
 * resolution at @Bean processing time blows up with {@code NoSuchBeanDefinitionException}. The
 * proxies created by {@code @Lazy} defer the lookup until the first method call (which happens when
 * the first HTTP request hits the trace explorer, well after plugins are up). Mirrors the pattern
 * used by {@code LogsServiceImpl} in the rest-api service layer.
 *
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan(
    basePackages = "io.gravitee.gamma.rest.core.tracing",
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class)
)
public class GammaTracingConfiguration {

    @Bean
    public TracingPort gammaTracingPort(@Lazy TracingRepository tracingRepository) {
        return new TracingPortAdapter(tracingRepository);
    }

    @Bean
    public OtelLogPort gammaOtelLogPort(@Lazy OtelLogRepository otelLogRepository) {
        return new OtelLogPortAdapter(otelLogRepository);
    }

    /**
     * Filter registry adapter discovers contributors via {@link java.util.ServiceLoader} at
     * construction — runs once when the rest-api context starts, captures every module's bundled
     * {@code META-INF/services/...TraceFilterContributor} entry. Singleton like the other adapters
     * because the SPI scan is pure boot-time work with no per-request state.
     */
    @Bean
    public TraceFilterRegistry gammaTraceFilterRegistry() {
        return new SpiTraceFilterRegistry();
    }
}
