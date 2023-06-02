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
package io.gravitee.apim.integration.tests.http.flows;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Provides:
 * - path of the request
 * - Expected headers on request
 * - Expected headers on response
 *
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowPhaseExecutionParameterProviders {

    public static String requestFlowHeader(int flowNumber) {
        return "X-Request-Flow-" + flowNumber;
    }

    public static String responseFlowHeader(int flowNumber) {
        return "X-Response-Flow-" + flowNumber;
    }

    public static String headerValue(String flowPath) {
        return "Flow " + flowPath;
    }

    public static Stream<Arguments> parametersEqualsOperatorCase() {
        return Stream.of(
            Arguments.of(
                "/products",
                Map.of(requestFlowHeader(1), headerValue("/products")),
                Map.of(responseFlowHeader(1), headerValue("/products"))
            ),
            Arguments.of("/", Map.of(requestFlowHeader(0), headerValue("/")), Map.of(responseFlowHeader(0), headerValue("/"))),
            Arguments.of("/products/id", Map.of(), Map.of()),
            Arguments.of("/random", Map.of(), Map.of())
        );
    }

    public static Stream<Arguments> parametersStartsWithOperatorCase() {
        return Stream.of(
            Arguments.of(
                "/products",
                Map.of(requestFlowHeader(0), headerValue("/"), requestFlowHeader(1), headerValue("/products")),
                Map.of(responseFlowHeader(0), headerValue("/"), responseFlowHeader(1), headerValue("/products"))
            ),
            Arguments.of("/", Map.of(requestFlowHeader(0), headerValue("/")), Map.of(responseFlowHeader(0), headerValue("/"))),
            Arguments.of(
                "/products/id",
                Map.of(requestFlowHeader(0), headerValue("/"), requestFlowHeader(1), headerValue("/products")),
                Map.of(responseFlowHeader(0), headerValue("/"), responseFlowHeader(1), headerValue("/products"))
            ),
            Arguments.of("/random", Map.of(requestFlowHeader(0), headerValue("/")), Map.of(responseFlowHeader(0), headerValue("/")))
        );
    }

    public static Stream<Arguments> parametersMixedOperatorCase() {
        return Stream.of(
            Arguments.of(
                "/products",
                Map.of(requestFlowHeader(1), headerValue("/products")),
                Map.of(responseFlowHeader(1), headerValue("/products"))
            ),
            Arguments.of("/", Map.of(requestFlowHeader(0), headerValue("/")), Map.of(responseFlowHeader(0), headerValue("/"))),
            Arguments.of(
                "/products/id",
                Map.of(requestFlowHeader(1), headerValue("/products")),
                Map.of(responseFlowHeader(1), headerValue("/products"))
            ),
            Arguments.of("/random", Map.of(), Map.of())
        );
    }

    /**
     * Provides:
     * - path of the request
     * - Header to send holding the value to satisfy condition
     * - Expected headers on request
     * - Expected headers on response
     */
    public static Stream<Arguments> parametersConditionalFlowsCase() {
        return Stream.of(
            // /products flow is executed
            Arguments.of(
                "/products",
                "product-condition",
                Map.of(requestFlowHeader(1), headerValue("/products")),
                Map.of(responseFlowHeader(1), headerValue("/products"))
            ),
            // no flow is executed
            Arguments.of("/products", "root-condition", Map.of(), Map.of()),
            // / flow is executed
            Arguments.of(
                "/",
                "root-condition",
                Map.of(requestFlowHeader(0), headerValue("/")),
                Map.of(responseFlowHeader(0), headerValue("/"))
            ),
            // no flow is executed
            Arguments.of("/", "random-condition", Map.of(), Map.of()),
            // /products flow is executed
            Arguments.of(
                "/products/id",
                "product-condition",
                Map.of(requestFlowHeader(1), headerValue("/products")),
                Map.of(responseFlowHeader(1), headerValue("/products"))
            ),
            // / flow is executed because it starts with / and condition is fulfilled
            Arguments.of(
                "/products/id",
                "root-condition",
                Map.of(requestFlowHeader(0), headerValue("/")),
                Map.of(responseFlowHeader(0), headerValue("/"))
            ),
            // no flow is executed
            Arguments.of("/random", "random-condition", Map.of(), Map.of()),
            // no flow is executed
            Arguments.of("/random", "root-condition", Map.of(), Map.of())
        );
    }
}
