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

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.reactivex.Single;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyResponseStep extends PolicyStep<MutableResponse> {

    public PolicyResponseStep(String policyId, ExecutionPhase executionPhase, final String flowPhase) {
        super(policyId, executionPhase, flowPhase);
    }

    @Override
    public Single<PolicyStepState> saveInputState(final MutableResponse response, final Map<String, Serializable> inputAttributes) {
        return response
            .bodyOrEmpty()
            .map(
                inputBody ->
                    new PolicyStepState()
                        .headers(response.headers())
                        .statusCode(response.status())
                        .reason(response.reason())
                        .attributes(inputAttributes)
                        .buffer(inputBody)
            );
    }

    @Override
    public Single<Map<String, Object>> computeDiff(
        final MutableResponse response,
        final PolicyStepState inputState,
        final Map<String, Serializable> outputAttributes
    ) {
        return response
            .bodyOrEmpty()
            .map(
                outputBody -> {
                    Map<String, Object> diffMap = new HashMap<>();
                    if (!inputState.headers().deeplyEquals(response.headers())) {
                        diffMap.put(DIFF_KEY_HEADERS, HttpHeaders.create(response.headers()));
                    }
                    if (inputState.statusCode() != response.status()) {
                        diffMap.put(DIFF_KEY_STATUS_CODE, response.status());
                    }
                    if (inputState.reason() != null && !inputState.reason().equals(response.reason())) {
                        diffMap.put(DIFF_KEY_REASON, response.reason());
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
