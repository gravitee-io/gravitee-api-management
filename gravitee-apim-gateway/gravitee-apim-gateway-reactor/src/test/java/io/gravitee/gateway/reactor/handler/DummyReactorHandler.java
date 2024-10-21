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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DummyReactorHandler extends AbstractReactorHandler<Reactable> {

    public DummyReactorHandler() {
        super(
            new Reactable() {
                @Override
                public boolean enabled() {
                    return true;
                }

                @Override
                public Set dependencies(Class type) {
                    return null;
                }
            },
            TracingContext.noop()
        );
    }

    @Override
    public List<Acceptor<?>> acceptors() {
        return null;
    }

    @Override
    public void handle(ExecutionContext context, Handler<ExecutionContext> endHandler) {
        doHandle(context, endHandler);
    }

    @Override
    protected void doHandle(ExecutionContext executionContext, Handler<ExecutionContext> endHandler) {
        Response proxyResponse = mock(Response.class);
        when(proxyResponse.headers()).thenReturn(HttpHeaders.create());
        when(proxyResponse.status()).thenReturn(HttpStatusCode.OK_200);
    }
}
