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
package io.gravitee.rest.api.service.impl.alert;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.model.alert.ApplicationAlertEventType;
import io.gravitee.rest.api.service.ApplicationAlertService;
import org.springframework.stereotype.Component;

@Component
public class ApplicationAlertEventListener implements EventListener<ApplicationAlertEventType, Object> {

    private ApplicationAlertService applicationAlertService;

    public ApplicationAlertEventListener(ApplicationAlertService applicationAlertService, EventManager eventManager) {
        this.applicationAlertService = applicationAlertService;
        eventManager.subscribeForEvents(this, ApplicationAlertEventType.class);
    }

    @Override
    public void onEvent(Event<ApplicationAlertEventType, Object> event) {
        applicationAlertService.handleEvent(event);
    }
}
