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
import io.reactivex.Single;
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
class DebugRequestExecutionContextTest {

    @Mock
    private MutableRequest mockRequest;

    @Mock
    private MutableResponse mockResponse;

    private DebugRequestExecutionContext debugRequestExecutionContext;

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

        debugRequestExecutionContext = new DebugRequestExecutionContext(mockRequest, mockResponse);
    }

    @Test
    public void shouldCallPostOnCreatedRequestPolicyStep() {
        debugRequestExecutionContext
            .prePolicyExecution("id", ExecutionPhase.REQUEST)
            .andThen(debugRequestExecutionContext.postPolicyExecution())
            .test()
            .assertResult();
        assertThat(debugRequestExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugRequestExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyRequestStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isTrue();
        assertThat(currentDebugStep.getError()).isNull();
        assertThat(currentDebugStep.getStatus()).isEqualTo(DebugStepStatus.COMPLETED);
    }

    @Test
    public void shouldCallErrorOnCreatedRequestPolicyStep() {
        debugRequestExecutionContext
            .prePolicyExecution("id", ExecutionPhase.REQUEST)
            .andThen(debugRequestExecutionContext.postPolicyExecution(new RuntimeException()))
            .test()
            .assertResult();
        assertThat(debugRequestExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugRequestExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyRequestStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isTrue();
        assertThat(currentDebugStep.getError()).isNotNull();
        assertThat(currentDebugStep.getStatus()).isEqualTo(DebugStepStatus.ERROR);
    }

    @Test
    public void shouldCallErrorWithFailureOnCreatedRequestPolicyStep() {
        debugRequestExecutionContext
            .prePolicyExecution("id", ExecutionPhase.REQUEST)
            .andThen(debugRequestExecutionContext.postPolicyExecution(new ExecutionFailure(400)))
            .test()
            .assertResult();
        assertThat(debugRequestExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugRequestExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyRequestStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isTrue();
        assertThat(currentDebugStep.getError()).isNotNull();
        assertThat(currentDebugStep.getStatus()).isEqualTo(DebugStepStatus.ERROR);
    }

    @Test
    public void shouldCallPostOnCreatedResponsePolicyStep() {
        debugRequestExecutionContext
            .prePolicyExecution("id", ExecutionPhase.RESPONSE)
            .andThen(debugRequestExecutionContext.postPolicyExecution())
            .test()
            .assertResult();
        assertThat(debugRequestExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugRequestExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyResponseStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isTrue();
        assertThat(currentDebugStep.getError()).isNull();
        assertThat(currentDebugStep.getStatus()).isEqualTo(DebugStepStatus.COMPLETED);
    }

    @Test
    public void shouldCallErrorOnCreatedResponsePolicyStep() {
        debugRequestExecutionContext
            .prePolicyExecution("id", ExecutionPhase.RESPONSE)
            .andThen(debugRequestExecutionContext.postPolicyExecution(new RuntimeException()))
            .test()
            .assertResult();
        assertThat(debugRequestExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugRequestExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyResponseStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isTrue();
        assertThat(currentDebugStep.getError()).isNotNull();
        assertThat(currentDebugStep.getStatus()).isEqualTo(DebugStepStatus.ERROR);
    }

    @Test
    public void shouldCallErrorWithFailureOnCreatedResponsePolicyStep() {
        debugRequestExecutionContext
            .prePolicyExecution("id", ExecutionPhase.RESPONSE)
            .andThen(debugRequestExecutionContext.postPolicyExecution(new ExecutionFailure(400)))
            .test()
            .assertResult();
        assertThat(debugRequestExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugRequestExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyResponseStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isTrue();
        assertThat(currentDebugStep.getError()).isNotNull();
        assertThat(currentDebugStep.getStatus()).isEqualTo(DebugStepStatus.ERROR);
    }

    @Test
    public void shouldNotCreatePolicyStepWithAsyncRequestExecutionMode() {
        debugRequestExecutionContext.prePolicyExecution("id", ExecutionPhase.ASYNC_REQUEST).test().assertResult();
        assertThat(debugRequestExecutionContext.getDebugSteps().size()).isEqualTo(0);
    }

    @Test
    public void shouldCreateRequestPolicyStepAndCallPre() {
        debugRequestExecutionContext.prePolicyExecution("id", ExecutionPhase.REQUEST).test().assertResult();
        assertThat(debugRequestExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugRequestExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyRequestStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isFalse();
    }

    @Test
    public void shouldCreateResponsePolicyStepAndCallPre() {
        debugRequestExecutionContext.prePolicyExecution("id", ExecutionPhase.RESPONSE).test().assertResult();
        assertThat(debugRequestExecutionContext.getDebugSteps().size()).isEqualTo(1);
        PolicyStep<?> currentDebugStep = debugRequestExecutionContext.getCurrentDebugStep();
        assertThat(currentDebugStep).isInstanceOf(PolicyResponseStep.class);
        assertThat(currentDebugStep.elapsedTime()).isPositive();
        assertThat(currentDebugStep.isEnded()).isFalse();
    }

    @Test
    public void shouldNotCreatePolicyStepWithAsyncResponseExecutionMode() {
        debugRequestExecutionContext.prePolicyExecution("id", ExecutionPhase.ASYNC_RESPONSE).test().assertResult();
        assertThat(debugRequestExecutionContext.getDebugSteps().size()).isEqualTo(0);
    }
}
