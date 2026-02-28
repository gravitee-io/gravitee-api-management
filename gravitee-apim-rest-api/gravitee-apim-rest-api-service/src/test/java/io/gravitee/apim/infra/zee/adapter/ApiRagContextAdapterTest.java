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
package io.gravitee.apim.infra.zee.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import io.gravitee.definition.model.DefinitionVersion;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiRagContextAdapterTest {

    private ApiCrudService apiCrudService;
    private ApiRagContextAdapter adapter;

    @BeforeEach
    void setUp() {
        apiCrudService = mock(ApiCrudService.class);
        adapter = new ApiRagContextAdapter(apiCrudService);
    }

    @Test
    void resource_type_is_api() {
        assertThat(adapter.resourceType()).isEqualTo(ZeeResourceType.API);
    }

    @Nested
    class RetrieveContext {

        @Test
        void returns_api_details_when_api_found() {
            var api = Api.builder()
                    .name("Pet Store API")
                    .version("1.0.0")
                    .description("A pet store sample API")
                    .definitionVersion(DefinitionVersion.V4)
                    .build();
            when(apiCrudService.findById("api-123")).thenReturn(Optional.of(api));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).contains("### Current API Details");
            assertThat(context).contains("Name: Pet Store API");
            assertThat(context).contains("Version: 1.0.0");
            assertThat(context).contains("Description: A pet store sample API");
            assertThat(context).contains("Definition Version: V4");
        }

        @Test
        void returns_empty_when_api_not_found() {
            when(apiCrudService.findById("api-missing")).thenReturn(Optional.empty());

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-missing"));

            assertThat(context).isEmpty();
        }

        @Test
        void returns_empty_when_api_id_missing() {
            var context = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(context).isEmpty();
        }

        @Test
        void degrades_gracefully_when_service_throws() {
            when(apiCrudService.findById("api-123")).thenThrow(new RuntimeException("DB unavailable"));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).isEmpty();
        }

        @Test
        void handles_unnamed_api_gracefully() {
            var api = Api.builder()
                    .name(null)
                    .version(null)
                    .build();
            when(apiCrudService.findById("api-123")).thenReturn(Optional.of(api));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).contains("(unnamed)");
            assertThat(context).contains("(unversioned)");
        }

        @Test
        void omits_description_when_blank() {
            var api = Api.builder()
                    .name("Test API")
                    .version("2.0")
                    .description("")
                    .build();
            when(apiCrudService.findById("api-123")).thenReturn(Optional.of(api));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).doesNotContain("Description:");
        }
    }
}
