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
package io.gravitee.gateway.services.sync.apikeys;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.sync.apikeys.handler.ApiKeyHandler;
import io.gravitee.gateway.services.sync.apikeys.handler.ApiKeysServiceHandler;
import io.gravitee.gateway.services.sync.apikeys.repository.ApiKeyRepositoryWrapper;
import io.gravitee.gateway.services.sync.apikeys.task.ApiKeyRefresher;
import io.gravitee.gateway.services.sync.cache.CacheManager;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.vertx.ext.web.Router;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeysCacheService extends AbstractService implements EventListener<ReactorEvent, Reactable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeysCacheService.class);

    private static final String API_KEY_CACHE_NAME = "apikeys";

    @Value("${services.apikeyscache.enabled:true}")
    private boolean enabled;

    @Value("${services.apikeyscache.delay:10000}")
    private int delay;

    @Value("${services.apikeyscache.unit:MILLISECONDS}")
    private TimeUnit unit;

    @Value("${services.apikeyscache.threads:3}")
    private int threads;

    @Value("${services.sync.distributed:false}")
    private boolean distributed;

    private static final String PATH = "/apikeys";

    @Autowired
    private EventManager eventManager;

    @Autowired
    private CacheManager cacheManager;

    private ApiKeyRepository apiKeyRepository;

    private ExecutorService executorService;

    private final Map<Api, ScheduledFuture> scheduledTasks = new HashMap<>();

    @Autowired
    @Qualifier("managementRouter")
    private Router router;

    @Autowired
    private ClusterManager clusterManager;

    private ApiKeyHandler apiKeyHandler;

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();

            LOGGER.info("Overriding API key repository implementation with cached API Key repository");
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) (
                (ConfigurableApplicationContext) applicationContext.getParent()
            ).getBeanFactory();

            this.apiKeyRepository = beanFactory.getBean(ApiKeyRepository.class);
            LOGGER.debug("Current API key repository implementation is {}", apiKeyRepository.getClass().getName());

            String[] beanNames = beanFactory.getBeanNamesForType(ApiKeyRepository.class);
            String oldBeanName = beanNames[0];

            beanFactory.destroySingleton(oldBeanName);

            LOGGER.debug("Register API key repository implementation {}", ApiKeyRepositoryWrapper.class.getName());
            beanFactory.registerSingleton(
                ApiKeyRepository.class.getName(),
                new ApiKeyRepositoryWrapper(this.apiKeyRepository, cacheManager.getCache(API_KEY_CACHE_NAME))
            );

            eventManager.subscribeForEvents(this, ReactorEvent.class);

            executorService =
                Executors.newScheduledThreadPool(
                    threads,
                    new ThreadFactory() {
                        private int counter = 0;
                        private String prefix = "gio.sync-apikeys";

                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, prefix + '-' + counter++);
                        }
                    }
                );

            LOGGER.info("Associate a new HTTP handler on {}", PATH);

            // Create handlers
            // Set API-keys handler
            ApiKeysServiceHandler apiKeysHandler = new ApiKeysServiceHandler((ScheduledThreadPoolExecutor) executorService);
            applicationContext.getAutowireCapableBeanFactory().autowireBean(apiKeysHandler);
            router.get(PATH).produces(MediaType.APPLICATION_JSON).handler(apiKeysHandler);

            // Set API handler
            apiKeyHandler = new ApiKeyHandler();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(apiKeyHandler);
            router.get(PATH + "/:apiId").produces(MediaType.APPLICATION_JSON).handler(apiKeyHandler);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (enabled) {
            super.doStop();

            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }

    @Override
    protected String name() {
        return "API keys cache repository";
    }

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> event) {
        final Api api = (Api) event.content();

        switch (event.type()) {
            case DEPLOY:
                startRefresher(api);
                break;
            case UNDEPLOY:
                stopRefresher(api);
                break;
            case UPDATE:
                stopRefresher(api);
                startRefresher(api);
                break;
            default:
                // Nothing to do with unknown event type
                break;
        }
    }

    private void startRefresher(Api api) {
        if (api.isEnabled()) {
            ApiKeyRefresher refresher = new ApiKeyRefresher(api);
            refresher.setCache(cacheManager.getCache(API_KEY_CACHE_NAME));
            refresher.setApiKeyRepository(apiKeyRepository);
            refresher.setClusterManager(clusterManager);
            refresher.setDistributed(distributed);
            refresher.initialize();

            LOGGER.info(
                "Add a task to refresh api-keys each {} {} for API name[{}] id[{}]",
                delay,
                unit.name(),
                api.getName(),
                api.getId()
            );
            ScheduledFuture scheduledFuture =
                ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(refresher, 0, delay, unit);

            apiKeyHandler.addRefresher(refresher);
            scheduledTasks.put(api, scheduledFuture);
        }
    }

    private void stopRefresher(Api api) {
        ScheduledFuture scheduledFuture = scheduledTasks.remove(api);
        if (scheduledFuture != null) {
            apiKeyHandler.removeRefresher(api.getId());

            if (!scheduledFuture.isCancelled()) {
                LOGGER.info("Stop api-keys refresher");
                scheduledFuture.cancel(true);
            } else {
                LOGGER.info("API-key refresher already shutdown");
            }
        }
    }

    public static String buildCacheKey(ApiKey apiKey) {
        return buildCacheKey(apiKey.getApi(), apiKey.getKey());
    }

    public static String buildCacheKey(String api, String key) {
        return String.format("%s.%s", api, key);
    }
}
