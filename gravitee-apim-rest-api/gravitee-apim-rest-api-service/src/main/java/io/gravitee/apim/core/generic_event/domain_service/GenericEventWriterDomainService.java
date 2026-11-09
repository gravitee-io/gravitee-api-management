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
package io.gravitee.apim.core.generic_event.domain_service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.generic_event.model.GenericEvent;
import io.gravitee.apim.core.generic_event.model.PublicationOptions;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Writes a generic event to the latest events the gateways read, and to the events history when asked.
 */
@DomainService
@RequiredArgsConstructor
public class GenericEventWriterDomainService {

    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;
    private final ObjectMapper objectMapper;

    public void write(EventType type, GenericEvent genericEvent, AuditInfo auditInfo, PublicationOptions options) {
        var properties = properties(genericEvent, auditInfo);
        if (options.deploymentLabel() != null) {
            properties.put(Event.EventProperties.DEPLOYMENT_LABEL, options.deploymentLabel());
        }
        var event = options.historized()
            ? eventCrudService.createEvent(
                auditInfo.organizationId(),
                auditInfo.environmentId(),
                Set.of(auditInfo.environmentId()),
                type,
                genericEvent,
                properties
            )
            : latestOnly(type, genericEvent, properties);
        var expiresAt = genericEvent.expiresAt() == null ? null : genericEvent.expiresAt().atZone(ZoneOffset.UTC);
        eventLatestCrudService.createOrPatchLatestEvent(
            auditInfo.organizationId(),
            genericEvent.latestEventId(),
            event.toBuilder().expiresAt(expiresAt).build()
        );
    }

    private Event latestOnly(EventType type, GenericEvent genericEvent, EnumMap<Event.EventProperties, String> properties) {
        var now = TimeProvider.now();
        return Event.builder()
            .id(genericEvent.latestEventId())
            .type(type)
            .payload(serialize(genericEvent))
            .environments(Set.of(genericEvent.environmentId()))
            .properties(properties)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private String serialize(GenericEvent genericEvent) {
        try {
            return objectMapper.writeValueAsString(genericEvent);
        } catch (JsonProcessingException e) {
            throw new TechnicalManagementException("Failed to serialize generic event [" + genericEvent.latestEventId() + "]", e);
        }
    }

    private static EnumMap<Event.EventProperties, String> properties(GenericEvent genericEvent, AuditInfo auditInfo) {
        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.GENERIC_EVENT_ID, genericEvent.latestEventId());
        properties.put(Event.EventProperties.GENERIC_EVENT_KIND, genericEvent.kind());
        if (auditInfo.actor() != null && auditInfo.actor().userId() != null) {
            properties.put(Event.EventProperties.USER, auditInfo.actor().userId());
        }
        return properties;
    }
}
