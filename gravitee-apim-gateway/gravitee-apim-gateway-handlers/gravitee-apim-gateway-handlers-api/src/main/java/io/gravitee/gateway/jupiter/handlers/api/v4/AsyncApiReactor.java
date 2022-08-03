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
package io.gravitee.gateway.jupiter.handlers.api.v4;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.context.MutableMessageExecutionContext;
import io.gravitee.gateway.jupiter.core.v4.entrypoint.HttpEntrypointResolver;
import io.gravitee.gateway.jupiter.handlers.api.v4.security.SecurityChain;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AsyncApiReactor
    extends AbstractLifecycleComponent<ReactorHandler>
    implements ApiReactor<io.gravitee.definition.model.v4.Api, MutableMessageExecutionContext> {

    private static final Logger log = LoggerFactory.getLogger(AsyncApiReactor.class);

    private final Api api;
    private final CompositeComponentProvider componentProvider;
    private final PolicyManager policyManager;
    private final HttpEntrypointResolver asyncEntrypointResolver;
    private SecurityChain securityChain;

    public AsyncApiReactor(
        final Api api,
        final CompositeComponentProvider apiComponentProvider,
        final PolicyManager policyManager,
        final HttpEntrypointResolver asyncEntrypointResolver
    ) {
        this.api = api;
        this.componentProvider = apiComponentProvider;
        this.policyManager = policyManager;
        this.asyncEntrypointResolver = asyncEntrypointResolver;
    }

    @Override
    public ApiType apiType() {
        return ApiType.ASYNC;
    }

    @Override
    public Completable handle(final MutableMessageExecutionContext ctx) {
        ctx.componentProvider(componentProvider);

        // Prepare attributes and metrics before handling the request.
        prepareContextAttributes(ctx);

        return handleRequest(ctx);
    }

    private void prepareContextAttributes(HttpExecutionContext ctx) {
        ctx.setAttribute(ExecutionContext.ATTR_CONTEXT_PATH, ctx.request().contextPath());
        ctx.setAttribute(ExecutionContext.ATTR_API, api.getId());
        ctx.setAttribute(ExecutionContext.ATTR_API_DEPLOYED_AT, api.getDeployedAt().getTime());
        ctx.setAttribute(ExecutionContext.ATTR_ORGANIZATION, api.getOrganizationId());
        ctx.setAttribute(ExecutionContext.ATTR_ENVIRONMENT, api.getEnvironmentId());
    }

    private Completable handleRequest(final MutableMessageExecutionContext ctx) {
        EntrypointAsyncConnector entrypointAsyncConnector = asyncEntrypointResolver.resolve(ctx);
        if (entrypointAsyncConnector == null) {
            return Completable.defer(
                () -> {
                    String noEntrypointMsg = "No entrypoint matches the incoming request";
                    log.debug(noEntrypointMsg);
                    ctx.response().status(HttpResponseStatus.NOT_FOUND.code());
                    ctx.response().reason(noEntrypointMsg);
                    return ctx.response().end();
                }
            );
        }

        return securityChain
            .execute(ctx)
            .andThen(Completable.defer(() -> entrypointAsyncConnector.handleRequest(ctx)))
            .andThen(sendDummyMessages(ctx))
            .andThen(Completable.defer(() -> entrypointAsyncConnector.handleResponse(ctx)));
    }

    private Completable sendDummyMessages(final MutableMessageExecutionContext ctx) {
        return Completable.defer(
            () -> {
                ctx
                    .response()
                    .messages(
                        Flowable
                            .interval(1, TimeUnit.SECONDS)
                            .map(
                                value ->
                                    new Message() {
                                        @Override
                                        public HttpHeaders headers() {
                                            return null;
                                        }

                                        @Override
                                        public Map<String, Object> metadata() {
                                            return Map.of("interval", value);
                                        }

                                        @Override
                                        public Buffer content() {
                                            return Buffer.buffer("test " + value);
                                        }
                                    }
                            )
                    );
                return Completable.complete();
            }
        );
    }

    @Override
    public ReactableApi<io.gravitee.definition.model.v4.Api> reactable() {
        return api;
    }

    @Override
    public Lifecycle.State lifecycleState() {
        return null;
    }

    @Override
    protected void doStart() throws Exception {
        log.debug("API reactor is now starting, preparing API context...");
        long startTime = System.currentTimeMillis(); // Get the start Time

        // Start resources before
        policyManager.start();
        // Create securityChain once policy manager has been started.
        this.securityChain = new SecurityChain(api.getDefinition(), policyManager, ExecutionPhase.MESSAGE_REQUEST);

        long endTime = System.currentTimeMillis(); // Get the end Time
        log.debug("API reactor started in {} ms", (endTime - startTime));
    }

    @Override
    protected void doStop() throws Exception {
        log.debug("API reactor is now stopping, closing context for {} ...", this);

        policyManager.stop();

        log.debug("API reactor is now stopped: {}", this);
    }

    @Override
    public String toString() {
        return "AsyncApiReactor API id[" + api.getId() + "] name[" + api.getName() + "] version[" + api.getApiVersion() + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsyncApiReactor that = (AsyncApiReactor) o;
        return api.equals(that.api);
    }

    @Override
    public int hashCode() {
        return Objects.hash(api);
    }
}
