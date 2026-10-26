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
package io.gravitee.gateway.reactive.handlers.common;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableProviderFactory;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.reactive.core.context.DefaultDeploymentContext;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.ResourceConfigurationFactoryImpl;
import io.gravitee.gateway.resource.internal.v4.DefaultResourceManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.resource.api.ResourceManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;

/**
 * Base of the reactor factories, whatever the kind of {@link Reactable} they create a reactor for. It builds what a
 * reactable needs to run on its own: its components, falling back on the global ones, its deployment context and its
 * resources, registered as its {@link ResourceManager}.
 *
 * @param <T> the kind of reactable.
 */
public abstract class AbstractReactorFactory<T extends Reactable> implements ReactorFactory<T> {

    protected final ApplicationContext applicationContext;

    protected AbstractReactorFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Builds the components of the reactable: the reactable itself registered under {@code reactableType}, its
     * resources registered as its {@link ResourceManager} (loaded from {@code reactable.dependencies(Resource.class)}),
     * then the global components.
     *
     * @param reactable the reactable.
     * @param reactableType the type the reactable is registered under in its components.
     * @param templateVariableProviders the template variable providers of the deployment context, used when the resource
     * configurations are evaluated.
     */
    protected ReactableComponents createComponents(
        final T reactable,
        final Class<? super T> reactableType,
        final List<TemplateVariableProvider> templateVariableProviders
    ) {
        final ComponentProvider globalComponentProvider = applicationContext.getBean(ComponentProvider.class);
        final CustomComponentProvider customComponentProvider = new CustomComponentProvider();
        add(customComponentProvider, reactableType, reactable);

        final CompositeComponentProvider componentProvider = new CompositeComponentProvider(
            customComponentProvider,
            globalComponentProvider
        );

        final DefaultDeploymentContext deploymentContext = new DefaultDeploymentContext();
        deploymentContext.componentProvider(componentProvider);
        deploymentContext.templateVariableProviders(templateVariableProviders);

        final ResourceLifecycleManager resourceLifecycleManager = resourceLifecycleManager(reactable, deploymentContext);
        customComponentProvider.add(ResourceManager.class, resourceLifecycleManager);

        return new ReactableComponents(customComponentProvider, componentProvider, deploymentContext, resourceLifecycleManager);
    }

    /**
     * The template variable providers of a reactable living in an environment: the dictionaries of the environment, then
     * the providers of the reactable itself, then the providers contributed for the API scope, which also applies to the
     * reactables executed in the context of an API.
     *
     * @param environmentId the environment of the reactable.
     * @param reactableTemplateVariableProviders the providers of the reactable itself.
     */
    protected List<TemplateVariableProvider> environmentTemplateVariableProviders(
        final String environmentId,
        final List<TemplateVariableProvider> reactableTemplateVariableProviders
    ) {
        final List<TemplateVariableProvider> templateVariableProviders = new ArrayList<>();
        templateVariableProviders.add(dictionaryManager().createTemplateVariableProvider(environmentId));
        templateVariableProviders.addAll(reactableTemplateVariableProviders);
        Stream.of(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, TemplateVariableProviderFactory.class))
            .map(name -> (TemplateVariableProviderFactory) applicationContext.getBean(name))
            .filter(factory -> factory.getTemplateVariableScope() == TemplateVariableScope.API)
            .flatMap(factory -> factory.getTemplateVariableProviders().stream())
            .forEach(templateVariableProviders::add);
        return templateVariableProviders;
    }

    protected DictionaryManager dictionaryManager() {
        return applicationContext.getBean(DictionaryManager.class);
    }

    private static <C> void add(final CustomComponentProvider componentProvider, final Class<C> type, final Object component) {
        componentProvider.add(type, type.cast(component));
    }

    @SuppressWarnings("unchecked")
    protected ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager() {
        String[] beanNamesForType = getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );
        return (ConfigurablePluginManager<PolicyPlugin<?>>) applicationContext.getBean(beanNamesForType[0]);
    }

    @SuppressWarnings("unchecked")
    private ResourceLifecycleManager resourceLifecycleManager(final Reactable reactable, final DefaultDeploymentContext deploymentContext) {
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
            applicationContext.getBean(ResourceClassLoaderFactory.class),
            new ResourceConfigurationFactoryImpl(),
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

    /**
     * What {@link #createComponents} built for a reactable.
     *
     * @param customComponentProvider the components of the reactable alone, to add more of them.
     * @param componentProvider the components of the reactable, then the global ones.
     * @param deploymentContext the deployment context of the reactable.
     * @param resourceLifecycleManager the resources of the reactable, to start and stop with its reactor.
     */
    public record ReactableComponents(
        CustomComponentProvider customComponentProvider,
        CompositeComponentProvider componentProvider,
        DefaultDeploymentContext deploymentContext,
        ResourceLifecycleManager resourceLifecycleManager
    ) {}
}
