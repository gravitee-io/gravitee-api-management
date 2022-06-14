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
package io.gravitee.gateway.jupiter.reactor.handler.context;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.el.EvaluableRequest;
import io.gravitee.gateway.jupiter.api.el.EvaluableResponse;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableRequestExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionException;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionFailureException;
import io.reactivex.Completable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

abstract class AbstractExecutionContext implements MutableRequestExecutionContext {

    private final Map<String, Object> attributes = new ContextAttributeMap();
    private final Map<String, Object> internalAttributes = new HashMap<>();
    private final MutableRequest request;
    private final MutableResponse response;
    private ComponentProvider componentProvider;
    private Collection<TemplateVariableProvider> templateVariableProviders;
    protected TemplateEngine templateEngine;

    protected AbstractExecutionContext(MutableRequest request, MutableResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public Completable interrupt() {
        return Completable.error(new InterruptionException());
    }

    @Override
    public Completable interruptWith(ExecutionFailure executionFailure) {
        return Completable.defer(
            () -> {
                internalAttributes.put(ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
                return Completable.error(new InterruptionFailureException(executionFailure));
            }
        );
    }

    @Override
    public MutableRequest request() {
        return request;
    }

    @Override
    public MutableResponse response() {
        return response;
    }

    @Override
    public <T> T getComponent(Class<T> componentClass) {
        return componentProvider.getComponent(componentClass);
    }

    @Override
    public void setAttribute(String name, Object value) {
        putAttribute(name, value);
    }

    @Override
    public void putAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public <T> T getAttribute(String name) {
        return (T) attributes.get(name);
    }

    @Override
    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    @Override
    public <T> Map<String, T> getAttributes() {
        return (Map<String, T>) this.attributes;
    }

    @Override
    public void setInternalAttribute(String name, Object value) {
        putInternalAttribute(name, value);
    }

    @Override
    public void putInternalAttribute(String name, Object value) {
        internalAttributes.put(name, value);
    }

    @Override
    public void removeInternalAttribute(String name) {
        internalAttributes.remove(name);
    }

    @Override
    public <T> T getInternalAttribute(String name) {
        return (T) internalAttributes.get(name);
    }

    @Override
    public <T> Map<String, T> getInternalAttributes() {
        return (Map<String, T>) internalAttributes;
    }

    @Override
    public TemplateEngine getTemplateEngine() {
        if (templateEngine == null) {
            templateEngine = TemplateEngine.templateEngine();

            final TemplateContext templateContext = templateEngine.getTemplateContext();
            final EvaluableRequest evaluableRequest = new EvaluableRequest(request());
            final EvaluableResponse evaluableResponse = new EvaluableResponse(response());

            templateContext.setVariable(TEMPLATE_ATTRIBUTE_REQUEST, evaluableRequest);
            templateContext.setVariable(TEMPLATE_ATTRIBUTE_RESPONSE, evaluableResponse);
            templateContext.setVariable(TEMPLATE_ATTRIBUTE_CONTEXT, new EvaluableExecutionContext(this));

            if (templateVariableProviders != null) {
                templateVariableProviders.forEach(templateVariableProvider -> templateVariableProvider.provide(this));
            }
        }

        return templateEngine;
    }

    public MutableRequestExecutionContext componentProvider(final ComponentProvider componentProvider) {
        this.componentProvider = componentProvider;
        return this;
    }

    public MutableRequestExecutionContext templateVariableProviders(final Collection<TemplateVariableProvider> templateVariableProviders) {
        this.templateVariableProviders = templateVariableProviders;
        return this;
    }
}
