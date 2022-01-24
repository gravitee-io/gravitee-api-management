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
package io.gravitee.gateway.policy;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.stream.TransformableRequestStreamBuilder;
import io.gravitee.gateway.api.http.stream.TransformableResponseStreamBuilder;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.exception.TransformationException;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponseContent;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DummyStreamablePolicy {

    @OnRequestContent
    public ReadWriteStream<Buffer> onRequestContent(Request request, ExecutionContext executionContext, PolicyChain policyChain) {
        return TransformableRequestStreamBuilder
            .on(request)
            .chain(policyChain)
            .transform(
                buffer -> {
                    try {
                        executionContext.setAttribute("stream", "On Request Content Dummy Streamable Policy");
                        return Buffer.buffer("On Request Content Dummy Streamable Policy");
                    } catch (Exception ioe) {
                        throw new TransformationException("Unable to transform Dummy Streamable Policy Request: " + ioe.getMessage(), ioe);
                    }
                }
            )
            .build();
    }

    @OnResponseContent
    public ReadWriteStream<Buffer> onResponseContent(Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        return TransformableResponseStreamBuilder
            .on(response)
            .chain(policyChain)
            .transform(
                buffer -> {
                    try {
                        executionContext.setAttribute("stream", "On Response Content Dummy Streamable Policy");
                        return Buffer.buffer("On Response Content Dummy Streamable Policy");
                    } catch (Exception ioe) {
                        throw new TransformationException("Unable to transform Dummy Streamable Policy Response: " + ioe.getMessage(), ioe);
                    }
                }
            )
            .build();
    }
}
