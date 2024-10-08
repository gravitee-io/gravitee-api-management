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
package io.gravitee.gateway.reactive.policy.adapter.context;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ADAPTED_CONTEXT;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_INVOKER;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_INVOKER_SKIP;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP;

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.gravitee.tracing.api.Tracer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExecutionContextAdapter implements io.gravitee.gateway.api.ExecutionContext, MutableExecutionContext {

    private final HttpPlainExecutionContext ctx;
    private Request adaptedRequest;
    private Response adaptedResponse;
    private TemplateEngineAdapter adaptedTemplateEngine;

    private ExecutionContextAdapter(HttpPlainExecutionContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Creates an {@link ExecutionContextAdapter} from a {@link HttpPlainExecutionContext}.
     * Once created, the adapted context is stored in internal attribute in order to be reused and avoid useless successive instantiations.
     *
     * @param ctx the context to adapt for v3 compatibility mode.
     * @return the v3 compatible {@link io.gravitee.gateway.api.ExecutionContext}.
     */
    public static ExecutionContextAdapter create(HttpPlainExecutionContext ctx) {
        ExecutionContextAdapter adaptedCtx = ctx.getInternalAttribute(ATTR_INTERNAL_ADAPTED_CONTEXT);

        if (adaptedCtx == null) {
            adaptedCtx = new ExecutionContextAdapter(ctx);
            ctx.setInternalAttribute(ATTR_INTERNAL_ADAPTED_CONTEXT, adaptedCtx);
        }

        return adaptedCtx;
    }

    public HttpPlainExecutionContext getDelegate() {
        return ctx;
    }

    @Override
    public Request request() {
        if (adaptedRequest == null) {
            adaptedRequest = new RequestAdapter(ctx.request(), ctx.metrics());
        }
        return adaptedRequest;
    }

    @Override
    public Response response() {
        if (adaptedResponse == null) {
            adaptedResponse = new ResponseAdapter(ctx.response());
        }
        return adaptedResponse;
    }

    @Override
    public <T> T getComponent(Class<T> componentClass) {
        return ctx.getComponent(componentClass);
    }

    @Override
    public void setAttribute(String name, Object value) {
        switch (name) {
            case ATTR_FAILURE_ATTRIBUTE:
                ProcessorFailure processorFailure = (ProcessorFailure) value;
                ctx.setInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE, new ProcessFailureAdapter(processorFailure).toExecutionFailure());
                break;
            case ATTR_INVOKER:
                ctx.setInternalAttribute(ATTR_INTERNAL_INVOKER, value);
                break;
            case ATTR_INVOKER_SKIP:
                ctx.setInternalAttribute(ATTR_INTERNAL_INVOKER_SKIP, value);
                break;
            case ATTR_SECURITY_SKIP:
                ctx.setInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP, value);
                break;
        }

        ctx.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        switch (name) {
            case ATTR_FAILURE_ATTRIBUTE:
                ctx.removeInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE);
                break;
            case ATTR_INVOKER:
                ctx.removeInternalAttribute(ATTR_INTERNAL_INVOKER);
                break;
            case ATTR_INVOKER_SKIP:
                ctx.removeInternalAttribute(ATTR_INTERNAL_INVOKER_SKIP);
                break;
            case ATTR_SECURITY_SKIP:
                ctx.removeInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP);
                break;
        }

        ctx.removeAttribute(name);
    }

    @Override
    public Object getAttribute(String name) {
        final Object attribute = ctx.getAttribute(name);

        switch (name) {
            case ATTR_FAILURE_ATTRIBUTE:
                if (attribute == null) {
                    final ExecutionFailure executionFailure = ctx.getInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE);
                    if (executionFailure != null) {
                        return new ProcessFailureAdapter(executionFailure);
                    }
                }
                break;
            case ATTR_INVOKER:
                if (attribute == null) {
                    final Invoker invoker = ctx.getInternalAttribute(ATTR_INTERNAL_INVOKER);
                    if (invoker instanceof io.gravitee.gateway.api.Invoker) {
                        return invoker;
                    }
                }
                break;
            case ATTR_INVOKER_SKIP:
                if (attribute == null) {
                    return ctx.getInternalAttribute(ATTR_INTERNAL_INVOKER_SKIP);
                }
                break;
            case ATTR_SECURITY_SKIP:
                if (attribute == null) {
                    return ctx.getInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP);
                }
                break;
        }

        return attribute;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(ctx.getAttributeNames());
    }

    @Override
    public Map<String, Object> getAttributes() {
        return ctx.getAttributes();
    }

    @Override
    public TemplateEngine getTemplateEngine() {
        if (adaptedTemplateEngine == null) {
            adaptedTemplateEngine = new TemplateEngineAdapter(ctx.getTemplateEngine());
        }
        return adaptedTemplateEngine;
    }

    @Override
    public Tracer getTracer() {
        return ctx.getComponent(Tracer.class);
    }

    /**
     * Restore method can be called to restore the template engine context and avoid clashes with v4 engine.
     */
    public void restore() {
        if (adaptedTemplateEngine != null) {
            adaptedTemplateEngine.restore();
        }
    }

    @Override
    public MutableExecutionContext request(Request request) {
        adaptedRequest = request;
        return this;
    }

    @Override
    public MutableExecutionContext response(Response response) {
        adaptedResponse = response;
        return this;
    }
}
