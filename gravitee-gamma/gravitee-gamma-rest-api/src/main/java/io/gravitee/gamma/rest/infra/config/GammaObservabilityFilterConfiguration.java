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

import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for the shared observability filter engine. The registry discovers
 * {@link io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterContributor}
 * implementations via {@link java.util.ServiceLoader} at construction — runs once when the rest-api
 * context starts.
 *
 * @author GraviteeSource Team
 */
@Configuration
public class GammaObservabilityFilterConfiguration {

    @Bean
    public FilterRegistry gammaFilterRegistry() {
        return new SpiFilterRegistry();
    }
}
