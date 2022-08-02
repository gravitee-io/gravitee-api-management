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

import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableHttpExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableHttpRequest;
import io.gravitee.gateway.jupiter.core.context.MutableHttpResponse;
import io.gravitee.gateway.jupiter.core.context.MutableMessageRequest;
import io.gravitee.gateway.jupiter.core.context.MutableMessageResponse;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionException;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionFailureException;
import io.reactivex.Completable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

abstract class AbstractExecutionContext<RQ extends MutableHttpRequest, RS extends MutableHttpResponse>
    implements MutableHttpExecutionContext {

    private final RQ request;
    private final RS response;
    private final Map<String, Object> attributes = new ContextAttributeMap();
    private final Map<String, Object> internalAttributes = new HashMap<>();
    protected ComponentProvider componentProvider;

    AbstractExecutionContext(final RQ request, final RS response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public RQ request() {
        return request;
    }

    @Override
    public RS response() {
        return response;
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

    public MutableHttpExecutionContext componentProvider(final ComponentProvider componentProvider) {
        this.componentProvider = componentProvider;
        return this;
    }
}
