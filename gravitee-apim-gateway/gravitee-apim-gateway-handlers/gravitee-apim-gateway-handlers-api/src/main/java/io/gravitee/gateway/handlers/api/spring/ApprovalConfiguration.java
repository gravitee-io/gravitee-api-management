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
package io.gravitee.gateway.handlers.api.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.approval.ApprovalEventKinds;
import io.gravitee.gateway.api.genericevent.GenericEventRegistry;
import io.gravitee.gateway.handlers.api.manager.ApprovalRuleManager;
import io.gravitee.gateway.handlers.api.manager.ApprovalStateManager;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalCommandWriterImpl;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalFeature;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalRuleEventSubscriber;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalRuleManagerImpl;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateEventSubscriber;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateManagerImpl;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.api.CommandRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.micrometer.backends.BackendRegistries;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

/**
 * The approval ports policies get with {@code ctx.getComponent(...)}. They live in the gateway's root context, the parent
 * of every API context, and are fed by the generic events of the control plane.
 */
@Configuration
public class ApprovalConfiguration {

    @Bean
    public ApprovalFeature approvalFeature(LicenseManager licenseManager, Environment environment) {
        return new ApprovalFeature(licenseManager, environment.getProperty("gamma.enabled", Boolean.class, false));
    }

    // Vertx creates the default metrics registry when metrics are enabled, so it must exist before this bean.
    @Bean
    public ApprovalRuleManagerImpl approvalRuleManager(ApprovalFeature approvalFeature, Vertx vertx) {
        ApprovalRuleManagerImpl manager = new ApprovalRuleManagerImpl(approvalFeature);
        MeterRegistry registry = BackendRegistries.getDefaultNow();
        if (registry != null) {
            manager.bindTo(registry);
        }
        return manager;
    }

    @Bean(destroyMethod = "cancel")
    public GenericEventRegistry.Subscription approvalRuleSubscription(
        GenericEventRegistry genericEventRegistry,
        ApprovalRuleManager approvalRuleManager,
        ObjectMapper objectMapper
    ) {
        return genericEventRegistry.subscribe(ApprovalEventKinds.RULE, new ApprovalRuleEventSubscriber(approvalRuleManager, objectMapper));
    }

    @Bean
    public ApprovalStateManagerImpl approvalStateManager(Vertx vertx) {
        ApprovalStateManagerImpl manager = new ApprovalStateManagerImpl(Clock.systemUTC());
        MeterRegistry registry = BackendRegistries.getDefaultNow();
        if (registry != null) {
            manager.bindTo(registry);
        }
        return manager;
    }

    @Bean(destroyMethod = "cancel")
    public GenericEventRegistry.Subscription approvalStateSubscription(
        GenericEventRegistry genericEventRegistry,
        ApprovalStateManager approvalStateManager,
        ObjectMapper objectMapper
    ) {
        return genericEventRegistry.subscribe(
            ApprovalEventKinds.STATE,
            new ApprovalStateEventSubscriber(approvalStateManager, objectMapper)
        );
    }

    // Lazy: the repositories are registered once the repository plugins are loaded, after this context starts.
    // The unused vertx parameter makes Vertx, which creates the default metrics registry, exist before bindTo.
    @Bean(destroyMethod = "shutdown")
    public ApprovalCommandWriterImpl approvalCommandWriter(
        @Lazy CommandRepository commandRepository,
        ObjectMapper objectMapper,
        Node node,
        Vertx vertx
    ) {
        ApprovalCommandWriterImpl writer = new ApprovalCommandWriterImpl(commandRepository, objectMapper, node, Clock.systemUTC());
        MeterRegistry registry = BackendRegistries.getDefaultNow();
        if (registry != null) {
            writer.bindTo(registry);
        }
        return writer;
    }
}
