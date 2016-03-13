package io.gravitee.management.service.impl;

import io.gravitee.management.model.EventEntity;
import io.gravitee.management.model.EventType;
import io.gravitee.management.model.InstanceEntity;
import io.gravitee.management.service.EventService;
import io.gravitee.management.service.InstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Component
public class InstanceServiceImpl implements InstanceService {

    @Autowired
    private EventService eventService;

    private List<EventType> instanceEventTypes = new ArrayList<>();
    {
        instanceEventTypes.add(EventType.START_GATEWAY);
        instanceEventTypes.add(EventType.STOP_GATEWAY);
    }

    @Override
    public Collection<InstanceEntity> findInstances() {
        Set<EventEntity> events = eventService.findByType(instanceEventTypes);
        Map<String, InstanceEntity> instances = new HashMap<>();

        events.stream().forEach(event -> {
            String gatewayId = event.getProperties().get("id");
            InstanceEntity instance = instances.getOrDefault(gatewayId, new InstanceEntity(gatewayId));

            switch (event.getType()) {
                case START_GATEWAY:
                    instance.setStartedAt(event.getCreatedAt());
                    instance.setHostname(event.getProperties().get("hostname"));
                    instance.setVersion(event.getProperties().get("version"));
                    instance.setIp(event.getProperties().get("ip"));
                    instance.setPort(Integer.parseInt(event.getProperties().get("port")));
                    String tags = event.getProperties().get("tags");
                    if (tags != null && ! tags.isEmpty()) {
                        instance.setTags(Arrays.asList(event.getProperties().get("tags").trim().split(",")));
                    } else {
                        instance.setTags(Collections.EMPTY_LIST);
                    }
                    break;
                case STOP_GATEWAY:
                    instance.setStoppedAt(event.getCreatedAt());
                    break;
            }

            instances.put(gatewayId, instance);
        });

        return instances.values();
    }
}
