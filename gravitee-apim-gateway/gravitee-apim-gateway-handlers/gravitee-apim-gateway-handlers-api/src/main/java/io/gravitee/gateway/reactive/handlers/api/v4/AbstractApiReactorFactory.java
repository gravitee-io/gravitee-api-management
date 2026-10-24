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
import io.gravitee.el.TemplateVariableProviderFactory;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.handlers.api.manager.CredentialResolver;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.gateway.reactive.core.context.DefaultDeploymentContext;
import io.gravitee.gateway.reactive.handlers.api.el.ApiTemplateVariableProvider;
import io.gravitee.gateway.reactive.handlers.api.el.CredentialsTemplateVariableProvider;
import io.gravitee.gateway.reactive.handlers.common.AbstractReactorFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactive.reactor.ApiReactor;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.CustomLog;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;

@CustomLog
public abstract class AbstractApiReactorFactory<T extends ReactableApi<? extends AbstractApi>> extends AbstractReactorFactory<T> {

    protected final PolicyFactoryManager policyFactoryManager;
    protected final Configuration configuration;
    protected final DictionaryManager dictionaryManager;

    protected AbstractApiReactorFactory(
        ApplicationContext applicationContext,
        PolicyFactoryManager policyFactoryManager,
        Configuration configuration,
        DictionaryManager dictionaryManager
    ) {
        super(applicationContext);
        this.policyFactoryManager = policyFactoryManager;
        this.configuration = configuration;
        this.dictionaryManager = dictionaryManager;
    }

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
            if (reactableApi.enabled()) {
                log.info("Creating Reactor Handler for api {}", reactableApi.getId());

                final ReactableComponents components = createComponents(
                    reactableApi,
                    ReactableApi.class,
                    commonTemplateVariableProviders(reactableApi)
                );

                final PolicyManager policyManager = getPolicyManager(
                    applicationContext.getBean(DefaultClassLoader.class),
                    reactableApi,
                    policyFactoryManager,
                    new CachedPolicyConfigurationFactory(),
                    policyPluginManager(),
                    applicationContext.getBean(PolicyClassLoaderFactory.class),
                    components.componentProvider()
                );

                addExtraComponents(components.customComponentProvider(), reactableApi, components.deploymentContext());

                return buildApiReactor(
                    reactableApi,
                    components.componentProvider(),
                    policyManager,
                    components.deploymentContext(),
                    components.resourceLifecycleManager()
                );
            }
        } catch (Exception ex) {
            log.error("Unexpected error while creating AsyncApiReactor", ex);
        }
        return null;
    }

    protected List<TemplateVariableProvider> commonTemplateVariableProviders(T reactableApi) {
        final List<TemplateVariableProvider> templateVariableProviders = new ArrayList<>();
        templateVariableProviders.add(dictionaryManager.createTemplateVariableProvider(reactableApi.getEnvironmentId()));
        templateVariableProviders.add(new ApiTemplateVariableProvider(reactableApi));
        Stream.of(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, CredentialResolver.class))
            .findFirst()
            .map(name -> applicationContext.getBean(name, CredentialResolver.class))
            .ifPresent(credentialResolver ->
                templateVariableProviders.add(
                    new CredentialsTemplateVariableProvider(reactableApi.getEnvironmentId(), reactableApi.getId(), credentialResolver)
                )
            );
        List<TemplateVariableProvider> list = Stream.of(
            BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, TemplateVariableProviderFactory.class)
        )
            .map(name -> (TemplateVariableProviderFactory) applicationContext.getBean(name))
            .filter(factory -> factory.getTemplateVariableScope() == TemplateVariableScope.API)
            .flatMap(factory -> factory.getTemplateVariableProviders().stream())
            .toList();
        templateVariableProviders.addAll(list);

        return templateVariableProviders;
    }
}
