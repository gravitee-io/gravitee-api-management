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

import com.google.common.base.Throwables;
import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.reactor.handler.context.ExecutionContextFactory;
import io.gravitee.gateway.reactor.handler.http.ContextualizedHttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractReactorHandler extends AbstractLifecycleComponent<ReactorHandler> implements ReactorHandler, ApplicationContextAware {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected ApplicationContext applicationContext;

    private ClassLoader classLoader;

    @Autowired
    private ExecutionContextFactory executionContextFactory;

    @Override
    public ClassLoader classloader() {
        return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    protected void doStart() throws Exception {
        // Nothing to do there
    }

    @Override
    protected void doStop() throws Exception {
        if (applicationContext != null) {
            ((ConfigurableApplicationContext) applicationContext).close();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void handle(Request request, Response response, Handler<Response> handler) {
        // Wrap the actual request to contextualize it
        request = new ContextualizedHttpServerRequest(reactable().contextPath(), request);

        // Prepare request execution context
        ExecutionContext executionContext = executionContextFactory.create(request, response);

        try {
            doHandle(request, response, executionContext, handler);
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while processing request", ex);

            request.metrics().setMessage(Throwables.getStackTraceAsString(ex));

            // Send an INTERNAL_SERVER_ERROR (500)
            response.status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            response.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
            response.end();

            handler.handle(response);
        }
    }

    protected abstract void doHandle(Request request, Response response, ExecutionContext executionContext, Handler<Response> handler);
}
