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
package io.gravitee.gateway.reactive.handlers.api.security.plan;

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.api.policy.http.HttpSecurityPolicy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import jakarta.annotation.Nonnull;

/**
 * {@link HttpSecurityPlan} allows to wrap a {@link io.gravitee.gateway.reactive.api.policy.http.HttpPolicy} implementing {@link HttpSecurityPolicy} and make it working in a security chain.
 * Security plan is responsible to
 * <ul>
 *     <li>Check if a policy can handle the security or not</li>
 *     <li>Check the eventual selection rule matches (useful when dealing with multiple plans relying on the same security scheme such as <code>Authorization: bearer xxx</code> for JWT and OAuth)</li>
 *     <li>Check if the policy requires to validate there is an associated subscription or not and validate the subscription accordingly</li>
 *     <li>Invoke the <code>onSubscriptionInvalid</code> method when necessary</li>
 * </ul>
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpSecurityPlan extends AbstractSecurityPlan<HttpSecurityPolicy, HttpPlainExecutionContext> {

    public HttpSecurityPlan(@Nonnull final String planId, @Nonnull final HttpSecurityPolicy policy, final String selectionRule) {
        super(planId, policy, selectionRule);
    }

    @Override
    protected Maybe<SecurityToken> extractSecurityToken(HttpPlainExecutionContext executionContext) {
        return policy.extractSecurityToken(executionContext);
    }

    @Override
    protected Completable executeSecurityPolicy(final HttpPlainExecutionContext ctx, final ExecutionPhase executionPhase) {
        if (ExecutionPhase.REQUEST == executionPhase) {
            return policy.onRequest(ctx);
        }
        throw new IllegalArgumentException("Execution phase unsupported for security plan execution");
    }

    @Override
    protected String getSelectionRule(String selectionRule) {
        if (selectionRule == null) {
            return null;
        }

        if (selectionRule.startsWith("#")) {
            // Backward compatibility. In V3 mode selection rule EL expression based can be defined with "#something" while it is usually defined with "{#something}" everywhere else.
            return "{" + selectionRule + "}";
        }
        return selectionRule;
    }
}
