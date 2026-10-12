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
package io.gravitee.apim.rest.api.mcp.core.model;

import io.gravitee.apim.rest.api.mcp.core.TriageException;

public enum StatusGroup {
    CLIENT_ERROR("4XX"),
    SERVER_ERROR("5XX");

    private final String label;

    StatusGroup(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** @return null when no group is asked for */
    public static StatusGroup parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var group : values()) {
            if (group.label.equalsIgnoreCase(value.trim())) {
                return group;
            }
        }
        throw new TriageException("Invalid statusGroup '" + value + "'. Use 4XX or 5XX.");
    }
}
