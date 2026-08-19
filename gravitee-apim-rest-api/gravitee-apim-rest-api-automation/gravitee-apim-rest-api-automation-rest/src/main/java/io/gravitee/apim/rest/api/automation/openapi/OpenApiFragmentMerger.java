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
package io.gravitee.apim.rest.api.automation.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Merges the OpenAPI fragments Gamma modules publish into the Automation API document, so a running
 * instance serves one document describing every resource it answers on.
 *
 * <p>Fragments contribute {@code paths}, {@code components.*} and {@code tags}; the base document keeps
 * its {@code info}, {@code servers} and {@code security}. A fragment must be self-contained and use
 * module-prefixed component names: a path or component that already exists is a packaging error and is
 * reported as such rather than silently overwritten.
 */
public final class OpenApiFragmentMerger {

    private static final YAMLMapper YAML = new YAMLMapper();

    private static final String PATHS = "paths";
    private static final String COMPONENTS = "components";
    private static final String TAGS = "tags";
    private static final String NAME = "name";

    private OpenApiFragmentMerger() {}

    /**
     * @param base the Automation API document (YAML)
     * @param fragmentsByModule each module's fragment (YAML), keyed by module id
     * @return the merged document (YAML)
     */
    public static String merge(InputStream base, Map<String, String> fragmentsByModule) throws IOException {
        ObjectNode document = readObject(base);
        fragmentsByModule.forEach((module, fragment) -> mergeFragment(document, module, readObject(fragment)));
        return YAML.writeValueAsString(document);
    }

    private static void mergeFragment(ObjectNode document, String module, ObjectNode fragment) {
        mergeMap(document, fragment, PATHS, module, "path");
        JsonNode fragmentComponents = fragment.get(COMPONENTS);
        if (fragmentComponents != null && fragmentComponents.isObject()) {
            ObjectNode components = document.withObject("/" + COMPONENTS);
            fragmentComponents
                .properties()
                .forEach(section -> mergeMap(components, (ObjectNode) fragmentComponents, section.getKey(), module, "component"));
        }
        JsonNode fragmentTags = fragment.get(TAGS);
        if (fragmentTags != null && fragmentTags.isArray()) {
            var tags = document.withArray("/" + TAGS);
            fragmentTags.forEach(tag -> {
                boolean known = false;
                for (JsonNode existing : tags) {
                    known |= existing.path(NAME).equals(tag.path(NAME));
                }
                if (!known) {
                    tags.add(tag);
                }
            });
        }
    }

    private static void mergeMap(ObjectNode target, ObjectNode source, String field, String module, String what) {
        JsonNode entries = source.get(field);
        if (entries == null || !entries.isObject()) {
            return;
        }
        ObjectNode into = target.withObject("/" + field);
        entries
            .properties()
            .forEach(entry -> {
                if (into.has(entry.getKey())) {
                    throw new IllegalStateException(
                        "Automation OpenAPI fragment of module [" + module + "] redefines " + what + " [" + entry.getKey() + "]"
                    );
                }
                into.set(entry.getKey(), entry.getValue());
            });
    }

    private static ObjectNode readObject(InputStream yaml) throws IOException {
        return asObject(YAML.readTree(yaml));
    }

    private static ObjectNode readObject(String yaml) {
        try {
            return asObject(YAML.readTree(yaml));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Automation OpenAPI fragment is not valid YAML", e);
        }
    }

    private static ObjectNode asObject(JsonNode node) {
        if (!(node instanceof ObjectNode object)) {
            throw new IllegalStateException("An OpenAPI document must be a YAML mapping");
        }
        return object;
    }
}
