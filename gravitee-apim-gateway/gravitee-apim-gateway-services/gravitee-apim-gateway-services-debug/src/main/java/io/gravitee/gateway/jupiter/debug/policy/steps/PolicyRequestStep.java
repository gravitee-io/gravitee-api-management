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

import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.reactivex.Single;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyRequestStep extends PolicyStep<MutableRequest> {

    public PolicyRequestStep(final String policyId, final ExecutionPhase executionPhase, final String flowPhase) {
        super(policyId, executionPhase, flowPhase);
    }

    @Override
    public Single<PolicyStepState> saveInputState(final MutableRequest request, final Map<String, Serializable> inputAttributes) {
        return request
            .bodyOrEmpty()
            .map(
                inputBody ->
                    new PolicyStepState()
                        .contextPath(request.contextPath())
                        .parameters(request.parameters())
                        .method(request.method())
                        .path(request.path())
                        .pathParameters(request.pathParameters())
                        .headers(request.headers())
                        .attributes(inputAttributes)
                        .buffer(inputBody)
            );
    }

    @Override
    public Single<Map<String, Object>> computeDiff(
        final MutableRequest request,
        final PolicyStepState inputState,
        final Map<String, Serializable> outputAttributes
    ) {
        return request
            .bodyOrEmpty()
            .map(
                outputBody -> {
                    Map<String, Object> diffMap = new HashMap<>();
                    if (!inputState.headers().deeplyEquals(request.headers())) {
                        diffMap.put(DIFF_KEY_HEADERS, HttpHeaders.create(request.headers()));
                    }
                    if (!inputState.parameters().equals(request.parameters())) {
                        diffMap.put(DIFF_KEY_PARAMETERS, new LinkedMultiValueMap<>(request.parameters()));
                    }
                    if (!inputState.pathParameters().equals(request.pathParameters())) {
                        diffMap.put(DIFF_KEY_PATH_PARAMETERS, new LinkedMultiValueMap<>(request.pathParameters()));
                    }
                    if (!inputState.method().equals(request.method())) {
                        diffMap.put(DIFF_KEY_METHOD, request.method());
                    }
                    if (!inputState.path().equals(request.path())) {
                        diffMap.put(DIFF_KEY_PATH, request.path());
                    }
                    if (!inputState.contextPath().equals(request.contextPath())) {
                        diffMap.put(DIFF_KEY_CONTEXT_PATH, request.contextPath());
                    }
                    if (!inputState.attributes().equals(outputAttributes)) {
                        diffMap.put(DIFF_KEY_ATTRIBUTES, new HashMap<>(outputAttributes));
                    }

                    if (!inputState.buffer().getNativeBuffer().equals(outputBody.getNativeBuffer())) {
                        diffMap.put(DIFF_KEY_BODY_BUFFER, Buffer.buffer(outputBody.getBytes()));
                    }
                    return diffMap;
                }
            );
    }
}
