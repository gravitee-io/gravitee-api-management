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
package io.gravitee.gateway.handlers.cluster.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.cluster.ClusterRegistry;
import io.gravitee.gateway.handlers.cluster.manager.ClusterManager;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ClusterConfigurationTest {

    /** A plugin context declaring its own registry, as the native Kafka reactor does up to 8.0. */
    @Configuration
    static class PluginWithItsOwnRegistry {

        @Bean
        ClusterRegistry clusterRegistry() {
            return (crossId, environmentId) -> null;
        }

        @Bean
        RegistryConsumer registryConsumer(ClusterRegistry clusterRegistry) {
            return new RegistryConsumer(clusterRegistry);
        }
    }

    record RegistryConsumer(ClusterRegistry clusterRegistry) {}

    @Test
    void should_let_a_plugin_declaring_its_own_registry_start_and_use_the_gateway_one() {
        try (AnnotationConfigApplicationContext gateway = new AnnotationConfigApplicationContext()) {
            gateway.registerBean(EventManager.class, () -> mock(EventManager.class));
            gateway.register(ClusterConfiguration.class);
            gateway.refresh();

            try (AnnotationConfigApplicationContext plugin = new AnnotationConfigApplicationContext()) {
                plugin.setParent(gateway);
                plugin.register(PluginWithItsOwnRegistry.class);
                plugin.refresh();

                assertThat(plugin.getBean(RegistryConsumer.class).clusterRegistry()).isSameAs(gateway.getBean(ClusterManager.class));
            }
        }
    }
}
