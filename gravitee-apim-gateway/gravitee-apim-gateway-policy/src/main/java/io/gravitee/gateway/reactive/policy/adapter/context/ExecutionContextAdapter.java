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
package io.gravitee.gateway.reactive.policy.adapter.context;

import static io.gravitee.gateway.reactive.api.context.ExecutionContext.ATTR_ADAPTED_CONTEXT;
import static io.gravitee.gateway.reactive.api.context.ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE;

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.core.processor.RuntimeProcessorFailure;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.gravitee.tracing.api.Tracer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExecutionContextAdapter implements io.gravitee.gateway.api.ExecutionContext {

    private final RequestExecutionContext ctx;
    private RequestAdapter adaptedRequest;
    private ResponseAdapter adaptedResponse;

    private ExecutionContextAdapter(RequestExecutionContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Creates an {@link ExecutionContextAdapter} from a {@link ExecutionContext}.
     * Once created, the adapted context is stored in internal attribute in order to be reused and avoid useless successive instantiations.
     *
     * @param ctx the context to adapt for v3 compatibility mode.
     * @return the v3 compatible {@link io.gravitee.gateway.api.ExecutionContext}.
     */
    public static ExecutionContextAdapter create(RequestExecutionContext ctx) {
        ExecutionContextAdapter adaptedCtx = ctx.getInternalAttribute(ATTR_ADAPTED_CONTEXT);

        if (adaptedCtx == null) {
            adaptedCtx = new ExecutionContextAdapter(ctx);
            ctx.setInternalAttribute(ATTR_ADAPTED_CONTEXT, adaptedCtx);
        }

        return adaptedCtx;
    }

    public RequestExecutionContext getDelegate() {
        return ctx;
    }

    @Override
    public RequestAdapter request() {
        if (adaptedRequest == null) {
            adaptedRequest = new RequestAdapter(ctx.request());
        }
        return adaptedRequest;
    }

    @Override
    public ResponseAdapter response() {
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
        if (ATTR_FAILURE_ATTRIBUTE.equals(name) && value instanceof ProcessorFailure) {
            ProcessorFailure processorFailure = (ProcessorFailure) value;
            ctx.setInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE, new ProcessFailureAdapter(processorFailure).toExecutionFailure());
        } else {
            ctx.setAttribute(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        if (ATTR_FAILURE_ATTRIBUTE.equals(name)) {
            ctx.removeInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE);
        } else {
            ctx.removeAttribute(name);
        }
    }

    @Override
    public Object getAttribute(String name) {
        if (ATTR_FAILURE_ATTRIBUTE.equals(name)) {
            ExecutionFailure executionFailure = ctx.getInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE);
            return new ProcessFailureAdapter(executionFailure);
        } else {
            return ctx.getAttribute(name);
        }
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
        return ctx.getTemplateEngine();
    }

    @Override
    public Tracer getTracer() {
        return ctx.getComponent(Tracer.class);
    }
}
