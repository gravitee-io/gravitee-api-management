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
package io.gravitee.apim.core.flow.domain_service;

import static inmemory.EntrypointPluginQueryServiceInMemory.SSE_CONNECTOR_ID;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.EntrypointPluginQueryServiceInMemory;
import io.gravitee.apim.core.api.exception.NativeApiWithMultipleFlowsException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.exception.InvalidFlowException;
import io.gravitee.apim.core.plugin.query_service.EntrypointPluginQueryService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Condition;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class FlowValidationDomainServiceTest {

    PolicyValidationDomainService policyValidationDomainService;
    EntrypointPluginQueryService entrypointPluginQueryService = new EntrypointPluginQueryServiceInMemory();

    FlowValidationDomainService service;

    @BeforeEach
    void setUp() {
        policyValidationDomainService = mock(PolicyValidationDomainService.class);
        lenient()
            .when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));

        service = new FlowValidationDomainService(policyValidationDomainService, entrypointPluginQueryService);
    }

    @Nested
    class ValidateAndSanitizeHttpV4 {

        @Test
        void should_accept_empty_flows() {
            Flow flow = new Flow();

            var result = service.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(flow));

            assertThat(result).hasSize(1).containsExactly(flow);
        }

        @Test
        public void should_accept_flow_with_only_selectors() {
            var flow = Flow
                .builder()
                .selectors(List.of(HttpSelector.builder().path("/").pathOperator(Operator.STARTS_WITH).build()))
                .build();

            var result = service.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(flow));

            assertThat(result).hasSize(1).containsExactly(flow);
        }

        @Test
        public void should_accept_flow_with_only_steps() {
            var flow = Flow.builder().request(List.of(Step.builder().policy("policy").configuration("configuration").build())).build();

            var result = service.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(flow));

            assertThat(result).hasSize(1).containsExactly(flow);
        }

        @Test
        public void should_accept_flow_with_selectors_and_steps() {
            var flow = Flow
                .builder()
                .selectors(List.of(HttpSelector.builder().path("/").pathOperator(Operator.STARTS_WITH).build()))
                .request(List.of(Step.builder().policy("policy").configuration("configuration").build()))
                .build();

            var flows = service.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(flow));

            assertThat(flows).hasSize(1).containsExactly(flow);
        }

        @Test
        public void should_throw_exception_with_duplicated_selectors() {
            var flow = Flow.builder().name("bad_flow").selectors(List.of(new HttpSelector(), new HttpSelector())).build();

            var throwable = catchThrowable(() -> service.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(flow)));

            assertThat(throwable)
                .isInstanceOf(InvalidFlowException.class)
                .hasMessage("The flow [bad_flow] contains duplicated selectors type")
                .extracting(th -> ((InvalidFlowException) th).getParameters())
                .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
                .contains(entry("flowName", "bad_flow"), entry("duplicatedSelectors", "http"));
        }

        @Test
        public void should_throw_exception_with_incorrect_policy_configuration() {
            when(policyValidationDomainService.validateAndSanitizeConfiguration(eq("my-policy"), eq("incorrect-configuration")))
                .thenThrow(InvalidDataException.class);

            var flow = Flow
                .builder()
                .name("bad_flow")
                .request(List.of(Step.builder().policy("my-policy").configuration("incorrect-configuration").build()))
                .build();

            var throwable = catchThrowable(() -> service.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(flow)));

            assertThat(throwable).isInstanceOf(InvalidDataException.class);
        }

        @Test
        public void should_throw_exception_with_invalid_selector_for_proxy_api() {
            var flow = Flow.builder().name("bad_flow").selectors(List.of(new ChannelSelector())).build();

            var throwable = catchThrowable(() -> service.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(flow)));

            assertThat(throwable)
                .isInstanceOf(InvalidFlowException.class)
                .hasMessage("The flow [bad_flow] contains selectors that couldn't apply to proxy API")
                .extracting(th -> ((InvalidFlowException) th).getParameters())
                .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
                .contains(entry("flowName", "bad_flow"), entry("invalidSelectors", "channel"));
        }

        @Test
        public void should_throw_exception_with_invalid_selector_for_message_api() {
            var flow = Flow.builder().name("bad_flow").selectors(List.of(new HttpSelector())).build();

            var throwable = catchThrowable(() -> service.validateAndSanitizeHttpV4(ApiType.MESSAGE, List.of(flow)));

            assertThat(throwable)
                .isInstanceOf(InvalidFlowException.class)
                .hasMessage("The flow [bad_flow] contains selectors that couldn't apply to message API")
                .extracting(th -> ((InvalidFlowException) th).getParameters())
                .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
                .contains(entry("flowName", "bad_flow"), entry("invalidSelectors", "http"));
        }

        @Test
        public void should_throw_exception_with_invalid_entrypoints() {
            var flow = Flow
                .builder()
                .name("bad_flow")
                .selectors(List.of(ChannelSelector.builder().entrypoints(Set.of(SSE_CONNECTOR_ID, "unknown", "unknown2")).build()))
                .build();

            var throwable = catchThrowable(() -> service.validateAndSanitizeHttpV4(ApiType.MESSAGE, List.of(flow)));

            assertThat(throwable)
                .isInstanceOf(InvalidFlowException.class)
                .hasMessage("The flow [bad_flow] contains channel selector with invalid entrypoints")
                .extracting(th -> ((InvalidFlowException) th).getParameters())
                .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
                .contains(entry("flowName", "bad_flow"), entry("invalidEntrypoints", "unknown2,unknown"));
        }
    }

    @Nested
    class ValidateAndSanitizeNativeV4 {

        @Test
        void should_accept_empty_flows() {
            NativeFlow flow = new NativeFlow();

            var result = service.validateAndSanitizeNativeV4(List.of(flow));

            assertThat(result).hasSize(1).containsExactly(flow);
        }

        @Test
        public void should_accept_flow_with_steps() {
            var flow = NativeFlow
                .builder()
                .interact(List.of(Step.builder().policy("policy").configuration("configuration").build()))
                .build();

            var result = service.validateAndSanitizeNativeV4(List.of(flow));

            assertThat(result).hasSize(1).containsExactly(flow);
        }

        @Test
        public void should_throw_exception_with_incorrect_policy_configuration() {
            when(policyValidationDomainService.validateAndSanitizeConfiguration(eq("my-policy"), eq("incorrect-configuration")))
                .thenThrow(InvalidDataException.class);

            var flow = NativeFlow
                .builder()
                .name("bad_flow")
                .interact(List.of(Step.builder().policy("my-policy").configuration("incorrect-configuration").build()))
                .build();

            var throwable = catchThrowable(() -> service.validateAndSanitizeNativeV4(List.of(flow)));

            assertThat(throwable).isInstanceOf(InvalidDataException.class);
        }

        @Test
        public void should_throw_exception_with_multiple_flows() {
            var flow = NativeFlow
                .builder()
                .interact(List.of(Step.builder().policy("policy").configuration("configuration").build()))
                .build();

            var throwable = catchThrowable(() -> service.validateAndSanitizeNativeV4(List.of(flow, flow)));

            assertThat(throwable).isInstanceOf(NativeApiWithMultipleFlowsException.class);
        }
    }

    @Nested
    class ValidatePathParameters {

        @ParameterizedTest
        @MethodSource("provideParameters")
        void should_test_overlapping_cases(String apiName, Map<String, List<String>> expectedOverlaps) throws IOException {
            final Api api = readApi(apiName);
            var throwable = catchThrowable(() -> service.validatePathParameters(api.getType(), api.getFlows().stream(), getPlanFlows(api)));

            if (expectedOverlaps.isEmpty()) {
                assertThat(throwable).isNull();
            } else {
                assertThat(throwable)
                    .isInstanceOf(ValidationDomainException.class)
                    .hasMessage("Some path parameters are used at different position across different flows.")
                    .is(
                        new Condition<>(
                            error -> {
                                final ValidationDomainException pathParamException = (ValidationDomainException) error;
                                assertThat(pathParamException.getParameters()).containsOnlyKeys(expectedOverlaps.keySet());
                                expectedOverlaps.forEach((key, value) ->
                                    value.forEach(expectedPath ->
                                        assertThat(pathParamException.getParameters().get(key)).contains(expectedPath)
                                    )
                                );
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
                Arguments.of(
                    "api-message-flows-overlap",
                    Map.of(":productId", List.of("/products/:productId/items/:itemId", "/:productId"))
                ),
                Arguments.of(
                    "api-message-plans-overlap",
                    Map.of(":productId", List.of("/products/:productId/items/:itemId", "/:productId"))
                ),
                Arguments.of(
                    "api-message-plans-and-flows-overlap",
                    Map.of(":productId", List.of("/products/:productId/items/:itemId", "/:productId"))
                ),
                Arguments.of("api-message-no-overlap", Map.of()),
                Arguments.of("api-message-no-flows", Map.of())
            );
        }

        private static Api readApi(String name) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                FlowValidationDomainService.class.getClassLoader().getResourceAsStream("apis/v4/pathparams/" + name + ".json"),
                Api.class
            );
        }

        @NotNull
        private static Stream<Flow> getPlanFlows(Api api) {
            return api.getPlans().stream().flatMap(plan -> plan.getFlows() == null ? Stream.empty() : plan.getFlows().stream());
        }
    }
}
