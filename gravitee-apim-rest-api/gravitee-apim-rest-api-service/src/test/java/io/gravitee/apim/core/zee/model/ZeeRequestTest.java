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
package io.gravitee.apim.core.zee.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ZeeRequestTest {

    @Nested
    class Validation {

        @Test
        void rejects_null_resource_type() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ZeeRequest(null, "create a flow", null, null))
                    .withMessage("resourceType is required");
        }

        @Test
        void rejects_null_prompt() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ZeeRequest(ZeeResourceType.FLOW, null, null, null))
                    .withMessage("prompt is required");
        }

        @Test
        void rejects_blank_prompt() {
            assertThatThrownBy(() -> new ZeeRequest(ZeeResourceType.FLOW, "   ", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("prompt cannot be blank");
        }

        @Test
        void rejects_empty_prompt() {
            assertThatThrownBy(() -> new ZeeRequest(ZeeResourceType.FLOW, "", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("prompt cannot be blank");
        }
    }

    @Nested
    class Defaults {

        @Test
        void null_files_defaults_to_empty_list() {
            var request = new ZeeRequest(ZeeResourceType.FLOW, "create a flow", null, null);
            assertThat(request.files()).isEmpty();
        }

        @Test
        void null_context_data_defaults_to_empty_map() {
            var request = new ZeeRequest(ZeeResourceType.FLOW, "create a flow", null, null);
            assertThat(request.contextData()).isEmpty();
        }
    }

    @Nested
    class Immutability {

        @Test
        void files_are_defensively_copied() {
            var files = new java.util.ArrayList<>(List.of(new FileContent("spec.yaml", "content", "text/yaml")));
            var request = new ZeeRequest(ZeeResourceType.API, "create an API", files, null);

            assertThatThrownBy(() -> request.files().add(new FileContent("extra.json", "{}", "application/json")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void context_data_is_defensively_copied() {
            var context = new java.util.HashMap<>(Map.of("apiId", (Object) "abc-123"));
            var request = new ZeeRequest(ZeeResourceType.API, "create an API", null, context);

            assertThatThrownBy(() -> request.contextData().put("extra", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    void valid_request_preserves_all_fields() {
        var files = List.of(new FileContent("openapi.json", "{}", "application/json"));
        var context = Map.of("apiId", (Object) "api-1", "envId", (Object) "env-1");

        var request = new ZeeRequest(ZeeResourceType.ENDPOINT, "create an endpoint", files, context);

        assertThat(request.resourceType()).isEqualTo(ZeeResourceType.ENDPOINT);
        assertThat(request.prompt()).isEqualTo("create an endpoint");
        assertThat(request.files()).hasSize(1);
        assertThat(request.contextData()).containsEntry("apiId", "api-1");
    }
}
