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
package io.gravitee.gamma.definition.authz;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Single source of truth for an agent's PRINCIPAL entity id, so the offline AM→entity sync and the
 * request-time PEP both compute the same value and policies match.
 *
 * <p>Like every other synced principal (users keyed on their {@code sub}, groups/roles on their id),
 * the id is a bare value with the type carried in the {@code _kind} attribute — no kind prefix. It is
 * a name-based UUID of the agent's OAuth {@code client_id}: hashing keeps it within
 * {@link AuthzEntityIdConstants#FORMAT_REGEX} even when the client_id is itself illegal in the grammar
 * (mixed case, slashes, dots — e.g. a CIMD URL). The {@code client_id} is unique within an org's single
 * AM connection, so the domain is not folded in here; that can change if multiple domains are synced.
 */
public final class AgentEntityId {

    private AgentEntityId() {}

    public static String derive(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be null or blank");
        }
        return UUID.nameUUIDFromBytes(clientId.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
