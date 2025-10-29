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
package io.gravitee.apim.core.portal_page.model;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nonnull;
import java.util.UUID;

public class PortalPageNavigationId {

    @Nonnull
    private final UUID id;

    private PortalPageNavigationId(@Nonnull UUID id) {
        this.id = id;
    }

    public static PortalPageNavigationId random() {
        return new PortalPageNavigationId(UUID.randomUUID());
    }

    public static PortalPageNavigationId of(String value) {
        return new PortalPageNavigationId(UUID.fromString(value));
    }

    public UUID id() {
        return id;
    }

    @JsonValue
    public String json() {
        return this.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalPageNavigationId portalPageNavigationId = (PortalPageNavigationId) o;
        return id.equals(portalPageNavigationId.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
