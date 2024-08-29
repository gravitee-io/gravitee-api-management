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
package io.gravitee.gateway.reactor.handler;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReactorEventListener extends AbstractService<ReactorEventListener> implements EventListener<ReactorEvent, Reactable> {

    protected final EventManager eventManager;
    protected final ReactorHandlerRegistry reactorHandlerRegistry;

    public ReactorEventListener(EventManager eventManager, ReactorHandlerRegistry reactorHandlerRegistry) {
        this.eventManager = eventManager;
        this.reactorHandlerRegistry = reactorHandlerRegistry;
    }

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> event) {
        switch (event.type()) {
            case DEPLOY:
                reactorHandlerRegistry.create(event.content());
                break;
            case UPDATE:
                reactorHandlerRegistry.update(event.content());
                break;
            case UNDEPLOY:
                reactorHandlerRegistry.remove(event.content());
                break;
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        eventManager.subscribeForEvents(this, ReactorEvent.class);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        eventManager.unsubscribeForEvents(this, ReactorEvent.class);
        reactorHandlerRegistry.clear();
    }
}
