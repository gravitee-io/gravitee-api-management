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
package io.gravitee.gateway.standalone.node;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.common.node.AbstractNode;
import io.gravitee.common.utils.UUIDGenerator;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.standalone.vertx.VertxEmbeddedContainer;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;

import java.util.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GatewayNode extends AbstractNode {

    @Override
    public String name() {
        return "Gravitee.io - Gateway";
    }

    @Override
    protected List<Class<? extends LifecycleComponent>> getLifecycleComponents() {
        List<Class<? extends LifecycleComponent>> components = new ArrayList<>();

        components.add(Reactor.class);
        components.add(VertxEmbeddedContainer.class);

        return components;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        sendEvent(EventType.START_GATEWAY);
    }

    @Override
    protected void doStop() throws Exception {
        sendEvent(EventType.STOP_GATEWAY);
        super.doStop();
    }

    private void sendEvent(EventType eventType) {
        Event event = new Event();
        event.setId(UUIDGenerator.generate().toString());
        event.setType(eventType);
        event.setCreatedAt(new Date());
        Map<String, String> properties = new HashMap<>();
        properties.put("gateway_id", this.id());
        event.setProperties(properties);
        try {
            applicationContext.getBean(EventRepository.class).create(event);
        } catch (Exception ex) {
            LOGGER.error("Unexpected error while sending a {} event into the repository", eventType, ex);
        }
    }
}
