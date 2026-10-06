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
package io.gravitee.apim.core.ai_workspace.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiWorkspaceEndpointReaderTest {

    @Test
    void returns_the_llm_proxy_listener_path() {
        var path = AiWorkspaceEndpointReader.read(
            "DEFAULT",
            List.of(proxy("api-1", "DEFAULT", ApiType.LLM_PROXY, "/test-local-spf/"), proxy("other", "DEFAULT", ApiType.PROXY, "/ignored/"))
        );

        assertThat(path).isEqualTo("/test-local-spf/");
    }

    @Test
    void skips_a_blank_path_and_returns_the_next_one() {
        Api api = Api.builder()
            .id("api-1")
            .environmentId("DEFAULT")
            .type(ApiType.LLM_PROXY)
            .apiDefinitionHttpV4(
                io.gravitee.definition.model.v4.Api.builder()
                    .listeners(
                        List.of(
                            HttpListener.builder()
                                .paths(
                                    List.of(
                                        Path.builder().path(" ").build(),
                                        Path.builder().path(null).build(),
                                        Path.builder().path("/alpha/").build()
                                    )
                                )
                                .build()
                        )
                    )
                    .build()
            )
            .build();

        assertThat(AiWorkspaceEndpointReader.read("DEFAULT", List.of(api))).isEqualTo("/alpha/");
    }

    @Test
    void ignores_a_proxy_from_another_environment_and_a_missing_path() {
        assertThat(AiWorkspaceEndpointReader.read("DEFAULT", List.of(proxy("api-1", "OTHER", ApiType.LLM_PROXY, "/elsewhere/")))).isNull();
        assertThat(AiWorkspaceEndpointReader.read("DEFAULT", List.of())).isNull();
        assertThat(AiWorkspaceEndpointReader.read("DEFAULT", null)).isNull();
    }

    private static Api proxy(String id, String environmentId, ApiType type, String path) {
        return Api.builder()
            .id(id)
            .environmentId(environmentId)
            .type(type)
            .apiDefinitionHttpV4(
                io.gravitee.definition.model.v4.Api.builder()
                    .listeners(List.of(HttpListener.builder().paths(List.of(Path.builder().path(path).build())).build()))
                    .build()
            )
            .build();
    }
}
