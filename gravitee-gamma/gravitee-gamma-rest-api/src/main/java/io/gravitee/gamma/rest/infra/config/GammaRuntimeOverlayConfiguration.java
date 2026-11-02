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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.gamma.rest.core.runtime_overlay.port.service_provider.RuntimeOverlayEventPort;
import io.gravitee.gamma.rest.infra.adapter.RuntimeOverlayEventPortAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
    basePackages = "io.gravitee.gamma.rest.core.runtime_overlay",
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class)
)
public class GammaRuntimeOverlayConfiguration {

    @Bean
    public RuntimeOverlayEventPort runtimeOverlayEventPort(
        EventCrudService eventCrudService,
        EventQueryService eventQueryService,
        ObjectMapper objectMapper
    ) {
        return new RuntimeOverlayEventPortAdapter(eventCrudService, eventQueryService, objectMapper);
    }
}
