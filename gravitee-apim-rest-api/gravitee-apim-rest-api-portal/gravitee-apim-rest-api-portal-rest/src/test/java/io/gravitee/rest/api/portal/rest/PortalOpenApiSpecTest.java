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
package io.gravitee.rest.api.portal.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * The published API reference shows a Classic Portal and a Next Gen Portal entry, filtered on the
 * {@code x-gravitee-portal} extension, so every operation must carry it.
 */
class PortalOpenApiSpecTest {

    private static final String PORTAL_EXTENSION = "x-gravitee-portal";
    private static final Set<String> PORTALS = Set.of("classic", "next", "both");
    private static final Set<String> HTTP_METHODS = Set.of("get", "put", "post", "delete", "patch", "head", "options");

    private static List<Operation> operations;

    record Operation(String method, String path, Map<String, Object> definition) {
        Object portal() {
            return definition.get(PORTAL_EXTENSION);
        }

        @Override
        public String toString() {
            return method.toUpperCase() + " " + path;
        }
    }

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void load_spec() throws IOException {
        // Read from the sources: a dummy portal-openapi.yaml in the test resources shadows it on the classpath.
        try (InputStream spec = Files.newInputStream(Path.of("src/main/resources/portal-openapi.yaml"))) {
            Map<String, Object> root = new Yaml().load(spec);
            var paths = (Map<String, Map<String, Object>>) root.get("paths");
            operations = new ArrayList<>();
            paths.forEach((path, item) ->
                item.forEach((key, value) -> {
                    if (HTTP_METHODS.contains(key)) {
                        operations.add(new Operation(key, path, (Map<String, Object>) value));
                    }
                })
            );
        }
    }

    @Test
    void should_declare_the_portal_of_every_operation() {
        assertThat(operations).isNotEmpty();
        assertThat(operations)
            .filteredOn(operation -> operation.portal() == null || !PORTALS.contains(operation.portal()))
            .as("operations without a valid %s (one of %s)", PORTAL_EXTENSION, PORTALS)
            .isEmpty();
    }
}
