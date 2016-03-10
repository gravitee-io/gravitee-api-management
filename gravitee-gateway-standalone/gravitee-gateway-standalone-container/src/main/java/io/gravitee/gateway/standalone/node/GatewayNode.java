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
import io.gravitee.common.util.Version;
import io.gravitee.common.utils.UUIDGenerator;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.standalone.vertx.VertxEmbeddedContainer;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GatewayNode extends AbstractNode {

    private static final String TAGS_PROP = "tags";
    private static final String TAGS_DELIMITER = ",";

    @Value("${tags:}")
    private String propertyTags;

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
        properties.put("id", this.id());
        if (eventType == EventType.START_GATEWAY) {
            properties.put("version", Version.RUNTIME_VERSION.toString());
            properties.put("tags", tags().stream().collect(Collectors.joining(TAGS_DELIMITER)));
            try {
                properties.put("hostname", InetAddress.getLocalHost().getHostName());
                properties.put("ip", InetAddress.getLocalHost().getHostAddress());
            } catch (UnknownHostException uhe) {}
        }
        event.setProperties(properties);
        try {
            applicationContext.getBean(EventRepository.class).create(event);
        } catch (Exception ex) {
            LOGGER.error("Unexpected error while sending a {} event into the repository", eventType, ex);
        }
    }

    public List<String> tags() {
        final String systemPropertyTags = System.getProperty(TAGS_PROP);
        final String tags = systemPropertyTags == null ? propertyTags : systemPropertyTags;
        return (tags == null || tags.isEmpty()) ?
                Collections.EMPTY_LIST :
                Arrays.asList(tags.trim().split(TAGS_DELIMITER));
    }
}
