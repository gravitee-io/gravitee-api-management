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

import io.gravitee.gateway.debug.sync.DebugApiSynchronizer;
import io.gravitee.gateway.debug.sync.DebugSyncManager;
import io.gravitee.gateway.debug.sync.DebugSyncService;
import io.gravitee.gateway.debug.vertx.VertxDebugService;
import io.gravitee.gateway.reactor.handler.EntrypointResolver;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.handler.impl.DefaultEntrypointResolver;
import io.gravitee.gateway.reactor.handler.impl.DefaultReactorHandlerRegistry;
import io.gravitee.gateway.services.sync.synchronizer.PlanFetcher;
import io.gravitee.repository.management.model.Plan;
import io.reactivex.annotations.NonNull;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DebugConfiguration {

    @Value("${services.debug.bulk_items:10}")
    private int bulkItems;

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
    public DebugApiSynchronizer debugApiSynchronizer() {
        return new DebugApiSynchronizer();
    }

    public static final int PARALLELISM = Runtime.getRuntime().availableProcessors() * 2;

    @Bean
    public PlanFetcher planFetcher() {
        return new PlanFetcher(
            bulkItems > 0 ? bulkItems : DebugApiSynchronizer.DEFAULT_BULK_SIZE,
            Plan.Status.STAGING,
            Plan.Status.PUBLISHED,
            Plan.Status.DEPRECATED
        );
    }

    @Bean("syncExecutor")
    public ThreadPoolExecutor syncExecutor() {
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            PARALLELISM,
            PARALLELISM,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactory() {
                private int counter = 0;

                @Override
                public Thread newThread(@NonNull Runnable r) {
                    return new Thread(r, "gio.sync-debug" + counter++);
                }
            }
        );

        threadPoolExecutor.allowCoreThreadTimeOut(true);

        return threadPoolExecutor;
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
