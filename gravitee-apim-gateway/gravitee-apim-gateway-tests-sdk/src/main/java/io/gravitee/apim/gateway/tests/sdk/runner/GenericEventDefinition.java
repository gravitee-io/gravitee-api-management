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
package io.gravitee.apim.gateway.tests.sdk.runner;

import static io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest.DEFAULT_ID;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * The JSON resource describing a generic event, whose payload is any JSON object, or a JSON string holding the payload as is.
 *
 * @author GraviteeSource Team
 */
record GenericEventDefinition(String kind, String id, String environmentId, String organizationId, String expiresAt, JsonNode payload) {
    /**
     * @param resourcePath the resource the definition was read from, named when it lacks what the sync requires or holds an
     *     invalid expiresAt
     */
    GenericEvent toGenericEvent(String resourcePath) {
        List<String> missing = new ArrayList<>();
        if (kind == null || kind.isBlank()) {
            missing.add("kind");
        }
        if (id == null || id.isBlank()) {
            missing.add("id");
        }
        if (payload == null || payload.isNull()) {
            missing.add("payload");
        }
        if (!missing.isEmpty()) {
            throw new PreconditionViolationException(
                "The generic event resource " + resourcePath + " has no " + String.join(", ", missing)
            );
        }
        return new GenericEvent(
            kind,
            id,
            environmentId != null ? environmentId : DEFAULT_ID,
            organizationId != null ? organizationId : DEFAULT_ID,
            expiresAt != null ? parseExpiresAt(resourcePath) : null,
            Instant.now(),
            payload.isTextual() ? payload.textValue() : payload.toString()
        );
    }

    private Instant parseExpiresAt(String resourcePath) {
        try {
            return Instant.parse(expiresAt);
        } catch (DateTimeParseException e) {
            throw new PreconditionViolationException("The generic event resource " + resourcePath + " has an invalid expiresAt", e);
        }
    }
}
