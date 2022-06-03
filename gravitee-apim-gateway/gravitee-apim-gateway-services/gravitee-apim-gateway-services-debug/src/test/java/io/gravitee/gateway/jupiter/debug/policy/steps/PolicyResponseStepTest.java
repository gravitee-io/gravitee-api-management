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

import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.definition.model.debug.DebugStepError;
import io.gravitee.definition.model.debug.DebugStepStatus;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStep;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.HashMap;
import java.util.List;
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
class PolicyResponseStepTest {

    @Mock
    private MutableResponse mockResponse;

    private HttpHeaders headers;

    private PolicyResponseStep policyResponseStep;

    @BeforeEach
    public void setUp() {
        headers = HttpHeaders.create();
        lenient().when(mockResponse.headers()).thenReturn(headers);
        lenient().when(mockResponse.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer()));
        lenient().when(mockResponse.status()).thenReturn(200);
        lenient().when(mockResponse.reason()).thenReturn("OK");

        policyResponseStep = new PolicyResponseStep("policy", ExecutionPhase.RESPONSE, "flowPhase");
    }

    @AfterEach
    public void afterEach() {
        Mockito.reset(mockResponse);
    }

    @Test
    public void shouldComputeHeadersDiff() {
        policyResponseStep
            .pre(mockResponse, Map.of())
            .doOnComplete(() -> headers.set("X-Header-New", "value"))
            .andThen(Completable.defer(() -> policyResponseStep.post(mockResponse, Map.of())))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyResponseStep.getDiff();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isTrue();
        assertThat(HttpHeaders.create(headers).deeplyEquals((HttpHeaders) resultDiff.get(DebugStep.DIFF_KEY_HEADERS))).isTrue();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldComputeStatusDiff() {
        policyResponseStep
            .pre(mockResponse, Map.of())
            .doOnComplete(() -> when(mockResponse.status()).thenReturn(500))
            .andThen(Completable.defer(() -> policyResponseStep.post(mockResponse, Map.of())))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyResponseStep.getDiff();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_STATUS_CODE)).isTrue();
        assertThat(resultDiff.get(DebugStep.DIFF_KEY_STATUS_CODE)).isEqualTo(500);

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_REASON)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldComputeReasonDiff() {
        policyResponseStep
            .pre(mockResponse, Map.of())
            .doOnComplete(() -> when(mockResponse.reason()).thenReturn("KO"))
            .andThen(Completable.defer(() -> policyResponseStep.post(mockResponse, Map.of())))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyResponseStep.getDiff();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_REASON)).isTrue();
        assertThat(resultDiff.get(DebugStep.DIFF_KEY_REASON)).isEqualTo("KO");

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_STATUS_CODE)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldComputeAttributesDiff() {
        policyResponseStep
            .pre(mockResponse, Map.of())
            .andThen(Completable.defer(() -> policyResponseStep.post(mockResponse, Map.of("A-Key", "value"))))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyResponseStep.getDiff();

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
        policyResponseStep
            .pre(mockResponse, Map.of())
            .doOnComplete(() -> when(mockResponse.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer("outPut"))))
            .andThen(Completable.defer(() -> policyResponseStep.post(mockResponse, Map.of())))
            .test()
            .assertResult();

        final Map<String, Object> resultDiff = policyResponseStep.getDiff();
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
        policyResponseStep
            .pre(mockResponse, Map.of())
            .andThen(Completable.defer(() -> policyResponseStep.error(new RuntimeException("error"))))
            .test()
            .assertResult();

        assertThat(policyResponseStep.getDiff()).isEmpty();
        assertThat(policyResponseStep.getStatus()).isEqualTo(DebugStepStatus.ERROR);
        DebugStepError error = policyResponseStep.getError();
        assertThat(error).isNotNull();
        assertThat(error.getMessage()).isEqualTo("error");
    }

    @Test
    public void shouldNotComputeDiffWhenStepIsInErrorWithFailure() {
        policyResponseStep
            .pre(mockResponse, Map.of())
            .andThen(
                Completable.defer(
                    () -> policyResponseStep.error(new ExecutionFailure(500).key("key").message("error").contentType("contentType"))
                )
            )
            .test()
            .assertResult();

        assertThat(policyResponseStep.getDiff()).isEmpty();
        assertThat(policyResponseStep.getStatus()).isEqualTo(DebugStepStatus.ERROR);
        DebugStepError error = policyResponseStep.getError();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(500);
        assertThat(error.getMessage()).isEqualTo("error");
        assertThat(error.getContentType()).isEqualTo("contentType");
        assertThat(error.getKey()).isEqualTo("key");
    }
}
