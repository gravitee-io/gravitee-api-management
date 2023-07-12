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
package io.gravitee.repository.mongodb.common;

import io.gravitee.repository.mongodb.management.mapper.GraviteeDozerMapper;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import io.gravitee.repository.mongodb.management.transaction.NoTransactionManager;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractRepositoryConfiguration extends AbstractMongoClientConfiguration implements ApplicationContextAware {

    protected final Environment environment;

    protected AbstractRepositoryConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        final ConfigurableListableBeanFactory beanFactory = getConfigurableApplicationContext(applicationContext).getBeanFactory();
        beanFactory.registerSingleton("graviteeTransactionManager", new NoTransactionManager());
    }

    private ConfigurableApplicationContext getConfigurableApplicationContext(ApplicationContext applicationContext) {
        return (ConfigurableApplicationContext) Optional.ofNullable(applicationContext.getParent()).orElse(applicationContext);
    }

    @Override
    protected String getDatabaseName() {
        String uri = environment.getProperty("management.mongodb.uri");
        if (uri != null && !uri.isEmpty()) {
            // Remove user:password from the URI as it can contain special characters and isn't needed for the database name
            String uriWithoutCredentials = uri.replaceAll("://.*@", "://");
            return URI.create(uriWithoutCredentials).getPath().substring(1);
        }

        return environment.getProperty("management.mongodb.dbname", "gravitee");
    }

    @Bean
    public GraviteeMapper graviteeMapper() {
        return new GraviteeDozerMapper();
    }

    @Override
    protected Set<Class<?>> getInitialEntitySet() throws ClassNotFoundException {
        Collection<String> basePackages = getMappingBasePackages();
        Set<Class<?>> initialEntitySet = new HashSet<>();

        for (String basePackage : basePackages) {
            if (StringUtils.hasText(basePackage)) {
                ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(false);
                componentProvider.addIncludeFilter(new AnnotationTypeFilter(Document.class));
                componentProvider.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));

                for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {
                    Class<?> entity = ClassUtils.forName(candidate.getBeanClassName(), this.getClass().getClassLoader());
                    initialEntitySet.add(entity);
                }
            }
        }
        return initialEntitySet;
    }
}
