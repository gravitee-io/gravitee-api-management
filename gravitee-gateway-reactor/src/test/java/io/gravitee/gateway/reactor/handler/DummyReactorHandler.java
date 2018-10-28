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
package io.gravitee.gateway.reactor.handler;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.context.ExecutionContextFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DummyReactorHandler extends AbstractReactorHandler {

    @Autowired
    private ExecutionContextFactory executionContextFactory;

    @Override
    public Reactable reactable() {
        return new Reactable() {
            @Override
            public Object item() {
                return null;
            }

            @Override
            public String contextPath() {
                return "";
            }

            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public Set dependencies(Class type) {
                return null;
            }

            @Override
            public Map<String, Object> properties() {
                return null;
            }
        };
    }

    public void setExecutionContextFactory(ExecutionContextFactory executionContextFactory) {
        this.executionContextFactory = executionContextFactory;
    }

    @Override
    public void handle(Request request, Response response, Handler<Response> handler) {
        // Prepare request execution context
        ExecutionContext executionContext = executionContextFactory.create(request, response);

        doHandle(request, response, executionContext, handler);
    }

    @Override
    public String contextPath() {
        return null;
    }

    @Override
    protected void doHandle(Request request, Response response, ExecutionContext executionContext, Handler<Response> handler) {
        Response proxyResponse = mock(Response.class);
        when(proxyResponse.headers()).thenReturn(new HttpHeaders());
        when(proxyResponse.status()).thenReturn(HttpStatusCode.OK_200);

        handler.handle(proxyResponse);
    }
}
