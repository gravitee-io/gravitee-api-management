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
package io.gravitee.apim.core.portal.model;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nonnull;
import java.util.UUID;

public record PortalId(@Nonnull UUID id) {
    private static final UUID ZERO_UUID = new UUID(0L, 0L);
    public static final PortalId ZERO = new PortalId(ZERO_UUID);

    public static PortalId random() {
        return new PortalId(UUID.randomUUID());
    }

    public static PortalId of(String value) {
        var uuid = UUID.fromString(value);
        return ZERO_UUID.equals(uuid) ? ZERO : new PortalId(uuid);
    }

    @JsonValue
    @Override
    public String toString() {
        return id.toString();
    }
}
