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

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.handlers.cluster.manager.ClusterManager;
import io.gravitee.gateway.handlers.cluster.manager.impl.ClusterManagerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ClusterConfiguration {

    /**
     * Primary: the cluster manager is also the gateway's {@link io.gravitee.definition.model.cluster.ClusterRegistry}, and
     * plugin contexts (child contexts of this one) may still declare their own registry, as the native Kafka reactor does
     * up to 8.0. Without a primary candidate, injecting a {@code ClusterRegistry} in such a plugin fails on two matching
     * beans and the gateway does not start; with it, the plugin uses the gateway's registry.
     */
    @Bean
    @Primary
    public ClusterManager clusterManager(EventManager eventManager) {
        return new ClusterManagerImpl(eventManager);
    }
}
