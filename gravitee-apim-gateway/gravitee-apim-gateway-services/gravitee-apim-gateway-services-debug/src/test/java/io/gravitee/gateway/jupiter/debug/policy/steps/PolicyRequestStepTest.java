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
package io.gravitee.gateway.jupiter.debug.policy.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.definition.model.debug.DebugStepError;
import io.gravitee.definition.model.debug.DebugStepStatus;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStep;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class PolicyRequestStepTest {

    @Mock
    private MutableRequest mockRequest;

    private HttpHeaders httpHeaders;

    private PolicyRequestStep policyRequestStep;

    @BeforeEach
    public void setUp() {
        httpHeaders = HttpHeaders.create();
        lenient().when(mockRequest.headers()).thenReturn(httpHeaders);
        lenient().when(mockRequest.contextPath()).thenReturn("contextPath");
        lenient().when(mockRequest.path()).thenReturn("path");
        lenient().when(mockRequest.method()).thenReturn(HttpMethod.GET);
        lenient().when(mockRequest.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer()));

        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("param1", "value1");
        parameters.add("param2", "value2");
        when(mockRequest.parameters()).thenReturn(parameters);

        final LinkedMultiValueMap<String, String> pathParameters = new LinkedMultiValueMap<>();
        pathParameters.add("path-param1", "path-value1");
        pathParameters.add("path-param2", "path-value2");
        when(mockRequest.pathParameters()).thenReturn(pathParameters);

        policyRequestStep = new PolicyRequestStep("policy", ExecutionPhase.REQUEST, "flowPhase");
    }

    @AfterEach
    public void afterEach() {
        Mockito.reset(mockRequest);
    }

    @Test
    public void shouldComputeHeadersDiff() {
        policyRequestStep
            .pre(mockRequest, Map.of())
            .doOnComplete(() -> httpHeaders.set("X-Header-New", "value"))
            .andThen(Completable.defer(() -> policyRequestStep.post(mockRequest, Map.of())))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyRequestStep.getDiff();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isTrue();
        assertThat(HttpHeaders.create(httpHeaders).deeplyEquals((HttpHeaders) resultDiff.get(DebugStep.DIFF_KEY_HEADERS))).isTrue();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldComputeParametersDiff() {
        final LinkedMultiValueMap<String, String> updatedParameters = new LinkedMultiValueMap<>();
        updatedParameters.add("parameters", "value");

        policyRequestStep
            .pre(mockRequest, Map.of())
            .doOnComplete(() -> when(mockRequest.parameters()).thenReturn(updatedParameters))
            .andThen(Completable.defer(() -> policyRequestStep.post(mockRequest, Map.of())))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyRequestStep.getDiff();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isTrue();
        assertThat(resultDiff.get(DebugStep.DIFF_KEY_PARAMETERS)).isEqualTo(updatedParameters);

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldComputePathParametersDiff() {
        final LinkedMultiValueMap<String, String> updatedPathParameters = new LinkedMultiValueMap<>();
        updatedPathParameters.add("path_parameters", "value");
        policyRequestStep
            .pre(mockRequest, Map.of())
            .doOnComplete(() -> when(mockRequest.pathParameters()).thenReturn(updatedPathParameters))
            .andThen(Completable.defer(() -> policyRequestStep.post(mockRequest, Map.of())))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyRequestStep.getDiff();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isTrue();
        assertThat(resultDiff.get(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isEqualTo(updatedPathParameters);

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldComputeMethodDiff() {
        policyRequestStep
            .pre(mockRequest, Map.of())
            .doOnComplete(() -> when(mockRequest.method()).thenReturn(HttpMethod.POST))
            .andThen(Completable.defer(() -> policyRequestStep.post(mockRequest, Map.of())))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyRequestStep.getDiff();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isTrue();
        assertThat(resultDiff.get(DebugStep.DIFF_KEY_METHOD)).isEqualTo(HttpMethod.POST);

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldComputePathDiff() {
        policyRequestStep
            .pre(mockRequest, Map.of())
            .doOnComplete(() -> when(mockRequest.path()).thenReturn("updated_path"))
            .andThen(Completable.defer(() -> policyRequestStep.post(mockRequest, Map.of())))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyRequestStep.getDiff();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isTrue();
        assertThat(resultDiff.get(DebugStep.DIFF_KEY_PATH)).isEqualTo("updated_path");

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldComputeContextPathDiff() {
        policyRequestStep
            .pre(mockRequest, Map.of())
            .doOnComplete(() -> when(mockRequest.contextPath()).thenReturn("contextPathUpdated"))
            .andThen(Completable.defer(() -> policyRequestStep.post(mockRequest, Map.of())))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyRequestStep.getDiff();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isTrue();
        assertThat(resultDiff.get(DebugStep.DIFF_KEY_CONTEXT_PATH)).isEqualTo("contextPathUpdated");

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldComputeAttributesDiff() {
        policyRequestStep
            .pre(mockRequest, Map.of())
            .andThen(Completable.defer(() -> policyRequestStep.post(mockRequest, Map.of("A-Key", "value"))))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyRequestStep.getDiff();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isTrue();
        assertThat(resultDiff.get(DebugStep.DIFF_KEY_ATTRIBUTES)).isEqualTo(Map.of("A-Key", "value"));

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldComputeBufferDiff() {
        policyRequestStep
            .pre(mockRequest, Map.of())
            .doOnComplete(() -> when(mockRequest.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer("outPut"))))
            .andThen(Completable.defer(() -> policyRequestStep.post(mockRequest, Map.of())))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyRequestStep.getDiff();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isTrue();
        assertThat(resultDiff.get(DebugStep.DIFF_KEY_BODY_BUFFER).toString()).isEqualTo("outPut");

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
    }

    @Test
    public void shouldNotComputeDiffWhenStepIsInError() {
        policyRequestStep
            .pre(mockRequest, Map.of())
            .andThen(Completable.defer(() -> policyRequestStep.error(new RuntimeException("error"))))
            .test()
            .assertResult();

        assertThat(policyRequestStep.getDiff()).isEmpty();
        assertThat(policyRequestStep.getStatus()).isEqualTo(DebugStepStatus.ERROR);
        DebugStepError error = policyRequestStep.getError();
        assertThat(error).isNotNull();
        assertThat(error.getMessage()).isEqualTo("error");
    }

    @Test
    public void shouldNotComputeDiffWhenStepIsInErrorWithFailure() {
        policyRequestStep
            .pre(mockRequest, Map.of())
            .andThen(
                Completable.defer(
                    () -> policyRequestStep.error(new ExecutionFailure(500).key("key").message("error").contentType("contentType"))
                )
            )
            .test()
            .assertResult();

        assertThat(policyRequestStep.getDiff()).isEmpty();
        assertThat(policyRequestStep.getStatus()).isEqualTo(DebugStepStatus.ERROR);
        DebugStepError error = policyRequestStep.getError();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(500);
        assertThat(error.getMessage()).isEqualTo("error");
        assertThat(error.getContentType()).isEqualTo("contentType");
        assertThat(error.getKey()).isEqualTo("key");
    }
}
