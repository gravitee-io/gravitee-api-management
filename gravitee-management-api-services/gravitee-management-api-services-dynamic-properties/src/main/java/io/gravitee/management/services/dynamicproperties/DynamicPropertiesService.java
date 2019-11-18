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
import io.gravitee.definition.model.services.schedule.Trigger;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.event.ApiEvent;
import io.gravitee.management.services.dynamicproperties.provider.http.HttpProvider;
import io.gravitee.node.api.Node;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexandre FARIA (lusoalex on github.com)
 */
public class DynamicPropertiesService extends AbstractService implements EventListener<ApiEvent, ApiEntity> {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DynamicPropertiesService.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private ApiService apiService;

    @Autowired
    private Vertx vertx;

    @Autowired
    private Node node;

    private final Map<ApiEntity, Long> timers = new HashMap<>();

    @Override
    protected String name() {
        return "Dynamic Properties Service";
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        eventManager.subscribeForEvents(this, ApiEvent.class);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
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
                    provider.setVertx(vertx);
                    provider.setNode(node);

                    updater.setProvider(provider);
                    updater.setApiService(apiService);
                    logger.info("Add a scheduled task to poll dynamic properties each {} {} ", dynamicPropertyService.getTrigger().getRate(),
                            dynamicPropertyService.getTrigger().getUnit());

                    // Force the first refresh, and then run it periodically
                    updater.handle(null);

                    long periodicTimer = vertx.setPeriodic(getDelayMillis(dynamicPropertyService.getTrigger()), updater);
                    timers.put(api, periodicTimer);
                }
            } else {
                logger.info("Dynamic properties service is disabled for: {} [{}]", api.getName(), api.getVersion());
            }
        }
    }

    private long getDelayMillis(Trigger trigger) {
        switch (trigger.getUnit()) {
            case MILLISECONDS:
                return trigger.getRate();
            case SECONDS:
                return trigger.getRate() * 1000;
            case MINUTES:
                return trigger.getRate() * 1000 * 60;
            case HOURS:
                return trigger.getRate() * 1000 * 60 * 60;
        }

        return -1;
    }

    private void stopDynamicProperties(ApiEntity api) {
        Long timer = timers.remove(api);
        if (timer != null) {
            logger.info("Stop Dynamic properties service for API id[{}] name[{}]", api.getId(), api.getName());
            vertx.cancelTimer(timer);
        }
    }
}
