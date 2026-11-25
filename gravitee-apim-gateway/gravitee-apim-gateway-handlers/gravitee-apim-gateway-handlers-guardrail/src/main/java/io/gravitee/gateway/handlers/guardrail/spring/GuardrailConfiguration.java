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
package io.gravitee.gateway.handlers.guardrail.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.genericevent.GenericEventRegistry;
import io.gravitee.gateway.handlers.guardrail.decision.GuardrailDecisionReporter;
import io.gravitee.gateway.handlers.guardrail.manager.GuardrailManager;
import io.gravitee.gateway.handlers.guardrail.manager.impl.GuardrailManagerImpl;
import io.gravitee.gateway.handlers.guardrail.policy.GuardrailRunPolicyFactory;
import io.gravitee.gateway.handlers.guardrail.policy.plugin.GuardrailRunPolicyPlugin;
import io.gravitee.gateway.handlers.guardrail.registry.GuardrailRegistry;
import io.gravitee.gateway.handlers.guardrail.sync.GuardrailGenericEventSubscriber;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.policy.PolicyFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.micrometer.backends.BackendRegistries;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

/**
 * @author GraviteeSource Team
 */
@Configuration
public class GuardrailConfiguration {

    @Autowired
    ApplicationContext applicationContext;

    @PostConstruct
    void registerGuardrailRunPolicyPlugin() {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager = (ConfigurablePluginManager<
            PolicyPlugin<?>
        >) applicationContext.getBean(beanNamesForType[0]);

        policyPluginManager.register(new GuardrailRunPolicyPlugin());
    }

    @Bean
    public GuardrailRegistry guardrailRegistry() {
        return new GuardrailRegistry();
    }

    @Bean
    public GuardrailDecisionReporter guardrailDecisionReporter(ReporterService reporterService, Node node) {
        return new GuardrailDecisionReporter(reporterService, node);
    }

    @Bean
    public GuardrailManager guardrailManager(
        EventManager eventManager,
        LicenseManager licenseManager,
        GuardrailRegistry guardrailRegistry,
        Vertx vertx
    ) {
        // Typed rather than @DependsOn: VertxFactory builds Vert.x, and with it the metrics registry, only when a bean
        // asks for the Vertx object itself, which this parameter does. Beans depending on this one inherit it.
        return new GuardrailManagerImpl(eventManager, licenseManager, guardrailRegistry, meterRegistry());
    }

    @Bean(destroyMethod = "cancel")
    public GenericEventRegistry.Subscription guardrailGenericEventSubscription(
        GenericEventRegistry genericEventRegistry,
        GuardrailManager guardrailManager,
        ObjectMapper objectMapper
    ) {
        return genericEventRegistry.subscribe(
            GuardrailGenericEventSubscriber.GUARDRAIL_KIND,
            new GuardrailGenericEventSubscriber(guardrailManager, objectMapper, meterRegistry())
        );
    }

    /** Only available once Vert.x is built: see {@link #guardrailManager}. */
    private static MeterRegistry meterRegistry() {
        var registry = BackendRegistries.getDefaultNow();
        return registry == null ? new CompositeMeterRegistry() : registry;
    }

    @Bean
    public PolicyFactory guardrailPolicyFactory(
        final io.gravitee.node.api.configuration.Configuration configuration,
        final PolicyPluginFactory policyPluginFactory
    ) {
        return new GuardrailRunPolicyFactory(configuration, policyPluginFactory, new ExpressionLanguageConditionFilter<>());
    }
}
