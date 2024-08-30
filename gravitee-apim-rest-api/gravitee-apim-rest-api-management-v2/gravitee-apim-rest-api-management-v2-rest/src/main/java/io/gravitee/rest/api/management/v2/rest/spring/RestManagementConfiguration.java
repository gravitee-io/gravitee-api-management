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
package io.gravitee.rest.api.management.v2.rest.spring;

import io.gravitee.apim.infra.spring.UsecaseSpringConfiguration;
import io.gravitee.el.ExpressionLanguageInitializer;
import io.gravitee.plugin.core.spring.PluginConfiguration;
import io.gravitee.rest.api.idp.core.spring.IdentityProviderPluginConfiguration;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import(
    { PluginConfiguration.class, ServiceConfiguration.class, IdentityProviderPluginConfiguration.class, UsecaseSpringConfiguration.class }
)
@EnableAsync
public class RestManagementConfiguration {

    @Bean
    public ExpressionLanguageInitializer expressionLanguageInitializer() {
        return new ExpressionLanguageInitializer();
    }
}
