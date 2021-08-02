/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.debug;

import io.gravitee.gateway.debug.sync.DebugSyncManager;
import io.gravitee.gateway.debug.sync.DebugSyncService;
import io.gravitee.gateway.debug.vertx.VertxDebugService;
import io.gravitee.gateway.reactor.handler.EntrypointResolver;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.handler.impl.DefaultEntrypointResolver;
import io.gravitee.gateway.reactor.handler.impl.DefaultReactorHandlerRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class DebugConfiguration {

    @Bean
    public VertxDebugService vertxDebugService() {
        return new VertxDebugService();
    }

    @Bean
    public DebugSyncService debugSyncService() {
        return new DebugSyncService();
    }

    @Bean
    public DebugSyncManager debugSyncManager() {
        return new DebugSyncManager();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("gio.debug-sync-");
        return scheduler;
    }

    @Bean
    @Qualifier("debugReactorHandlerRegistry")
    public ReactorHandlerRegistry reactorHandlerRegistry() {
        return new DefaultReactorHandlerRegistry();
    }

    @Bean
    @Qualifier("debugEntryPointResolver")
    public EntrypointResolver reactorHandlerResolver(
        @Qualifier("debugReactorHandlerRegistry") ReactorHandlerRegistry reactorHandlerRegistry
    ) {
        return new DefaultEntrypointResolver(reactorHandlerRegistry);
    }
}
