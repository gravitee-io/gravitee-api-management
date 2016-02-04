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
package io.gravitee.gateway.services.apikeyscache;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.event.ApiEvent;
import io.gravitee.repository.management.api.ApiKeyRepository;
import net.sf.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiKeysCacheService extends AbstractService implements EventListener<ApiEvent, Api> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeysCacheService.class);

    @Value("${services.apikeyscache.enabled:true}")
    private boolean enabled;

    @Value("${services.apikeyscache.delay:10}")
    private int delay;

    @Value("${services.apikeyscache.unit:SECONDS}")
    private TimeUnit unit;

    @Autowired
    private EventManager eventManager;

    @Autowired
    private Cache cache;

    private ApiKeyRepository apiKeyRepository;

    private final Map<Api, ExecutorService> refreshers = new HashMap<>();

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();

            LOGGER.info("Overriding API key repository implementation with cached API Key repository");
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) ((ConfigurableApplicationContext) applicationContext.getParent()).getBeanFactory();

            this.apiKeyRepository = beanFactory.getBean(ApiKeyRepository.class);
            LOGGER.debug("Current API key repository implementation is {}", apiKeyRepository.getClass().getName());

            String [] beanNames = beanFactory.getBeanNamesForType(ApiKeyRepository.class);
            String oldBeanName = beanNames[0];

            beanFactory.destroySingleton(oldBeanName);
            beanFactory.removeBeanDefinition(oldBeanName);
            LOGGER.debug("API key repository implementation {} has been removed from context", oldBeanName);

            LOGGER.debug("Register API key repository implementation {}", CachedApiKeyRepository.class.getName());
            beanFactory.registerSingleton(ApiKeyRepository.class.getName(),
                    new CachedApiKeyRepository(cache));

            eventManager.subscribeForEvents(this, ApiEvent.class);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (enabled) {
            super.doStop();

            Iterator<Map.Entry<Api, ExecutorService>> ite = refreshers.entrySet().iterator();
            while (ite.hasNext())
            {
                Map.Entry<Api, ExecutorService> entry = ite.next();
                stopRefresher(entry.getKey(), entry.getValue());
                ite.remove();
            }

            LOGGER.info("Clear API keys from in-memory cache before stopping service");
            cache.removeAll();
            cache.dispose();
        }
    }

    @Override
    protected String name() {
        return "API keys cache repository";
    }

    @Override
    public void onEvent(Event<ApiEvent, Api> event) {
        final Api api = event.content();

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
        }
    }

    private void startRefresher(Api api) {
        if (api.isEnabled()) {
            ApiKeyRefresher refresher = new ApiKeyRefresher(api);
            refresher.setCache(cache);
            refresher.setApiKeyRepository(apiKeyRepository);

            ExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                    r -> new Thread(r, "apikeys-refresher-" + api.getId()));

            refreshers.put(api, executor);

            LOGGER.info("Start api-keys refresher for {} each {} {} ", api, delay, unit.name());
            ((ScheduledExecutorService) executor).scheduleWithFixedDelay(
                    refresher, 0, delay, unit);
        }
    }

    private void stopRefresher(Api api, ExecutorService executor) {
        if (executor != null) {
            if (! executor.isShutdown()) {
                LOGGER.info("Stop api-keys refresher for {}", api);
                executor.shutdownNow();
            } else {
                LOGGER.info("API-key refresher already shutdown for {}", api);
            }
        }
    }

    private void stopRefresher(Api api) {
        stopRefresher(api, refreshers.get(api));
    }
}
