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
package io.gravitee.rest.api.model.platform.plugin;

import java.util.Map;

public enum SchemaDisplayFormat {
    GV_SCHEMA_FORM("gv-schema-form"),
    GIO_FORM_JSON_SCHEMA("gio-form-json-schema");

    private static final Map<String, SchemaDisplayFormat> BY_LABEL = Map.of(
        GV_SCHEMA_FORM.label,
        GV_SCHEMA_FORM,
        GIO_FORM_JSON_SCHEMA.label,
        GIO_FORM_JSON_SCHEMA
    );

    private final String label;

    SchemaDisplayFormat(String label) {
        this.label = label;
    }

    public static SchemaDisplayFormat fromLabel(final String label) {
        return BY_LABEL.getOrDefault(label, GIO_FORM_JSON_SCHEMA);
    }

    public String getLabel() {
        return label;
    }
}
