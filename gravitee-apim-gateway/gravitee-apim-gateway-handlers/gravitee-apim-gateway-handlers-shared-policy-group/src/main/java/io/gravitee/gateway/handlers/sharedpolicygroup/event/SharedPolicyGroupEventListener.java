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
package io.gravitee.gateway.handlers.sharedpolicygroup.event;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.registry.SharedPolicyGroupRegistry;

public class SharedPolicyGroupEventListener
    extends AbstractService<SharedPolicyGroupEventListener>
    implements EventListener<SharedPolicyGroupEvent, ReactableSharedPolicyGroup> {

    private final SharedPolicyGroupRegistry sharedPolicyGroupRegistry;

    public SharedPolicyGroupEventListener(EventManager eventManager, SharedPolicyGroupRegistry sharedPolicyGroupRegistry) {
        this.sharedPolicyGroupRegistry = sharedPolicyGroupRegistry;
        eventManager.subscribeForEvents(this, SharedPolicyGroupEvent.class);
    }

    @Override
    public void onEvent(Event<SharedPolicyGroupEvent, ReactableSharedPolicyGroup> event) {
        switch (event.type()) {
            case DEPLOY -> sharedPolicyGroupRegistry.create(event.content());
            case UNDEPLOY -> sharedPolicyGroupRegistry.remove(event.content());
            case UPDATE -> sharedPolicyGroupRegistry.update(event.content());
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        this.sharedPolicyGroupRegistry.clear();
    }
}
