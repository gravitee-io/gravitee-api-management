/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.spring;

import io.gravitee.el.ExpressionLanguageInitializer;
import io.gravitee.plugin.core.spring.PluginConfiguration;
import io.gravitee.rest.api.idp.core.spring.IdentityProviderPluginConfiguration;
import io.gravitee.rest.api.management.security.SecurityManagementConfiguration;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;

import java.util.Properties;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import({
        PropertiesConfiguration.class, PluginConfiguration.class, ServiceConfiguration.class,
        SecurityManagementConfiguration.class, IdentityProviderPluginConfiguration.class
})
public class RestManagementConfiguration {

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties(@Qualifier("graviteeProperties") Properties graviteeProperties) {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setProperties(graviteeProperties);
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);

        return propertySourcesPlaceholderConfigurer;
    }

    @Bean
    public static EnvironmentBeanFactoryPostProcessor environmentBeanFactoryPostProcessor() {
        return new EnvironmentBeanFactoryPostProcessor();
    }

    @Bean
    public static PropertySourceBeanProcessor propertySourceBeanProcessor(@Qualifier("graviteeProperties") Properties graviteeProperties,
                                                                          Environment environment) {
        // Using this we are now able to use {@link org.springframework.core.env.Environment} in Spring beans
        return new PropertySourceBeanProcessor(graviteeProperties, environment);
    }

    @Bean
    public ExpressionLanguageInitializer expressionLanguageInitializer() {

        return new ExpressionLanguageInitializer();
    }
}
