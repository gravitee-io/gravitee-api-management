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
package io.gravitee.gateway.handlers.sharedpolicygroup.policy;

import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.SharedPolicyGroupReactor;
import io.gravitee.gateway.handlers.sharedpolicygroup.registry.SharedPolicyGroupRegistry;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.hook.HttpHook;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.CustomLog;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class SharedPolicyGroupPolicy implements HttpPolicy {

    public static final String POLICY_ID = "shared-policy-group-policy";

    private final String id;
    public final io.gravitee.definition.model.sharedpolicygroup.SharedPolicyGroupPolicyConfiguration policyConfiguration;

    public SharedPolicyGroupPolicy() {
        this(POLICY_ID, null);
    }

    public SharedPolicyGroupPolicy(
        io.gravitee.definition.model.sharedpolicygroup.SharedPolicyGroupPolicyConfiguration policyConfiguration
    ) {
        this(POLICY_ID, policyConfiguration);
    }

    public SharedPolicyGroupPolicy(
        String id,
        io.gravitee.definition.model.sharedpolicygroup.SharedPolicyGroupPolicyConfiguration policyConfiguration
    ) {
        this.id = id;
        this.policyConfiguration = policyConfiguration;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Completable onRequest(HttpPlainExecutionContext ctx) {
        return getPolicyChain(ctx)
            .map(policyChain -> policyChain.execute(ctx))
            .orElseGet(warnNotFoundAndComplete(ctx));
    }

    @Override
    public Completable onResponse(HttpPlainExecutionContext ctx) {
        return getPolicyChain(ctx)
            .map(policyChain -> policyChain.execute(ctx))
            .orElseGet(warnNotFoundAndComplete(ctx));
    }

    /**
     * To be removed when Message Reactor implementation will use {@link this#warnNotFoundAndComplete(HttpBaseExecutionContext)}
     * @param environmentId
     * @return
     */
    @Deprecated(forRemoval = true)
    protected Supplier<Completable> warnNotFoundAndComplete(String environmentId) {
        return () -> {
            log.warn(
                "No Shared Policy Group found for id {} on environment {}",
                policyConfiguration.getSharedPolicyGroupId(),
                environmentId
            );
            return Completable.complete();
        };
    }

    protected Supplier<Completable> warnNotFoundAndComplete(HttpBaseExecutionContext ctx) {
        return () -> {
            ctx
                .withLogger(log)
                .warn(
                    "No Shared Policy Group found for id {} on environment {}",
                    policyConfiguration.getSharedPolicyGroupId(),
                    ctx.getAttribute(ContextAttributes.ATTR_ENVIRONMENT)
                );
            return Completable.complete();
        };
    }

    // Internal attribute key set by the debug infrastructure to inject DebugPolicyHook into SPG inner chains.
    // Kept as a package-visible constant so DebugInitProcessor can reference it without introducing
    // a compile-time dependency on the debug module from this module.
    static final String ATTR_INTERNAL_SPG_DEBUG_HOOKS = "gravitee.internal.spg.debugHooks";

    private Optional<HttpPolicyChain> getPolicyChain(BaseExecutionContext ctx) {
        final SharedPolicyGroupRegistry sharedPolicyGroupRegistry = ctx.getComponent(SharedPolicyGroupRegistry.class);
        final List<HttpHook> debugHooks = ctx.getInternalAttribute(ATTR_INTERNAL_SPG_DEBUG_HOOKS);
        return Optional.ofNullable(
            sharedPolicyGroupRegistry.get(
                policyConfiguration.getSharedPolicyGroupId(),
                ctx.getAttribute(ContextAttributes.ATTR_ENVIRONMENT)
            )
        ).map(reactor -> debugHooks != null && !debugHooks.isEmpty() ? reactor.policyChain(debugHooks) : reactor.policyChain());
    }
}
