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
package io.gravitee.apim.infra.domain_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.exception.ValidationDomainException;
import org.junit.jupiter.api.Test;

class OpenApiPathMappingsExtractorImplTest {

    private final OpenApiPathMappingsExtractorImpl extractor = new OpenApiPathMappingsExtractorImpl();

    @Test
    void should_extract_openapi3_paths() {
        var yaml = """
            openapi: 3.0.0
            info:
              title: pets
              version: 1.0.0
            paths:
              /pets/{petId}:
                get:
                  responses:
                    '200':
                      description: ok
            """;

        assertThat(extractor.extract(yaml)).containsExactly("/pets/:petId");
    }

    @Test
    void should_extract_swagger2_paths() {
        var json = """
            {"swagger":"2.0","info":{"title":"pets","version":"1.0.0"},"paths":{"/pets/{petId}":{"get":{"responses":{"200":{"description":"ok"}}}}}}
            """;

        assertThat(extractor.extract(json)).containsExactly("/pets/:petId");
    }

    @Test
    void should_reject_invalid_content() {
        assertThatThrownBy(() -> extractor.extract("not a spec")).isInstanceOf(ValidationDomainException.class);
    }

    @Test
    void should_reject_url_page_content() {
        assertThatThrownBy(() -> extractor.extract("https://example.com/openapi.yaml"))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("inline");
    }

    @Test
    void should_extract_paths_when_spec_contains_an_unresolved_remote_ref() {
        var yaml = """
            openapi: 3.0.0
            info:
              title: pets
              version: 1.0.0
            paths:
              /pets:
                get:
                  responses:
                    '200':
                      description: ok
                      content:
                        application/json:
                          schema:
                            $ref: https://example.com/does-not-exist.yaml#/components/schemas/Pet
            """;

        assertThat(extractor.extract(yaml)).containsExactly("/pets");
    }
}
