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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenApiFragmentMergerTest {

    private static final YAMLMapper YAML = new YAMLMapper();

    private static final String BASE = """
        openapi: 3.1.0
        info:
          title: Automation · API
          version: 1.0.0
        paths:
          /organizations/{orgId}/environments/{envId}/apis:
            put:
              operationId: createOrUpdateApis
              responses:
                "200":
                  description: ok
        """;

    private static final String AIM_FRAGMENT = """
        openapi: 3.1.0
        info:
          title: aim automation
          version: 1.0.0
        tags:
          - name: AI Catalog
        paths:
          /organizations/{orgId}/environments/{envId}/aim/catalog/mcp-servers:
            put:
              operationId: createOrUpdateAimCatalogMcpServer
              responses:
                "200":
                  description: ok
        """;

    @Nested
    class Main {

        @TempDir
        Path dir;

        private final PrintStream stdout = System.out;
        private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

        @BeforeEach
        void captureStdout() {
            System.setOut(new PrintStream(captured, true, UTF_8));
        }

        @AfterEach
        void restoreStdout() {
            System.setOut(stdout);
        }

        @Test
        void should_print_the_base_document_merged_with_each_module_fragment() throws Exception {
            Path base = write("open-api.yaml", BASE);
            Path aim = write("aim.yaml", AIM_FRAGMENT);

            OpenApiFragmentMerger.main(new String[] { base.toString(), "aim=" + aim });

            JsonNode merged = YAML.readTree(captured.toString(UTF_8));
            assertThat(merged.path("paths").has("/organizations/{orgId}/environments/{envId}/apis")).isTrue();
            assertThat(merged.path("paths").has("/organizations/{orgId}/environments/{envId}/aim/catalog/mcp-servers")).isTrue();
            assertThat(merged.path("tags").findValuesAsText("name")).containsExactly("AI Catalog");
            assertThat(merged.path("info").path("title").asText()).isEqualTo("Automation · API");
        }

        @Test
        void should_reject_a_fragment_argument_without_a_module() throws Exception {
            Path base = write("open-api.yaml", BASE);
            Path aim = write("aim.yaml", AIM_FRAGMENT);

            assertThatThrownBy(() -> OpenApiFragmentMerger.main(new String[] { base.toString(), aim.toString() }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("<module>=<fragment.yaml>");
        }

        @Test
        void should_reject_a_call_without_a_base_document() {
            assertThatThrownBy(() -> OpenApiFragmentMerger.main(new String[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usage");
        }

        private Path write(String name, String content) throws Exception {
            return Files.writeString(dir.resolve(name), content, UTF_8);
        }
    }
}
