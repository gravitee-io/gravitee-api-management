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
package io.gravitee.gamma.authorization.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.common.utils.UUID;
import io.gravitee.gamma.authorization.api.AuthzEventPublisher;
import io.gravitee.gamma.authorization.domain.AuthzEntity;
import io.gravitee.gamma.authorization.domain.AuthzPolicy;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class EventRepositoryAuthzEventPublisher implements AuthzEventPublisher {

    private static final List<String> SENSITIVE_KEY_SUBSTRINGS = List.of("password", "secret", "token", "credential", "apikey");

    static final String REDACTED = "[REDACTED]";

    private final EventRepository eventRepository;
    private final EventLatestRepository eventLatestRepository;
    private final ObjectMapper objectMapper;

    public EventRepositoryAuthzEventPublisher(
        EventRepository eventRepository,
        EventLatestRepository eventLatestRepository,
        ObjectMapper objectMapper
    ) {
        this.eventRepository = eventRepository;
        this.eventLatestRepository = eventLatestRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishPolicyDeployed(AuthzPolicy policy) {
        Objects.requireNonNull(policy, "policy must not be null");
        io.gravitee.gamma.definition.authz.AuthzPolicy wire = new io.gravitee.gamma.definition.authz.AuthzPolicy(
            policy.id(),
            policy.name(),
            io.gravitee.gamma.definition.authz.AuthzPolicyKind.valueOf(policy.kind().name()),
            policy.entityId(),
            policy.policyText(),
            policy.environmentId(),
            policy.updatedAt().toString()
        );
        emit(
            policy.environmentId(),
            EventType.PUBLISH_AUTHZ_POLICY,
            wire,
            Map.of(Event.EventProperties.AUTHZ_POLICY_ID.getValue(), policy.id())
        );
    }

    @Override
    public void unpublishPolicy(AuthzPolicy policy) {
        Objects.requireNonNull(policy, "policy must not be null");
        // Unpublish carries minimal context — gateway only needs id + kind + envId to evict.
        // We still emit a typed AuthzPolicy with placeholders for required fields the gateway
        // will ignore (name/policyText) to keep one wire shape per event family.
        io.gravitee.gamma.definition.authz.AuthzPolicy wire = new io.gravitee.gamma.definition.authz.AuthzPolicy(
            policy.id(),
            policy.name(),
            io.gravitee.gamma.definition.authz.AuthzPolicyKind.valueOf(policy.kind().name()),
            policy.entityId(),
            policy.policyText(),
            policy.environmentId(),
            policy.updatedAt().toString()
        );
        emit(
            policy.environmentId(),
            EventType.UNPUBLISH_AUTHZ_POLICY,
            wire,
            Map.of(Event.EventProperties.AUTHZ_POLICY_ID.getValue(), policy.id())
        );
    }

    @Override
    public void publishEntityUpserted(AuthzEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        io.gravitee.gamma.definition.authz.AuthzEntity wire = io.gravitee.gamma.definition.authz.AuthzEntity
            .builder()
            .id(entity.id())
            .entityId(entity.entityId())
            .kind(io.gravitee.gamma.definition.authz.AuthzEntityKind.valueOf(entity.kind().name()))
            .attributes(redactSensitive(entity.attributes()))
            .parents(entity.parents())
            .source(entity.source())
            .environmentId(entity.environmentId())
            .updatedAt(entity.updatedAt().toString())
            .entityType(entity.entityType())
            .build();
        emit(
            entity.environmentId(),
            EventType.PUBLISH_AUTHZ_ENTITY,
            wire,
            Map.of(Event.EventProperties.AUTHZ_ENTITY_ID.getValue(), entity.entityId())
        );
    }

    @Override
    public void unpublishEntity(AuthzEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        io.gravitee.gamma.definition.authz.AuthzEntity wire = io.gravitee.gamma.definition.authz.AuthzEntity
            .builder()
            .id(entity.id())
            .entityId(entity.entityId())
            .kind(io.gravitee.gamma.definition.authz.AuthzEntityKind.valueOf(entity.kind().name()))
            .source(entity.source())
            .environmentId(entity.environmentId())
            .updatedAt(entity.updatedAt().toString())
            .entityType(entity.entityType())
            .build();
        emit(
            entity.environmentId(),
            EventType.UNPUBLISH_AUTHZ_ENTITY,
            wire,
            Map.of(Event.EventProperties.AUTHZ_ENTITY_ID.getValue(), entity.entityId())
        );
    }

    /**
     * Writes the append-only event log first, then refreshes the per-resource latest snapshot.
     * The latest write keys by resource id (not a fresh event id) so a later UNPUBLISH overwrites
     * the matching PUBLISH; a random id per emit would accumulate one row per event and never evict
     * a deleted resource. Order matters: if the second write fails the canonical log still has the
     * entry and a later emit for the same id reconciles the latest collection.
     */
    private void emit(String environmentId, EventType type, Object payload, Map<String, String> properties) {
        Event event = buildEvent(environmentId, type, payload, properties);
        Event latest = buildEvent(environmentId, type, payload, properties);
        latest.setId(latestEventId(properties));
        try {
            eventRepository.create(event);
            eventLatestRepository.createOrUpdate(latest);
        } catch (TechnicalException e) {
            throw new AuthzEventPublishException("Failed to emit authz event " + type + " for env " + environmentId, e);
        }
    }

    private static String latestEventId(Map<String, String> properties) {
        // events_latest must hold one row per resource; reuse the single {AUTHZ_*_ID: id} property,
        // prefixed with its key to avoid _id collisions across resource types in the shared collection.
        Map.Entry<String, String> resource = properties.entrySet().iterator().next();
        return resource.getKey() + ":" + resource.getValue();
    }

    private Event buildEvent(String environmentId, EventType type, Object payload, Map<String, String> properties) {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        event.setEnvironments(Set.of(environmentId));
        event.setType(type);
        event.setPayload(serialise(payload));
        event.setProperties(properties);
        Date now = Date.from(TimeProvider.instantNow());
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return event;
    }

    private String serialise(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise authz event payload", e);
        }
    }

    static Map<String, Object> redactSensitive(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return attributes;
        }
        Map<String, Object> redacted = new LinkedHashMap<>(attributes.size());
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            redacted.put(entry.getKey(), isSensitiveKey(entry.getKey()) ? REDACTED : entry.getValue());
        }
        return redacted;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String normalised = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        for (String marker : SENSITIVE_KEY_SUBSTRINGS) {
            if (normalised.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
