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
package io.gravitee.gateway.services.healthcheck.spring;

import io.gravitee.el.TemplateVariableProviderFactory;
import io.gravitee.gateway.services.healthcheck.EndpointHealthcheckResolver;
import io.gravitee.gateway.services.healthcheck.context.HealthCheckContextFactory;
import io.gravitee.gateway.services.healthcheck.context.HealthCheckTemplateVariableProviderFactory;
import io.gravitee.gateway.services.healthcheck.reporter.StatusReporter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class EndpointHealthcheckConfiguration {

    @Bean
    public EndpointHealthcheckResolver endpointHealthcheckResolver() {
        return new EndpointHealthcheckResolver();
    }

    @Bean
    public StatusReporter statusReporter() {
        return new StatusReporter();
    }

    @Bean
    public TemplateVariableProviderFactory templateVariableProviderFactory(ApplicationContext applicationContext) {
        return new HealthCheckTemplateVariableProviderFactory(applicationContext);
    }

    @Bean
    public HealthCheckContextFactory healthCheckContextFactory() {
        return new HealthCheckContextFactory();
    }
}
