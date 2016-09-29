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
package io.gravitee.gateway.policy.impl;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.policy.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class StreamablePolicyChain extends PolicyChain {

    private final Logger LOGGER = LoggerFactory.getLogger(ResponsePolicyChain.class);

    private ReadWriteStream<Buffer> streamablePolicyHandlerChain;
    private boolean initialized;

    protected StreamablePolicyChain(List<Policy> policies, final ExecutionContext executionContext) {
        super(policies, executionContext);
    }

    @Override
    public void doNext(Request request, Response response) {
        if (! initialized && ! policies.isEmpty()) {
            prepareStreamablePolicyChain(request, response);
            initialized = true;
        }

        super.doNext(request, response);
    }

    private void prepareStreamablePolicyChain(final Request request, final Response response) {
        ReadWriteStream<Buffer> previousPolicyStreamer = null;
        for (Policy policy : policies) {
            if (policy.isStreamable()) {
                try {
                    // Run OnXXXContent to get ReadWriteStream object
                    ReadWriteStream streamer = stream(policy, request, response, executionContext);
                    if (streamer != null) {
                        //    final ReadWriteStream streamer = result;

                        // An handler was never assigned to start the chain, so let's do it
                        if (streamablePolicyHandlerChain == null) {
                            streamablePolicyHandlerChain = streamer;
                        }

                        // Chain policy stream using the previous one
                        if (previousPolicyStreamer != null) {
                            previousPolicyStreamer.bodyHandler(streamer::write);
                            previousPolicyStreamer.endHandler(result1 -> streamer.end());
                        }

                        // Previous stream is now the current policy stream
                        previousPolicyStreamer = streamer;
                    }
                } catch (Exception ex) {
                    LOGGER.error("Unexpected error while running onXXXXContent for policy {}", policy, ex);
                }
            }
        }

        ReadWriteStream<Buffer> tailPolicyStreamer = previousPolicyStreamer;
        if (streamablePolicyHandlerChain != null && tailPolicyStreamer != null) {
            tailPolicyStreamer.bodyHandler(bodyPart -> bodyHandler.handle(bodyPart));
            tailPolicyStreamer.endHandler(result -> endHandler.handle(result));
        }
    }

    @Override
    public StreamablePolicyChain write(Buffer chunk) {
        if (streamablePolicyHandlerChain != null) {
            streamablePolicyHandlerChain.write(chunk);
        } else {
            this.bodyHandler.handle(chunk);
        }

        return this;
    }

    @Override
    public void end() {
        if (streamablePolicyHandlerChain != null) {
            streamablePolicyHandlerChain.end();
        } else {
            this.endHandler.handle(null);
        }
    }

    protected abstract ReadWriteStream<?> stream(Policy policy, Object... args) throws Exception;
}
