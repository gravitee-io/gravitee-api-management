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
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.reactivex.rxjava3.core.Completable;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class SharedPolicyGroupPolicy implements Policy {

    public static final String POLICY_ID = "shared-policy-group-policy";

    private final String id;
    public final SharedPolicyGroupPolicyConfiguration policyConfiguration;

    public SharedPolicyGroupPolicy(String id, SharedPolicyGroupPolicyConfiguration policyConfiguration) {
        this.id = id;
        this.policyConfiguration = policyConfiguration;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        return getPolicyChain(ctx)
            .map(policyChain -> policyChain.execute((ExecutionContext) ctx))
            .orElseGet(warnNotFoundAndComplete(ctx.getAttribute(ContextAttributes.ATTR_ENVIRONMENT)));
    }

    @Override
    public Completable onResponse(HttpExecutionContext ctx) {
        return getPolicyChain(ctx)
            .map(policyChain -> policyChain.execute((ExecutionContext) ctx))
            .orElseGet(warnNotFoundAndComplete(ctx.getAttribute(ContextAttributes.ATTR_ENVIRONMENT)));
    }

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

    private Optional<HttpPolicyChain> getPolicyChain(HttpExecutionContext ctx) {
        final SharedPolicyGroupRegistry sharedPolicyGroupRegistry = ctx.getComponent(SharedPolicyGroupRegistry.class);
        return Optional
            .ofNullable(
                sharedPolicyGroupRegistry.get(
                    policyConfiguration.getSharedPolicyGroupId(),
                    ctx.getAttribute(ContextAttributes.ATTR_ENVIRONMENT)
                )
            )
            .map(SharedPolicyGroupReactor::policyChain);
    }
}
