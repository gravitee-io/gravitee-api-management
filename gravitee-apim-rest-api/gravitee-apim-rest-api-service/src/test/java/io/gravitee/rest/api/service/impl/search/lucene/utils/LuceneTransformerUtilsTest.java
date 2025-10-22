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
package io.gravitee.rest.api.service.impl.search.lucene.utils;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import java.util.List;
import org.junit.jupiter.api.Test;

class LuceneTransformerUtilsTest {

    @Test
    public void generate_api_type_v4_native() {
        Api api = Api.builder().definitionVersion(DefinitionVersion.V4).type(ApiType.NATIVE).build();
        String apiType = LuceneTransformerUtils.generateApiType(api);
        assertThat(apiType).isEqualTo("V4_KAFKA");
    }

    @Test
    public void generate_api_type_v4_proxy() {
        Api api = Api.builder().definitionVersion(DefinitionVersion.V4).type(ApiType.PROXY).build();
        String apiType = LuceneTransformerUtils.generateApiType(api);
        assertThat(apiType).isEqualTo("V4_HTTP_PROXY");
    }

    @Test
    public void generate_api_type_v4_tcp_proxy() {
        Api api = Api.builder()
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.PROXY)
            .apiDefinitionHttpV4(
                io.gravitee.definition.model.v4.Api.builder()
                    .listeners(List.of(TcpListener.builder().entrypoints(List.of()).build()))
                    .build()
            )
            .build();
        String apiType = LuceneTransformerUtils.generateApiType(api);
        assertThat(apiType).isEqualTo("V4_TCP_PROXY");
    }

    @Test
    public void generate_api_type_v4_mcp_proxy() {
        Api api = Api.builder().definitionVersion(DefinitionVersion.V4).type(ApiType.MCP_PROXY).build();
        String apiType = LuceneTransformerUtils.generateApiType(api);
        assertThat(apiType).isEqualTo("V4_MCP_PROXY");
    }

    @Test
    public void generate_api_type_v4_llm_proxy() {
        Api api = Api.builder().definitionVersion(DefinitionVersion.V4).type(ApiType.LLM_PROXY).build();
        String apiType = LuceneTransformerUtils.generateApiType(api);
        assertThat(apiType).isEqualTo("V4_LLM_PROXY");
    }

    @Test
    public void generate_api_type_v4_message() {
        Api api = Api.builder().definitionVersion(DefinitionVersion.V4).type(ApiType.MESSAGE).build();
        String apiType = LuceneTransformerUtils.generateApiType(api);
        assertThat(apiType).isEqualTo("V4_MESSAGE");
    }

    @Test
    public void generate_api_type_v2() {
        Api api = Api.builder().definitionVersion(DefinitionVersion.V2).build();
        String apiType = LuceneTransformerUtils.generateApiType(api);
        assertThat(apiType).isEqualTo("V2");
    }

    @Test
    public void generate_api_type_federated() {
        Api api = Api.builder().definitionVersion(DefinitionVersion.FEDERATED).build();
        String apiType = LuceneTransformerUtils.generateApiType(api);
        assertThat(apiType).isEqualTo("FEDERATED");
    }

    @Test
    public void generate_api_type_federated_agent() {
        Api api = Api.builder().definitionVersion(DefinitionVersion.FEDERATED_AGENT).build();
        String apiType = LuceneTransformerUtils.generateApiType(api);
        assertThat(apiType).isEqualTo("FEDERATED_AGENT");
    }
}
