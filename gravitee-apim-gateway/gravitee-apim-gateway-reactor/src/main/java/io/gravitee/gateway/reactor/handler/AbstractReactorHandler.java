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
package io.gravitee.gateway.reactor.handler;

import com.google.common.base.Throwables;
import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.context.V3ExecutionContextFactory;
import io.gravitee.gateway.reactor.handler.http.ContextualizedHttpServerRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractReactorHandler<T extends Reactable>
    extends AbstractLifecycleComponent<ReactorHandler>
    implements ReactorHandler {

    public static final String ATTR_ENTRYPOINT = ExecutionContext.ATTR_PREFIX + "entrypoint";
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final T reactable;
    protected final TracingContext tracingContext;
    private V3ExecutionContextFactory executionContextFactory;

    protected AbstractReactorHandler(T reactable, final TracingContext tracingContext) {
        this.reactable = reactable;
        this.tracingContext = tracingContext;
    }

    @Override
    protected void doStart() throws Exception {
        tracingContext.start();
    }

    @Override
    protected void doStop() throws Exception {
        tracingContext.stop();
    }

    @Override
    public TracingContext tracingContext() {
        return tracingContext;
    }

    @Override
    public void handle(ExecutionContext context, Handler<ExecutionContext> endHandler) {
        // Wrap the actual request to contextualize it
        contextualizeRequest(context);

        try {
            doHandle(executionContextFactory.create(context), endHandler);
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while processing request", ex);

            context.request().metrics().setMessage(Throwables.getStackTraceAsString(ex));

            // Send an INTERNAL_SERVER_ERROR (500)
            context.response().status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            context.response().headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);

            endHandler.handle(context);
        }
    }

    protected void contextualizeRequest(ExecutionContext context) {
        ((MutableExecutionContext) context).request(
                new ContextualizedHttpServerRequest(((HttpAcceptor) context.getAttribute(ATTR_ENTRYPOINT)).path(), context.request())
            );
    }

    protected void dumpVirtualHosts() {
        List<Acceptor<?>> httpAcceptors = acceptors();
        logger.debug("{} ready to accept requests on:", this);
        httpAcceptors.forEach(httpAcceptor -> {
            logger.debug("\t{}", httpAcceptor);
        });
    }

    protected abstract void doHandle(ExecutionContext executionContext, Handler<ExecutionContext> endHandler);

    public void setExecutionContextFactory(V3ExecutionContextFactory executionContextFactory) {
        this.executionContextFactory = executionContextFactory;
    }
}
