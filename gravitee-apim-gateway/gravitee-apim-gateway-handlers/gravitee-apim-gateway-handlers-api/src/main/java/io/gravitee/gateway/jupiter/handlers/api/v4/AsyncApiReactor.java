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

import static io.gravitee.gateway.jupiter.api.ExecutionPhase.REQUEST;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.RESPONSE;
import static io.reactivex.Completable.defer;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.gravitee.gateway.jupiter.api.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.core.context.MutableMessageExecutionContext;
import io.gravitee.gateway.jupiter.core.v4.entrypoint.HttpEntrypointConnectorResolver;
import io.gravitee.gateway.jupiter.handlers.api.adapter.invoker.InvokerAdapter;
import io.gravitee.gateway.jupiter.handlers.api.flow.FlowChain;
import io.gravitee.gateway.jupiter.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.v4.security.SecurityChain;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import java.util.Objects;
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
    private static final String ATTR_INVOKER_SKIP = "invoker.skip";

    private final Api api;
    private final CompositeComponentProvider componentProvider;
    private final PolicyManager policyManager;
    private final HttpEntrypointConnectorResolver asyncEntrypointResolver;
    private final Invoker defaultInvoker;
    private final FlowChain platformFlowChain;
    private SecurityChain securityChain;

    public AsyncApiReactor(
        final Api api,
        final CompositeComponentProvider apiComponentProvider,
        final PolicyManager policyManager,
        final HttpEntrypointConnectorResolver asyncEntrypointResolver,
        final Invoker defaultInvoker,
        final FlowChainFactory flowChainFactory
    ) {
        this.api = api;
        this.componentProvider = apiComponentProvider;
        this.policyManager = policyManager;
        this.asyncEntrypointResolver = asyncEntrypointResolver;
        this.defaultInvoker = defaultInvoker;

        this.platformFlowChain = flowChainFactory.createPlatformFlow(api);
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
        ctx.setAttribute(ExecutionContext.ATTR_INVOKER, defaultInvoker);
        ctx.setAttribute(ExecutionContext.ATTR_ORGANIZATION, api.getOrganizationId());
        ctx.setAttribute(ExecutionContext.ATTR_ENVIRONMENT, api.getEnvironmentId());
        ctx.setInternalAttribute(ExecutionContext.ATTR_API, api);
    }

    private Completable handleRequest(final MutableMessageExecutionContext ctx) {
        EntrypointAsyncConnector entrypointConnector = asyncEntrypointResolver.resolve(ctx);
        if (entrypointConnector == null) {
            return Completable.defer(
                () -> {
                    String noEntrypointMsg = "No entrypoint matches the incoming request";
                    log.debug(noEntrypointMsg);
                    ctx.response().status(HttpStatusCode.NOT_FOUND_404);
                    ctx.response().reason(noEntrypointMsg);
                    return ctx.response().end();
                }
            );
        }

        ctx.setInternalAttribute("entrypointConnector", entrypointConnector);

        return platformFlowChain
            .execute(ctx, REQUEST)
            // Execute security chain.
            .andThen(securityChain.execute(ctx))
            .andThen(entrypointConnector.handleRequest(ctx))
            .andThen(invokeBackend(ctx))
            .andThen(entrypointConnector.handleResponse(ctx))
            // Platform post flows must always be executed
            .andThen(platformFlowChain.execute(ctx, RESPONSE))
            // Catch all possible unexpected errors.
            .onErrorResumeNext(t -> handleUnexpectedError(ctx, t));
    }

    private Completable invokeBackend(final MutableMessageExecutionContext ctx) {
        return defer(
                () -> {
                    if (!Objects.equals(false, ctx.<Boolean>getAttribute(ATTR_INVOKER_SKIP))) {
                        Invoker invoker = getInvoker(ctx);

                        if (invoker != null) {
                            return invoker.invoke(ctx);
                        }
                    }
                    return Completable.complete();
                }
            )
            .doOnSubscribe(disposable -> ctx.request().metrics().setApiResponseTimeMs(System.currentTimeMillis()))
            .doOnDispose(() -> setApiResponseTimeMetric(ctx))
            .doOnTerminate(() -> setApiResponseTimeMetric(ctx));
    }

    private Invoker getInvoker(MutableMessageExecutionContext ctx) {
        final Object invoker = ctx.getAttribute(ExecutionContext.ATTR_INVOKER);

        if (invoker == null) {
            return null;
        }

        if (!(invoker instanceof Invoker)) {
            return new InvokerAdapter((io.gravitee.gateway.api.Invoker) invoker);
        }

        return (Invoker) invoker;
    }

    private Completable handleUnexpectedError(final HttpExecutionContext ctx, final Throwable throwable) {
        return Completable.fromRunnable(
            () -> {
                log.error("Unexpected error while handling request", throwable);
                setApiResponseTimeMetric(ctx);

                ctx.response().status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                ctx.response().reason(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
            }
        );
    }

    private void setApiResponseTimeMetric(HttpExecutionContext ctx) {
        if (ctx.request().metrics().getApiResponseTimeMs() > Integer.MAX_VALUE) {
            ctx.request().metrics().setApiResponseTimeMs(System.currentTimeMillis() - ctx.request().metrics().getApiResponseTimeMs());
        }
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
