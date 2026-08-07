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
package io.gravitee.gamma.rest.core.observability.filter.model;

/**
 * Which document contract a logs row is read from. Orthogonal to {@link ApiType}: that one narrows
 * <em>which APIs</em> are in scope, this one selects <em>what kind of record</em> is returned. A
 * request log and an authorization decision share neither an index nor a shape.
 *
 * @author GraviteeSource Team
 */
public enum RecordType {
    REQUEST("Request"),
    AUTHZ_DECISION("Authz decision");

    private final String label;

    RecordType(String label) {
        this.label = label;
    }

    /** Human-readable display label for this record kind. */
    public String label() {
        return label;
    }

    public static RecordType fromNameOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return REQUEST;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return REQUEST;
        }
    }
}
