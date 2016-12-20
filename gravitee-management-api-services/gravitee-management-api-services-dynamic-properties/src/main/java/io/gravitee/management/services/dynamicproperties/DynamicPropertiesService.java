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
package io.gravitee.management.services.dynamicproperties;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProvider;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.event.ApiEvent;
import io.gravitee.management.services.dynamicproperties.provider.http.HttpProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Alexandre FARIA (lusoalex on github.com)
 */
public class DynamicPropertiesService extends AbstractService  implements EventListener<ApiEvent, ApiEntity> {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DynamicPropertiesService.class);

    @Value("${services.dynamicproperties.threads:2}")
    private int threads;

    @Autowired
    private EventManager eventManager;

    @Autowired
    private ApiService apiService;

    private ExecutorService executorService;

    private final Map<ApiEntity, ScheduledFuture> scheduledTasks = new HashMap<>();

    @Override
    protected String name() {
        return "Dynamic Properties Service";
    }

    @Override
    protected void doStart() throws Exception {
            super.doStart();

            eventManager.subscribeForEvents(this, ApiEvent.class);

            executorService = Executors.newScheduledThreadPool(threads, new ThreadFactory() {
                private int counter = 0;
                private String prefix = "dynamic-properties";

                public Thread newThread(Runnable r) {
                    return new Thread(r, prefix + '-' + counter++);
                }
            });
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public void onEvent(Event<ApiEvent, ApiEntity> event) {
        final ApiEntity api = event.content();

        switch (event.type()) {
            case DEPLOY:
                startDynamicProperties(api);
                break;
            case UNDEPLOY:
                stopDynamicProperties(api);
                break;
            case UPDATE:
                stopDynamicProperties(api);
                startDynamicProperties(api);
                break;
        }
    }

    private void startDynamicProperties(ApiEntity api) {
        if (api.getState() == Lifecycle.State.STARTED) {
            DynamicPropertyService dynamicPropertyService = api.getServices().get(DynamicPropertyService.class);
            if (dynamicPropertyService != null && dynamicPropertyService.isEnabled()) {
                DynamicPropertyUpdater updater = new DynamicPropertyUpdater(api);

                if (dynamicPropertyService.getProvider() == DynamicPropertyProvider.HTTP) {
                    HttpProvider provider = new HttpProvider(dynamicPropertyService);
                    updater.setProvider(provider);
                    updater.setApiService(apiService);
                    logger.info("Add a scheduled task to poll dynamic properties each {} {} ", dynamicPropertyService.getInterval(), dynamicPropertyService.getUnit());
                    ScheduledFuture scheduledFuture = ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(
                            updater, 0, dynamicPropertyService.getInterval(), dynamicPropertyService.getUnit());

                    scheduledTasks.put(api, scheduledFuture);
                }
            } else {
                logger.info("Dynamic properties service is disabled for: {} [{}]", api.getName(), api.getVersion());
            }
        }
    }

    private void stopDynamicProperties(ApiEntity api) {
        ScheduledFuture scheduledFuture = scheduledTasks.remove(api);
        if (scheduledFuture != null) {
            if (! scheduledFuture.isCancelled()) {
                logger.info("Stop Dynamic properties");
                scheduledFuture.cancel(true);
            } else {
                logger.info("Dynamic properties service already shutdown");
            }
        }
    }
}
