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

public class PortalNavigationItemId implements Comparable<PortalNavigationItemId> {

    @Nonnull
    private final UUID id;

    private PortalNavigationItemId(@Nonnull UUID id) {
        this.id = id;
    }

    public static final PortalNavigationItemId ZERO = new PortalNavigationItemId(new UUID(0L, 0L));

    public static PortalNavigationItemId zero() {
        return ZERO;
    }

    public static PortalNavigationItemId random() {
        return new PortalNavigationItemId(UUID.randomUUID());
    }

    public static PortalNavigationItemId of(String value) {
        return new PortalNavigationItemId(UUID.fromString(value));
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
        PortalNavigationItemId portalNavigationItemId = (PortalNavigationItemId) o;
        return id.equals(portalNavigationItemId.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public int compareTo(PortalNavigationItemId other) {
        return this.id.compareTo(other.id);
    }
}
