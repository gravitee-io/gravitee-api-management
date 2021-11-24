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
package io.gravitee.gateway.standalone.policy;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.stream.TransformableRequestStreamBuilder;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransformResponseContentPolicy {

    @OnResponse
    public void onResponse(Request request, Response response, PolicyChain policyChain) {
        response.headers().set(HttpHeaders.TRANSFER_ENCODING, HttpHeadersValues.TRANSFER_ENCODING_CHUNKED);

        policyChain.doNext(request, response);
    }

    @OnResponseContent
    public ReadWriteStream onResponseContent(Request request, ExecutionContext context) {
        return TransformableRequestStreamBuilder
                .on(request)
                .transform(buffer -> {
                    String content = context.getTemplateEngine().convert(buffer.toString());
                    return Buffer.buffer(content);
                })
                .build();
    }
}
