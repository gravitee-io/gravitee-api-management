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
package io.gravitee.apim.core.generic_event.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonRawValue;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.With;

/**
 * An event a feature publishes to the gateways of an environment: configuration, state or a time-boxed action applied
 * without redeploying an API.
 *
 * @param kind <code>&lt;feature&gt;.&lt;type&gt;</code>
 * @param organizationId the organization of the environment, for the gateways' license checks
 * @param payload the feature's own JSON, sent as is
 */
@Builder(toBuilder = true)
@With
public record GenericEvent(
    String kind,
    String id,
    String environmentId,
    String organizationId,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expiresAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant updatedAt,
    @JsonRawValue String payload
) {
    /** Id of the genericEvent's events_latest row: one per environment, kind and id. */
    public String latestEventId() {
        return latestEventId(environmentId, kind, id);
    }

    public static String latestEventId(String environmentId, String kind, String id) {
        return UUID.nameUUIDFromBytes((environmentId + ":" + kind + ":" + id).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
