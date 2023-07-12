/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.handlers.api.v4.subscription;

import static io.gravitee.gateway.jupiter.core.v4.endpoint.DefaultEndpointConnectorResolver.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static io.reactivex.Completable.defer;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.jupiter.api.connector.AbstractConnectorFactory;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.handlers.api.adapter.invoker.InvokerAdapter;
import io.gravitee.gateway.jupiter.handlers.api.v4.Api;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.gravitee.gateway.jupiter.reactor.v4.subscription.DefaultSubscriptionAcceptor;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.reactivex.Completable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This concrete implementation of {@link ReactorHandler} is used to handle {@link Subscription} type of call,
 * meaning that the call to this {@link ReactorHandler} are based on the {@link Subscription} sync process.
 *
 * Call to this {@link ReactorHandler} are directly done by the {@link io.gravitee.gateway.jupiter.reactor.v4.subscription.SubscriptionDispatcher}
 * which is responsible for listening for incoming subscription.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionReactor extends AbstractLifecycleComponent<ReactorHandler> implements ApiReactor {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionReactor.class);

    private final Api api;
    private final ComponentProvider componentProvider;
    private final Invoker defaultInvoker;
    private final EntrypointConnectorPluginManager entrypointConnectorPluginManager;

    public SubscriptionReactor(
        Api api,
        ComponentProvider apiComponentProvider,
        EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        Invoker defaultInvoker
    ) {
        this.api = api;
        this.componentProvider = apiComponentProvider;
        this.entrypointConnectorPluginManager = entrypointConnectorPluginManager;
        this.defaultInvoker = defaultInvoker;
    }

    @Override
    public Completable handle(final MutableExecutionContext ctx) {
        ctx.componentProvider(componentProvider);

        // Prepare attributes and metrics before handling the request.
        prepareContextAttributes(ctx);

        return handleRequest(ctx);
    }

    private Completable handleRequest(final MutableExecutionContext ctx) {
        Subscription subscription = ctx.getInternalAttribute(ContextAttributes.ATTR_SUBSCRIPTION);
        AbstractConnectorFactory<? extends EntrypointConnector> connectorFactory = entrypointConnectorPluginManager.getFactoryById(
            ctx.getInternalAttribute(ContextAttributes.ATTR_SUBSCRIPTION_TYPE)
        );

        EntrypointAsyncConnector connector = (EntrypointAsyncConnector) connectorFactory.createConnector(subscription.getConfiguration());

        if (connector == null) {
            return Completable.defer(() -> {
                String noEntrypointMsg = "No entrypoint matches the incoming request";
                log.debug(noEntrypointMsg);
                ctx.response().status(HttpStatusCode.NOT_FOUND_404);
                ctx.response().reason(noEntrypointMsg);
                return ctx.response().end();
            });
        }

        // Add the resolved entrypoint connector into the internal attributes, so it can be used later
        // (ex: for endpoint connector resolution).
        ctx.setInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR, connector);

        return connector
            .handleRequest(ctx)
            .andThen(invokeBackend(ctx))
            .andThen(connector.handleResponse(ctx))
            .onErrorResumeNext(t -> handleUnexpectedError(ctx, t));
    }

    private Completable invokeBackend(final MutableExecutionContext ctx) {
        return defer(() -> {
            Invoker invoker = getInvoker(ctx);

            if (invoker != null) {
                return invoker.invoke(ctx);
            }

            return Completable.complete();
        });
    }

    private Completable handleUnexpectedError(final MessageExecutionContext ctx, final Throwable throwable) {
        return Completable.fromRunnable(() -> log.error("Unexpected error while handling subscription request", throwable));
    }

    private Invoker getInvoker(MutableExecutionContext ctx) {
        final Object invoker = ctx.getAttribute(ContextAttributes.ATTR_INVOKER);

        if (invoker == null) {
            return null;
        }

        if (!(invoker instanceof Invoker)) {
            return new InvokerAdapter((io.gravitee.gateway.api.Invoker) invoker);
        }

        return (Invoker) invoker;
    }

    private void prepareContextAttributes(MessageExecutionContext ctx) {
        ctx.setAttribute(ContextAttributes.ATTR_API, api.getId());
        ctx.setAttribute(ContextAttributes.ATTR_API_DEPLOYED_AT, api.getDeployedAt().getTime());
        ctx.setAttribute(ContextAttributes.ATTR_INVOKER, defaultInvoker);
        ctx.setAttribute(ContextAttributes.ATTR_ORGANIZATION, api.getOrganizationId());
        ctx.setAttribute(ContextAttributes.ATTR_ENVIRONMENT, api.getEnvironmentId());
        ctx.setInternalAttribute(ContextAttributes.ATTR_API, api);
    }

    @Override
    public ApiType apiType() {
        return ApiType.ASYNC;
    }

    @Override
    public List<Acceptor<?>> acceptors() {
        return api
            .getDefinition()
            .getListeners()
            .stream()
            .filter(listener -> listener.getType().equals(ListenerType.SUBSCRIPTION))
            .map(listener -> ((SubscriptionListener) listener))
            .map(path -> new DefaultSubscriptionAcceptor(this, api.getId()))
            .collect(Collectors.toList());
    }

    @Override
    protected void doStart() throws Exception {
        log.debug("Subscription reactor is now starting, preparing context...");
        long startTime = System.currentTimeMillis(); // Get the start Time

        long endTime = System.currentTimeMillis(); // Get the end Time
        log.debug("Subscription reactor started in {} ms", (endTime - startTime));
    }

    @Override
    protected void doStop() throws Exception {
        log.debug("Subscription reactor is now stopping, closing context for {} ...", this);

        log.debug("Subscription reactor is now stopped: {}", this);
    }

    @Override
    public String toString() {
        return "SubscriptionReactor API id[" + api.getId() + "] name[" + api.getName() + "] version[" + api.getApiVersion() + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionReactor that = (SubscriptionReactor) o;
        return api.equals(that.api);
    }

    @Override
    public int hashCode() {
        return Objects.hash(api);
    }
}
