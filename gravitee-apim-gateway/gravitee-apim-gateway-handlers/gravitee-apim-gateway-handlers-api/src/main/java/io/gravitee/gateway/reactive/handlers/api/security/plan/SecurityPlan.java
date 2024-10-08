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

import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_API;
import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_APPLICATION;
import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_PLAN;
import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_SUBSCRIPTION_ID;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_TOKEN;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_VALIDATE_SUBSCRIPTION;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.api.policy.http.HttpSecurityPolicy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import jakarta.annotation.Nonnull;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SecurityPlan} allows to wrap a {@link io.gravitee.gateway.reactive.api.policy.http.HttpPolicy} implementing {@link HttpSecurityPolicy} and make it working in a security chain.
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
public class SecurityPlan {

    protected static final Maybe<Boolean> TRUE = Maybe.just(true);
    protected static final Maybe<Boolean> FALSE = Maybe.just(false);
    private static final Logger log = LoggerFactory.getLogger(SecurityPlan.class);
    private final String planId;
    private final HttpSecurityPolicy policy;
    private final String selectionRule;

    public SecurityPlan(@Nonnull final String planId, @Nonnull final HttpSecurityPolicy policy, final String selectionRule) {
        this.planId = planId;
        this.policy = policy;
        this.selectionRule = getSelectionRule(selectionRule);
    }

    public String id() {
        return policy.id();
    }

    /**
     * Make sure the security plan can be executed for the current request.
     *
     * @param ctx the current execution context.
     * @return <code>true</code> if this security plan can be executed for the request, <code>false</code> otherwise.
     */
    public Single<Boolean> canExecute(HttpPlainExecutionContext ctx) {
        return policy
            .extractSecurityToken(ctx)
            .flatMap(securityToken -> {
                ctx.setInternalAttribute(ATTR_INTERNAL_SECURITY_TOKEN, securityToken);
                if (!securityToken.isInvalid()) {
                    return isApplicableWithValidSubscription(ctx, securityToken);
                }
                return Maybe.empty();
            })
            .defaultIfEmpty(false);
    }

    /**
     * Invokes the policy's <code>onRequest</code> method.
     *
     * @param ctx the current execution context.
     * @return a {@link Completable} that completes when the security policy has been successfully executed or returns an error otherwise.
     */
    public Completable execute(final HttpPlainExecutionContext ctx, final ExecutionPhase executionPhase) {
        return executeSecurityPolicy(ctx, executionPhase).doOnSubscribe(disposable -> ctx.setAttribute(ATTR_PLAN, planId));
    }

    private Completable executeSecurityPolicy(final HttpPlainExecutionContext ctx, final ExecutionPhase executionPhase) {
        switch (executionPhase) {
            case REQUEST:
                return policy.onRequest(ctx);
            case MESSAGE_REQUEST:
            case RESPONSE:
            case MESSAGE_RESPONSE:
            default:
                throw new IllegalArgumentException("Execution phase unsupported for security plan execution");
        }
    }

    public int order() {
        return policy.order();
    }

    private String getSelectionRule(String selectionRule) {
        if (selectionRule == null) {
            return null;
        }

        if (selectionRule.startsWith("#")) {
            // Backward compatibility. In V3 mode selection rule EL expression based can be defined with "#something" while it is usually defined with "{#something}" everywhere else.
            return "{" + selectionRule + "}";
        }
        return selectionRule;
    }

    private Maybe<Boolean> isApplicableWithValidSubscription(HttpPlainExecutionContext ctx, SecurityToken securityToken) {
        if (selectionRule == null || selectionRule.isEmpty()) {
            return Maybe.just(validateSubscription(ctx, securityToken));
        }

        return ctx
            .getTemplateEngine()
            .eval(selectionRule, Boolean.class)
            .map(matches -> {
                if (Boolean.TRUE.equals(matches)) {
                    return validateSubscription(ctx, securityToken);
                }
                // Remove any security  token as the selection rule don't match
                ctx.removeInternalAttribute(ATTR_INTERNAL_SECURITY_TOKEN);
                return false;
            });
    }

    private boolean validateSubscription(HttpPlainExecutionContext ctx, SecurityToken securityToken) {
        Boolean validateSubscriptionEnabled = ctx.getInternalAttribute(ATTR_INTERNAL_VALIDATE_SUBSCRIPTION);

        // Skip validating the subscription
        if (validateSubscriptionEnabled != null && !validateSubscriptionEnabled) {
            return true;
        }

        if (!policy.requireSubscription()) {
            return true;
        }

        try {
            SubscriptionService subscriptionService = ctx.getComponent(SubscriptionService.class);
            String api = ctx.getAttribute(ATTR_API);

            Optional<Subscription> subscriptionOpt = subscriptionService.getByApiAndSecurityToken(api, securityToken, planId);

            if (subscriptionOpt.isPresent()) {
                Subscription subscription = subscriptionOpt.get();

                if (planId.equals(subscription.getPlan()) && subscription.isTimeValid(ctx.request().timestamp())) {
                    ctx.setAttribute(ATTR_APPLICATION, subscription.getApplication());
                    ctx.setAttribute(ATTR_SUBSCRIPTION_ID, subscription.getId());
                    ctx.setInternalAttribute(ATTR_INTERNAL_SUBSCRIPTION, subscription);
                    return true;
                }
            }
            return false;
        } catch (Exception t) {
            log.warn("An error occurred during subscription validation", t);
            return false;
        }
    }
}
