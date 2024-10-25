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
package io.gravitee.gateway.reactive.handlers.api.v4;

import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.core.context.DefaultDeploymentContext;
import io.gravitee.gateway.reactive.handlers.api.el.ApiTemplateVariableProvider;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactive.reactor.ApiReactor;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.reactor.handler.context.ApiTemplateVariableProviderFactory;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.ResourceConfigurationFactoryImpl;
import io.gravitee.gateway.resource.internal.v4.DefaultResourceManager;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.resource.api.ResourceManager;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;

@Slf4j
public abstract class AbstractReactorFactory<T extends ReactableApi<? extends AbstractApi>> implements ReactorFactory<T> {

    protected final ApplicationContext applicationContext;
    protected final PolicyFactoryManager policyFactoryManager;
    protected final Configuration configuration;

    protected AbstractReactorFactory(
        ApplicationContext applicationContext,
        PolicyFactoryManager policyFactoryManager,
        Configuration configuration
    ) {
        this.applicationContext = applicationContext;
        this.policyFactoryManager = policyFactoryManager;
        this.configuration = configuration;
    }

    @Override
    public abstract boolean support(Class<? extends Reactable> clazz);

    @Override
    public abstract boolean canCreate(T reactableApi);

    protected abstract void addExtraComponents(
        CustomComponentProvider componentProvider,
        T reactableApi,
        DefaultDeploymentContext deploymentContext
    );

    protected abstract PolicyManager getPolicyManager(
        DefaultClassLoader classLoader,
        T reactableApi,
        PolicyFactoryManager factoryManager,
        PolicyConfigurationFactory policyConfigurationFactory,
        ConfigurablePluginManager<PolicyPlugin<?>> ppm,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider
    );

    protected abstract ApiReactor<T> buildApiReactor(
        T reactableApi,
        CompositeComponentProvider componentProvider,
        PolicyManager policyManager,
        DefaultDeploymentContext deploymentContext,
        ResourceLifecycleManager resourceLifecycleManager
    );

    @Override
    public ApiReactor<T> create(final T reactableApi) {
        try {
            if (reactableApi.isEnabled()) {
                log.info("Creating Reactor Handler for api {}", reactableApi.getId());

                final ComponentProvider globalComponentProvider = applicationContext.getBean(ComponentProvider.class);
                final CustomComponentProvider customComponentProvider = new CustomComponentProvider();
                customComponentProvider.add(ReactableApi.class, reactableApi);

                final CompositeComponentProvider componentProvider = new CompositeComponentProvider(
                    customComponentProvider,
                    globalComponentProvider
                );

                final DefaultDeploymentContext deploymentContext = new DefaultDeploymentContext();
                deploymentContext.componentProvider(componentProvider);
                deploymentContext.templateVariableProviders(commonTemplateVariableProviders(reactableApi));

                final ResourceLifecycleManager resourceLifecycleManager = resourceLifecycleManager(
                    reactableApi,
                    applicationContext.getBean(ResourceClassLoaderFactory.class),
                    new ResourceConfigurationFactoryImpl(),
                    applicationContext,
                    deploymentContext
                );
                customComponentProvider.add(ResourceManager.class, resourceLifecycleManager);

                String[] beanNamesForType = getBeanNamesForType(
                    ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
                );

                ConfigurablePluginManager<PolicyPlugin<?>> ppm = (ConfigurablePluginManager<PolicyPlugin<?>>) applicationContext.getBean(
                    beanNamesForType[0]
                );

                final PolicyManager policyManager = getPolicyManager(
                    applicationContext.getBean(DefaultClassLoader.class),
                    reactableApi,
                    policyFactoryManager,
                    new CachedPolicyConfigurationFactory(),
                    ppm,
                    applicationContext.getBean(PolicyClassLoaderFactory.class),
                    componentProvider
                );

                addExtraComponents(customComponentProvider, reactableApi, deploymentContext);

                return buildApiReactor(reactableApi, componentProvider, policyManager, deploymentContext, resourceLifecycleManager);
            }
        } catch (Exception ex) {
            log.error("Unexpected error while creating AsyncApiReactor", ex);
        }
        return null;
    }

    protected List<TemplateVariableProvider> commonTemplateVariableProviders(T reactableApi) {
        final List<TemplateVariableProvider> templateVariableProviders = new ArrayList<>();
        templateVariableProviders.add(new ApiTemplateVariableProvider(reactableApi));
        templateVariableProviders.addAll(
            applicationContext.getBean(ApiTemplateVariableProviderFactory.class).getTemplateVariableProviders()
        );

        return templateVariableProviders;
    }

    @SuppressWarnings("unchecked")
    private ResourceLifecycleManager resourceLifecycleManager(
        Reactable reactable,
        ResourceClassLoaderFactory resourceClassLoaderFactory,
        ResourceConfigurationFactory resourceConfigurationFactory,
        ApplicationContext applicationContext,
        DeploymentContext deploymentContext
    ) {
        String[] beanNamesForType = getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, ResourcePlugin.class)
        );

        ConfigurablePluginManager<ResourcePlugin<?>> cpm = (ConfigurablePluginManager<ResourcePlugin<?>>) applicationContext.getBean(
            beanNamesForType[0]
        );

        return new DefaultResourceManager(
            applicationContext.getBean(DefaultClassLoader.class),
            reactable,
            cpm,
            resourceClassLoaderFactory,
            resourceConfigurationFactory,
            applicationContext,
            deploymentContext
        );
    }

    /**
     * Search across tree of BeanFactory in order to find bean in a parent application context.
     * @param resolvableType
     * @return
     */
    protected String[] getBeanNamesForType(ResolvableType resolvableType) {
        return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
            ((ConfigurableApplicationContext) applicationContext).getBeanFactory(),
            resolvableType
        );
    }
}
