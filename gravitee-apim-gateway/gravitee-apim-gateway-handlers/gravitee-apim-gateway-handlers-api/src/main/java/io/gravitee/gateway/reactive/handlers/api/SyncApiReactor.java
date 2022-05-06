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
package io.gravitee.gateway.reactive.handlers.api;

import static io.reactivex.Completable.defer;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecycleManager;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.gravitee.gateway.reactive.reactor.ApiReactor;
import io.gravitee.gateway.reactive.reactor.handler.context.ExecutionContextFactory;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.Entrypoint;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.reactivex.Completable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SyncApiReactor extends AbstractLifecycleComponent<ReactorHandler> implements ApiReactor, ReactorHandler {

    private final Logger log = LoggerFactory.getLogger(SyncApiReactor.class);
    private final Api api;
    private final ExecutionContextFactory ctxFactory;
    private final Invoker defaultInvoker;
    private final ResourceLifecycleManager resourceLifecycleManager;
    private final PolicyManager policyManager;
    private final GroupLifecycleManager groupLifecycleManager;

    public SyncApiReactor(
        Api api,
        ExecutionContextFactory ctxFactory,
        Invoker defaultInvoker,
        ResourceLifecycleManager resourceLifecycleManager,
        PolicyManager policyManager,
        GroupLifecycleManager groupLifecycleManager
    ) {
        this.api = api;
        this.ctxFactory = ctxFactory;
        this.defaultInvoker = defaultInvoker;
        this.resourceLifecycleManager = resourceLifecycleManager;
        this.policyManager = policyManager;
        this.groupLifecycleManager = groupLifecycleManager;
    }

    @Override
    public Completable handle(Request request, Response response) {
        final RequestExecutionContext ctx = ctxFactory.createRequestResponseContext(request, response);

        ctx.setAttribute(ExecutionContext.ATTR_CONTEXT_PATH, ctx.request().contextPath());
        ctx.setAttribute(ExecutionContext.ATTR_API, api.getId());
        ctx.setAttribute(ExecutionContext.ATTR_API_DEPLOYED_AT, api.getDeployedAt().getTime());
        ctx.setAttribute(ExecutionContext.ATTR_INVOKER, defaultInvoker);
        ctx.setAttribute(ExecutionContext.ATTR_ORGANIZATION, api.getOrganizationId());
        ctx.setAttribute(ExecutionContext.ATTR_ENVIRONMENT, api.getEnvironmentId());

        // Prepare request metrics
        request.metrics().setApi(api.getId());
        request.metrics().setPath(request.pathInfo());

        ctx.request().metrics().setApiResponseTimeMs(System.currentTimeMillis());

        // FIXME: security chain with plan resolution.
        ctx.setAttribute(io.gravitee.gateway.api.ExecutionContext.ATTR_PLAN, "cd593616-5a15-4972-9936-165a15c972be");

        return invokeBackend(ctx)
            .andThen(end(ctx))
            .doFinally(() -> setApiResponseTime(ctx))
            .doFinally(() -> log.debug("Request {} has been processed in {}ms.", request.id(), getApiResponseTime(ctx)));
    }

    /**
     * Invokes the backend if the context is not interrupted and if there is nothing in the context that indicates to skip the invoker.
     *
     * @param ctx the current execution context.
     *
     * @return a {@link Completable} that will complete once the invoker has been invoked or that completes immediately if execution isn't required.
     */
    private Completable invokeBackend(RequestExecutionContext ctx) {
        return defer(
            () -> {
                if (!ctx.isInterrupted() && !(boolean) Boolean.FALSE.equals(ctx.getAttribute("invoker.skip"))) {
                    Invoker invoker = this.getInvoker(ctx);
                    return invoker.invoke(ctx);
                }
                return Completable.complete();
            }
        );
    }

    private long getApiResponseTime(RequestExecutionContext ctx) {
        return ctx.request().metrics().getApiResponseTimeMs();
    }

    protected Invoker getInvoker(RequestExecutionContext context) {
        return context.getAttribute(ExecutionContext.ATTR_INVOKER);
    }

    private Completable end(RequestExecutionContext ctx) {
        return defer(
            () -> {
                setApiResponseTime(ctx);
                return ctx.response().end().timeout(10, TimeUnit.SECONDS, handleTimeout(ctx));
            }
        );
    }

    private void setApiResponseTime(RequestExecutionContext ctx) {
        if (ctx.request().metrics().getApiResponseTimeMs() > Integer.MAX_VALUE) {
            ctx.request().metrics().setApiResponseTimeMs(System.currentTimeMillis() - ctx.request().metrics().getApiResponseTimeMs());
        }
    }

    private Completable handleTimeout(RequestExecutionContext ctx) {
        return defer(() -> handleError(ctx, null).andThen(defer(() -> ctx.response().end())));
    }

    private Completable handleError(RequestExecutionContext ctx, Throwable t) {
        if (t != null) {
            log.error("Unexpected error", t);
        }

        setApiResponseTime(ctx);

        // TODO: execute error processors

        return Completable.complete();
    }

    @Override
    public Reactable reactable() {
        return api;
    }

    @Override
    public SyncApiReactor handler(Handler<io.gravitee.gateway.api.ExecutionContext> handler) {
        return this;
    }

    @Override
    protected void doStart() throws Exception {
        log.debug("API handler is now starting, preparing API context...");
        long startTime = System.currentTimeMillis(); // Get the start Time

        // Start resources before
        resourceLifecycleManager.start();
        policyManager.start();
        groupLifecycleManager.start();

        dumpVirtualHosts();

        long endTime = System.currentTimeMillis(); // Get the end Time
        log.debug("API handler started in {} ms", (endTime - startTime));
    }

    @Override
    protected void doStop() throws Exception {
        log.debug("API handler is now stopping, closing context for {} ...", this);

        policyManager.stop();
        resourceLifecycleManager.stop();
        groupLifecycleManager.stop();

        log.debug("API handler is now stopped: {}", this);
    }

    @Override
    public String toString() {
        return "SyncApiReactor API id[" + api.getId() + "] name[" + api.getName() + "] version[" + api.getVersion() + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncApiReactor that = (SyncApiReactor) o;
        return api.equals(that.api);
    }

    protected void dumpVirtualHosts() {
        List<Entrypoint> entrypoints = api.entrypoints();
        log.debug("{} ready to accept requests on:", this);
        entrypoints.forEach(
            entrypoint -> {
                log.debug("\t{}", entrypoint);
            }
        );
    }

    @Override
    public void handle(io.gravitee.gateway.api.ExecutionContext result) {
        throw new RuntimeException(new IllegalAccessException("Handle method can be called on SyncApiReactor"));
    }
}
