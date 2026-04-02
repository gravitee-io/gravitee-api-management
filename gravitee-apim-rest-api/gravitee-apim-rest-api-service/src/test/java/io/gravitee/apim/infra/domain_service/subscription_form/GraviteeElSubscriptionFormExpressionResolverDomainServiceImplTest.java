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
package io.gravitee.apim.infra.domain_service.subscription_form;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GraviteeElSubscriptionFormExpressionResolverDomainServiceImplTest {

    private final GraviteeElSubscriptionFormExpressionResolverDomainServiceImpl cut =
        new GraviteeElSubscriptionFormExpressionResolverDomainServiceImpl();

    @ParameterizedTest
    @MethodSource("should_resolve_to_expected_options_cases")
    void should_resolve_to_expected_options(String expression, Map<String, Object> templateParams, List<String> expected) {
        var options = cut.resolveToOptions(expression, templateParams);

        assertThat(options).containsExactlyElementsOf(expected);
    }

    private static Stream<Arguments> should_resolve_to_expected_options_cases() {
        return Stream.of(
            Arguments.of("{#api.metadata['envs']}", metadataParams("envs", "Dev,Staging,Prod"), List.of("Dev", "Staging", "Prod")),
            Arguments.of("{#api.metadata['env']}", metadataParams("env", "Production"), List.of("Production")),
            Arguments.of("{#api.metadata['envs']}", metadataParams("envs", " Dev , Staging , Prod "), List.of("Dev", "Staging", "Prod")),
            Arguments.of("{#api.metadata['envs']}", metadataParams("envs", ",Dev,,Staging,"), List.of("Dev", "Staging")),
            Arguments.of("{#'   '}", Map.of(), List.of()),
            Arguments.of("{#api.metadata['nonexistent']}", metadataParams("envs", "Dev"), List.of()),
            Arguments.of("{#api.metadata['envs']}", Map.of(), List.of()),
            Arguments.of("{#null}", Map.of(), List.of()),
            Arguments.of("{#api.metadata['envs']", metadataParams("envs", "Dev"), List.of())
        );
    }

    private static Map<String, Object> metadataParams(String key, String value) {
        return Map.of("api", Map.of("metadata", Map.of(key, value)));
    }
}
