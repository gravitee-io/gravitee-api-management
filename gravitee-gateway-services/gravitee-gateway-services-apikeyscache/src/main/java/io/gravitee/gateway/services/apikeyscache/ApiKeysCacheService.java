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
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import net.sf.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeysCacheService extends AbstractService implements EventListener<ReactorEvent, Reactable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeysCacheService.class);

    @Value("${services.apikeyscache.enabled:true}")
    private boolean enabled;

    @Value("${services.apikeyscache.delay:10000}")
    private int delay;

    @Value("${services.apikeyscache.unit:MILLISECONDS}")
    private TimeUnit unit;

    @Value("${services.apikeyscache.threads:3}")
    private int threads;

    @Autowired
    private EventManager eventManager;

    @Autowired
    private Cache cache;

    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private PlanRepository planRepository;

    private ExecutorService executorService;

    private final Map<Api, ScheduledFuture> scheduledTasks = new HashMap<>();

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

            LOGGER.debug("Register API key repository implementation {}", ApiKeyRepositoryWrapper.class.getName());
            beanFactory.registerSingleton(ApiKeyRepository.class.getName(),
                    new ApiKeyRepositoryWrapper(this.apiKeyRepository, cache));

            eventManager.subscribeForEvents(this, ReactorEvent.class);

            executorService = Executors.newScheduledThreadPool(threads, new ThreadFactory() {
                        private int counter = 0;
                        private String prefix = "apikeys-refresher";

                        public Thread newThread(Runnable r) {
                            return new Thread(r, prefix + '-' + counter++);
                        }
                    });
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (enabled) {
            super.doStop();

            if (executorService != null) {
                executorService.shutdown();
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
    public void onEvent(Event<ReactorEvent, Reactable> event) {
        final Api api = (Api) event.content().item();

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
            try {
                Set<Plan> plans = planRepository.findByApi(api.getId());
                ApiKeyRefresher refresher = new ApiKeyRefresher(api);
                refresher.setCache(cache);
                refresher.setApiKeyRepository(apiKeyRepository);
                refresher.setPlans(plans);

                LOGGER.info("Add a task to refresh keys each {} {} ", delay, unit.name());
                ScheduledFuture scheduledFuture = ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(
                        refresher, 0, delay, unit);

                scheduledTasks.put(api, scheduledFuture);
            } catch (TechnicalException te) {
                LOGGER.error("Unable to retrieve plans for API {} to refresh api-keys", api.getId(), te);
            }
        }
    }

    private void stopRefresher(Api api) {
        ScheduledFuture scheduledFuture = scheduledTasks.remove(api);
        if (scheduledFuture != null) {
            if (! scheduledFuture.isCancelled()) {
                LOGGER.info("Stop api-keys refresher");
                scheduledFuture.cancel(true);
            } else {
                LOGGER.info("API-key refresher already shutdown");
            }
        }
    }
}
