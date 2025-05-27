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
package io.gravitee.apim.rest.api.automation.spring;

import io.gravitee.apim.infra.spring.UsecaseSpringConfiguration;
import io.gravitee.el.ExpressionLanguageInitializer;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import({ ServiceConfiguration.class, UsecaseSpringConfiguration.class })
@EnableAsync
public class RestAutomationConfiguration {

    @Bean
    public ExpressionLanguageInitializer expressionLanguageInitializer() {
        return new ExpressionLanguageInitializer();
    }
}
