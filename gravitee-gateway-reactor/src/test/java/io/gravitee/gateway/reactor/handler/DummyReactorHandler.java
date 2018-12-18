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
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.reactor.Reactable;

import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DummyReactorHandler extends AbstractReactorHandler {

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

    @Override
    public void handle(ExecutionContext context) {
        doHandle(context);
    }

    @Override
    public String contextPath() {
        return null;
    }

    @Override
    protected void doHandle(ExecutionContext executionContext) {
        Response proxyResponse = mock(Response.class);
        when(proxyResponse.headers()).thenReturn(new HttpHeaders());
        when(proxyResponse.status()).thenReturn(HttpStatusCode.OK_200);
    }
}
