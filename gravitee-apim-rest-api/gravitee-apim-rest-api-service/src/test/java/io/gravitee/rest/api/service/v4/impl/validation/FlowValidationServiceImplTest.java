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

import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.SelectorHttp;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.v4.FlowValidationService;
import io.gravitee.rest.api.service.v4.exception.FlowSelectorsDuplicatedException;
import java.util.List;
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

    private FlowValidationService flowValidationService;

    @Before
    public void setUp() throws Exception {
        lenient().when(policyService.validatePolicyConfiguration(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        flowValidationService = new FlowValidationServiceImpl(policyService);
    }

    @Test
    public void shouldReturnValidatedFlowsWithoutSelectorsNorSteps() {
        Flow flow = new Flow();

        List<Flow> flows = flowValidationService.validateAndSanitize(List.of(flow));
        assertThat(flows.size()).isEqualTo(1);
        Flow ValidatedFlows = flows.get(0);
        assertThat(ValidatedFlows).isEqualTo(flow);
    }

    @Test
    public void shouldReturnValidatedFlowsWithSelectorsAndWithoutSteps() {
        Flow flow = new Flow();
        SelectorHttp selectorHttp = new SelectorHttp();
        selectorHttp.setPath("/");
        selectorHttp.setPathOperator(Operator.STARTS_WITH);
        flow.setSelectors(List.of(selectorHttp));

        List<Flow> flows = flowValidationService.validateAndSanitize(List.of(flow));
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

        List<Flow> flows = flowValidationService.validateAndSanitize(List.of(flow));
        assertThat(flows.size()).isEqualTo(1);
        Flow ValidatedFlows = flows.get(0);
        assertThat(ValidatedFlows).isEqualTo(flow);
        verify(policyService).validatePolicyConfiguration(eq("policy"), eq("configuration"));
    }

    @Test
    public void shouldReturnValidatedFlowsWithSelectorsAndSteps() {
        Flow flow = new Flow();
        SelectorHttp selectorHttp = new SelectorHttp();
        selectorHttp.setPath("/");
        selectorHttp.setPathOperator(Operator.STARTS_WITH);
        flow.setSelectors(List.of(selectorHttp));

        Step step = new Step();
        step.setPolicy("policy");
        step.setConfiguration("configuration");
        flow.setRequest(List.of(step));

        List<Flow> flows = flowValidationService.validateAndSanitize(List.of(flow));
        assertThat(flows.size()).isEqualTo(1);
        Flow ValidatedFlows = flows.get(0);
        assertThat(ValidatedFlows).isEqualTo(flow);
        verify(policyService).validatePolicyConfiguration(eq("policy"), eq("configuration"));
    }

    @Test
    public void shouldThrowValidationExceptionWithDuplicatedSelectors() {
        Flow flow = new Flow();
        SelectorHttp selectorHttp = new SelectorHttp();
        flow.setSelectors(List.of(selectorHttp, selectorHttp));

        assertThatExceptionOfType(FlowSelectorsDuplicatedException.class)
            .isThrownBy(() -> flowValidationService.validateAndSanitize(List.of(flow)));
    }

    @Test
    public void shouldThrowInvalidDataExceptionWithWrongPolicyConfiguration() {
        Flow flow = new Flow();

        Step step = new Step();
        step.setPolicy("policy");
        step.setConfiguration("configuration");
        flow.setRequest(List.of(step));
        lenient().when(policyService.validatePolicyConfiguration(any(), any())).thenThrow(InvalidDataException.class);

        assertThatExceptionOfType(InvalidDataException.class).isThrownBy(() -> flowValidationService.validateAndSanitize(List.of(flow)));
    }

    @Test
    public void shouldIgnoreEmptyList() {
        List<Flow> emptyFlows = List.of();
        List<Flow> ValidatedFlowss = flowValidationService.validateAndSanitize(emptyFlows);
        assertThat(ValidatedFlowss).isEmpty();
        assertThat(ValidatedFlowss).isEqualTo(emptyFlows);
    }
}
