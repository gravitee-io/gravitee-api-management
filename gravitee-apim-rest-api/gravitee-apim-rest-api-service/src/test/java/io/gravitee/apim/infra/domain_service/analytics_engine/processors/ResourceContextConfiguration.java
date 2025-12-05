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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import static org.mockito.Mockito.mock;

import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;

@Configuration
public class ResourceContextConfiguration {

    @Bean
    public ApiAuthorizationService apiAuthorizationService() {
        return mock(ApiAuthorizationService.class);
    }

    @Bean
    public Authentication authentication() {
        return mock(Authentication.class);
    }

    @Bean
    public ApiSearchService apiSearchService() {
        return mock(ApiSearchService.class);
    }

    @Bean
    public ApplicationService applicationSearchService() {
        return mock(ApplicationService.class);
    }

    @Bean
    public PermissionsPreprocessorImpl permissionsPreprocessor(ApiSearchService apiSearchService) {
        return new PermissionsPreprocessorImpl(apiSearchService);
    }

    @Bean
    public NamesPostprocessorImpl namesPostProcessorImpl(ApplicationService applicationSearchService) {
        return new NamesPostprocessorImpl(applicationSearchService);
    }
}
