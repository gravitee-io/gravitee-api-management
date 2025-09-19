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
package io.gravitee.gateway.reactive.handlers.api.v4.processor.pathparameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PathParametersExtractorTest {

    @Test
    void can_not_extract_param_null_api() {
        assertThatThrownBy(() -> new PathParametersExtractor(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Api is mandatory");
    }

    @Test
    void can_not_extract_param_no_flow() {
        assertThat(new PathParametersExtractor(new Api()).canExtractPathParams()).isFalse();
    }

    @Test
    void can_extract_param_flow_with_path_param() {
        final Api api = new Api();
        final Flow flow = new Flow();
        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setPath("/products/:productId");
        httpSelector.setPathOperator(Operator.STARTS_WITH);
        httpSelector.setMethods(Set.of());
        flow.setSelectors(List.of(httpSelector));
        api.setFlows(List.of(flow));
        assertThat(new PathParametersExtractor(api).canExtractPathParams()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("io.gravitee.gateway.handlers.api.processor.pathparameters.PathParametersExtractorTest#provideParameters")
    void can_extract_flow_and_extract_param_on_request(
        String api,
        String method,
        String path,
        Map<String, String> expectedPathParam,
        Set<String> excludedPathParam
    ) throws IOException {
        final PathParametersExtractor cut = new PathParametersExtractor(readApi(api));
        final Map<String, String> pathParams = cut.extract(method, path);
        assertThat(pathParams).isEqualTo(expectedPathParam).doesNotContainKeys(excludedPathParam.toArray(new String[0]));
    }

    private static Api readApi(String name) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(
            PathParametersExtractorTest.class.getClassLoader().getResourceAsStream("v4/apis/pathparams/" + name + ".json"),
            Api.class
        );
    }
}
