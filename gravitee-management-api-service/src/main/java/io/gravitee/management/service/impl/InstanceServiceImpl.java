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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.EventEntity;
import io.gravitee.management.model.EventType;
import io.gravitee.management.model.InstanceEntity;
import io.gravitee.management.service.EventService;
import io.gravitee.management.service.InstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.EMPTY_LIST;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Component
public class InstanceServiceImpl implements InstanceService {

    @Autowired
    private EventService eventService;

    private static final List<EventType> instancesAllState = new ArrayList<>();
    {
        instancesAllState.add(EventType.GATEWAY_STARTED);
        instancesAllState.add(EventType.GATEWAY_STOPPED);
    }

    private static final List<EventType> instancesRunningOnly = new ArrayList<>();
    {
        instancesRunningOnly.add(EventType.GATEWAY_STARTED);
    }

    @Override
    public Collection<InstanceEntity> findInstances(boolean includeStopped) {
        Set<EventEntity> events;

        if (includeStopped) {
            events = eventService.findByType(instancesAllState);
        } else {
            events = eventService.findByType(instancesRunningOnly);
        }

        Instant nowMinusXMinutes = Instant.now().minus(5, ChronoUnit.MINUTES);
        return events.stream().map(
                event -> {
                    Map<String, String> props = event.getProperties();
                    InstanceEntity instance = new InstanceEntity(props.get("id"));
                    instance.setHostname(props.get("hostname"));
                    instance.setVersion(props.get("version"));
                    instance.setIp(props.get("ip"));
                    instance.setPort(-1);
                    // instance.setPort(Integer.parseInt(event.getProperties().get("port")));
                    String tags = props.get("tags");
                    if (tags != null && ! tags.isEmpty()) {
                        instance.setTags(Arrays.asList(tags.trim().split(",")));
                    } else {
                        instance.setTags(EMPTY_LIST);
                    }

                    instance.setLastHeartbeatAt(new Date(Long.parseLong(props.get("last_heartbeat_at"))));
                    instance.setStartedAt(new Date(Long.parseLong(props.get("started_at"))));

                    if (event.getType() == EventType.GATEWAY_STARTED) {
                        instance.setState(InstanceEntity.State.STARTED);
                        // If last heartbeat timestamp is < now - 5m, set as unknown state
                        Instant lastHeartbeat = Instant.ofEpochMilli(instance.getLastHeartbeatAt().getTime());
                        if (lastHeartbeat.isBefore(nowMinusXMinutes)) {
                            instance.setState(InstanceEntity.State.UNKNOWN);
                        }
                    } else {
                        instance.setState(InstanceEntity.State.STOPPED);
                        instance.setStoppedAt(new Date(Long.parseLong(props.get("stopped_at"))));
                    }
                    return instance;
                }
        ).collect(Collectors.toList());
    }
}
