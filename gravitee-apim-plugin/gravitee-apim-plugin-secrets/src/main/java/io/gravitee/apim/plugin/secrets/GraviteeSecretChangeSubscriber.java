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
package io.gravitee.apim.plugin.secrets;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.api.genericevent.GenericEventSubscriber;
import io.gravitee.secrets.api.event.SecretInvalidationEvent;
import io.gravitee.secrets.api.event.SecretInvalidationEventType;

/**
 * Turns the change events of the Management API secret store into invalidations, so that the secrets service resolves
 * the cached references to that secret again and redeploys what uses them.
 */
class GraviteeSecretChangeSubscriber implements GenericEventSubscriber {

    private final EventManager eventManager;

    GraviteeSecretChangeSubscriber(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public void onUpsert(GenericEvent event) {
        eventManager.publishEvent(
            SecretInvalidationEventType.INVALIDATE,
            new SecretInvalidationEvent(event.environmentId(), GraviteeSecretProvider.ID, event.id())
        );
    }

    // A change event only expires: the change it reported is already applied.
    @Override
    public void onRemove(GenericEvent event) {}
}
