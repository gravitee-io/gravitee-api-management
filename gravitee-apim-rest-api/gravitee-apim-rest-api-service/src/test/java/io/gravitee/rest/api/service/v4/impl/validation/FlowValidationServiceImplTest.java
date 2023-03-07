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
package io.gravitee.rest.api.service.v4.impl.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.FlowSelectorsDuplicatedException;
import io.gravitee.rest.api.service.v4.exception.FlowSelectorsEntrypointInvalidException;
import io.gravitee.rest.api.service.v4.exception.FlowSelectorsInvalidException;
import io.gravitee.rest.api.service.v4.validation.FlowValidationService;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class FlowValidationServiceImplTest {

    @Mock
    private PolicyService policyService;

    @Mock
    private EntrypointConnectorPluginService entrypointConnectorPluginService;

    private FlowValidationService flowValidationService;

    @Before
    public void setUp() throws Exception {
        lenient().when(policyService.validatePolicyConfiguration(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        flowValidationService = new FlowValidationServiceImpl(policyService, entrypointConnectorPluginService);
    }

    @Test
    public void shouldReturnValidatedFlowsWithoutSelectorsNorSteps() {
        Flow flow = new Flow();

        List<Flow> flows = flowValidationService.validateAndSanitize(ApiType.PROXY, List.of(flow));
        assertThat(flows.size()).isEqualTo(1);
        Flow ValidatedFlows = flows.get(0);
        assertThat(ValidatedFlows).isEqualTo(flow);
    }

    @Test
    public void shouldReturnValidatedFlowsWithSelectorsAndWithoutSteps() {
        Flow flow = new Flow();
        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setPath("/");
        httpSelector.setPathOperator(Operator.STARTS_WITH);
        flow.setSelectors(List.of(httpSelector));

        List<Flow> flows = flowValidationService.validateAndSanitize(ApiType.PROXY, List.of(flow));
        assertThat(flows.size()).isEqualTo(1);
        Flow ValidatedFlows = flows.get(0);
        assertThat(ValidatedFlows).isEqualTo(flow);
    }

    @Test
    public void shouldReturnValidatedFlowsWithStepsAndWithoutSelectors() {
        Flow flow = new Flow();

        Step step = new Step();
        step.setPolicy("policy");
        step.setConfiguration("configuration");
        flow.setRequest(List.of(step));

        List<Flow> flows = flowValidationService.validateAndSanitize(ApiType.PROXY, List.of(flow));
        assertThat(flows.size()).isEqualTo(1);
        Flow ValidatedFlows = flows.get(0);
        assertThat(ValidatedFlows).isEqualTo(flow);
        verify(policyService).validatePolicyConfiguration(eq("policy"), eq("configuration"));
    }

    @Test
    public void shouldReturnValidatedFlowsWithSelectorsAndSteps() {
        Flow flow = new Flow();
        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setPath("/");
        httpSelector.setPathOperator(Operator.STARTS_WITH);
        flow.setSelectors(List.of(httpSelector));

        Step step = new Step();
        step.setPolicy("policy");
        step.setConfiguration("configuration");
        flow.setRequest(List.of(step));

        List<Flow> flows = flowValidationService.validateAndSanitize(ApiType.PROXY, List.of(flow));
        assertThat(flows.size()).isEqualTo(1);
        Flow ValidatedFlows = flows.get(0);
        assertThat(ValidatedFlows).isEqualTo(flow);
        verify(policyService).validatePolicyConfiguration(eq("policy"), eq("configuration"));
    }

    @Test
    public void shouldThrowValidationExceptionWithDuplicatedSelectors() {
        Flow flow = new Flow();
        HttpSelector httpSelector = new HttpSelector();
        flow.setSelectors(List.of(httpSelector, httpSelector));

        assertThatExceptionOfType(FlowSelectorsDuplicatedException.class)
            .isThrownBy(() -> flowValidationService.validateAndSanitize(ApiType.PROXY, List.of(flow)));
    }

    @Test
    public void shouldThrowInvalidDataExceptionWithWrongPolicyConfiguration() {
        Flow flow = new Flow();

        Step step = new Step();
        step.setPolicy("policy");
        step.setConfiguration("configuration");
        flow.setRequest(List.of(step));
        lenient().when(policyService.validatePolicyConfiguration(any(), any())).thenThrow(InvalidDataException.class);

        assertThatExceptionOfType(InvalidDataException.class)
            .isThrownBy(() -> flowValidationService.validateAndSanitize(ApiType.PROXY, List.of(flow)));
    }

    @Test
    public void shouldThrowValidationExceptionWithInvalidSelectorsAndSyncApi() {
        Flow flow = new Flow();
        ChannelSelector channelSelector = new ChannelSelector();
        flow.setSelectors(List.of(channelSelector));

        assertThatExceptionOfType(FlowSelectorsInvalidException.class)
            .isThrownBy(() -> flowValidationService.validateAndSanitize(ApiType.PROXY, List.of(flow)));
    }

    @Test
    public void shouldThrowValidationExceptionWithInvalidSelectorsAndAsyncApi() {
        Flow flow = new Flow();
        HttpSelector httpSelector = new HttpSelector();
        flow.setSelectors(List.of(httpSelector));

        assertThatExceptionOfType(FlowSelectorsInvalidException.class)
            .isThrownBy(() -> flowValidationService.validateAndSanitize(ApiType.MESSAGE, List.of(flow)));
    }

    @Test
    public void shouldThrowValidationExceptionWithInvalidEntrypoints() {
        Flow flow = new Flow();
        ChannelSelector channelSelector = new ChannelSelector();
        channelSelector.setEntrypoints(Set.of("entrypoint"));
        flow.setSelectors(List.of(channelSelector));
        when(entrypointConnectorPluginService.findBySupportedApi(ApiType.MESSAGE)).thenReturn(Set.of());

        assertThatExceptionOfType(FlowSelectorsEntrypointInvalidException.class)
            .isThrownBy(() -> flowValidationService.validateAndSanitize(ApiType.MESSAGE, List.of(flow)));
    }

    @Test
    public void shouldIgnoreEmptyList() {
        List<Flow> emptyFlows = List.of();
        List<Flow> validatedFlows = flowValidationService.validateAndSanitize(ApiType.PROXY, emptyFlows);
        assertThat(validatedFlows).isEmpty();
        assertThat(validatedFlows).isEqualTo(emptyFlows);
    }
}
