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
package io.gravitee.gateway.handlers.api.processor.pathparameters;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.flow.Operator;
import java.util.regex.Pattern;
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
class PathParametersTest {

    @Test
    void should_not_have_parameters_starts_with_operator() {
        final PathParameters cut = new PathParameters("/product/apim/item/portal", Operator.STARTS_WITH);
        assertThat(cut.getParameters()).isEmpty();
        assertThat(cut.getPathPattern()).hasToString(Pattern.compile("^/product/apim/item/portal(?:/.*)?$").toString());
    }

    @Test
    void should_not_have_parameters_equals_operator() {
        final PathParameters cut = new PathParameters("/product/apim/item/portal", Operator.EQUALS);
        assertThat(cut.getParameters()).isEmpty();
        assertThat(cut.getPathPattern()).hasToString(Pattern.compile("^/product/apim/item/portal/?$").toString());
    }

    @Test
    void should_have_parameters_starts_with_operator() {
        final PathParameters cut = new PathParameters("/product/:productId/item/:itemId", Operator.STARTS_WITH);
        assertThat(cut.getParameters()).hasSize(2).extracting(PathParameter::getName).contains("productId", "itemId");
        assertThat(cut.getPathPattern()).hasToString(
            Pattern.compile(
                "^/product/(?<group0>[a-zA-Z0-9\\-._~%!$&'()* +,;=:@|]+)/item/(?<group1>[a-zA-Z0-9\\-._~%!$&'()* +,;=:@|]+)(?:/.*)?$"
            ).toString()
        );
    }

    @Test
    void should_have_parameters_equals_operator() {
        final PathParameters cut = new PathParameters("/product/:productId/item/:itemId", Operator.EQUALS);
        assertThat(cut.getParameters()).hasSize(2).extracting(PathParameter::getName).contains("productId", "itemId");
        assertThat(cut.getPathPattern()).hasToString(
            Pattern.compile(
                "^/product/(?<group0>[a-zA-Z0-9\\-._~%!$&'()* +,;=:@|]+)/item/(?<group1>[a-zA-Z0-9\\-._~%!$&'()* +,;=:@|]+)/?$"
            ).toString()
        );
    }

    @Test
    void should_have_parameters_with_special_character() {
        final PathParameters cut = new PathParameters("/product/:product-id/item/:It€m_Id", Operator.STARTS_WITH);
        assertThat(cut.getParameters()).hasSize(2).extracting(PathParameter::getName).contains("product-id", "It€m_Id");
        assertThat(cut.getPathPattern()).hasToString(
            Pattern.compile(
                "^/product/(?<group0>[a-zA-Z0-9\\-._~%!$&'()* +,;=:@|]+)/item/(?<group1>[a-zA-Z0-9\\-._~%!$&'()* +,;=:@|]+)(?:/.*)?$"
            ).toString()
        );
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void should_check_equality(final PathParameters first, final PathParameters second, boolean expectedResult) {
        assertThat(first.equals(second)).isEqualTo(expectedResult);
    }

    public static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of(
                new PathParameters("/products/", Operator.STARTS_WITH),
                new PathParameters("/products/", Operator.STARTS_WITH),
                true
            ),
            Arguments.of(
                new PathParameters("/products/", Operator.STARTS_WITH),
                new PathParameters("/products/second", Operator.STARTS_WITH),
                false
            ),
            Arguments.of(new PathParameters("/products/", Operator.STARTS_WITH), null, false),
            Arguments.of(new PathParameters("/products/", Operator.EQUALS), new PathParameters("/products/", Operator.STARTS_WITH), false)
        );
    }
}
