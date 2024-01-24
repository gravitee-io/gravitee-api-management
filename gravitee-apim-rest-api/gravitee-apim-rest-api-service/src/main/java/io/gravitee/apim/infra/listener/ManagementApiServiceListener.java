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

import io.gravitee.apim.core.api.use_case.UpdateDynamicPropertiesUseCase;
import io.gravitee.apim.rest.api.common.apiservices.events.DynamicPropertiesEvent;
import io.gravitee.apim.rest.api.common.apiservices.events.ManagementApiServiceEvent;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ManagementApiServiceListener implements EventListener<ManagementApiServiceEvent, Object> {

    private final UpdateDynamicPropertiesUseCase updateDynamicPropertiesUsecase;

    public ManagementApiServiceListener(EventManager eventManager, UpdateDynamicPropertiesUseCase updateDynamicPropertiesUsecase) {
        this.updateDynamicPropertiesUsecase = updateDynamicPropertiesUsecase;
        eventManager.subscribeForEvents(this, ManagementApiServiceEvent.class);
    }

    @Override
    public void onEvent(Event<ManagementApiServiceEvent, Object> event) {
        switch (event.type()) {
            case DYNAMIC_PROPERTY_UPDATE -> {
                if (event.content() instanceof DynamicPropertiesEvent dynamicPropertiesEvent) {
                    updateDynamicPropertiesUsecase.execute(
                        new UpdateDynamicPropertiesUseCase.Input(
                            dynamicPropertiesEvent.apiId(),
                            dynamicPropertiesEvent.pluginId(),
                            dynamicPropertiesEvent.dynamicProperties()
                        )
                    );
                    return;
                }
                log.warn("Not able to parse the DYNAMIC_PROPERTY_UPDATE event");
            }
        }
    }
}
