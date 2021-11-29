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
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.policy.Policy;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class StreamablePolicyChain extends PolicyChain {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ReadWriteStream<Buffer> streamablePolicyHandlerChain;
    private boolean initialized;

    protected StreamablePolicyChain(final List<Policy> policies, final ExecutionContext executionContext) {
        super(policies, executionContext);
    }

    @Override
    public void doNext(Request request, Response response) {
        if (!initialized && !policies.isEmpty()) {
            prepareStreamablePolicyChain();
            initialized = true;
        }

        super.doNext(request, response);
    }

    private void prepareStreamablePolicyChain() {
        ReadWriteStream<Buffer> previousPolicyStreamer = null;
        for (Policy policy : policies) {
            if (policy.isStreamable()) {
                try {
                    // Run OnXXXContent to get ReadWriteStream object
                    final ReadWriteStream<Buffer> streamer = policy.stream(this, executionContext);
                    if (streamer != null) {
                        // An handler was never assigned to start the chain, so let's do it
                        if (streamablePolicyHandlerChain == null) {
                            streamablePolicyHandlerChain = streamer;
                        }

                        // Chain policy stream using the previous one
                        if (previousPolicyStreamer != null) {
                            previousPolicyStreamer.bodyHandler(
                                result -> {
                                    if (!streamErrorHandle) {
                                        streamer.write(result);
                                    }
                                }
                            );
                            previousPolicyStreamer.endHandler(
                                result -> {
                                    if (!streamErrorHandle) {
                                        streamer.end();
                                    }
                                }
                            );
                        }

                        // Previous stream is now the current policy stream
                        previousPolicyStreamer = streamer;
                    }
                } catch (Exception ex) {
                    logger.error("Unexpected error while running onXXXXContent for policy {}", policy, ex);
                }
            }
        }

        ReadWriteStream<Buffer> tailPolicyStreamer = previousPolicyStreamer;
        if (streamablePolicyHandlerChain != null && tailPolicyStreamer != null) {
            tailPolicyStreamer.bodyHandler(
                bodyPart -> {
                    if (bodyHandler != null) bodyHandler.handle(bodyPart);
                }
            );
            tailPolicyStreamer.endHandler(
                result -> {
                    if (endHandler != null) endHandler.handle(result);
                }
            );
        }
    }

    private boolean streamErrorHandle = false;

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> streamErrorHandler(Handler<ProcessorFailure> handler) {
        super.streamErrorHandler(
            processorFailure -> {
                streamErrorHandle = true;
                handler.handle(processorFailure);
            }
        );

        return this;
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
        if (!streamErrorHandle) {
            if (streamablePolicyHandlerChain != null) {
                streamablePolicyHandlerChain.end();
            } else if (endHandler != null) {
                this.endHandler.handle(null);
            }
        }
    }
}
