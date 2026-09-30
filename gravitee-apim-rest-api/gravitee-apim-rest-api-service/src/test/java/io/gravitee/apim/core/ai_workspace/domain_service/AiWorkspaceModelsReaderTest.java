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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import org.junit.jupiter.api.Test;

class AiWorkspaceModelsReaderTest {

    private static final String SECRET = "sk-live-do-not-leak";

    @Test
    void reads_name_and_prices_and_drops_credentials() throws Exception {
        var models = AiWorkspaceModelsReader.read(proxy(configuration()));

        assertThat(models).hasSize(2);
        assertThat(models.get(0).name()).isEqualTo("gpt-4o");
        assertThat(models.get(0).inputPrice()).isEqualTo(2.5);
        assertThat(models.get(0).outputPrice()).isEqualTo(10.0);
        assertThat(models.get(1).name()).isEqualTo("no-price");
        assertThat(models.get(1).inputPrice()).isNull();
        assertThat(models.get(1).outputPrice()).isNull();

        String serialized = new ObjectMapper().writeValueAsString(models);
        assertThat(serialized).doesNotContain(SECRET);
        assertThat(serialized).doesNotContain("authentication");
        assertThat(serialized).doesNotContain("provider");
    }

    @Test
    void missing_or_broken_configuration_is_an_empty_list() {
        assertThat(AiWorkspaceModelsReader.read(null)).isEmpty();
        assertThat(AiWorkspaceModelsReader.read(proxy("not-json"))).isEmpty();
        assertThat(AiWorkspaceModelsReader.read(proxy("{\"models\":[]}"))).isEmpty();
    }

    private static Api proxy(String configuration) {
        Endpoint endpoint = new Endpoint();
        endpoint.setConfiguration(configuration);
        EndpointGroup group = new EndpointGroup();
        group.setEndpoints(java.util.List.of(endpoint));
        Api api = new Api();
        api.setEndpointGroups(java.util.List.of(group));
        return api;
    }

    private static String configuration() {
        return (
            "{\"provider\":\"openai\",\"target\":\"https://api.openai.com\",\"authentication\":{\"apiKey\":\"" +
            SECRET +
            "\"},\"models\":[" +
            "{\"name\":\"gpt-4o\",\"inputPrice\":2.5,\"outputPrice\":10.0}," +
            "{\"name\":\"no-price\"}," +
            "{\"inputPrice\":1}" +
            "]}"
        );
    }
}
