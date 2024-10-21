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
package io.gravitee.gateway.debug.reactor.handler.context;

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.debug.core.invoker.InvokerResponse;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugRequestStep;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugResponseStep;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStep;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import java.io.Serializable;
import java.util.*;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugExecutionContext implements MutableExecutionContext {

    private final MutableExecutionContext context;

    private final LinkedList<DebugStep<?>> steps = new LinkedList<>();
    private final Map<String, Serializable> initialAttributes;
    private final InvokerResponse invokerResponse = new InvokerResponse();
    private final HttpHeaders initialHeaders;

    public DebugExecutionContext(ExecutionContext context) {
        this.context = (MutableExecutionContext) context;
        this.initialAttributes = AttributeHelper.filterAndSerializeAttributes(context.getAttributes());
        this.initialHeaders = HttpHeaders.create(request().headers());
    }

    public Map<String, Serializable> getInitialAttributes() {
        return initialAttributes;
    }

    /**
     * Some streamable policies returns null when a condition is not fulfilled as a fail-fast strategy.
     * This method allows to save the debug step as a regular one, with the {@link io.gravitee.definition.model.debug.DebugStepStatus#NO_TRANSFORMATION} status
     * @param debugStep, the debug step to save
     */
    public void saveNoTransformationDebugStep(DebugStep<?> debugStep) {
        debugStep.noTransformation();
        beforePolicyExecution(debugStep);
        afterPolicyExecution(debugStep);
    }

    public void beforePolicyExecution(DebugStep<?> debugStep) {
        if (!steps.contains(debugStep)) {
            steps.add(debugStep);

            if (StreamType.ON_REQUEST.equals(debugStep.getStreamType())) {
                ((DebugRequestStep) debugStep).before(request(), context.getAttributes());
            } else {
                ((DebugResponseStep) debugStep).before(response(), context.getAttributes());
            }
        }
    }

    public void afterPolicyExecution(DebugStep<?> debugStep) {
        afterPolicyExecution(debugStep, null, null);
    }

    public void afterPolicyExecution(DebugStep<?> debugStep, Buffer initialBuffer, Buffer finalBuffer) {
        if (!debugStep.isEnded()) {
            if (StreamType.ON_REQUEST.equals(debugStep.getStreamType())) {
                ((DebugRequestStep) debugStep).after(request(), context.getAttributes(), initialBuffer, finalBuffer);
            } else {
                ((DebugResponseStep) debugStep).after(response(), context.getAttributes(), initialBuffer, finalBuffer);
            }
            debugStep.ended();
        }
    }

    @Override
    public Request request() {
        return context.request();
    }

    @Override
    public Response response() {
        return context.response();
    }

    @Override
    public <T> T getComponent(Class<T> componentClass) {
        return context.getComponent(componentClass);
    }

    @Override
    public void setAttribute(String name, Object value) {
        context.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        context.removeAttribute(name);
    }

    @Override
    public Object getAttribute(String name) {
        return context.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return context.getAttributeNames();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return context.getAttributes();
    }

    @Override
    public TemplateEngine getTemplateEngine() {
        return context.getTemplateEngine();
    }

    @Override
    public Tracer getTracer() {
        return context.getTracer();
    }

    public List<DebugStep<?>> getDebugSteps() {
        return this.steps;
    }

    public InvokerResponse getInvokerResponse() {
        return invokerResponse;
    }

    public HttpHeaders getInitialHeaders() {
        return initialHeaders;
    }

    @Override
    public MutableExecutionContext request(Request request) {
        context.request(request);
        return this;
    }

    @Override
    public MutableExecutionContext response(Response response) {
        context.response(response);
        return this;
    }

    @Override
    public MutableExecutionContext tracer(final Tracer tracer) {
        context.tracer(tracer);
        return this;
    }
}
