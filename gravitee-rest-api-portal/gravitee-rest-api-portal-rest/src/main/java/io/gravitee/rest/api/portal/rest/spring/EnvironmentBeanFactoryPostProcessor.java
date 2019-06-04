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
package io.gravitee.rest.api.portal.rest.spring;


import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import io.gravitee.common.util.RelaxedPropertySource;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private static final String[] PROPERTY_PREFIXES = new String[] {"gravitee.", "gravitee_", "GRAVITEE." , "GRAVITEE_"};

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        StandardEnvironment environment = (StandardEnvironment) beanFactory.getBean(Environment.class);

        Map<String, Object> systemEnvironment = environment.getSystemEnvironment();
        Map<String, Object> prefixlessSystemEnvironment = new HashMap<>(systemEnvironment.size());
        systemEnvironment
                .keySet()
                .forEach(key -> {
                    String prefixKey = key;
                    for (String propertyPrefix : PROPERTY_PREFIXES) {
                        if (key.startsWith(propertyPrefix)) {
                            prefixKey = key.substring(propertyPrefix.length());
                            break;
                        }
                    }
                    prefixlessSystemEnvironment.put(prefixKey, systemEnvironment.get(key));
                });
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new RelaxedPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, prefixlessSystemEnvironment));
    }
}