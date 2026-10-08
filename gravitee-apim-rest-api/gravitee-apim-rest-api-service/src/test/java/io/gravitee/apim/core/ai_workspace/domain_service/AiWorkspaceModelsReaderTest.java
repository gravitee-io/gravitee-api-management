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

import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceModel;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiWorkspaceModelsReaderTest {

    @Test
    void lists_models_from_every_provider_and_keeps_a_model_with_no_price() {
        Api proxy = llmProxy(
            "api-1",
            "DEFAULT",
            List.of(
                group(
                    endpoint(
                        "{\"provider\":\"OPEN_AI\",\"target\":\"https://provider.example\",\"authentication\":{\"apiKey\":\"sk-secret\"}," +
                            "\"models\":[{\"name\":\"gpt-4o\",\"inputPrice\":2.5,\"outputPrice\":10}," +
                            "{\"name\":\"draft\"},{\"name\":\" \"}]}"
                    )
                ),
                group(endpoint("{\"provider\":\"ANTHROPIC\",\"models\":[{\"name\":\"claude\",\"outputPrice\":0.5}]}"))
            )
        );

        List<AiWorkspaceModel> models = AiWorkspaceModelsReader.read("DEFAULT", List.of(proxy));

        assertThat(models).containsExactly(
            new AiWorkspaceModel("gpt-4o", 2.5, 10.0),
            new AiWorkspaceModel("draft", null, null),
            new AiWorkspaceModel("claude", null, 0.5)
        );
        assertThat(models.toString()).doesNotContain("sk-secret").doesNotContain("provider.example").doesNotContain("authentication");
    }

    @Test
    void lists_each_model_name_once_and_keeps_the_first_price() {
        Api proxy = llmProxy(
            "api-1",
            "DEFAULT",
            List.of(
                group(
                    endpoint(
                        "{\"aliasOnly\":true,\"models\":[{\"name\":\"gpt-4o\",\"aliases\":[\"chat\"],\"inputPrice\":2.5,\"outputPrice\":10},{\"name\":\"gpt-4o-mini\",\"inputPrice\":0.15,\"outputPrice\":0.6}]}"
                    ),
                    endpoint(
                        "{\"models\":[{\"name\":\"gpt-4o\",\"inputPrice\":9,\"outputPrice\":9},{\"name\":\"o3-mini\",\"inputPrice\":1.1}]}"
                    )
                )
            )
        );

        List<AiWorkspaceModel> models = AiWorkspaceModelsReader.read("DEFAULT", List.of(proxy));

        assertThat(models).containsExactly(
            new AiWorkspaceModel("gpt-4o", 2.5, 10.0),
            new AiWorkspaceModel("gpt-4o-mini", 0.15, 0.6),
            new AiWorkspaceModel("o3-mini", 1.1, null)
        );
        assertThat(models.toString()).doesNotContain("chat").doesNotContain("aliasOnly").doesNotContain("aliases");
    }

    @Test
    void returns_an_empty_list_when_there_is_no_proxy_no_models_or_the_configuration_cannot_be_read() {
        assertThat(AiWorkspaceModelsReader.read("DEFAULT", null)).isEmpty();
        assertThat(AiWorkspaceModelsReader.read("DEFAULT", List.of())).isEmpty();
        assertThat(
            AiWorkspaceModelsReader.read("DEFAULT", List.of(llmProxy("api-1", "OTHER", List.of(group(endpoint("{\"models\":[]}"))))))
        ).isEmpty();
        assertThat(
            AiWorkspaceModelsReader.read("DEFAULT", List.of(Api.builder().id("plain").environmentId("DEFAULT").type(ApiType.PROXY).build()))
        ).isEmpty();
        assertThat(
            AiWorkspaceModelsReader.read(
                "DEFAULT",
                List.of(
                    llmProxy("api-1", "DEFAULT", List.of(group(endpoint("{\"models\":[],\"authentication\":{\"apiKey\":\"sk-secret\"}}"))))
                )
            )
        ).isEmpty();
        assertThat(
            AiWorkspaceModelsReader.read("DEFAULT", List.of(llmProxy("api-1", "DEFAULT", List.of(group(endpoint("{not json"))))))
        ).isEmpty();
    }

    private static Api llmProxy(String id, String environmentId, List<EndpointGroup> groups) {
        return Api.builder()
            .id(id)
            .environmentId(environmentId)
            .type(ApiType.LLM_PROXY)
            .apiDefinitionHttpV4(io.gravitee.definition.model.v4.Api.builder().endpointGroups(groups).build())
            .build();
    }

    private static EndpointGroup group(Endpoint... endpoints) {
        return EndpointGroup.builder().name("provider").type("llm").endpoints(List.of(endpoints)).build();
    }

    private static Endpoint endpoint(String configuration) {
        return Endpoint.builder().name("endpoint").type("llm-proxy").configuration(configuration).build();
    }
}
