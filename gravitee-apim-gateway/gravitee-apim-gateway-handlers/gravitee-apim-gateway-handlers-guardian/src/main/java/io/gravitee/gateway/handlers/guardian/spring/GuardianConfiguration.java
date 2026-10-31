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
package io.gravitee.gateway.handlers.guardian.spring;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.handlers.guardian.manager.GuardianManager;
import io.gravitee.gateway.handlers.guardian.manager.impl.GuardianManagerImpl;
import io.gravitee.gateway.handlers.guardian.policy.GuardianPolicyFactory;
import io.gravitee.gateway.handlers.guardian.policy.plugin.GuardianPolicyPlugin;
import io.gravitee.gateway.handlers.guardian.registry.GuardianRegistry;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.policy.PolicyFactory;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
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
public class GuardianConfiguration {

    @Autowired
    ApplicationContext applicationContext;

    @PostConstruct
    void registerGuardianPolicyPlugin() {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class)
        );

        ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager = (ConfigurablePluginManager<
            PolicyPlugin<?>
        >) applicationContext.getBean(beanNamesForType[0]);

        policyPluginManager.register(new GuardianPolicyPlugin());
    }

    @Bean
    public GuardianRegistry guardianRegistry() {
        return new GuardianRegistry();
    }

    @Bean
    public GuardianManager guardianManager(EventManager eventManager, LicenseManager licenseManager) {
        return new GuardianManagerImpl(eventManager, licenseManager);
    }

    @Bean
    public PolicyFactory guardianPolicyFactory(
        final io.gravitee.node.api.configuration.Configuration configuration,
        final PolicyPluginFactory policyPluginFactory
    ) {
        return new GuardianPolicyFactory(configuration, policyPluginFactory, new ExpressionLanguageConditionFilter<>());
    }
}
