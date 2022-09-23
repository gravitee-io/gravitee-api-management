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
package io.gravitee.gateway.jupiter.debug.reactor.context;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.lenient;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.definition.model.debug.DebugStepStatus;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.debug.policy.steps.PolicyRequestStep;
import io.gravitee.gateway.jupiter.debug.policy.steps.PolicyResponseStep;
import io.gravitee.gateway.jupiter.debug.policy.steps.PolicyStep;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DebugExecutionContextTest {

    @Mock
    private MutableRequest mockRequest;

    @Mock
    private MutableResponse mockResponse;

    private DebugExecutionContext debugExecutionContext;

    @BeforeEach
    public void setUp() {
        lenient().when(mockRequest.headers()).thenReturn(HttpHeaders.create());
        lenient().when(mockRequest.contextPath()).thenReturn("contextPath");
        lenient().when(mockRequest.path()).thenReturn("path");
        lenient().when(mockRequest.method()).thenReturn(HttpMethod.GET);
        lenient().when(mockRequest.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer()));
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("param1", "value1");
        parameters.add("param2", "value2");
        lenient().when(mockRequest.parameters()).thenReturn(parameters);

        final LinkedMultiValueMap<String, String> pathParameters = new LinkedMultiValueMap<>();
        pathParameters.add("path-param1", "path-value1");
        pathParameters.add("path-param2", "path-value2");
        lenient().when(mockRequest.pathParameters()).thenReturn(pathParameters);

        lenient().when(mockResponse.headers()).thenReturn(HttpHeaders.create());
        lenient().when(mockResponse.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer()));
        lenient().when(mockResponse.status()).thenReturn(200);
        lenient().when(mockResponse.reason()).thenReturn("OK");

        debugExecutionContext = new DebugExecutionContext(mockRequest, mockResponse);
    }

    @Test
    public void shouldCallPostOnCreatedRequestPolicyStep() {
        debugExecutionContext
            .prePolicyExecution("id", ExecutionPhase.REQUEST)
            .andThen(debugExecutionContext.postPolicyExecution())
            .test()
            .assertResult();
        assertThat(debugExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyRequestStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isTrue();
        assertThat(currentDebugStep.getError()).isNull();
        assertThat(currentDebugStep.getStatus()).isEqualTo(DebugStepStatus.COMPLETED);
    }

    @Test
    public void shouldCallErrorOnCreatedRequestPolicyStep() {
        debugExecutionContext
            .prePolicyExecution("id", ExecutionPhase.REQUEST)
            .andThen(debugExecutionContext.postPolicyExecution(new RuntimeException()))
            .test()
            .assertResult();
        assertThat(debugExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyRequestStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isTrue();
        assertThat(currentDebugStep.getError()).isNotNull();
        assertThat(currentDebugStep.getStatus()).isEqualTo(DebugStepStatus.ERROR);
    }

    @Test
    public void shouldCallErrorWithFailureOnCreatedRequestPolicyStep() {
        debugExecutionContext
            .prePolicyExecution("id", ExecutionPhase.REQUEST)
            .andThen(debugExecutionContext.postPolicyExecution(new ExecutionFailure(400)))
            .test()
            .assertResult();
        assertThat(debugExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyRequestStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isTrue();
        assertThat(currentDebugStep.getError()).isNotNull();
        assertThat(currentDebugStep.getStatus()).isEqualTo(DebugStepStatus.ERROR);
    }

    @Test
    public void shouldCallPostOnCreatedResponsePolicyStep() {
        debugExecutionContext
            .prePolicyExecution("id", ExecutionPhase.RESPONSE)
            .andThen(debugExecutionContext.postPolicyExecution())
            .test()
            .assertResult();
        assertThat(debugExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyResponseStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isTrue();
        assertThat(currentDebugStep.getError()).isNull();
        assertThat(currentDebugStep.getStatus()).isEqualTo(DebugStepStatus.COMPLETED);
    }

    @Test
    public void shouldCallErrorOnCreatedResponsePolicyStep() {
        debugExecutionContext
            .prePolicyExecution("id", ExecutionPhase.RESPONSE)
            .andThen(debugExecutionContext.postPolicyExecution(new RuntimeException()))
            .test()
            .assertResult();
        assertThat(debugExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyResponseStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isTrue();
        assertThat(currentDebugStep.getError()).isNotNull();
        assertThat(currentDebugStep.getStatus()).isEqualTo(DebugStepStatus.ERROR);
    }

    @Test
    public void shouldCallErrorWithFailureOnCreatedResponsePolicyStep() {
        debugExecutionContext
            .prePolicyExecution("id", ExecutionPhase.RESPONSE)
            .andThen(debugExecutionContext.postPolicyExecution(new ExecutionFailure(400)))
            .test()
            .assertResult();
        assertThat(debugExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyResponseStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isTrue();
        assertThat(currentDebugStep.getError()).isNotNull();
        assertThat(currentDebugStep.getStatus()).isEqualTo(DebugStepStatus.ERROR);
    }

    @Test
    public void shouldNotCreatePolicyStepWithAsyncRequestExecutionMode() {
        debugExecutionContext.prePolicyExecution("id", ExecutionPhase.MESSAGE_REQUEST).test().assertResult();
        assertThat(debugExecutionContext.getDebugSteps().size()).isEqualTo(0);
    }

    @Test
    public void shouldCreateRequestPolicyStepAndCallPre() {
        debugExecutionContext.prePolicyExecution("id", ExecutionPhase.REQUEST).test().assertResult();
        assertThat(debugExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyRequestStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isFalse();
    }

    @Test
    public void shouldCreateResponsePolicyStepAndCallPre() {
        debugExecutionContext.prePolicyExecution("id", ExecutionPhase.RESPONSE).test().assertResult();
        assertThat(debugExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyResponseStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isFalse();
    }

    @Test
    public void shouldNotCreatePolicyStepWithAsyncResponseExecutionMode() {
        debugExecutionContext.prePolicyExecution("id", ExecutionPhase.MESSAGE_RESPONSE).test().assertResult();
        assertThat(debugExecutionContext.getDebugSteps().size()).isEqualTo(0);
    }
}
