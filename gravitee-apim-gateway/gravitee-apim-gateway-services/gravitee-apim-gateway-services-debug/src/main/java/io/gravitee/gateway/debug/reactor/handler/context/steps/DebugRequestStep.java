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
package io.gravitee.gateway.debug.reactor.handler.context.steps;

import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.debug.reactor.handler.context.DebugScope;
import io.gravitee.gateway.policy.StreamType;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugRequestStep extends DebugStep<Request> {

    public DebugRequestStep(String policyId, StreamType streamType, String uuid, DebugScope debugScope) {
        super(policyId, streamType, uuid, debugScope);
    }

    @Override
    protected void snapshotInputData(Request request, Map<String, Object> attributes) {
        policyInputContent
            .contextPath(request.contextPath())
            .parameters(request.parameters())
            .method(request.method())
            .path(request.path())
            .pathParameters(request.pathParameters())
            .headers(request.headers())
            .attributes(attributes);
    }

    @Override
    protected void generateDiffMap(Request request, Map<String, Object> attributes, Buffer inputBuffer, Buffer outputBuffer) {
        if (!policyInputContent.getHeaders().deeplyEquals(request.headers())) {
            diffMap.put("headers", HttpHeaders.create(request.headers()));
        }
        if (!policyInputContent.getParameters().equals(request.parameters())) {
            diffMap.put("parameters", new LinkedMultiValueMap<>(request.parameters()));
        }
        if (!policyInputContent.getPathParameters().equals(request.pathParameters())) {
            diffMap.put("pathParameters", new LinkedMultiValueMap<>(request.pathParameters()));
        }
        if (!policyInputContent.getMethod().equals(request.method())) {
            diffMap.put("method", request.method());
        }
        if (!policyInputContent.getPath().equals(request.path())) {
            diffMap.put("path", request.path());
        }
        if (!policyInputContent.getContextPath().equals(request.contextPath())) {
            diffMap.put("contextPath", request.contextPath());
        }
        if (!policyInputContent.getAttributes().equals(attributes)) {
            diffMap.put("attributes", new HashMap<>(attributes));
        }

        Buffer input = inputBuffer != null ? inputBuffer : Buffer.buffer();
        Buffer output = outputBuffer != null ? outputBuffer : Buffer.buffer();

        if (!input.getNativeBuffer().equals(output.getNativeBuffer())) {
            diffMap.put("buffer", Buffer.buffer(output.getBytes()));
        }
    }
}
