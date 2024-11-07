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
package io.gravitee.gateway.reactive.core.context;

import io.gravitee.common.util.ListUtils;
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.TlsSession;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.el.EvaluableMessage;
import io.gravitee.gateway.reactive.api.el.EvaluableRequest;
import io.gravitee.gateway.reactive.api.el.EvaluableResponse;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionException;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractExecutionContext<RQ extends MutableRequest, RS extends MutableResponse>
    extends AbstractBaseExecutionContext
    implements ExecutionContext, io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext {

    protected RQ request;
    protected RS response;
    protected Map<String, Object> attributes = new ContextAttributeMap();
    protected Map<String, Object> internalAttributes = new HashMap<>();
    protected Metrics metrics;
    protected ComponentProvider componentProvider;
    protected TemplateEngine templateEngine;
    protected Collection<TemplateVariableProvider> templateVariableProviders;

    private EvaluableRequest evaluableRequest;
    private EvaluableResponse evaluableResponse;
    private EvaluableExecutionContext evaluableExecutionContext;

    public AbstractExecutionContext(final RQ request, final RS response) {
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
    public Metrics metrics() {
        return metrics;
    }

    @Override
    public Completable interrupt() {
        return messagesInterruption();
    }

    @Override
    public Completable interruptWith(ExecutionFailure executionFailure) {
        return Completable.defer(() -> {
            internalAttributes.put(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
            return Completable.error(new InterruptionFailureException(executionFailure));
        });
    }

    @Override
    public Maybe<Buffer> interruptBody() {
        return interrupt().toMaybe();
    }

    @Override
    public Maybe<Buffer> interruptBodyWith(final ExecutionFailure failure) {
        return interruptWith(failure).toMaybe();
    }

    @Override
    public Flowable<Message> interruptMessages() {
        return messagesInterruption().toFlowable();
    }

    @Override
    public Maybe<Message> interruptMessage() {
        return messagesInterruption().toMaybe();
    }

    private Completable messagesInterruption() {
        return Completable.error(new InterruptionException());
    }

    @Override
    public Flowable<Message> interruptMessagesWith(final ExecutionFailure executionFailure) {
        return messageInterruptionFailure(executionFailure).toFlowable();
    }

    @Override
    public Maybe<Message> interruptMessageWith(final ExecutionFailure executionFailure) {
        return messageInterruptionFailure(executionFailure).toMaybe();
    }

    private Completable messageInterruptionFailure(final ExecutionFailure executionFailure) {
        return Completable.defer(() -> {
            internalAttributes.put(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
            return Completable.error(new InterruptionFailureException(executionFailure));
        });
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
    @SuppressWarnings("unchecked")
    public <T> List<T> getAttributeAsList(String name) {
        return ListUtils.toList(attributes.get(name));
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
    public String remoteAddress() {
        return this.request.remoteAddress();
    }

    @Override
    public String localAddress() {
        return this.request.localAddress();
    }

    @Override
    public TlsSession tlsSession() {
        return this.request.tlsSession();
    }

    @Override
    public TemplateEngine getTemplateEngine() {
        if (templateEngine == null) {
            templateEngine = TemplateEngine.templateEngine();
            prepareTemplateEngine(templateEngine);
            if (templateVariableProviders != null) {
                templateVariableProviders.forEach(templateVariableProvider -> templateVariableProvider.provide(this));
            }
        }

        return templateEngine;
    }

    @Override
    public TemplateEngine getTemplateEngine(Message message) {
        final TemplateEngine engine = TemplateEngine.templateEngine();
        prepareTemplateEngine(engine);
        if (templateVariableProviders != null) {
            templateVariableProviders.forEach(templateVariableProvider -> templateVariableProvider.provide(engine.getTemplateContext()));
        }
        engine.getTemplateContext().setVariable(TEMPLATE_ATTRIBUTE_MESSAGE, new EvaluableMessage(message));
        return engine;
    }

    private void prepareTemplateEngine(final TemplateEngine templateEngine) {
        final TemplateContext templateContext = templateEngine.getTemplateContext();
        final EvaluableRequest evaluableReq = getEvaluableRequest();
        final EvaluableResponse evaluableResp = getEvaluableResponse();
        final EvaluableExecutionContext evaluableCtx = getEvaluableExecutionContext();

        templateContext.setVariable(HttpPlainExecutionContext.TEMPLATE_ATTRIBUTE_REQUEST, evaluableReq);
        templateContext.setVariable(HttpPlainExecutionContext.TEMPLATE_ATTRIBUTE_RESPONSE, evaluableResp);
        templateContext.setVariable(HttpPlainExecutionContext.TEMPLATE_ATTRIBUTE_CONTEXT, evaluableCtx);
    }

    private EvaluableRequest getEvaluableRequest() {
        if (evaluableRequest == null) {
            this.evaluableRequest = new EvaluableRequest(request());
        }
        return evaluableRequest;
    }

    private EvaluableResponse getEvaluableResponse() {
        if (evaluableResponse == null) {
            this.evaluableResponse = new EvaluableResponse(response());
        }
        return evaluableResponse;
    }

    private EvaluableExecutionContext getEvaluableExecutionContext() {
        if (evaluableExecutionContext == null) {
            this.evaluableExecutionContext = new EvaluableExecutionContext(this);
        }
        return evaluableExecutionContext;
    }
}
