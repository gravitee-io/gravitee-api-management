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
package io.gravitee.rest.api.repository.plugins;

import io.gravitee.plugin.core.api.*;
import io.gravitee.plugin.core.internal.AnnotationBasedPluginContextConfigurer;
import io.gravitee.repository.Repository;
import io.gravitee.repository.Scope;
import io.gravitee.rest.api.repository.proxy.AbstractProxy;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RepositoryPluginHandler implements PluginHandler, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryPluginHandler.class);

    private static final String PLUGIN_TYPE = "repository";

    @Autowired
    private Environment environment;

    @Autowired
    private PluginContextFactory pluginContextFactory;

    @Autowired
    @Qualifier("pluginClassLoaderFactory")
    private PluginClassLoaderFactory pluginClassLoaderFactory;

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<Scope, Repository> repositories = new HashMap<>();
    private final Map<Scope, String> repositoryTypeByScope = new HashMap<>();
    private final Map<String, Collection<Scope>> scopeByRepositoryType = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        // The gateway need 2 repositories :
        // 1_ Management
        lookForRepositoryType(Scope.MANAGEMENT);
        // 2_ Analytics
        lookForRepositoryType(Scope.ANALYTICS);
    }

    @Override
    public boolean canHandle(Plugin plugin) {
        return PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
    }

    @Override
    public void handle(Plugin plugin) {
        try {
            ClassLoader classloader = pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());

            final Class<?> repositoryClass = classloader.loadClass(plugin.clazz());
            LOGGER.info("Register a new repository: {} [{}]", plugin.id(), plugin.clazz());

            Assert.isAssignable(Repository.class, repositoryClass);

            Repository repository = createInstance((Class<Repository>) repositoryClass);
            Collection<Scope> scopes = scopeByRepositoryType.getOrDefault(repository.type(), Collections.EMPTY_LIST);

            for (Scope scope : scopes) {
                if (!repositories.containsKey(scope)) {
                    boolean loaded = false;
                    int tries = 0;

                    while (!loaded) {
                        if (tries > 0) {
                            // Wait for 5 seconds before giving an other try
                            Thread.sleep(5000);
                        }
                        loaded = loadRepository(scope, repository, plugin);
                        tries++;

                        if (!loaded) {
                            LOGGER.error("Unable to load repository {} for scope {}. Retry in 5 seconds...", scope, plugin.id());
                        }
                    }
                } else {
                    LOGGER.warn("Repository scope {} already loaded by {}", scope, repositories.get(scope));
                }
            }
        } catch (Exception iae) {
            LOGGER.error("Unexpected error while create repository instance", iae);
        }
    }

    private boolean loadRepository(Scope scope, Repository repository, Plugin plugin) {
        LOGGER.info("Repository [{}] loaded by {}", scope, repository.type());

        // Not yet loaded, let's mount the repository in application context
        try {
            ApplicationContext repoApplicationContext = pluginContextFactory.create(
                new AnnotationBasedPluginContextConfigurer(plugin) {
                    @Override
                    public Set<Class<?>> configurations() {
                        return Collections.singleton(repository.configuration(scope));
                    }
                }
            );

            registerRepositoryDefinitions(repository, repoApplicationContext);
            repositories.put(scope, repository);
            return true;
        } catch (Exception iae) {
            LOGGER.error("Unexpected error while creating context for repository instance", iae);
            pluginContextFactory.remove(plugin);

            return false;
        }
    }

    private void registerRepositoryDefinitions(Repository repository, ApplicationContext repoApplicationContext) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) (
            (ConfigurableApplicationContext) applicationContext
        ).getBeanFactory();

        String[] beanNames = repoApplicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object repositoryClassInstance = repoApplicationContext.getBean(beanName);
            Class<?> repositoryObjectClass = repositoryClassInstance.getClass();
            if (
                (beanName.endsWith("Repository") || (beanName.endsWith("Manager") && !beanName.endsWith("TransactionManager"))) &&
                !repository.getClass().equals(repositoryClassInstance.getClass())
            ) {
                if (repositoryObjectClass.getInterfaces().length > 0) {
                    Class<?> repositoryItfClass = repositoryObjectClass.getInterfaces()[0];
                    LOGGER.debug("Set proxy target for {} [{}]", beanName, repositoryItfClass);
                    try {
                        Object proxyRepository = beanFactory.getBean(repositoryItfClass);
                        if (proxyRepository instanceof AbstractProxy) {
                            AbstractProxy proxy = (AbstractProxy) proxyRepository;
                            proxy.setTarget(repositoryClassInstance);
                        }
                    } catch (NoSuchBeanDefinitionException nsbde) {
                        LOGGER.debug("Unable to proxify {} [{}]", beanName, repositoryItfClass);
                    }
                }
            } else if (beanName.endsWith("TransactionManager")) {
                beanFactory.registerSingleton(beanName, repositoryClassInstance);
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
        Collection<Scope> scopes = scopeByRepositoryType.getOrDefault(repositoryType, new ArrayList<>());
        scopes.add(scope);
        scopeByRepositoryType.put(repositoryType, scopes);
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
