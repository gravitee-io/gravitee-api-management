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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportedFileContentTypeTest {

    @Nested
    class Importable {

        @ParameterizedTest
        @ValueSource(strings = { "guide.md", "spec.yaml", "spec.yml", "spec.json", "SPEC.JSON", "docs/nested/guide.md" })
        void should_accept_the_extensions_the_portal_renders(String fileName) {
            assertThat(ImportedFileContentType.isImportable(fileName)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = { "legacy.adoc", "logo.png", "Makefile", "archive.tar.gz" })
        void should_reject_any_other_extension(String fileName) {
            assertThat(ImportedFileContentType.isImportable(fileName)).isFalse();
        }
    }

    @Nested
    class Markdown {

        @Test
        void should_map_markdown_from_its_extension_alone() {
            assertThat(ImportedFileContentType.from("guide.md", null)).contains(PortalPageContentType.GRAVITEE_MARKDOWN);
        }
    }

    @Nested
    class Specs {

        @Test
        void should_detect_openapi_from_the_root_property() {
            assertThat(ImportedFileContentType.from("petstore.yaml", "openapi: 3.0.3\ninfo:\n  title: Petstore")).contains(
                PortalPageContentType.OPENAPI
            );
        }

        @Test
        void should_detect_swagger_from_the_root_property() {
            assertThat(ImportedFileContentType.from("petstore.json", "{\"swagger\": \"2.0\", \"info\": {}}")).contains(
                PortalPageContentType.OPENAPI
            );
        }

        @Test
        void should_detect_asyncapi_from_the_root_property() {
            assertThat(ImportedFileContentType.from("events.yaml", "asyncapi: 3.0.0\ninfo:\n  title: Events")).contains(
                PortalPageContentType.ASYNCAPI
            );
        }

        @Test
        void should_detect_asyncapi_in_a_compact_json_document() {
            // A single-line JSON spec: nothing puts the asyncapi key at the start of a line
            assertThat(ImportedFileContentType.from("events.json", "{\"asyncapi\":\"3.0.0\",\"info\":{}}")).contains(
                PortalPageContentType.ASYNCAPI
            );
        }

        @Test
        void should_not_let_a_nested_asyncapi_key_reclassify_an_openapi_document() {
            var openApiMentioningAsyncApi = """
                openapi: 3.0.3
                info:
                  title: Petstore
                components:
                  schemas:
                    Link:
                      properties:
                        asyncapi: { type: string }
                """;
            assertThat(ImportedFileContentType.from("petstore.yaml", openApiMentioningAsyncApi)).contains(PortalPageContentType.OPENAPI);
        }
    }

    @Nested
    class NotADocument {

        @Test
        void should_reject_a_json_file_that_is_not_a_spec() {
            // The repository file that made every mirror import create a broken OpenAPI page
            assertThat(ImportedFileContentType.from("package.json", "{\"name\": \"docs\", \"version\": \"1.0.0\"}")).isEmpty();
        }

        @Test
        void should_reject_a_yaml_file_that_is_not_a_spec() {
            assertThat(ImportedFileContentType.from("ci.yml", "name: build\non:\n  push:\n    branches: [master]")).isEmpty();
        }

        @Test
        void should_reject_a_document_that_parses_to_something_else_than_an_object() {
            assertThat(ImportedFileContentType.from("list.yaml", "- one\n- two")).isEmpty();
        }

        @Test
        void should_reject_an_unparseable_document() {
            assertThat(ImportedFileContentType.from("broken.yaml", "openapi: [3.0.3\n  bad: {")).isEmpty();
        }

        @Test
        void should_reject_a_spec_extension_without_content() {
            assertThat(ImportedFileContentType.from("spec.yaml", null)).isEmpty();
            assertThat(ImportedFileContentType.from("spec.yaml", "   ")).isEmpty();
        }

        @Test
        void should_reject_an_unsupported_extension() {
            assertThat(ImportedFileContentType.from("legacy.adoc", "= Legacy")).isEmpty();
        }
    }
}
