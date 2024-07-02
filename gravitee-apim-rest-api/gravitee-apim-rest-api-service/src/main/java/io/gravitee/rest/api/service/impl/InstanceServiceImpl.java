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
package io.gravitee.rest.api.service.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.InstanceEntity;
import io.gravitee.rest.api.model.InstanceListItem;
import io.gravitee.rest.api.model.InstanceQuery;
import io.gravitee.rest.api.model.InstanceState;
import io.gravitee.rest.api.model.PluginEntity;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.InstanceService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.EventNotFoundException;
import io.gravitee.rest.api.service.exceptions.InstanceNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class InstanceServiceImpl implements InstanceService {

    private static final String REDACTED = "REDACTED";

    private final EventService eventService;

    private final ObjectMapper objectMapper;

    private final long unknownExpireAfterInSec;

    private static final List<EventType> instancesAllState = List.of(EventType.GATEWAY_STARTED, EventType.GATEWAY_STOPPED);

    private static final List<EventType> instancesRunningOnly = List.of(EventType.GATEWAY_STARTED);

    public InstanceServiceImpl(
        EventService eventService,
        ObjectMapper objectMapper,
        @Value("${gateway.unknown-expire-after:604800}") long unknownExpireAfterInSec
    ) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.unknownExpireAfterInSec = unknownExpireAfterInSec;
    }

    @Override
    public Page<InstanceListItem> search(ExecutionContext executionContext, InstanceQuery query) {
        List<EventType> types;

        if (query.isIncludeStopped()) {
            types = instancesAllState;
        } else {
            types = instancesRunningOnly;
        }

        ExpiredPredicate filter = new ExpiredPredicate(Duration.ofSeconds(unknownExpireAfterInSec));
        long from = query.getFrom() > 0 ? query.getFrom() : Instant.now().minus(unknownExpireAfterInSec, ChronoUnit.SECONDS).toEpochMilli();

        Page<EventEntity> eventEntitiesPage = eventService.search(
            executionContext,
            types,
            query.getProperties(),
            from,
            query.getTo(),
            query.getPage(),
            query.getSize(),
            Collections.singletonList(executionContext.getEnvironmentId())
        );

        List<InstanceListItem> result = eventEntitiesPage
            .getContent()
            .stream()
            .map(eventEntity -> {
                InstanceEntity instanceEntity = convert(eventEntity);

                InstanceListItem item = new InstanceListItem();
                item.setId(instanceEntity.getId());
                item.setEvent(instanceEntity.getEvent());
                item.setHostname(instanceEntity.getHostname());
                item.setIp(instanceEntity.getIp());
                item.setPort(instanceEntity.getPort());
                item.setLastHeartbeatAt(instanceEntity.getLastHeartbeatAt());
                item.setStartedAt(instanceEntity.getStartedAt());
                item.setStoppedAt(instanceEntity.getStoppedAt());
                item.setVersion(instanceEntity.getVersion());
                item.setTags(instanceEntity.getTags());
                item.setTenant(instanceEntity.getTenant());
                item.setOperatingSystemName(
                    instanceEntity.getSystemProperties() != null ? instanceEntity.getSystemProperties().get("os.name") : ""
                );
                item.setState(instanceEntity.getState());

                return item;
            })
            .filter(filter)
            .collect(Collectors.toList());

        return new Page<>(result, eventEntitiesPage.getPageNumber(), result.size(), eventEntitiesPage.getTotalElements());
    }

    @Override
    public InstanceEntity findById(final ExecutionContext executionContext, String instanceId) {
        final EventQuery query = new EventQuery();
        query.setId(instanceId);
        query.setTypes(instancesAllState);

        final Collection<EventEntity> events = eventService.search(executionContext, query);
        if (events == null || events.isEmpty()) {
            throw new InstanceNotFoundException(instanceId);
        }

        return convert(events.iterator().next());
    }

    @Override
    public InstanceEntity findByEvent(ExecutionContext executionContext, String eventId) {
        try {
            log.debug("Find instance by event ID: {}", eventId);

            EventEntity event = eventService.findById(executionContext, eventId);
            return convert(event);
        } catch (EventNotFoundException enfe) {
            throw new InstanceNotFoundException(eventId);
        }
    }

    @Override
    public List<InstanceEntity> findAllStarted(final ExecutionContext executionContext) {
        log.debug("Find started instances by event");

        final EventQuery query = new EventQuery();
        query.setTypes(instancesRunningOnly);

        Collection<EventEntity> events = eventService.search(executionContext, query);

        return events.stream().map(this::convert).collect(Collectors.toList());
    }

    private InstanceEntity convert(EventEntity event) {
        Instant nowMinusXMinutes = Instant.now().minus(5, ChronoUnit.MINUTES);

        Map<String, String> props = event.getProperties();
        InstanceEntity instance = new InstanceEntity(props.get("id"));
        instance.setEvent(event.getId());
        if (!isBlank(props.get("last_heartbeat_at"))) {
            instance.setLastHeartbeatAt(new Date(Long.parseLong(props.get("last_heartbeat_at"))));
        }
        if (!isBlank(props.get("started_at"))) {
            instance.setStartedAt(new Date(Long.parseLong(props.get("started_at"))));
        }
        instance.setEnvironments(event.getEnvironments());

        if (event.getPayload() != null) {
            try {
                InstanceInfo info = objectMapper.readValue(event.getPayload(), InstanceInfo.class);
                instance.setHostname(info.getHostname());
                instance.setIp(info.getIp());
                instance.setPort(info.getPort());
                instance.setTenant(info.getTenant());
                instance.setVersion(info.getVersion());
                instance.setTags(info.getTags());
                instance.setSystemProperties(
                    info.getSystemProperties() == null
                        ? Collections.emptyMap()
                        : info
                            .getSystemProperties()
                            .entrySet()
                            .stream()
                            .map(e -> {
                                if (e.getKey().toLowerCase().contains("password")) {
                                    return Map.entry(e.getKey(), REDACTED);
                                } else {
                                    return e;
                                }
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                );
                instance.setPlugins(info.getPlugins());
                instance.setClusterId(info.getClusterId());
            } catch (IOException ioe) {
                log.error("Unexpected error while getting instance data from event payload", ioe);
            }
        }
        if (!isBlank(props.get("cluster_primary_node"))) {
            instance.setClusterPrimaryNode(Boolean.parseBoolean(props.get("cluster_primary_node")));
        }

        if (event.getType() == EventType.GATEWAY_STARTED) {
            // If last heartbeat timestamp is < now - 5m, set as unknown state
            if (
                instance.getLastHeartbeatAt() == null ||
                Instant.ofEpochMilli(instance.getLastHeartbeatAt().getTime()).isBefore(nowMinusXMinutes)
            ) {
                instance.setState(InstanceState.UNKNOWN);
            } else {
                instance.setState(InstanceState.STARTED);
            }
        } else {
            instance.setState(InstanceState.STOPPED);
            if (!isBlank(props.get("stopped_at"))) {
                instance.setStoppedAt(new Date(Long.parseLong(props.get("stopped_at"))));
            }
        }

        return instance;
    }

    @Getter
    @Setter
    private static class InstanceInfo {

        private String id;
        private String version;
        private List<String> tags;
        private Set<PluginEntity> plugins;
        private String hostname;
        private String ip;
        private String port;
        private String tenant;
        private Map<String, String> systemProperties;
        private String clusterId;
    }

    public static final class ExpiredPredicate implements Predicate<InstanceListItem> {

        private final Duration threshold;

        public ExpiredPredicate(Duration threshold) {
            this.threshold = threshold;
        }

        @Override
        public boolean test(InstanceListItem instance) {
            boolean result = true;
            if (instance.getState().equals(InstanceState.UNKNOWN)) {
                Instant now = Instant.now();
                result = (now.toEpochMilli() - instance.getLastHeartbeatAt().getTime() <= threshold.toMillis());
            }
            return result;
        }
    }
}
