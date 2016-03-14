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
package io.gravitee.gateway.services.monitoring;

import io.gravitee.common.node.Node;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.util.Version;
import io.gravitee.common.utils.UUIDGenerator;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class MonitoringService extends AbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringService.class);

    private static final String TAGS_PROP = "tags";
    private static final String TAGS_DELIMITER = ",";

    @Value("${services.monitoring.enabled:true}")
    private boolean enabled;

    @Value("${services.monitoring.delay:5000}")
    private int delay;

    @Value("${services.monitoring.unit:MILLISECONDS}")
    private TimeUnit unit;

    @Value("${tags:}")
    private String propertyTags;

    @Autowired
    private Node node;

    @Autowired
    private EventRepository eventRepository;

    private ExecutorService executorService;

    private Event heartbeatEvent;

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();
            LOGGER.info("Start gateway monitor");

            Event evt = prepareEvent();
            LOGGER.debug("Sending a {} event", evt.getType());
            heartbeatEvent = eventRepository.create(evt);

            executorService = Executors.newSingleThreadScheduledExecutor(
                    r -> new Thread(r, "gateway-monitor"));

            MonitorThread monitorThread = new MonitorThread(heartbeatEvent);
            this.applicationContext.getAutowireCapableBeanFactory().autowireBean(monitorThread);

            LOGGER.info("Monitoring scheduled with fixed delay {} {} ", delay, unit.name());

            ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(
                    monitorThread, 0, delay, unit);

            LOGGER.info("Start gateway monitor : DONE");
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (enabled) {
            if (! executorService.isShutdown()) {
                LOGGER.info("Stop gateway monitor");
                executorService.shutdownNow();
            } else {
                LOGGER.info("Gateway monitor already shut-downed");
            }

            heartbeatEvent.setType(EventType.GATEWAY_STOPPED);
            heartbeatEvent.getProperties().put("stopped_at", Long.toString(new Date().getTime()));
            LOGGER.debug("Sending a {} event", heartbeatEvent.getType());
            eventRepository.update(heartbeatEvent);

            super.doStop();
            LOGGER.info("Stop gateway monitor : DONE");
        }
    }

    private Event prepareEvent() {
        Event event = new Event();
        event.setId(UUIDGenerator.generate().toString());
        event.setType(EventType.GATEWAY_STARTED);
        event.setCreatedAt(new Date());
        event.setUpdatedAt(event.getCreatedAt());
        Map<String, String> properties = new HashMap<>();
        properties.put("id", node.id());
        properties.put("version", Version.RUNTIME_VERSION.toString());
        properties.put("tags", tags().stream().collect(Collectors.joining(TAGS_DELIMITER)));
        properties.put("started_at", Long.toString(event.getCreatedAt().getTime()));
        try {
            properties.put("hostname", InetAddress.getLocalHost().getHostName());
            properties.put("ip", InetAddress.getLocalHost().getHostAddress());
                //TODO: how to get HTTP(S) port ?
            // properties.put("port", Integer.toString(serverConfiguration.getPort()));
        } catch (UnknownHostException uhe) {}

        event.setProperties(properties);
        return event;
    }

    public List<String> tags() {
        final String systemPropertyTags = System.getProperty(TAGS_PROP);
        final String tags = systemPropertyTags == null ? propertyTags : systemPropertyTags;
        return (tags == null || tags.isEmpty()) ?
                Collections.EMPTY_LIST :
                Arrays.asList(tags.trim().split(TAGS_DELIMITER));
    }

    @Override
    protected String name() {
        return "Gateway Monitor";
    }
}
