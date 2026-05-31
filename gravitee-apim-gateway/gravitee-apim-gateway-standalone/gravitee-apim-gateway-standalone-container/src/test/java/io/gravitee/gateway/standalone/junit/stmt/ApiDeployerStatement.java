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
package io.gravitee.gateway.standalone.junit.stmt;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.standalone.ApiLoaderInterceptor;
import io.gravitee.gateway.standalone.container.GatewayTestContainer;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.plugin.PluginRegister;
import io.gravitee.gateway.standalone.reporter.FakeReporter;
import io.gravitee.gateway.standalone.vertx.VertxEmbeddedContainer;
import io.gravitee.node.reporter.ReporterManager;
import io.gravitee.plugin.connector.ConnectorPluginManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;

/**
 * Deploys an API on a fresh embedded gateway before a test and tears it down afterwards.
 * Formerly a JUnit 4 {@code Statement}; now driven by the {@code ApiDeployer} JUnit 5 extension
 * through {@link #deploy()} / {@link #undeploy()}.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiDeployerStatement {

    private final Logger logger = LoggerFactory.getLogger(ApiDeployerStatement.class);

    private final Object target;
    private ApplicationContext applicationContext;
    private GatewayTestContainer container;
    private VertxEmbeddedContainer vertxContainer;
    private ApiManager apiManager;
    private Api api;

    public ApiDeployerStatement(Object target) {
        this.target = target;
    }

    public Supplier<ApplicationContext> getApplicationContext() {
        return () -> applicationContext;
    }

    public void deploy() throws Throwable {
        final String homeFolder = target.getClass().getAnnotation(ApiDescriptor.class).configFolder();
        URL home = ApiDeployerStatement.class.getResource(homeFolder);
        String graviteeHome = URLDecoder.decode(home.getPath(), StandardCharsets.UTF_8.name());
        System.setProperty("gravitee.home", graviteeHome);
        System.setProperty("gravitee.conf", graviteeHome + File.separator + "config" + File.separator + "gravitee.yml");

        container = new GatewayTestContainer();
        container.initialize();
        applicationContext = container.applicationContext();
        final Environment environment = applicationContext.getBean(Environment.class);

        DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) ((ConfigurableApplicationContext) applicationContext).getBeanFactory();

        FakeReporter fakeReporter = (FakeReporter) applicationContext.getBean("fakeReporter");
        ReporterManager reporterManager = applicationContext.getBean(ReporterManager.class);
        reporterManager.register(fakeReporter);

        if (target instanceof ApplicationContextAware) {
            ((ApplicationContextAware) target).setApplicationContext(applicationContext);
        }

        if (target instanceof PluginRegister) {
            final PluginRegister register = ((PluginRegister) target);

            // Register connectors
            ConnectorPluginManager cpm = container.applicationContext().getBean(ConnectorPluginManager.class);
            register.registerConnector(cpm);

            // Register policies
            String[] policyBeanNamesForType = applicationContext.getBeanNamesForType(
                ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
            );

            register.registerPolicy((ConfigurablePluginManager<PolicyPlugin>) applicationContext.getBean(policyBeanNamesForType[0]));

            // Register resources
            String[] resourceBeanNamesForType = container
                .applicationContext()
                .getBeanNamesForType(ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, ResourcePlugin.class));

            register.registerResource(
                (ConfigurablePluginManager<ResourcePlugin>) container.applicationContext().getBean(resourceBeanNamesForType[0])
            );
        }

        if (target instanceof PolicyFactory) {
            String[] beanNames = applicationContext.getBeanNamesForType(PolicyFactory.class);
            String oldBeanName = beanNames[0];

            beanFactory.destroyBean(oldBeanName, beanFactory.getBean(oldBeanName));
            beanFactory.destroySingleton(oldBeanName);

            beanFactory.removeBeanDefinition(oldBeanName);

            beanFactory.registerSingleton(oldBeanName, target);
        }

        vertxContainer = startServer(container);

        apiManager = applicationContext.getBean(ApiManager.class);
        api = loadApi(target.getClass().getAnnotation(ApiDescriptor.class).value());

        if (api.getPlans() == null || api.getPlans().isEmpty()) {
            Plan defaultPlan = new Plan();
            defaultPlan.setSecurity("key_less");
            defaultPlan.setStatus("published");

            api.setPlans(Collections.singletonList(defaultPlan));
        }

        try {
            final io.gravitee.gateway.handlers.api.definition.Api apiToRegister = new io.gravitee.gateway.handlers.api.definition.Api(api);

            final String v2EmulateV4EngineDefault = environment.getProperty("api.v2.emulateV4Engine.default", String.class, "yes");
            if (v2EmulateV4EngineDefault.equalsIgnoreCase("yes") || v2EmulateV4EngineDefault.equalsIgnoreCase("creation_only")) {
                // Force the execution mode to V4 EMULATION ENGINE as required by the environment variable.
                apiToRegister.getDefinition().setExecutionMode(ExecutionMode.V4_EMULATION_ENGINE);
            } else if (v2EmulateV4EngineDefault.equalsIgnoreCase("no")) {
                // Switch back the execution mode to V3 as required by the environment variable.
                apiToRegister.getDefinition().setExecutionMode(ExecutionMode.V3);
            }
            apiToRegister.setDeployedAt(new Date());
            apiManager.register(apiToRegister);
            api.setExecutionMode(apiToRegister.getDefinition().getExecutionMode());
        } catch (Exception e) {
            logger.error("An error occurred", e);
            // Make sure the embedded gateway is stopped if the API could not be deployed.
            try {
                stopServer(container, vertxContainer);
            } catch (Exception stopError) {
                logger.error("An error occurred while stopping the server after a deployment failure", stopError);
            }
            throw e;
        }
    }

    public void undeploy() throws Exception {
        try {
            if (apiManager != null && api != null) {
                apiManager.unregister(api.getId());
            }
        } finally {
            if (container != null && vertxContainer != null) {
                stopServer(container, vertxContainer);
            }
        }
    }

    private VertxEmbeddedContainer startServer(GatewayTestContainer container) throws InterruptedException {
        final Thread starterThread = new Thread(() -> {
            try {
                container.start();
            } catch (Exception e) {
                System.exit(-1);
            }
        });

        starterThread.start();

        final VertxEmbeddedContainer vertxEmbeddedContainer = container.applicationContext().getBean(VertxEmbeddedContainer.class);

        while (vertxEmbeddedContainer.lifecycleState() != Lifecycle.State.STARTED) {
            Thread.sleep(5);
        }
        return vertxEmbeddedContainer;
    }

    private void stopServer(GatewayTestContainer container, VertxEmbeddedContainer vertxContainer) throws InterruptedException {
        final Thread stopThread = new Thread(() -> {
            try {
                container.stop();
            } catch (Exception e) {
                System.exit(-1);
            }
        });

        stopThread.start();

        while (vertxContainer.lifecycleState() != Lifecycle.State.STOPPED) {
            Thread.sleep(5);
        }
    }

    private Api loadApi(String apiDescriptorPath) throws Exception {
        URL jsonFile = ApiDeployerStatement.class.getResource(apiDescriptorPath);
        Api api = new GraviteeMapper().readValue(jsonFile, Api.class);

        if (api.getProxy().getGroups() == null || api.getProxy().getGroups().isEmpty()) {
            // Create a default endpoint group
            EndpointGroup group = new EndpointGroup();
            group.setName("default");
            group.setEndpoints(Collections.emptySet());
            api.getProxy().setGroups(Collections.singleton(group));
        }

        if (ApiLoaderInterceptor.class.isAssignableFrom(target.getClass())) {
            ApiLoaderInterceptor loader = (ApiLoaderInterceptor) target;
            loader.before(api);
        }

        if (ApiLoaderInterceptor.class.isAssignableFrom(target.getClass())) {
            ApiLoaderInterceptor loader = (ApiLoaderInterceptor) target;
            loader.after(api);
        }

        return api;
    }
}
