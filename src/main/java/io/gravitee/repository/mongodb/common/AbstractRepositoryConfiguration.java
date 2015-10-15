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
package io.gravitee.repository.mongodb.common;

import io.gravitee.repository.mongodb.management.mapper.GraviteeDozerMapper;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import io.gravitee.repository.mongodb.management.transaction.NoTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractRepositoryConfiguration extends AbstractMongoConfiguration {

    @Autowired
    private Environment environment;

    @Override
    protected String getMappingBasePackage() {
        return getClass().getPackage().getName();
    }

    @Override
    protected String getDatabaseName() {
        return environment.getProperty("management.mongodb.dbname", "gravitee");
    }

    @Bean
    public GraviteeMapper graviteeMapper() {
        return new GraviteeDozerMapper();
    }


    protected Set<Class<?>> getInitialEntitySet() throws ClassNotFoundException {

        String basePackage = getMappingBasePackage();
        Set<Class<?>> initialEntitySet = new HashSet<Class<?>>();

        if (StringUtils.hasText(basePackage)) {
            ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
                    false);
            componentProvider.addIncludeFilter(new AnnotationTypeFilter(Document.class));
            componentProvider.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));

            for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {
                initialEntitySet.add(ClassUtils.forName(candidate.getBeanClassName(),
                        this.getClass().getClassLoader()));
            }
        }

        return initialEntitySet;
    }

    @Bean
    public AbstractPlatformTransactionManager graviteeTransactionManager() {
        return new NoTransactionManager();
    }
}
