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
package io.gravitee.gateway.handlers.guardian.context;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ADAPTED_CONTEXT;

import io.gravitee.gateway.reactive.api.ComponentType;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.gravitee.gateway.reactive.core.context.ComponentScope;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.resource.api.ResourceManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.experimental.Delegate;

/**
 * The execution context a Guardian runs its policies with: the execution context of the calling API, except that the
 * resources resolved through {@link #getComponent(Class)} are the Guardian ones. Everything else (request, response,
 * attributes, interruptions, tracing…) is the API context's, so that a Guardian policy acts on the very same exchange.
 * <p>
 * Policies keep the context they are given for what they do later on ({@code onBody}, {@code onChunks}, registered
 * actions), so the Guardian resources stay the ones resolved, whenever it runs.
 *
 * @author GraviteeSource Team
 */
@SuppressWarnings("removal")
public class GuardianExecutionContext implements MutableExecutionContext {

    @Delegate(types = MutableExecutionContext.class, excludes = Overridden.class)
    private final MutableExecutionContext delegate;

    private final ResourceManager resourceManager;
    private final BasePolicy source;
    private List<ResponseAction> responseActions;
    private Object adaptedContext;

    /**
     * @param delegate the execution context of the calling API.
     * @param resourceManager the resources of the Guardian.
     * @param source the policy of the API flow executing the Guardian, under which the response actions of the Guardian
     * policies are registered on the API context.
     */
    public GuardianExecutionContext(MutableExecutionContext delegate, ResourceManager resourceManager, BasePolicy source) {
        this.delegate = delegate;
        this.resourceManager = resourceManager;
        this.source = source;
    }

    @Override
    public <T> T getComponent(Class<T> componentClass) {
        if (ResourceManager.class.equals(componentClass)) {
            return componentClass.cast(resourceManager);
        }
        return delegate.getComponent(componentClass);
    }

    /**
     * The API flow chain only executes the response actions of its own policies, which the Guardian policies are not part
     * of. They are collected here and executed by a single action registered under the policy executing the Guardian, in
     * reverse order of registration as the API flow does for its policies.
     */
    @Override
    public void addActionOnResponse(BasePolicy policy, Function<HttpExecutionContext, Completable> action) {
        if (responseActions == null) {
            responseActions = new ArrayList<>();
            delegate.addActionOnResponse(source, ctx -> executeResponseActions());
        }
        responseActions.add(new ResponseAction(policy, action));
    }

    @Override
    public void addActionOnEndpointRequest(BasePolicy policy, Function<HttpExecutionContext, Completable> action) {
        delegate.addActionOnEndpointRequest(policy, ctx -> action.apply(this));
    }

    @Override
    public void addActionOnEntrypointResponse(BasePolicy policy, Function<HttpExecutionContext, Completable> action) {
        delegate.addActionOnEntrypointResponse(policy, ctx -> action.apply(this));
    }

    @Override
    public void addActionOnTerminate(BasePolicy policy, Function<HttpExecutionContext, Completable> action) {
        delegate.addActionOnTerminate(policy, ctx -> action.apply(this));
    }

    /**
     * The v3 adapter of the context is cached as an internal attribute. Shared with the API context, a legacy Guardian
     * policy would get the adapter of the API context, and the API resources with it, and the API legacy policies would
     * get the Guardian one afterwards: this context keeps its own.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInternalAttribute(String name) {
        if (ATTR_INTERNAL_ADAPTED_CONTEXT.equals(name)) {
            return (T) adaptedContext;
        }
        return delegate.getInternalAttribute(name);
    }

    @Override
    public void setInternalAttribute(String name, Object value) {
        if (ATTR_INTERNAL_ADAPTED_CONTEXT.equals(name)) {
            adaptedContext = value;
            return;
        }
        delegate.setInternalAttribute(name, value);
    }

    @Override
    public void putInternalAttribute(String name, Object value) {
        if (ATTR_INTERNAL_ADAPTED_CONTEXT.equals(name)) {
            adaptedContext = value;
            return;
        }
        delegate.putInternalAttribute(name, value);
    }

    @Override
    public void removeInternalAttribute(String name) {
        if (ATTR_INTERNAL_ADAPTED_CONTEXT.equals(name)) {
            adaptedContext = null;
            return;
        }
        delegate.removeInternalAttribute(name);
    }

    private Completable executeResponseActions() {
        return Flowable.fromIterable(responseActions.reversed()).concatMapCompletable(responseAction -> {
            String policyId = responseAction.policy().id();
            ComponentScope.push(this, ComponentType.POLICY, policyId);
            return Completable.defer(() -> responseAction.action().apply(this)).doFinally(() ->
                ComponentScope.remove(this, ComponentType.POLICY, policyId)
            );
        });
    }

    private record ResponseAction(BasePolicy policy, Function<HttpExecutionContext, Completable> action) {}

    /**
     * The methods this context does not delegate.
     */
    private interface Overridden {
        <T> T getComponent(Class<T> componentClass);

        void addActionOnResponse(BasePolicy source, Function<HttpExecutionContext, Completable> onResponseCallback);

        void addActionOnEndpointRequest(BasePolicy source, Function<HttpExecutionContext, Completable> onEndpointRequestCallback);

        void addActionOnEntrypointResponse(BasePolicy source, Function<HttpExecutionContext, Completable> onEntrypointResponseCallback);

        void addActionOnTerminate(BasePolicy source, Function<HttpExecutionContext, Completable> onTerminateCallback);

        <T> T getInternalAttribute(String name);

        void setInternalAttribute(String name, Object value);

        void putInternalAttribute(String name, Object value);

        void removeInternalAttribute(String name);
    }
}
