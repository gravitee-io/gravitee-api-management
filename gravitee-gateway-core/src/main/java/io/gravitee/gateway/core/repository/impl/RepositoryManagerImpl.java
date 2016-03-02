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
package io.gravitee.gateway.core.repository.impl;

import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginContextFactory;
import io.gravitee.plugin.core.api.PluginHandler;
import io.gravitee.plugin.core.api.PluginType;
import io.gravitee.repository.Repository;
import io.gravitee.repository.Scope;
import io.gravitee.repository.management.api.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class RepositoryManagerImpl implements PluginHandler, InitializingBean {

    private final static Logger LOGGER = LoggerFactory.getLogger(RepositoryManagerImpl.class);

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PluginContextFactory pluginContextFactory;

    private final Map<Scope, Repository> repositories = new HashMap<>();
    private final Map<Scope, String> repositoryTypeByScope = new HashMap<>();

    public void afterPropertiesSet() throws Exception {
        // The gateway need 2 repositories :
        // 1_ Management
        lookForRepositoryType(Scope.MANAGEMENT);
        // 2_ Rate limit
        lookForRepositoryType(Scope.RATE_LIMIT);
    }

    @Override
    public boolean canHandle(Plugin plugin) {
        return plugin.type() == PluginType.REPOSITORY;
    }

    @Override
    public void handle(Plugin plugin) {
        try {
            final Class<?> repositoryClass = plugin.clazz();
            Assert.isAssignable(Repository.class, repositoryClass);

            Repository repository = createInstance((Class<Repository>) repositoryClass);
            for(Scope scope : repository.scopes()) {
                if (! repositories.containsKey(scope)) {
                    // Not yet loaded, let's mount the repository in application context
                    try {
                        ApplicationContext repoApplicationContext = pluginContextFactory.create(
                                plugin1 -> Collections.singleton(repository.configuration(scope)),
                                plugin);

                        registerRepositoryDefinitions(repository, repoApplicationContext);
                        repositories.put(scope, repository);
                    } catch (Exception iae) {
                        LOGGER.error("Unexpected error while creating context for repository instance", iae);
                        pluginContextFactory.remove(plugin);
                    }

                } else {
                    LOGGER.warn("Repository scope {} already loaded by {}", scope,
                            repositories.get(scope));
                }
            }
        } catch (Exception iae) {
            LOGGER.error("Unexpected error while create repository instance", iae);
        }
    }

    private void registerRepositoryDefinitions(Repository repository, ApplicationContext repoApplicationContext) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)
                ((ConfigurableApplicationContext) applicationContext).getBeanFactory();

        String [] beanNames = repoApplicationContext.getBeanDefinitionNames();
        for(String beanName : beanNames) {
            Object repositoryClassInstance = repoApplicationContext.getBean(beanName);
            if (beanName.endsWith("Repository") && ! repository.getClass().equals(repositoryClassInstance.getClass())) {
                Class<?> repositoryClass = repositoryClassInstance.getClass().getInterfaces()[0];
                LOGGER.info("Register {} [{}] in gateway context", beanName, repositoryClass);
                beanFactory.registerSingleton(repositoryClass.getName(),
                        repositoryClassInstance);

                /*
                beanFactory.registerResolvableDependency(
                        repositoryClass, repositoryClassInstance);
                        */
                //);registerSingleton(beanName, repositoryClassInstance);
            }
        }
    }

    private String lookForRepositoryType(Scope scope) throws Exception {
        String repositoryType = environment.getProperty(scope.getName() + ".type");
        LOGGER.info("Loading repository for scope {}: {}", scope, repositoryType);

        if (repositoryType == null || repositoryType.isEmpty()) {
            LOGGER.error("No repository type defined in configuration for {}", scope.getName());
            throw new IllegalStateException("No repository type defined in configuration for " + scope.getName());
        }

        repositoryTypeByScope.put(scope, repositoryType);
        return repositoryType;
    }

    private <T> T createInstance(Class<T> clazz) throws Exception {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            LOGGER.error("Unable to instantiate class: {}", ex);
            throw ex;
        }
    }
}
