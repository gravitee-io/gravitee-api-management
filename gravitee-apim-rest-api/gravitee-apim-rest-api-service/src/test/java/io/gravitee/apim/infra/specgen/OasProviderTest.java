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
package io.gravitee.apim.infra.specgen;

import static io.gravitee.rest.api.service.common.UuidString.generateRandom;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.apim.core.specgen.service_provider.OasProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.swagger.v3.core.util.Yaml;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class OasProviderTest {

    static final String BAD_SPEC = "bad spec";
    static final String RAW_SPEC = "openapi: 3.0.3";
    static final String RAW_SPEC_WITH_DESC = "openapi: 3.0.3\ninfo:\n  description: Made with love, powered by NewtAI";
    static final String DECORATED_SPEC = "openapi: 3.0.3\ninfo:\n  name: Api name\n  version: 1.0.0\n  description: Some desc";
    static final String DECORATED_SPEC_WITH_DESC =
        "openapi: 3.0.3\ninfo:\n  name: Api name\n  version: 1.0.0\n  description: Made with love, powered by NewtAI";

    OasProvider oasProvider;

    public static Stream<Arguments> params_that_must_parse() {
        return Stream.of(
            Arguments.of(RAW_SPEC, getApiSpecGen(""), DECORATED_SPEC),
            Arguments.of(RAW_SPEC_WITH_DESC, getApiSpecGen(""), DECORATED_SPEC_WITH_DESC),
            Arguments.of(RAW_SPEC, getApiSpecGen("Some desc"), DECORATED_SPEC),
            Arguments.of(RAW_SPEC_WITH_DESC, getApiSpecGen("Some desc"), DECORATED_SPEC)
        );
    }

    @BeforeEach
    void setUp() {
        oasProvider = new OasProviderImpl();
    }

    @ParameterizedTest
    @MethodSource("params_that_must_parse")
    void must_parse(String inputSpec, ApiSpecGen api, String expectedOutput) throws JsonProcessingException {
        var output = oasProvider.decorateSpecification(api, inputSpec);

        var actual = Yaml.mapper().readTree(output);
        var expected = Yaml.mapper().readTree(expectedOutput);

        assertThat(actual.get("openapi").asText()).isEqualTo(expected.get("openapi").asText());

        var actualInfo = actual.get("info");
        var expectedInfo = actual.get("info");

        assertThat(actualInfo).isNotNull();
        assertThat(expectedInfo).isNotNull();
        assertThat(actualInfo.get("title")).isEqualTo(expectedInfo.get("title"));
        assertThat(actualInfo.get("description")).isEqualTo(expectedInfo.get("description"));
        assertThat(actualInfo.get("version")).isEqualTo(expectedInfo.get("version"));
    }

    @Test
    void must_not_parse_bad_spec() {
        var output = oasProvider.decorateSpecification(getApiSpecGen(""), BAD_SPEC);

        assertThat(output).isEqualTo(BAD_SPEC);
    }

    private static ApiSpecGen getApiSpecGen(String description) {
        return new ApiSpecGen(generateRandom(), "Api name", description, "1.0.0", ApiType.PROXY, "env-id");
    }
}
