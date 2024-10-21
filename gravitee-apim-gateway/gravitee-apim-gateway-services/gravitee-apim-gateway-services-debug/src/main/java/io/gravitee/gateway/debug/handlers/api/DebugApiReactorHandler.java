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
package io.gravitee.gateway.debug.handlers.api;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.debug.core.invoker.InvokerDebugDecorator;
import io.gravitee.gateway.debug.definition.DebugApi;
import io.gravitee.gateway.debug.reactor.handler.http.ContextualizedDebugHttpServerRequest;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.ApiReactorHandler;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.opentelemetry.Tracer;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugApiReactorHandler extends ApiReactorHandler {

    public DebugApiReactorHandler(
        Configuration configuration,
        Api api,
        AccessPointManager accessPointManager,
        EventManager eventManager,
        TracingContext tracingContext
    ) {
        super(configuration, api, accessPointManager, eventManager, tracingContext);
    }

    @Override
    protected void contextualizeRequest(ExecutionContext context) {
        final String path = ((HttpAcceptor) context.getAttribute(ATTR_ENTRYPOINT)).path();
        ((MutableExecutionContext) context).request(
                new ContextualizedDebugHttpServerRequest(path, context.request(), ((DebugApi) reactable).getEventId())
            );
    }

    @Override
    protected Invoker getInvoker(ExecutionContext context) {
        return new InvokerDebugDecorator(super.getInvoker(context));
    }
}
