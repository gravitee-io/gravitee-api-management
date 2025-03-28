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
package io.gravitee.apim.gateway.tests.sdk.plugin;

import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.plugin.connector.ConnectorPlugin;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.reporter.api.Reporter;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PluginRegister {
    /**
     * Override this method to register a policy to be used by the gateway.
     * This method is useful if you want to register a dummy policy to transform request or response before or after the policy you want to test.
     * For example:
     * <pre>
     *     {@code
     *     @Override
     *     public void configurePolicies(Map<String, PolicyPlugin> policies) {
     *         super.configurePolicies(policies);
     *
     *         policies.put("header-policy1", PolicyBuilder.build("header-policy1", Header1Policy.class));
     *     }
     *     }
     * </pre>
     * You can check {@link io.gravitee.apim.gateway.tests.sdk.policy.KeylessPolicy} to know how to implement it.
     * @param policies is the map containing policies to deploy
     */
    default void configurePolicies(Map<String, PolicyPlugin> policies) {}

    /**
     * Override this method to load the policy into the Gateway as a regular plugin.
     * This method differs from {@link PluginRegister#configurePolicies(Map)} in the way that it will initialize and register the policy as a regular plugin in the gateway.
     * Useful for policies with initializers inherited from {@link io.gravitee.policy.api.PolicyContext}.
     * @param policies is the map containing policies to deploy
     */
    default void loadPolicy(PluginManifest manifest, Map<String, PolicyPlugin> policies) {}

    /**
     * Override this method to register a connector to be used by the gateway.
     * This method is useful if you want to register a dummy connector.
     * For example:
     * <pre>
     *     {@code
     *     @Override
     *     public void configureConnectors(Map<String, ConnectorPlugin> connectors) {
     *         connectors.put("connector-http", ConnectorBuilder.build("connector-http", HttpConnectorFactory.class));
     *     }
     *     }
     * </pre>
     * You can check {@link io.gravitee.connector.http.HttpConnectorFactory} to know how to implement it.
     * @param connectors is the map containing connectors to deploy
     */
    default void configureConnectors(Map<String, ConnectorPlugin> connectors) {}

    /**
     * Override this method to register an entrypoint to be used by the gateway.
     * This method is useful if you want to register a dummy entrypoint.
     * For example:
     * <pre>
     *     {@code
     *     @Override
     *     public void configureEntrypoints(Map<String, EntrypointPlugin> entrypoints) {
     *         entrypoints.put("entrypoint-dummy", EntrypointBuilder.build("entrypoint-dummy", DummyEntrypointConnectorFactory.class));
     *     }
     *     }
     * </pre>
     * @param entrypoints is the map containing entrypoints to deploy
     */
    default void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {}

    /**
     * Override this method to register a reactor to be used by the gateway.
     * This method is useful if you want to register a new reactor.
     * For example:
     * <pre>
     *     {@code
     *     @Override
     *     public void configureReactor(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
     *         entrypoints.add(ReactorBuilder.build(DummyReactorFactory.class));
     *     }
     *     }
     * </pre>
     * @param reactors is the map containing reactors to deploy
     */
    default void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {}

    /**
     * Override this method to register an endpoint to be used by the gateway.
     * This method is useful if you want to register a dummy endpoint.
     * For example:
     * <pre>
     *     {@code
     *     @Override
     *     public void configureEndpoints(Map<String, EndpointPlugin> endpoints) {
     *         endpoints.put("mock", EndpointBuilder.build("endpoint-dummy", DummyEndpointConnectorFactory.class));
     *     }
     *     }
     * </pre>
     * You can check {@link io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory} to know how to implement it.
     * @param endpoints is the map containing endpoints to deploy
     */
    default void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {}

    /**
     * Override this method to register a resource to be used by the gateway.
     * This method is useful if you want to register a dummy resource.
     * For example:
     * <pre>
     *    {@code
     *     @Override
     *     public void configureResources(Map<String, ResourcePlugin> resources)  {
     *         super.configureResources(resources);
     *         resources.put("my-resource", ResourceBuilder.build("my-resource", DummyResource.class));
     *     }
     *     }
     * </pre>
     * You can check {@link io.gravitee.apim.gateway.tests.sdk.resource.DummyResource} to know how to implement it.
     * @param resources is the map containing resources to deploy
     */
    default void configureResources(Map<String, ResourcePlugin> resources) {}

    /**
     * Override this method to register a reporter to be used by the gateway.
     * This method is useful if you want to register a dummy reporter.
     * For example:
     * <pre>
     *    {@code
     *     @Override
     *     public void configureReporters(Map<String, Reporter> reporters)  {
     *         super.configureReporters(reporters);
     *         reporters.put("fakeReporter", (FakeReporter) applicationContext.getBean("fakeReporter"));
     *     }
     *     }
     * </pre>
     * You can check {@link io.gravitee.apim.gateway.tests.sdk.resource.DummyResource} to know how to implement it.
     * @param reporters is the map containing reporters to deploy
     */
    default void configureReporters(Map<String, Reporter> reporters) {}

    /**
     * Override this method to register a secret provider plugins.
     * <pre>
     *    {@code
     *     @Override
     *     public void configureSecretProviders(Map<String, Reporter> secretProviderFactories)  {
     *            super.configureSecretProviders(secretProviderFactories);
     *            secretProviderFactories.add(SecretProviderBuilder.build("kubernetes", KubernetesSecretProviderFactory.class, K8sConfig.class));
     *
     *            // add secrets in kubernetes here
     *     }
     *     }
     *
     * </pre>
     *
     * @param secretProviderPlugins plugins to register
     */
    default void configureSecretProviders(
        Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
    ) throws Exception {
        Objects.requireNonNull(secretProviderPlugins);
    }

    /**
     * Override this method to register a service plugin to be used by the gateway.
     * For example:
     * <pre>
     *    {@code
     *     @Override
     *     public void configureServices(Set<Class<AbstractService<?>>> services)  {
     *         super.configureServices(services);
     *         services.add(new MyService());
     *     }
     *     }
     * </pre>
     * @param services is the map containing resvices to deploy
     */
    default void configureServices(Set<Class<? extends AbstractService<?>>> services) {}
}
