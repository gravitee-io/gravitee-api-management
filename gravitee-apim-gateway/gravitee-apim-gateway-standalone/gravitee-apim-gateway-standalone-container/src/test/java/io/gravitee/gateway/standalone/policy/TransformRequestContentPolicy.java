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
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.SimpleReadWriteStream;
import io.gravitee.policy.api.annotations.OnRequestContent;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransformRequestContentPolicy {

    @OnRequestContent
    public ReadWriteStream onRequestContent(Request request, ExecutionContext executionContext) {
        return new BufferedReadWriteStream() {
            Buffer buffer = Buffer.buffer();

            @Override
            public SimpleReadWriteStream<Buffer> write(Buffer content) {
                // We dot want to get the request content, skipping
                buffer.appendBuffer(content);
                return this;
            }

            @Override
            public void end() {
                String content = executionContext.getTemplateEngine().convert(buffer.toString());
                Buffer contentBuf = Buffer.buffer(content);

                request.headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(contentBuf.length()));

                // Write content
                super.write(contentBuf);

                // Mark the end of content
                super.end();
            }
        };
    }
}
