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
package io.gravitee.gateway.genericevent.spring;

import io.gravitee.gateway.genericevent.DefaultGenericEventManager;
import io.gravitee.gateway.genericevent.GenericEventManager;
import io.micrometer.core.instrument.Gauge;
import io.vertx.core.Vertx;
import io.vertx.micrometer.backends.BackendRegistries;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenericEventConfiguration {

    @Bean
    public GenericEventManager genericEventManager(Vertx vertx) {
        var manager = new DefaultGenericEventManager();
        // Vertx creates the registry, if metrics are enabled, so it must exist before this bean.
        var registry = BackendRegistries.getDefaultNow();
        if (registry != null) {
            Gauge.builder("generic_events", manager, DefaultGenericEventManager::size)
                .description("Generic events held by the gateway")
                .register(registry);
        }
        return manager;
    }
}
