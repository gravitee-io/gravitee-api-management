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
package io.gravitee.apim.infra.listener;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.plugin.ManagementApiServicesManager;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.service.event.ApiEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ApiEventListener implements EventListener<ApiEvent, io.gravitee.repository.management.model.Api> {

    private static final ApiAdapter apiAdapter = ApiAdapter.INSTANCE;
    private final ManagementApiServicesManager managementApiServicesManager;

    public ApiEventListener(EventManager eventManager, ManagementApiServicesManager managementApiServicesManager) {
        this.managementApiServicesManager = managementApiServicesManager;
        eventManager.subscribeForEvents(this, ApiEvent.class);
    }

    @Override
    public void onEvent(Event<ApiEvent, io.gravitee.repository.management.model.Api> event) {
        final io.gravitee.repository.management.model.Api eventPayload = event.content();
        if (!isV4Api(eventPayload)) {
            return;
        }
        final Api api = apiAdapter.toCoreModel(eventPayload);
        switch (event.type()) {
            case DEPLOY -> onApiDeploy(api);
            case UNDEPLOY -> onApiUndeploy(api);
            case UPDATE -> onApiUpdate(api);
            case START_DYNAMIC_PROPERTY_V4 -> onDynamicPropertiesStarted(api);
            case STOP_DYNAMIC_PROPERTY_V4 -> onDynamicPropertiesStopped(api);
        }
    }

    private void onDynamicPropertiesStopped(Api api) {
        log.info("stopping Dynamic properties event for api: {}", api.getId());
        managementApiServicesManager.stopDynamicProperties(api);
    }

    private void onDynamicPropertiesStarted(Api api) {
        log.info("starting Dynamic properties event for api: {}", api.getId());
        managementApiServicesManager.startDynamicProperties(api);
    }

    private void onApiDeploy(Api api) {
        log.debug("Dispatching DEPLOY event for api: {}", api.getId());
        managementApiServicesManager.deployServices(api);
    }

    private void onApiUndeploy(Api api) {
        log.debug("Dispatching UNDEPLOY event for api: {}", api.getId());
        managementApiServicesManager.undeployServices(api);
    }

    private void onApiUpdate(Api api) {
        log.debug("Dispatching UPDATE event for api: {}", api.getId());
        managementApiServicesManager.updateServices(api);
    }

    private static boolean isV4Api(io.gravitee.repository.management.model.Api eventPayload) {
        return eventPayload.getDefinitionVersion() != null && eventPayload.getDefinitionVersion().equals(DefinitionVersion.V4);
    }
}
