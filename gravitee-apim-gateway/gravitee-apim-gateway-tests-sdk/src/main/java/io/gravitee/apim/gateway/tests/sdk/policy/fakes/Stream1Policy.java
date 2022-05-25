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
package io.gravitee.apim.gateway.tests.sdk.policy.fakes;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.stream.TransformableRequestStreamBuilder;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.SimpleReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponseContent;
import java.util.function.Function;

/**
 * Transforms the content of the request/response with "OnRequestContent1Policy" or "OnResponseContent1Policy".
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Stream1Policy {

    @OnRequestContent
    public ReadWriteStream<Buffer> onRequestContent(Request request, Response response, PolicyChain policyChain) {
        return TransformableRequestStreamBuilder
            .on(request)
            .chain(policyChain)
            .transform((Function<Buffer, Buffer>) buffer -> Buffer.buffer("OnRequestContent1Policy"))
            .build();
    }

    @OnResponseContent
    public ReadWriteStream<Buffer> onResponseContent(Request request, Response response, PolicyChain policyChain) {
        return new BufferedReadWriteStream() {
            @Override
            public SimpleReadWriteStream<Buffer> write(Buffer content) {
                return this;
            }

            @Override
            public void end() {
                super.write(Buffer.buffer("OnResponseContent1Policy"));
                super.end();
            }
        };
    }
}
