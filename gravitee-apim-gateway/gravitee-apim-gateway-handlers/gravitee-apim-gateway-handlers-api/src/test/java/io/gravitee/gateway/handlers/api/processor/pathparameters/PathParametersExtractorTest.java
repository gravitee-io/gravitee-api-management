/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.handlers.api.processor.pathparameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.gateway.handlers.api.definition.Api;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
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
        assertThatThrownBy(() -> new PathParametersExtractor(null)).isInstanceOf(NullPointerException.class).hasMessage("Api is mandatory");
    }

    @Test
    void can_not_extract_param_no_flow() {
        assertThat(new PathParametersExtractor(new Api(new io.gravitee.definition.model.Api())).canExtractPathParams()).isFalse();
    }

    @Test
    void can_not_extract_param_no_flow_with_path_param() {
        final Api api = new Api(new io.gravitee.definition.model.Api());
        final Flow flow = new Flow();
        final PathOperator pathOperator = new PathOperator();
        pathOperator.setOperator(Operator.STARTS_WITH);
        pathOperator.setPath("/products");
        flow.setPathOperator(pathOperator);
        api.getDefinition().setFlows(List.of(flow));
        assertThat(new PathParametersExtractor(api).canExtractPathParams()).isFalse();
    }

    @Test
    void can_extract_param_flow_with_path_param() {
        final Api api = new Api(new io.gravitee.definition.model.Api());
        final Flow flow = new Flow();
        final PathOperator pathOperator = new PathOperator();
        pathOperator.setOperator(Operator.STARTS_WITH);
        pathOperator.setPath("/products/:productId");
        flow.setPathOperator(pathOperator);
        api.getDefinition().setFlows(List.of(flow));
        assertThat(new PathParametersExtractor(api).canExtractPathParams()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void can_extract_flow_and_extract_param_on_request(
        Api api,
        String method,
        String path,
        Map<String, String> expectedPathParam,
        Set<String> excludedPathParam
    ) {
        final PathParametersExtractor cut = new PathParametersExtractor(api);
        final Map<String, String> pathParams = cut.extract(method, path);
        assertThat(pathParams).isEqualTo(expectedPathParam).doesNotContainKeys(excludedPathParam.toArray(new String[0]));
    }

    public static Stream<Arguments> provideParameters() throws IOException {
        return Stream.of(
            Arguments.of(readApi("simple-api"), "GET", "/products", Map.of(), Set.of()),
            Arguments.of(readApi("simple-api"), "TRACE", "/products", Map.of(), Set.of()),
            Arguments.of(readApi("simple-api"), "GET", "/products/my-product", Map.of("productId", "my-product"), Set.of()),
            Arguments.of(readApi("simple-api"), "GET", "/products-special-char/my-product", Map.of("product-id", "my-product"), Set.of()),
            Arguments.of(
                readApi("simple-api"),
                "GET",
                "/products/my-product/hello",
                Map.of("productId", "my-product", "id", "my-product"),
                Set.of()
            ),
            Arguments.of(readApi("simple-api"), "DELETE", "/products/my-product/hello", Map.of("productId", "my-product"), Set.of("id")),
            Arguments.of(readApi("simple-api"), "PUT", "/products/my-product/hello", Map.of("id", "my-product"), Set.of("productId")),
            Arguments.of(
                readApi("simple-api"),
                "GET",
                "/products/my-product/hello/something",
                Map.of("productId", "my-product"),
                Set.of("id")
            ),
            Arguments.of(
                readApi("simple-api"),
                "GET",
                "/products/my-product/items/my-item",
                Map.of("productId", "my-product", "itemId", "my-item"),
                Set.of()
            ),
            Arguments.of(
                readApi("simple-api"),
                "GET",
                "/products-special-char/my-product/items/my-item",
                Map.of("product-id", "my-product", "It€m_Id", "my-item"),
                Set.of()
            ),
            Arguments.of(readApi("api-flows-equals-operator"), "GET", "/products", Map.of(), Set.of()),
            Arguments.of(readApi("api-flows-equals-operator"), "TRACE", "/products", Map.of(), Set.of()),
            Arguments.of(readApi("api-flows-equals-operator"), "GET", "/products/my-product", Map.of("productId", "my-product"), Set.of()),
            Arguments.of(
                readApi("api-flows-equals-operator"),
                "GET",
                "/products/my-product/hello",
                Map.of("id", "my-product"),
                Set.of("productId")
            ),
            Arguments.of(readApi("api-flows-equals-operator"), "DELETE", "/products/my-product/hello", Map.of(), Set.of("productId", "id")),
            Arguments.of(
                readApi("api-flows-equals-operator"),
                "DELETE",
                "/products/my-product",
                Map.of("productId", "my-product"),
                Set.of("id")
            ),
            Arguments.of(
                readApi("api-flows-equals-operator"),
                "PUT",
                "/products/my-product/hello",
                Map.of("id", "my-product"),
                Set.of("productId")
            ),
            Arguments.of(
                readApi("api-flows-equals-operator"),
                "GET",
                "/products/my-product/hello/something",
                Map.of(),
                Set.of("productId", "id")
            ),
            Arguments.of(
                readApi("api-flows-equals-operator"),
                "GET",
                "/products/my-product/items/my-item",
                Map.of("productId", "my-product", "itemId", "my-item"),
                Set.of()
            ),
            // This test is a particular overlapping case:
            // - GET starts with /products/:productId/item/:itemId
            // - *   starts with /:productId
            // As wildcard flows are evaluated first, 'productId' will have the value overridden from GET flow.
            Arguments.of(
                readApi("api-overlap"),
                "GET",
                "/products/my-product/items/my-item",
                Map.of("productId", "my-product", "itemId", "my-item"),
                Set.of()
            ),
            // This test is a particular overlapping case:
            // - *   starts with /products/:productId/item/:itemId
            // - GET starts with /:productId
            // As wildcard flows are evaluated first, 'productId' will have the value overridden from * (wildcard) flow.
            Arguments.of(
                readApi("api-overlap-reverse-wildcard"),
                "GET",
                "/products/my-product/items/my-item",
                Map.of("productId", "products", "itemId", "my-item"),
                Set.of()
            )
        );
    }

    private static Api readApi(String name) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return new Api(
            mapper.readValue(
                PathParametersExtractorTest.class.getClassLoader().getResourceAsStream("apis/pathparams/" + name + ".json"),
                io.gravitee.definition.model.Api.class
            )
        );
    }
}
