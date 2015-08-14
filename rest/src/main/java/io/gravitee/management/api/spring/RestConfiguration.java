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
package io.gravitee.management.api.spring;

import io.gravitee.management.api.repository.RepositoryConfiguration;
import io.gravitee.management.api.service.ServiceConfiguration;
import io.gravitee.management.security.config.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.IOException;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
@Import({PropertiesConfiguration.class, RepositoryConfiguration.class, ServiceConfiguration.class, SecurityConfig.class})
public class RestConfiguration {

    protected final static Logger LOGGER = LoggerFactory.getLogger(RestConfiguration.class);

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() throws IOException {
        LOGGER.info("Loading Gravitee Management placeholder.");

        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setProperties(PropertiesConfiguration.graviteeProperties());

        LOGGER.info("Loading Gravitee Management placeholder. DONE");

        return propertySourcesPlaceholderConfigurer;
    }
}
