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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.EnvironmentApiLog;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LogsEngineMapperTest {

    @Test
    void should_map_null_api_type_to_null() {
        assertThat(LogsEngineMapper.INSTANCE.mapApiType(null)).isNull();
    }

    @Test
    void should_map_proxy_to_http_proxy() {
        assertThat(LogsEngineMapper.INSTANCE.mapApiType(ApiType.PROXY)).isEqualTo(EnvironmentApiLog.ApiTypeEnum.HTTP_PROXY);
    }

    @Test
    void should_map_llm_proxy_to_llm_proxy() {
        assertThat(LogsEngineMapper.INSTANCE.mapApiType(ApiType.LLM_PROXY)).isEqualTo(EnvironmentApiLog.ApiTypeEnum.LLM_PROXY);
    }

    @Test
    void should_map_mcp_proxy_to_mcp_proxy() {
        assertThat(LogsEngineMapper.INSTANCE.mapApiType(ApiType.MCP_PROXY)).isEqualTo(EnvironmentApiLog.ApiTypeEnum.MCP_PROXY);
    }

    @ParameterizedTest
    @ValueSource(strings = { "A2A_PROXY", "MESSAGE", "NATIVE" })
    void should_map_non_http_types_to_null(String apiTypeName) {
        var result = LogsEngineMapper.INSTANCE.mapApiType(ApiType.valueOf(apiTypeName));
        assertThat(result).isNull();
    }
}
