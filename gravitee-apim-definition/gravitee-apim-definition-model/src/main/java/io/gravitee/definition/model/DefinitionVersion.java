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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux@graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public enum DefinitionVersion {
    @JsonEnumDefaultValue
    V1("1.0.0"),
    V2("2.0.0"),
    V4("4.0.0"),
    FEDERATED("FEDERATED"),
    FEDERATED_AGENT("FEDERATED_AGENT");

    private static final Map<String, DefinitionVersion> BY_LABEL = new HashMap<>();

    static {
        for (DefinitionVersion e : values()) {
            BY_LABEL.put(e.label, e);
        }
    }

    private final String label;

    @JsonCreator
    public static DefinitionVersion valueOfLabel(String label) {
        return BY_LABEL.get(label);
    }

    public static Set<String> versions() {
        return BY_LABEL.keySet();
    }

    // First argument is a regex so ignore sonar because of false positive
    @SuppressWarnings("java:S5361")
    public Integer asInteger() {
        return Integer.valueOf(label.replaceAll("\\.", ""));
    }

    // @JsonValue on the getter instead of attribute makes enum values relevant in generated openapi file
    @JsonValue
    public String getLabel() {
        return label;
    }
}
