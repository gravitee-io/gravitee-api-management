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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.genericevent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * The JSON of a generic event, as the control plane publishes it in an event and the primary node distributes it.
 */
public final class GenericEventPayload {

    private GenericEventPayload() {}

    public static GenericEvent read(ObjectMapper objectMapper, String payload) throws JsonProcessingException {
        var node = objectMapper.readTree(payload);
        var content = node.get("payload");
        return new GenericEvent(
            text(node, "kind"),
            text(node, "id"),
            text(node, "environmentId"),
            text(node, "organizationId"),
            instant(node, "expiresAt"),
            instant(node, "updatedAt"),
            content == null || content.isNull() ? null : content.toString()
        );
    }

    public static String write(ObjectMapper objectMapper, GenericEvent genericEvent) throws JsonProcessingException {
        var node = objectMapper.createObjectNode();
        node.put("kind", genericEvent.kind());
        node.put("id", genericEvent.id());
        node.put("environmentId", genericEvent.environmentId());
        node.put("organizationId", genericEvent.organizationId());
        if (genericEvent.expiresAt() != null) {
            node.put("expiresAt", genericEvent.expiresAt().toString());
        }
        if (genericEvent.updatedAt() != null) {
            node.put("updatedAt", genericEvent.updatedAt().toString());
        }
        if (genericEvent.payload() != null) {
            node.set("payload", objectMapper.readTree(genericEvent.payload()));
        }
        return objectMapper.writeValueAsString(node);
    }

    /** Id of the event's events_latest row, computed as the control plane does. */
    public static String latestEventId(GenericEvent genericEvent) {
        var key = genericEvent.environmentId() + ":" + genericEvent.kind() + ":" + genericEvent.id();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** Why the event cannot be applied for this action; null when it can. */
    public static String invalid(GenericEvent genericEvent, SyncAction syncAction) {
        if (isBlank(genericEvent.kind()) || isBlank(genericEvent.id()) || isBlank(genericEvent.environmentId())) {
            return "missing kind, id or environmentId";
        }
        if (syncAction == SyncAction.DEPLOY && genericEvent.payload() == null) {
            return "missing payload";
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Instant instant(JsonNode node, String field) {
        var value = text(node, field);
        return value == null ? null : Instant.parse(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
