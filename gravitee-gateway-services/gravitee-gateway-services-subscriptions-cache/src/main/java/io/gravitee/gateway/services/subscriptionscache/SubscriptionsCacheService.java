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
package io.gravitee.gateway.services.subscriptionscache;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.subscriptionscache.repository.SubscriptionRepositoryWrapper;
import io.gravitee.gateway.services.subscriptionscache.task.SubscriptionRefresher;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import net.sf.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionsCacheService extends AbstractService implements EventListener<ReactorEvent, Reactable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionsCacheService.class);

    @Value("${services.subscriptions.enabled:true}")
    private boolean enabled;

    @Value("${services.subscriptions.delay:10000}")
    private int delay;

    @Value("${services.subscriptions.unit:MILLISECONDS}")
    private TimeUnit unit;

    @Value("${services.subscriptions.threads:3}")
    private int threads;

    @Autowired
    private EventManager eventManager;

    @Autowired
    private Cache cache;

    private SubscriptionRepository subscriptionRepository;

    private ExecutorService executorService;

    private final Map<Api, ScheduledFuture> scheduledTasks = new HashMap<>();

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();

            LOGGER.info("Overriding subscription repository implementation with a cached subscription repository");
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) ((ConfigurableApplicationContext) applicationContext.getParent()).getBeanFactory();

            this.subscriptionRepository = beanFactory.getBean(SubscriptionRepository.class);
            LOGGER.debug("Current subscription repository implementation is {}", subscriptionRepository.getClass().getName());

            String [] beanNames = beanFactory.getBeanNamesForType(SubscriptionRepository.class);
            String oldBeanName = beanNames[0];

            beanFactory.destroySingleton(oldBeanName);

            LOGGER.debug("Register subscription repository implementation {}", SubscriptionRepositoryWrapper.class.getName());
            beanFactory.registerSingleton(SubscriptionRepository.class.getName(),
                    new SubscriptionRepositoryWrapper(this.subscriptionRepository, cache));

            eventManager.subscribeForEvents(this, ReactorEvent.class);

            executorService = Executors.newScheduledThreadPool(threads, new ThreadFactory() {
                        private int counter = 0;
                        private String prefix = "subscriptions-refresher";

                        @Override
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

            LOGGER.info("Clear subscriptions from cache before stopping service");
            cache.removeAll();
            cache.dispose();
        }
    }

    @Override
    protected String name() {
        return "Subscriptions cache repository";
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
                break;
            default:
                // Nothing to do with unknown event type
                break;
        }
    }

    private void startRefresher(Api api) {
        if (api.isEnabled()) {
            SubscriptionRefresher refresher = new SubscriptionRefresher(api);
            refresher.setCache(cache);
            refresher.setSubscriptionRepository(subscriptionRepository);
            refresher.initialize();

            LOGGER.info("Add a task to refresh subscriptions each {} {} for API id[{}] name[{}]", delay, unit.name(), api.getName(), api.getId());
            ScheduledFuture scheduledFuture = ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(
                    refresher, 0, delay, unit);

            scheduledTasks.put(api, scheduledFuture);
        }
    }

    private void stopRefresher(Api api) {
        ScheduledFuture scheduledFuture = scheduledTasks.remove(api);
        if (scheduledFuture != null) {
            if (! scheduledFuture.isCancelled()) {
                LOGGER.info("Stop subscriptions refresher");
                scheduledFuture.cancel(true);
            } else {
                LOGGER.info("Subscriptions refresher already shutdown");
            }
        }
    }
}
