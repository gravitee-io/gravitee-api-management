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
package io.gravitee.rest.api.service.v4.impl.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.service.v4.exception.PathParameterOverlapValidationException;
import io.gravitee.rest.api.service.v4.validation.PathParametersValidationService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.Condition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Deprecated
class PathParametersValidationServiceImplTest {

    private PathParametersValidationService cut;

    @BeforeEach
    void setUp() {
        cut = new PathParametersValidationServiceImpl();
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void should_test_overlaping_cases(String apiName, Map<String, List<String>> expectedOverlaps) throws IOException {
        final Api api = readApi(apiName);

        if (expectedOverlaps.isEmpty()) {
            assertThatNoException().isThrownBy(() -> cut.validate(api.getType(), api.getFlows().stream(), getPlanFlows(api)));
        } else {
            assertThatThrownBy(() -> cut.validate(api.getType(), api.getFlows().stream(), getPlanFlows(api)))
                .isInstanceOf(PathParameterOverlapValidationException.class)
                .hasMessage("Some path parameters are used at different position across different flows.")
                .is(
                    new Condition<>(
                        error -> {
                            final PathParameterOverlapValidationException pathParamException =
                                (PathParameterOverlapValidationException) error;
                            assertThat(pathParamException.getDetailMessage()).isEqualTo("There is a path parameter overlap");
                            assertThat(pathParamException.getConstraints()).containsOnlyKeys(expectedOverlaps.keySet());
                            expectedOverlaps.forEach((key, value) -> {
                                value.forEach(expectedPath -> {
                                    assertThat(pathParamException.getConstraints().get(key)).contains(expectedPath);
                                });
                            });
                            return true;
                        },
                        ""
                    )
                );
        }
    }

    public static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("api-proxy-flows-overlap", Map.of(":productId", List.of("/products/:productId/items/:itemId", "/:productId"))),
            Arguments.of("api-proxy-plans-overlap", Map.of(":productId", List.of("/products/:productId/items/:itemId", "/:productId"))),
            Arguments.of(
                "api-proxy-plans-and-flows-overlap",
                Map.of(":productId", List.of("/products/:productId/items/:itemId", "/:productId"))
            ),
            Arguments.of("api-proxy-no-overlap", Map.of()),
            Arguments.of("api-proxy-no-flows", Map.of()),
            Arguments.of("api-message-flows-overlap", Map.of(":productId", List.of("/products/:productId/items/:itemId", "/:productId"))),
            Arguments.of("api-message-plans-overlap", Map.of(":productId", List.of("/products/:productId/items/:itemId", "/:productId"))),
            Arguments.of(
                "api-message-plans-and-flows-overlap",
                Map.of(":productId", List.of("/products/:productId/items/:itemId", "/:productId"))
            ),
            Arguments.of("api-message-no-overlap", Map.of()),
            Arguments.of("api-message-no-flows", Map.of()),
            Arguments.of("api-llm-proxy-no-flows", Map.of()),
            Arguments.of("api-llm-proxy-with-flows", Map.of())
        );
    }

    private static Api readApi(String name) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(
            PathParametersValidationServiceImplTest.class.getClassLoader().getResourceAsStream("apis/v4/pathparams/" + name + ".json"),
            Api.class
        );
    }

    @NotNull
    private static Stream<Flow> getPlanFlows(Api api) {
        return api
            .getPlans()
            .stream()
            .flatMap(plan -> plan.getFlows() == null ? Stream.empty() : plan.getFlows().stream());
    }
}
